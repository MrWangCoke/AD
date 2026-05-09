from config import RUN_MODE
from tasks.campus_portal_task import open_campus_portal
from tasks.ticket_queue import fetch_pending_tickets, print_pending_tickets_summary


def main():
    if RUN_MODE == "portal":
        open_campus_portal()
        return

    if RUN_MODE != "tickets":
        raise RuntimeError(f"不支持的 RUN_MODE: {RUN_MODE}")

    tickets = fetch_pending_tickets()
    print_pending_tickets_summary(tickets)


if __name__ == "__main__":
    main()
