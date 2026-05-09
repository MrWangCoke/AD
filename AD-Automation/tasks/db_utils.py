"""
数据库工具模块 - 用于连接和操作 PostgreSQL 数据库的 tickets 表
仅支持查询(SELECT)和更新(UPDATE)，禁止插入(INSERT)和删除(DELETE)
"""

import psycopg2
from contextlib import contextmanager
from config import DATABASE_CONFIG


class TicketDatabaseManager:
    """工单数据库管理器 - 安全的查改操作"""
    
    def __init__(self):
        self.conn = None
        self.cursor = None
    
    def connect(self):
        """建立数据库连接"""
        try:
            self.conn = psycopg2.connect(
                host=DATABASE_CONFIG["host"],
                port=DATABASE_CONFIG["port"],
                database=DATABASE_CONFIG["database"],
                user=DATABASE_CONFIG["user"],
                password=DATABASE_CONFIG["password"]
            )
            self.cursor = self.conn.cursor()
            print(f"✅ 数据库连接成功: {DATABASE_CONFIG['database']}")
            return True
        except Exception as e:
            print(f"❌ 数据库连接失败: {e}")
            print("提示：请检查 .env 中的 DATABASE_PASSWORD 是否正确")
            return False
    
    def disconnect(self):
        """关闭数据库连接"""
        if self.cursor:
            self.cursor.close()
        if self.conn:
            self.conn.close()
            print("🔒 数据库连接已关闭")
    
    def query(self, query_str, params=None, fetch_one=False):
        """
        执行查询操作（SELECT）
        
        参数:
            query_str: SQL 查询语句
            params: 参数元组或列表
            fetch_one: 是否只返回一条记录
        
        返回:
            查询结果（列表或单条记录）
        """
        if not self.conn:
            raise RuntimeError("数据库未连接，请先调用 connect()")
        
        try:
            # 安全检查：确保只执行 SELECT 语句
            cleaned_query = query_str.strip().upper()
            if not cleaned_query.startswith("SELECT"):
                raise ValueError(
                    f"⚠️ 安全限制：query() 方法只允许执行 SELECT 语句\n"
                    f"当前尝试执行: {query_str[:50]}..."
                )
            
            self.cursor.execute(query_str, params)
            
            if fetch_one:
                result = self.cursor.fetchone()
                if result:
                    print(f"✅ 查询成功，返回 1 条记录")
                else:
                    print(f"ℹ️ 查询成功，未找到记录")
                return result
            else:
                result = self.cursor.fetchall()
                print(f"✅ 查询成功，返回 {len(result)} 条记录")
                return result
        
        except Exception as e:
            print(f"❌ 查询失败: {e}")
            raise
    
    def update(self, set_fields, where_clause, where_params):
        """
        执行更新操作（UPDATE tickets 表）
        
        参数:
            set_fields: 要更新的字段字典 {"字段名": 新值}
            where_clause: WHERE 条件字符串（如 "id = %s"）
            where_params: WHERE 条件的参数列表
        
        返回:
            受影响的行数
        """
        if not self.conn:
            raise RuntimeError("数据库未连接，请先调用 connect()")
        
        try:
            # 构建 SET 子句
            set_clause = ", ".join([f"{key} = %s" for key in set_fields.keys()])
            set_values = list(set_fields.values())
            
            # 完整的 UPDATE 语句
            update_query = f"UPDATE tickets SET {set_clause} WHERE {where_clause}"
            
            # 安全检查
            if not update_query.strip().upper().startswith("UPDATE"):
                raise ValueError("⚠️ 安全限制：update() 方法只允许执行 UPDATE 语句")
            
            # 合并参数
            all_params = set_values + where_params
            
            self.cursor.execute(update_query, all_params)
            self.conn.commit()
            
            row_count = self.cursor.rowcount
            print(f"✅ 更新成功，影响 {row_count} 行")
            return row_count
        
        except Exception as e:
            self.conn.rollback()  # 出错时回滚
            print(f"❌ 更新失败，已回滚: {e}")
            raise
    
    # ===== 便捷方法：针对 tickets 表 =====
    
    def get_pending_tickets(self, limit=10):
        """
        获取待处理的工单（status = 0）
        
        参数:
            limit: 最多返回多少条
        
        返回:
            工单记录列表
        """
        query = """
            SELECT id, ticket_no, user_id, student_id, ticket_type, 
                   status, broadband_account, new_password, phone, 
                   result_message, created_at, updated_at
            FROM tickets
            WHERE status = 0
            ORDER BY created_at ASC
            LIMIT %s
        """
        return self.query(query, (limit,))
    
    def get_ticket_by_id(self, ticket_id):
        """
        根据工单ID查询
        
        参数:
            ticket_id: 工单ID
        
        返回:
            单条工单记录
        """
        query = """
            SELECT id, ticket_no, user_id, student_id, ticket_type, 
                   status, broadband_account, new_password, phone, 
                   result_message, created_at, updated_at
            FROM tickets
            WHERE id = %s
        """
        return self.query(query, (ticket_id,), fetch_one=True)
    
    def get_ticket_by_ticket_no(self, ticket_no):
        """
        根据工单号查询
        
        参数:
            ticket_no: 工单号（如 "TK-1234567890"）
        
        返回:
            单条工单记录
        """
        query = """
            SELECT id, ticket_no, user_id, student_id, ticket_type, 
                   status, broadband_account, new_password, phone, 
                   result_message, created_at, updated_at
            FROM tickets
            WHERE ticket_no = %s
        """
        return self.query(query, (ticket_no,), fetch_one=True)
    
    def get_tickets_by_student_id(self, student_id, status=None):
        """
        根据学号查询工单
        
        参数:
            student_id: 学号
            status: 状态过滤（可选，None表示查询所有状态）
        
        返回:
            工单记录列表
        """
        if status is not None:
            query = """
                SELECT id, ticket_no, user_id, student_id, ticket_type, 
                       status, broadband_account, new_password, phone, 
                       result_message, created_at, updated_at
                FROM tickets
                WHERE student_id = %s AND status = %s
                ORDER BY created_at DESC
            """
            return self.query(query, (student_id, status))
        else:
            query = """
                SELECT id, ticket_no, user_id, student_id, ticket_type, 
                       status, broadband_account, new_password, phone, 
                       result_message, created_at, updated_at
                FROM tickets
                WHERE student_id = %s
                ORDER BY created_at DESC
            """
            return self.query(query, (student_id,))
    
    def get_tickets_by_type(self, ticket_type, status=None):
        """
        根据工单类型查询
        
        参数:
            ticket_type: 工单类型（1=新用户绑定, 2=账号不存在, 3=宽带密码）
            status: 状态过滤（可选）
        
        返回:
            工单记录列表
        """
        if status is not None:
            query = """
                SELECT id, ticket_no, user_id, student_id, ticket_type, 
                       status, broadband_account, new_password, phone, 
                       result_message, created_at, updated_at
                FROM tickets
                WHERE ticket_type = %s AND status = %s
                ORDER BY created_at ASC
            """
            return self.query(query, (ticket_type, status))
        else:
            query = """
                SELECT id, ticket_no, user_id, student_id, ticket_type, 
                       status, broadband_account, new_password, phone, 
                       result_message, created_at, updated_at
                FROM tickets
                WHERE ticket_type = %s
                ORDER BY created_at ASC
            """
            return self.query(query, (ticket_type,))
    
    def update_ticket_status(self, ticket_id, status, result_message=None):
        """
        更新工单状态
        
        参数:
            ticket_id: 工单ID
            status: 新状态（0=待处理, 1=排队中, 2=处理中, 3=已完成）
            result_message: 结果消息（可选）
        
        返回:
            受影响的行数
        """
        set_fields = {"status": status}
        if result_message is not None:
            set_fields["result_message"] = result_message
        
        return self.update(
            set_fields=set_fields,
            where_clause="id = %s",
            where_params=[ticket_id]
        )
    
    def update_ticket_broadband(self, ticket_id, broadband_account, new_password):
        """
        更新工单的宽带账号和密码
        
        参数:
            ticket_id: 工单ID
            broadband_account: 宽带账号
            new_password: 新密码
        
        返回:
            受影响的行数
        """
        return self.update(
            set_fields={
                "broadband_account": broadband_account,
                "new_password": new_password
            },
            where_clause="id = %s",
            where_params=[ticket_id]
        )
    
    def update_ticket_phone(self, ticket_id, phone):
        """
        更新工单的联系电话
        
        参数:
            ticket_id: 工单ID
            phone: 联系电话
        
        返回:
            受影响的行数
        """
        return self.update(
            set_fields={"phone": phone},
            where_clause="id = %s",
            where_params=[ticket_id]
        )
    
    def mark_ticket_completed(self, ticket_id, result_message="处理完成"):
        """
        标记工单为已完成（快捷方法）
        
        参数:
            ticket_id: 工单ID
            result_message: 结果消息
        
        返回:
            受影响的行数
        """
        return self.update_ticket_status(ticket_id, 3, result_message)
    
    def mark_ticket_processing(self, ticket_id):
        """
        标记工单为处理中（快捷方法）
        
        参数:
            ticket_id: 工单ID
        
        返回:
            受影响的行数
        """
        return self.update_ticket_status(ticket_id, 2, "开始处理")
    
    def count_pending_tickets(self):
        """
        统计待处理工单数量
        
        返回:
            待处理工单数量
        """
        query = "SELECT COUNT(*) FROM tickets WHERE status = 0"
        result = self.query(query, fetch_one=True)
        return result[0] if result else 0


