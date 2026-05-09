from dataclasses import dataclass

import requests
import psycopg

from config import BACKEND_URL, DATABASE_CONFIG, DATABASE_PASSWORD, DATABASE_URL, DATABASE_USERNAME, TICKET_SOURCE


@dataclass
class PendingTicket:
    id: int
    user_id: int | None
    ticket_no: str
    student_id: str
    ticket_type: int
    status: int
    broadband_account: str
    new_password: str
    phone: str
    result_message: str
    created_at: str | None
    updated_at: str | None

    @classmethod
    def from_dict(cls, data: dict) -> "PendingTicket":
        return cls(
            id=int(data["id"]),
            user_id=data.get("userId"),
            ticket_no=data.get("ticketNo", ""),
            student_id=(data.get("studentId") or "").strip(),
            ticket_type=int(data.get("ticketType") or 0),
            status=int(data.get("status") or 0),
            broadband_account=(data.get("broadbandAccount") or "").strip(),
            new_password=(data.get("newPassword") or "").strip(),
            phone=(data.get("phone") or "").strip(),
            result_message=(data.get("resultMessage") or "").strip(),
            created_at=data.get("createdAt"),
            updated_at=data.get("updatedAt"),
        )


def fetch_pending_tickets(timeout_seconds: int = 15) -> list[PendingTicket]:
    if TICKET_SOURCE == "db":
        return fetch_pending_tickets_from_db(timeout_seconds=timeout_seconds)

    if TICKET_SOURCE != "api":
        raise RuntimeError(f"不支持的 TICKET_SOURCE: {TICKET_SOURCE}")

    endpoint = f"{BACKEND_URL.rstrip('/')}/api/tickets/pending"
    print("开始读取待处理工单:", endpoint)
    response = requests.get(endpoint, timeout=timeout_seconds)
    response.raise_for_status()
    payload = response.json()
    if not isinstance(payload, list):
        raise RuntimeError(f"待处理工单接口返回格式不正确: {type(payload)!r}")
    tickets = [PendingTicket.from_dict(item) for item in payload]
    print(f"已读取待处理工单 {len(tickets)} 条")
    return tickets


def fetch_pending_tickets_from_db(timeout_seconds: int = 15) -> list[PendingTicket]:
    connect_kwargs = build_connect_kwargs(timeout_seconds)

    print("开始直连数据库读取待处理工单")
    sql = """
        SELECT
            t.id,
            t.user_id,
            t.ticket_no,
            t.student_id,
            t.ticket_type,
            t.status,
            t.broadband_account,
            t.new_password,
            t.phone,
            t.result_message,
            t.created_at,
            t.updated_at
        FROM tickets t
        WHERE t.status = 0
        ORDER BY t.created_at ASC, t.id ASC
    """
    with psycopg.connect(**connect_kwargs) as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()

    tickets = [
        PendingTicket(
            id=int(row[0]),
            user_id=row[1],
            ticket_no=row[2] or "",
            student_id=(row[3] or "").strip(),
            ticket_type=int(row[4] or 0),
            status=int(row[5] or 0),
            broadband_account=(row[6] or "").strip(),
            new_password=(row[7] or "").strip(),
            phone=(row[8] or "").strip(),
            result_message=(row[9] or "").strip(),
            created_at=str(row[10]) if row[10] is not None else None,
            updated_at=str(row[11]) if row[11] is not None else None,
        )
        for row in rows
    ]
    print(f"已从数据库读取待处理工单 {len(tickets)} 条")
    return tickets


