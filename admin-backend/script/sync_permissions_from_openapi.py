#!/usr/bin/env python
"""
从OpenAPI规范同步权限到PostgreSQL数据库

使用方法:
    python script/sync_permissions_from_openapi.py [选项]

选项:
    --openapi-url  OpenAPI JSON的URL (默认: http://localhost:8080/api-docs)
    --db-host      数据库主机 (默认: 环境变量 DATABASE_HOST 或 localhost)
    --db-port      数据库端口 (默认: 环境变量 DATABASE_PORT 或 5432)
    --db-name      数据库名称 (默认: 环境变量 DATABASE_NAME 或 admin)
    --db-user      数据库用户 (默认: 环境变量 DATABASE_USERNAME 或 postgres)
    --db-pass      数据库密码 (默认: 环境变量 DATABASE_PASSWORD 或 postgres)
    --dry-run      预览模式，不实际修改数据库
    --verbose      详细输出

同步逻辑:
1. 从OpenAPI提取所有API路径
2. 对比数据库中的权限:
   - 数据库中不存在的 -> 新增
   - 数据库中存在的 -> 更新 group_key/group_name（如有变化）
   - 数据库中存在但OpenAPI中没有的 -> 软删除
3. 组权限处理:
   - 取所有API路径的前三段（如 /api/admin/admin-users）
   - 去重后生成组权限（path=前缀/**, method=*）
   - 同样执行新增、更新、删除逻辑
"""
import sys
import os
import re
import argparse
import json
import urllib.request
from typing import List, Set, Dict, Tuple
from dataclasses import dataclass
from datetime import datetime, timezone

try:
    import psycopg2
    import psycopg2.extras
except ImportError:
    print("错误: 需要安装 psycopg2，请运行: pip install psycopg2-binary")
    sys.exit(1)

try:
    import yaml
except ImportError:
    yaml = None


@dataclass
class RouteInfo:
    """路由信息"""
    path: str
    method: str
    tags: List[str]
    summary: str
    operation_id: str


@dataclass
class SyncResult:
    """同步结果"""
    permissions_added: int = 0
    permissions_unchanged: int = 0
    permissions_updated: int = 0
    permissions_deleted: int = 0
    groups_added: int = 0
    groups_unchanged: int = 0
    groups_updated: int = 0
    groups_deleted: int = 0
    errors: List[str] = None

    def __post_init__(self):
        if self.errors is None:
            self.errors = []


# ==================== 工具函数 ====================

def fetch_openapi_spec(url: str) -> dict:
    """从URL获取OpenAPI规范"""
    with urllib.request.urlopen(url, timeout=10) as response:
        return json.loads(response.read().decode('utf-8'))


def extract_routes_from_openapi(spec: dict) -> List[RouteInfo]:
    """从OpenAPI规范提取所有路由信息"""
    routes = []
    paths = spec.get('paths', {})

    for path, path_item in paths.items():
        for method, operation in path_item.items():
            if method.upper() not in ('GET', 'POST', 'PUT', 'DELETE', 'PATCH'):
                continue

            routes.append(RouteInfo(
                path=path,
                method=method.upper(),
                tags=operation.get('tags', []),
                summary=operation.get('summary', ''),
                operation_id=operation.get('operationId', '')
            ))

    return routes


def convert_path_params_to_wildcard(path: str) -> str:
    """
    将路径参数转换为通配符
    /api/admin/roles/{id} -> /api/admin/roles/*
    """
    path = re.sub(r'/\{[^}]+:\w+\}', '/*', path)
    path = re.sub(r'/\{[a-zA-Z_]+\}', '/*', path)
    return path


def extract_group_prefix(path: str) -> str:
    """
    提取路径的前三段作为组权限前缀
    /api/admin/admin-users/{id}/roles -> /api/admin/admin-users
    """
    clean_path = re.sub(r'/\{[^}]+:\w+\}', '', path)
    clean_path = re.sub(r'/\{[a-zA-Z_]+\}', '', clean_path)

    parts = clean_path.strip("/").split("/")
    if len(parts) >= 3:
        return "/" + "/".join(parts[:3])
    elif len(parts) >= 1:
        return "/" + "/".join(parts)
    return path


def generate_group_key(prefix: str) -> str:
    """
    从前缀生成 group_key（下划线分隔）
    /api/admin/admin-users -> admin_admin-users
    """
    parts = prefix.strip("/").split("/")
    if parts and parts[0] == "api":
        parts = parts[1:]
    return "_".join(parts)


