#!/usr/bin/env python
"""
将 MinIO 中已有的文件导入 admin_file 表

使用方法:
    pip install minio psycopg2-binary pyyaml
    python script/import_minio_files.py [选项]

选项:
    --dry-run      预览模式，不实际写入数据库
    --verbose      详细输出

逻辑:
1. 连接 MinIO，遍历指定桶的所有文件
2. 连接 PostgreSQL，检查 admin_file 表中是否已有记录（按 object_name 匹配）
3. 不存在的文件 → 插入 admin_file 表（status=active）
4. 已存在的跳过
"""
import sys
import os
import argparse
from datetime import datetime

import yaml
import psycopg2
from minio import Minio


def load_config():
    """从 application-dev.yml 读取数据库和 MinIO 配置"""
    config_path = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'application-dev.yml')
    if not os.path.exists(config_path):
        print(f"❌ 配置文件不存在: {config_path}")
        sys.exit(1)

    with open(config_path, 'r', encoding='utf-8') as f:
        config = yaml.safe_load(f)

    return config


def get_db_connection(config):
    db = config.get('database', {})
    return psycopg2.connect(
        host=db.get('host', 'localhost'),
        port=db.get('port', 5432),
        dbname=db.get('name', 'admin'),
        user=db.get('username', 'postgres'),
        password=db.get('password', 'postgres'),
    )


def get_minio_client(config):
    minio_cfg = config.get('minio', {})
    endpoint = minio_cfg.get('endpoint', 'http://localhost:9000')
    # 去掉 http:// 或 https:// 前缀
    secure = endpoint.startswith('https://')
    endpoint_clean = endpoint.replace('https://', '').replace('http://', '')

    return Minio(
        endpoint_clean,
        access_key=minio_cfg.get('access-key', 'minioadmin'),
        secret_key=minio_cfg.get('secret-key', 'minioadmin'),
        secure=secure,
    ), minio_cfg.get('bucket-name', 'admin-uploads'), endpoint


def guess_category(object_name: str) -> str:
    """根据路径前缀猜测文件分类"""
    lower = object_name.lower()
    if lower.startswith('avatars/') or 'avatar' in lower:
        return 'avatar'
    if lower.startswith('images/') or lower.startswith('image/'):
        return 'image'
    if lower.startswith('documents/') or lower.startswith('document/'):
        return 'document'
    return 'general'


def main():
    parser = argparse.ArgumentParser(description='将 MinIO 文件导入 admin_file 表')
    parser.add_argument('--dry-run', action='store_true', help='预览模式，不写入数据库')
    parser.add_argument('--verbose', action='store_true', help='详细输出')
    args = parser.parse_args()

    config = load_config()
    conn = get_db_connection(config)
    minio_client, bucket_name, endpoint = get_minio_client(config)

    print(f"🔗 数据库: {config['database']['host']}:{config['database']['port']}/{config['database']['name']}")
    print(f"🪣 MinIO: {endpoint} / {bucket_name}")
    print()

    # 1. 获取 admin_file ��中已有��� object_name
    cursor = conn.cursor()
    cursor.execute("SELECT object_name FROM admin_file WHERE is_deleted = 0")
    existing = set(row[0] for row in cursor.fetchall())
    print(f"📋 数据库已有 {len(existing)} 条文件记录")

    # 2. 遍历 MinIO 桶
    objects = minio_client.list_objects(bucket_name, recursive=True)
    new_count = 0
    skip_count = 0

    for obj in objects:
        object_name = obj.object_name

        # 跳过目录
        if object_name.endswith('/'):
            continue

        if object_name in existing:
            skip_count += 1
            if args.verbose:
                print(f"  ⏭ 已存在: {object_name}")
            continue

        # 提取文件名
        file_name = object_name.split('/')[-1] if '/' in object_name else object_name
        url = f"{endpoint}/{bucket_name}/{object_name}"
        category = guess_category(object_name)
        content_type = obj.content_type or ''
        size = obj.size or 0

        if args.verbose or args.dry_run:
            print(f"  ➕ 新增: {object_name} ({category}, {size} bytes)")

        if not args.dry_run:
            cursor.execute(
                """
                INSERT INTO admin_file
                    (file_name, object_name, bucket_name, url, size, content_type, category, status, create_time, update_time, is_deleted)
                VALUES
                    (%s, %s, %s, %s, %s, %s, %s, 'active', %s, %s, 0)
                """,
                (file_name, object_name, bucket_name, url, size, content_type, category,
                 obj.last_modified or datetime.now(), obj.last_modified or datetime.now())
            )

        new_count += 1

    if not args.dry_run:
        conn.commit()

    print()
    print(f"✅ 完成！新增 {new_count} 条，跳过 {skip_count} 条（已存在）")
    if args.dry_run:
        print("⚠️  预览模式，未实际写入数据库")

    cursor.close()
    conn.close()


if __name__ == '__main__':
    main()