def fetch_next_ticket_from_db(
    after_created_at: str | None = None,
    after_id: int | None = None,
    timeout_seconds: int = 15,
) -> PendingTicket | None:
    connect_kwargs = build_connect_kwargs(timeout_seconds)

    base_select = """
        SELECT
            t.id,
            t.user_id,
            t.ticket_no,
            t.student_id,
            t.ticket_type,
            t.status,
            t.broadband_account,
            t.new_password,
            t.phone,
            t.result_message,
            t.created_at,
            t.updated_at
        FROM tickets t
        WHERE t.status IN (0, 1)
    """
    order_clause = """
        ORDER BY t.created_at ASC, t.id ASC
        LIMIT 1
    """

    if after_created_at:
        sql = (
            base_select
            + """
          AND (
                t.created_at > %(after_created_at)s::timestamp
                OR (t.created_at = %(after_created_at)s::timestamp AND t.id > %(after_id)s)
          )
    """
            + order_clause
        )
        params = {
            "after_created_at": after_created_at,
            "after_id": after_id or 0,
        }
    else:
        sql = base_select + order_clause
        params = None

    with psycopg.connect(**connect_kwargs) as conn:
        with conn.cursor() as cur:
            if params is None:
                cur.execute(sql)
            else:
                cur.execute(sql, params)
            row = cur.fetchone()

    if row is None:
        return None

    ticket = PendingTicket(
        id=int(row[0]),
        user_id=row[1],
        ticket_no=row[2] or "",
        student_id=(row[3] or "").strip(),
        ticket_type=int(row[4] or 0),
        status=int(row[5] or 0),
        broadband_account=(row[6] or "").strip(),
        new_password=(row[7] or "").strip(),
        phone=(row[8] or "").strip(),
        result_message=(row[9] or "").strip(),
        created_at=row[10].isoformat(sep=" ") if row[10] is not None else None,
        updated_at=row[11].isoformat(sep=" ") if row[11] is not None else None,
    )
    print(f"已读取下一条工单: id={ticket.id}, studentId={ticket.student_id}, status={ticket.status}")
    return ticket


def update_ticket_status_in_db(ticket_id: int, status: int, result_message: str | None = None, timeout_seconds: int = 15) -> None:
    connect_kwargs = build_connect_kwargs(timeout_seconds)

    sql = """
        UPDATE tickets
        SET status = %s,
            result_message = COALESCE(%s, result_message)
        WHERE id = %s
    """
    with psycopg.connect(**connect_kwargs) as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (status, result_message, ticket_id))
        conn.commit()

    print(f"已更新工单状态: id={ticket_id}, status={status}")


def print_pending_tickets_summary(tickets: list[PendingTicket]) -> None:
    if not tickets:
        print("当前没有待处理工单")
        return

    print("待处理工单列表:")
    for index, ticket in enumerate(tickets, start=1):
        masked_password = "*" * len(ticket.new_password) if ticket.new_password else "(空)"
        print(
            f"[{index}] id={ticket.id}, ticketNo={ticket.ticket_no}, "
            f"type={ticket.ticket_type}, status={ticket.status}, userId={ticket.user_id}"
        )
        print(
            f"    studentId={ticket.student_id}, broadbandAccount={ticket.broadband_account or '(空)'}, "
            f"newPassword={masked_password}, phone={ticket.phone or '(空)'}"
        )
        print(
            f"    createdAt={ticket.created_at or '(空)'}, resultMessage={ticket.result_message or '(空)'}"
        )


def normalize_database_url(value: str) -> str:
    return value.replace("jdbc:", "", 1) if value.startswith("jdbc:") else value


def build_connect_kwargs(timeout_seconds: int) -> dict:
    if DATABASE_URL:
        connect_kwargs = {
            "conninfo": normalize_database_url(DATABASE_URL),
            "connect_timeout": timeout_seconds,
        }
        if DATABASE_USERNAME:
            connect_kwargs["user"] = DATABASE_USERNAME
        if DATABASE_PASSWORD:
            connect_kwargs["password"] = DATABASE_PASSWORD
        return connect_kwargs

    host = DATABASE_CONFIG.get("host")
    database = DATABASE_CONFIG.get("database")
    user = DATABASE_CONFIG.get("user")
    if not host or not database or not user:
        raise RuntimeError("数据库配置不完整，请检查 DATABASE_URL 或 DATABASE_HOST / DATABASE_NAME / DATABASE_USER")

    connect_kwargs = {
        "host": host,
        "port": DATABASE_CONFIG.get("port", 5432),
        "dbname": database,
        "user": user,
        "connect_timeout": timeout_seconds,
    }
    password = DATABASE_CONFIG.get("password")
    if password:
        connect_kwargs["password"] = password
    return connect_kwargs


def mask_database_url(value: str) -> str:
    normalized = normalize_database_url(value)
    if "@" not in normalized:
        return normalized
    prefix, suffix = normalized.split("@", 1)
    if "://" not in prefix:
        return normalized
    scheme, _ = prefix.split("://", 1)
    return f"{scheme}://***@{suffix}"
