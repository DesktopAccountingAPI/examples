"""create-invoice: create a uniquely named customer and an invoice with a fixed idempotency key,
update the invoice memo, then void it.

python create_invoice.py        # RUN_ID=<id> repeats a run: the same idempotency keys replay instead of duplicating
"""

from __future__ import annotations

import datetime as dt
import os
from decimal import Decimal

from common import make_client, run_id

from desktopaccountingapi.types import InvoiceLineCreateInput


def main() -> None:
    run = run_id()
    with make_client() as client:
        customer = client.qbd.customers.create(
            name=f"Example Customer {run}",
            company_name="Example Supply Co.",
            idempotency_key=f"example-{run}-customer",
        )
        print(f"Customer {customer.id}: {customer.full_name}")

        item_id = os.environ.get("DAAPI_ITEM_ID")
        if not item_id:
            items = client.qbd.service_items.list(limit=1, status="active").first_page().data
            if not items:
                raise SystemExit("The company file needs an active service item (or set DAAPI_ITEM_ID).")
            item_id = items[0].id

        # A fixed key derived from the run ID: re-running with the same RUN_ID replays this create.
        invoice = client.qbd.invoices.create(
            customer_id=customer.id,
            transaction_date=dt.date.today(),
            ref_number=f"EX{run}"[:11],
            memo=f"create-invoice example {run}",
            lines=[
                InvoiceLineCreateInput(item_id=item_id, description="Consulting", quantity=2, rate=Decimal("52.75")),
            ],
            idempotency_key=f"example-{run}-invoice",
        )
        print(f"Invoice {invoice.id}: ref {invoice.ref_number}, subtotal {invoice.subtotal}")

        updated = client.qbd.invoices.update(
            invoice.id,
            revision_number=invoice.revision_number,
            memo=f"create-invoice example {run} (updated)",
        )
        print(f"Updated memo: {updated.memo!r} (revision {updated.revision_number})")

        voided = client.qbd.invoices.void(invoice.id)
        print(f"Voided invoice {voided.id} (ref {voided.ref_number}): {voided.voided}")


if __name__ == "__main__":
    main()
