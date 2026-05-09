from dataclasses import dataclass

import requests
import psycopg

from config import BACKEND_URL, DATABASE_PASSWORD, DATABASE_URL, DATABASE_USERNAME, TICKET_SOURCE


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
    if not DATABASE_URL:
        raise RuntimeError("TICKET_SOURCE=db 时必须配置 DATABASE_URL")

    connect_kwargs = {
        "conninfo": normalize_database_url(DATABASE_URL),
        "connect_timeout": timeout_seconds,
    }
    if DATABASE_USERNAME:
        connect_kwargs["user"] = DATABASE_USERNAME
    if DATABASE_PASSWORD:
        connect_kwargs["password"] = DATABASE_PASSWORD

    print("开始直连数据库读取待处理工单:", mask_database_url(DATABASE_URL))
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


def mask_database_url(value: str) -> str:
    normalized = normalize_database_url(value)
    if "@" not in normalized:
        return normalized
    prefix, suffix = normalized.split("@", 1)
    if "://" not in prefix:
        return normalized
    scheme, _ = prefix.split("://", 1)
    return f"{scheme}://***@{suffix}"