def generate_group_name(group_key: str) -> str:
    """从 group_key 生成可读的组名称"""
    parts = group_key.replace("_", " ").replace("-", " ").split()
    return " ".join(part.capitalize() for part in parts)


def generate_code_from_path(path: str, method: str) -> str:
    """从路径和方法生成权限编码"""
    clean_path = re.sub(r'[^a-zA-Z0-9]', '_', path.strip('/'))
    clean_path = re.sub(r'_+', '_', clean_path)
    clean_path = clean_path.strip('_')
    return f"{clean_path}_{method}".upper()


# ==================== 数据库操作 ====================

def get_db_connection(args) -> 'psycopg2.connection':
    """创建数据库连接"""
    return psycopg2.connect(
        host=args.db_host,
        port=args.db_port,
        dbname=args.db_name,
        user=args.db_user,
        password=args.db_pass,
    )


def fetch_db_permissions(cursor, is_group: bool) -> Dict[Tuple[str, str], dict]:
    """获取数据库中的权限，返回 {(path, method): row_dict}"""
    cursor.execute(
        "SELECT id, name, code, path, method, group_key, group_name, is_group, status "
        "FROM admin_permission WHERE is_group = %s AND is_deleted = 0",
        (1 if is_group else 0,)
    )
    columns = [desc[0] for desc in cursor.description]
    rows = cursor.fetchall()

    result = {}
    for row in rows:
        row_dict = dict(zip(columns, row))
        key = (row_dict['path'], row_dict['method'])
        result[key] = row_dict
    return result


def insert_permission(cursor, *, name, code, path, method, group_key, group_name, is_group, description, status=1):
    """插入新权限"""
    now = datetime.now(timezone.utc)
    cursor.execute(
        "INSERT INTO admin_permission "
        "(name, code, path, method, group_key, group_name, is_group, description, status, is_deleted, create_time, update_time) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 0, %s, %s)",
        (name, code, path, method, group_key, group_name, 1 if is_group else 0, description, status, now, now)
    )


def update_permission_group(cursor, perm_id: int, group_key: str, group_name: str):
    """更新权限的 group_key 和 group_name"""
    now = datetime.now(timezone.utc)
    cursor.execute(
        "UPDATE admin_permission SET group_key = %s, group_name = %s, update_time = %s WHERE id = %s",
        (group_key, group_name, now, perm_id)
    )


def soft_delete_permission(cursor, perm_id: int):
    """软删除权限"""
    now = datetime.now(timezone.utc)
    cursor.execute(
        "UPDATE admin_permission SET is_deleted = 1, update_time = %s WHERE id = %s",
        (now, perm_id)
    )


# ==================== 同步逻辑 ====================