# ===== 上下文管理器 =====

@contextmanager
def get_ticket_db_connection():
    """
    获取数据库连接的上下文管理器（推荐用法）
    
    用法示例:
        with get_ticket_db_connection() as db:
            tickets = db.get_pending_tickets(limit=5)
            db.mark_ticket_completed(ticket_id=123)
    """
    db = TicketDatabaseManager()
    if not db.connect():
        raise RuntimeError("无法连接到数据库")
    try:
        yield db
    finally:
        db.disconnect()


# ===== 便捷的函数式接口 =====

def query_pending_tickets(limit=10):
    """快捷查询：获取待处理工单"""
    with get_ticket_db_connection() as db:
        return db.get_pending_tickets(limit)


def query_ticket_by_id(ticket_id):
    """快捷查询：根据ID获取工单"""
    with get_ticket_db_connection() as db:
        return db.get_ticket_by_id(ticket_id)


def query_tickets_by_student_id(student_id, status=None):
    """快捷查询：根据学号获取工单"""
    with get_ticket_db_connection() as db:
        return db.get_tickets_by_student_id(student_id, status)


def update_ticket_to_completed(ticket_id, result_message="自动化处理完成"):
    """快捷更新：将工单标记为已完成"""
    with get_ticket_db_connection() as db:
        return db.mark_ticket_completed(ticket_id, result_message)


def update_ticket_broadband_info(ticket_id, broadband_account, new_password):
    """快捷更新：更新工单的宽带信息"""
    with get_ticket_db_connection() as db:
        return db.update_ticket_broadband(ticket_id, broadband_account, new_password)