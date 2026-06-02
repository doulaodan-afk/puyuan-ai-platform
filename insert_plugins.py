# -*- coding: utf-8 -*-
import sys
import subprocess
import os

# 设置控制台输出编码为 UTF-8（Windows）
if sys.platform == 'win32':
    os.system('chcp 65001 >nul')

# 检查并安装 pymysql
try:
    import pymysql
except ImportError:
    print("未安装 pymysql，正在安装...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pymysql"])
    import pymysql

# 数据库配置（从 application-dev.yml 读取）
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'database': 'puyuan_ai_mvp',
    'user': 'root',
    'password': '123456',
    'charset': 'utf8mb4'
}

# 插件数据（根据实际表结构）
PLUGINS = [
    ('ai_image_gen', 'AI图片生成', '1.0.0', '/api/plugin/invoke/ai_image_gen', '/ai-tools/image-gen', 'token', 20, 1, 'pass'),
    ('ai_script_gen', 'AI视频脚本生成', '1.0.0', '/api/plugin/invoke/ai_script_gen', '/ai-tools/script-gen', 'token', 20, 1, 'pass'),
    ('ai_translate', '跨境翻译', '1.0.0', '/api/plugin/invoke/ai_translate', '/ai-tools/translate', 'token', 5, 1, 'pass')
]

def main():
    try:
        # 连接数据库
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 执行批量插入/更新
        sql = '''
            INSERT INTO plugin (plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                version = VALUES(version),
                backend_api = VALUES(backend_api),
                frontend_entry = VALUES(frontend_entry),
                billing_type = VALUES(billing_type),
                default_token_cost = VALUES(default_token_cost),
                status = VALUES(status),
                review_status = VALUES(review_status),
                updated_at = NOW()
        '''
        cursor.executemany(sql, PLUGINS)
        conn.commit()

        print(f"成功插入/更新 {len(PLUGINS)} 个插件！")
        print("\n插件列表：")
        cursor.execute("SELECT plugin_id, name, status, billing_type, default_token_cost FROM plugin WHERE plugin_id IN ('ai_image_gen', 'ai_script_gen', 'ai_translate')")
        for row in cursor.fetchall():
            print(f"  - {row[0]}: {row[1]} (status={row[2]}, billing={row[3]}, cost={row[4]} tokens)")

    except Exception as e:
        print(f"错误: {e}")
        conn.rollback()
    finally:
        conn.close()

if __name__ == '__main__':
    main()