def sync_permissions(cursor, routes: List[RouteInfo], dry_run: bool, verbose: bool) -> SyncResult:
    """同步权限到数据库"""
    result = SyncResult()

    # ========== 1. 处理子权限（非组权限）==========

    openapi_permissions: Dict[Tuple[str, str], RouteInfo] = {}
    group_prefixes: Set[str] = set()

    for route in routes:
        resource_pattern = convert_path_params_to_wildcard(route.path)
        key = (resource_pattern, route.method)
        openapi_permissions[key] = route
        group_prefixes.add(extract_group_prefix(route.path))

    if verbose:
        print(f"\n从OpenAPI提取到 {len(openapi_permissions)} 个权限")
        print(f"提取到 {len(group_prefixes)} 个组前缀:")
        for prefix in sorted(group_prefixes):
            print(f"  - {prefix} -> {generate_group_key(prefix)}")

    db_permission_map = fetch_db_permissions(cursor, is_group=False)

    if verbose:
        print(f"\n数据库中有 {len(db_permission_map)} 个非组权限")

    # 新增 + 更新
    for key, route in openapi_permissions.items():
        resource_pattern, method = key
        prefix = extract_group_prefix(route.path)
        group_key = generate_group_key(prefix)
        group_name = generate_group_name(group_key)

        if key not in db_permission_map:
            code = generate_code_from_path(route.path, route.method)
            if not dry_run:
                insert_permission(
                    cursor,
                    name=route.summary or route.operation_id or route.path,
                    code=code,
                    path=resource_pattern,
                    method=method,
                    group_key=group_key,
                    group_name=group_name,
                    is_group=False,
                    description=f"从OpenAPI同步: {route.operation_id or route.summary}",
                )
            result.permissions_added += 1
            if verbose:
                print(f"  [新增] {method} {resource_pattern}")
        else:
            existing = db_permission_map[key]
            if existing['group_key'] != group_key or existing['group_name'] != group_name:
                if not dry_run:
                    update_permission_group(cursor, existing['id'], group_key, group_name)
                result.permissions_updated += 1
                if verbose:
                    print(f"  [更新] {method} {resource_pattern} -> group: {group_key}")
            else:
                result.permissions_unchanged += 1

    # 删除: 数据库有，OpenAPI没有
    for key, perm in db_permission_map.items():
        if key not in openapi_permissions:
            if not dry_run:
                soft_delete_permission(cursor, perm['id'])
            result.permissions_deleted += 1
            if verbose:
                print(f"  [删除] {perm['method']} {perm['path']}")

    # ========== 2. 处理组权限 ==========

    openapi_groups: Dict[Tuple[str, str], str] = {}  # {(path, method): group_key}
    for prefix in group_prefixes:
        group_path = prefix + "/**"
        group_key = generate_group_key(prefix)
        openapi_groups[(group_path, "*")] = group_key

    if verbose:
        print(f"\n需要的组权限 ({len(openapi_groups)} 个):")
        for (gp, _), gk in sorted(openapi_groups.items()):
            print(f"  - {gp} (group_key: {gk})")

    db_group_map = fetch_db_permissions(cursor, is_group=True)

    if verbose:
        print(f"\n数据库中有 {len(db_group_map)} 个组权限")

    # 新增 + 更新
    for (group_path, group_method), group_key in openapi_groups.items():
        key = (group_path, group_method)
        group_name = generate_group_name(group_key)

        if key not in db_group_map:
            if not dry_run:
                insert_permission(
                    cursor,
                    name=f"{group_name} 全部权限",
                    code=generate_code_from_path(group_path, "STAR"),
                    path=group_path,
                    method="*",
                    group_key=group_key,
                    group_name=group_name,
                    is_group=True,
                    description=f"自动生成的组权限，匹配 {group_path}",
                )
            result.groups_added += 1
            if verbose:
                print(f"  [新增组] {group_path} (group_key: {group_key})")
        else:
            existing = db_group_map[key]
            if existing['group_key'] != group_key or existing['group_name'] != group_name:
                if not dry_run:
                    update_permission_group(cursor, existing['id'], group_key, group_name)
                result.groups_updated += 1
                if verbose:
                    print(f"  [更新组] {group_path} -> group_key: {group_key}")
            else:
                result.groups_unchanged += 1

    # 删除: 数据库有，OpenAPI没有
    for key, perm in db_group_map.items():
        if key not in openapi_groups:
            if not dry_run:
                soft_delete_permission(cursor, perm['id'])
            result.groups_deleted += 1
            if verbose:
                print(f"  [删除组] {perm['path']}")

    return result


# ==================== Spring Boot 配置读取 ====================

def load_spring_db_config(profile: str = "dev") -> dict:
    """
    从 Spring Boot 的 application-{profile}.yml 读取数据库配置
    返回 {host, port, name, username, password} 或空 dict
    """
    if yaml is None:
        print("⚠ 未安装 PyYAML，无法自动读取 Spring 配置（pip install pyyaml）")
        return {}

    script_dir = os.path.dirname(os.path.abspath(__file__))
    resources_dir = os.path.join(script_dir, "..", "src", "main", "resources")

    profile_file = os.path.join(resources_dir, f"application-{profile}.yml")
    if not os.path.exists(profile_file):
        print(f"⚠ 配置文件不存在: {profile_file}")
        return {}

    with open(profile_file, 'r', encoding='utf-8') as f:
        config = yaml.safe_load(f) or {}

    db = config.get("database", {})
    result = {}
    if db.get("host"):
        result["host"] = str(db["host"])
    if db.get("port"):
        result["port"] = str(db["port"])
    if db.get("name"):
        result["name"] = str(db["name"])
    if db.get("username"):
        result["username"] = str(db["username"])
    if db.get("password"):
        result["password"] = str(db["password"])

    if result:
        print(f"✓ 从 application-{profile}.yml 读取数据库配置: {result['host']}:{result.get('port', '5432')}/{result.get('name', '?')}")

    return result


# ==================== 主函数 ====================

