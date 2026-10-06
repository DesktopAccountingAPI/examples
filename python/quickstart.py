"""quickstart: health check of the end user's QuickBooks connection, then the first 10 invoices.

python quickstart.py
"""

from __future__ import annotations

from common import make_client


def main() -> None:
    with make_client() as client:
        health = client.qbd.health_check()
        qb = health.quickbooks
        print(f"QuickBooks: {qb.product or '-'} (qbXML {qb.qbxml_version or '-'}), company {qb.company_name or '-'}")
        print(f"Round trip: {health.duration} ms")

        page = client.qbd.invoices.list(limit=10).first_page()
        print(f"First {len(page.data)} invoices (more: {page.has_more}):")
        for invoice in page.data:
            customer = invoice.customer.full_name if invoice.customer else "-"
            print(f"  {invoice.ref_number or '-':<12} {invoice.transaction_date or '-'}  {customer:<30} {invoice.subtotal}")


if __name__ == "__main__":
    main()
