"""
测试 tickets 表数据库连接和操作
"""

from tasks.db_utils import get_ticket_db_connection


def test_database_connection():
    """测试数据库连接和基本查询"""
    print("=" * 60)
    print("开始测试 tickets 表数据库连接")
    print("=" * 60)
    
    try:
        with get_ticket_db_connection() as db:
            # 测试1：统计待处理工单数量
            print("\n📊 测试1：统计待处理工单数量")
            count = db.count_pending_tickets()
            print(f"   待处理工单数量: {count}")
            
            # 测试2：查询所有待处理工单
            print("\n📋 测试2：查询待处理工单（最多5条）")
            tickets = db.get_pending_tickets(limit=5)
            
            if tickets:
                for i, ticket in enumerate(tickets, 1):
                    print(f"\n   工单 {i}:")
                    print(f"     ID: {ticket[0]}")
                    print(f"     工单号: {ticket[1]}")
                    print(f"     用户ID: {ticket[2]}")
                    print(f"     学号: {ticket[3]}")
                    print(f"     类型: {ticket[4]} (1=新用户绑定, 2=账号不存在, 3=宽带密码)")
                    print(f"     状态: {ticket[5]} (0=待处理, 1=排队中, 2=处理中, 3=已完成)")
                    print(f"     宽带账号: {ticket[6]}")
                    print(f"     新密码: {ticket[7]}")
                    print(f"     电话: {ticket[8]}")
                    print(f"     结果消息: {ticket[9]}")
                    print(f"     创建时间: {ticket[10]}")
            else:
                print("   ℹ️ 没有待处理工单")
            
            # 测试3：根据学号查询工单
            print("\n📋 测试3：根据学号查询工单")
            test_student_id = input("请输入要查询的学号（直接回车跳过）: ").strip()
            
            if test_student_id:
                student_tickets = db.get_tickets_by_student_id(test_student_id)
                
                if student_tickets:
                    print(f"   ✅ 找到 {len(student_tickets)} 条工单:")
                    for ticket in student_tickets:
                        print(f"     工单号: {ticket[1]}, 状态: {ticket[5]}, 类型: {ticket[4]}")
                else:
                    print(f"   ℹ️ 学号 {test_student_id} 没有工单记录")
            
            # 测试4：根据工单ID查询
            print("\n📋 测试4：根据工单ID查询")
            test_ticket_id = input("请输入要查询的工单ID（直接回车跳过）: ").strip()
            
            if test_ticket_id:
                ticket = db.get_ticket_by_id(int(test_ticket_id))
                
                if ticket:
                    print(f"   ✅ 找到工单:")
                    print(f"     工单号: {ticket[1]}")
                    print(f"     学号: {ticket[3]}")
                    print(f"     状态: {ticket[5]}")
                    print(f"     宽带账号: {ticket[6]}")
                else:
                    print(f"   ℹ️ 未找到ID为 {test_ticket_id} 的工单")
            
            print("\n" + "=" * 60)
            print("✅ 所有测试通过！")
            print("=" * 60)
    
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()


def test_update_operations():
    """测试更新操作（可选，需要确认后才执行）"""
    print("\n" + "=" * 60)
    print("⚠️  警告：以下操作会修改数据库")
    print("=" * 60)
    
    confirm = input("是否继续测试更新操作？(yes/no): ").strip().lower()
    
    if confirm != 'yes':
        print("ℹ️  已跳过更新测试")
        return
    
    try:
        with get_ticket_db_connection() as db:
            # 获取一个待处理工单用于测试
            tickets = db.get_pending_tickets(limit=1)
            
            if not tickets:
                print("ℹ️  没有待处理工单，无法测试更新")
                return
            
            ticket = tickets[0]
            ticket_id = ticket[0]
            
            print(f"\n📝 使用工单ID {ticket_id} 进行测试")
            
            # 测试1：标记为处理中
            print("\n🔄 测试1：标记工单为处理中")
            db.mark_ticket_processing(ticket_id)
            
            # 验证更新
            updated_ticket = db.get_ticket_by_id(ticket_id)
            print(f"   更新后状态: {updated_ticket[5]} (应该是 2)")
            
            # 测试2：更新宽带信息
            print("\n🔄 测试2：更新宽带账号和密码")
            test_account = "test_broadband_001"
            test_password = "test_pass_123"
            db.update_ticket_broadband(ticket_id, test_account, test_password)
            
            # 验证更新
            updated_ticket = db.get_ticket_by_id(ticket_id)
            print(f"   更新后宽带账号: {updated_ticket[6]}")
            print(f"   更新后密码: {updated_ticket[7]}")
            
            # 测试3：标记为已完成
            print("\n🔄 测试3：标记工单为已完成")
            db.mark_ticket_completed(ticket_id, "自动化测试完成")
            
            # 验证更新
            updated_ticket = db.get_ticket_by_id(ticket_id)
            print(f"   更新后状态: {updated_ticket[5]} (应该是 3)")
            print(f"   结果消息: {updated_ticket[9]}")
            
            print("\n" + "=" * 60)
            print("✅ 更新测试通过！")
            print("=" * 60)
    
    except Exception as e:
        print(f"\n❌ 更新测试失败: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    # 先测试查询
    test_database_connection()
    
    # 再测试更新（需要确认）
    test_update_operations()

    