def main():
    parser = argparse.ArgumentParser(description="从OpenAPI同步权限到数据库")
    parser.add_argument("--openapi-url", default="http://localhost:8080/api-docs",
                        help="OpenAPI JSON的URL地址 (默认: http://localhost:8080/api-docs)")
    parser.add_argument("--profile", default="dev",
                        help="Spring Boot profile 名称，用于读取 application-{profile}.yml (默认: dev)")
    parser.add_argument("--db-host", default=None, help="数据库主机（覆盖配置文件）")
    parser.add_argument("--db-port", default=None, help="数据库端口（覆盖配置文件）")
    parser.add_argument("--db-name", default=None, help="数据库名称（覆盖配置文件）")
    parser.add_argument("--db-user", default=None, help="数据库用户（覆盖配置文件）")
    parser.add_argument("--db-pass", default=None, help="数据库密码（覆盖配置文件）")
    parser.add_argument("--dry-run", action="store_true", help="预览模式，不实际修改数据库")
    parser.add_argument("--verbose", "-v", action="store_true", help="详细输出")
    args = parser.parse_args()

    print("=" * 60)
    print("权限同步脚本 (PostgreSQL)")
    print("=" * 60)

    if args.dry_run:
        print("[预览模式] 不会实际修改数据库")

    # 读取 Spring Boot 配置（优先级: 命令行参数 > 环境变量 > application-{profile}.yml > 默认值）
    spring_config = load_spring_db_config(args.profile)

    args.db_host = args.db_host or os.environ.get("DATABASE_HOST") or spring_config.get("host", "localhost")
    args.db_port = args.db_port or os.environ.get("DATABASE_PORT") or spring_config.get("port", "5432")
    args.db_name = args.db_name or os.environ.get("DATABASE_NAME") or spring_config.get("name", "admin")
    args.db_user = args.db_user or os.environ.get("DATABASE_USERNAME") or spring_config.get("username", "postgres")
    args.db_pass = args.db_pass or os.environ.get("DATABASE_PASSWORD") or spring_config.get("password", "postgres")

    # 1. 获取OpenAPI规范
    print(f"\n正在从 {args.openapi_url} 获取OpenAPI规范...")
    try:
        spec = fetch_openapi_spec(args.openapi_url)
        print(f"✓ OpenAPI规范获取成功: {spec.get('info', {}).get('title', 'Unknown')}")
    except Exception as e:
        print(f"✗ OpenAPI规范获取失败: {e}")
        sys.exit(1)

    # 2. 提取路由
    print("\n正在解析路由...")
    routes = extract_routes_from_openapi(spec)
    print(f"✓ 提取到 {len(routes)} 个路由")

    if args.verbose:
        print("\n路由列表:")
        for route in routes:
            resource_pattern = convert_path_params_to_wildcard(route.path)
            prefix = extract_group_prefix(route.path)
            group_key = generate_group_key(prefix)
            print(f"  [{route.method:6s}] {resource_pattern:50s} -> {group_key}")

    # 3. 连接数据库
    print(f"\n正在连接数据库 {args.db_host}:{args.db_port}/{args.db_name}...")
    try:
        conn = get_db_connection(args)
        cursor = conn.cursor()
        print("✓ 数据库连接成功")
    except Exception as e:
        print(f"✗ 数据库连接失败: {e}")
        sys.exit(1)

    # 4. 同步权限
    print("\n正在同步权限...")
    try:
        result = sync_permissions(cursor, routes, dry_run=args.dry_run, verbose=args.verbose)

        if not args.dry_run:
            conn.commit()

        print("\n" + "=" * 60)
        print("同步结果:")
        print("=" * 60)
        print(f"  子权限:")
        print(f"    新增: {result.permissions_added}")
        print(f"    更新: {result.permissions_updated}")
        print(f"    不变: {result.permissions_unchanged}")
        print(f"    删除: {result.permissions_deleted}")
        print(f"  组权限:")
        print(f"    新增: {result.groups_added}")
        print(f"    更新: {result.groups_updated}")
        print(f"    不变: {result.groups_unchanged}")
        print(f"    删除: {result.groups_deleted}")

        if result.errors:
            print(f"\n错误 ({len(result.errors)}):")
            for error in result.errors:
                print(f"  - {error}")

        if args.dry_run:
            print("\n[预览模式] 以上为预览结果，使用不带 --dry-run 参数运行以实际同步")
        else:
            print("\n✓ 同步完成!")

    except Exception as e:
        print(f"✗ 同步失败: {e}")
        import traceback
        traceback.print_exc()
        conn.rollback()
        sys.exit(1)
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    main()
