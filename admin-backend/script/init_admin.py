#!/usr/bin/env python3
"""
初始化管理员用户脚本

用法:
    python init_admin.py [username] [password] [nickname] [config_path]

参数:
    username    管理员用户名 (默认: admin)
    password    管理员密码 (默认: admin123)
    nickname    管理员昵称 (默认: 管理员)
    config_path application.yml 配置文件路径 (默认: ../src/main/resources/application-dev.yml)

示例:
    python init_admin.py admin admin123 管理员
    python init_admin.py admin admin123 管理员 ./application-prod.yml

依赖:
    pip install bcrypt psycopg2-binary pyyaml
"""

import sys
import os
import bcrypt
import psycopg2
import yaml
from datetime import datetime
from typing import Optional


def find_config_file(config_path: Optional[str]) -> str:
    """查找配置文件路径"""
    if config_path and os.path.exists(config_path):
        return config_path

    # 尝试相对于脚本目录查找
    script_dir = os.path.dirname(os.path.abspath(__file__))
    default_path = os.path.join(script_dir, "..", "src", "main", "resources", "application-dev.yml")
    if os.path.exists(default_path):
        return default_path

    # 尝试当前工作目录
    if os.path.exists("application-dev.yml"):
        return "application-dev.yml"

    raise FileNotFoundError("未找到配置文件，请手动指定配置文件路径")


def load_db_config(config_path: str) -> dict:
    """从 application.yml 读取数据库配置"""
    with open(config_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)

    db = config.get("database", {})
    return {
        "host": db.get("host", "localhost"),
        "port": db.get("port", 5432),
        "database": db.get("name", "admin_db"),
        "user": db.get("username", "postgres"),
        "password": db.get("password", ""),
    }


def hash_password(password: str) -> str:
    """BCrypt 加密密码"""
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(password.encode("utf-8"), salt).decode("utf-8")


def check_user_exists(conn, username: str) -> bool:
    """检查用户是否已存在"""
    cursor = conn.cursor()
    cursor.execute(
        "SELECT 1 FROM admin_user WHERE username = %s AND is_deleted = 0",
        (username,)
    )
    exists = cursor.fetchone() is not None
    cursor.close()
    return exists


def insert_admin_user(conn, username: str, password: str, nickname: str) -> int:
    """插入管理员用户"""
    hashed_password = hash_password(password)
    now = datetime.now()

    cursor = conn.cursor()
    cursor.execute(
        """
        INSERT INTO admin_user (username, password, nickname, status, is_deleted, create_time, update_time)
        VALUES (%s, %s, %s, 1, 0, %s, %s)
        RETURNING id
        """,
        (username, hashed_password, nickname, now, now)
    )
    user_id = cursor.fetchone()[0]
    conn.commit()
    cursor.close()
    return user_id


def main():
    # 默认值
    username = "admin"
    password = "admin123"
    nickname = "管理员"
    config_path = None

    # 命令行参数
    if len(sys.argv) >= 2:
        username = sys.argv[1]
    if len(sys.argv) >= 3:
        password = sys.argv[2]
    if len(sys.argv) >= 4:
        nickname = sys.argv[3]
    if len(sys.argv) >= 5:
        config_path = sys.argv[4]

    # 查找配置文件
    try:
        config_file = find_config_file(config_path)
        print(f"[INFO] 使用配置文件: {os.path.abspath(config_file)}")
    except FileNotFoundError as e:
        print(f"[ERROR] {e}")
        sys.exit(1)

    print(f"[INFO] 准备创建管理员用户:")
    print(f"       用户名: {username}")
    print(f"       昵称:   {nickname}")
    print(f"       密码:   {password}")
    print()

    # 读取数据库配置
    try:
        db_config = load_db_config(config_file)
    except Exception as e:
        print(f"[ERROR] 读取配置文件失败: {e}")
        sys.exit(1)

    print(f"[INFO] 连接数据库 {db_config['host']}:{db_config['port']}/{db_config['database']} ...")

    # 连接数据库
    try:
        conn = psycopg2.connect(**db_config)
        print("[INFO] 数据库连接成功")
    except Exception as e:
        print(f"[ERROR] 数据库连接失败: {e}")
        sys.exit(1)

    try:
        # 检查是否已存在
        if check_user_exists(conn, username):
            print(f"[WARN] 用户 '{username}' 已存在，跳过创建")
            sys.exit(0)

        # 创建用户
        user_id = insert_admin_user(conn, username, password, nickname)
        print(f"[SUCCESS] 管理员用户创建成功!")
        print(f"          用户ID: {user_id}")
        print(f"          用户名: {username}")
        print(f"          昵称:   {nickname}")
        print(f"          密码:   {password} (BCrypt加密存储)")
    except Exception as e:
        print(f"[ERROR] 创建用户失败: {e}")
        conn.rollback()
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
