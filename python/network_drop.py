"""network-drop: the first create attempt reaches the API, but its response is thrown away and the
client sees a network error. The SDK retries with the same Idempotency-Key, the API replays the
stored result, and exactly one invoice exists afterwards.

python network_drop.py
"""

from __future__ import annotations

from decimal import Decimal

import httpx
from common import invoice_parties, make_client, run_id

from desktopaccountingapi.types import InvoiceLineCreateInput


class DroppingTransport(httpx.HTTPTransport):
    """Sends every request for real; for the first invoice POST it discards the response and
    fails like a dropped connection."""

    def __init__(self) -> None:
        super().__init__()
        self.dropped = False
        self.keys_sent: list[str] = []

    def handle_request(self, request: httpx.Request) -> httpx.Response:
        response = super().handle_request(request)
        if request.method == "POST" and request.url.path.endswith("/invoices"):
            self.keys_sent.append(request.headers.get("idempotency-key", "(none)"))
            if not self.dropped:
                self.dropped = True
                response.read()
                response.close()
                print(
                    f"Attempt 1: the API answered {response.status_code}; dropping the response to simulate a lost connection"
                )
                raise httpx.ReadError("simulated network failure after the request was sent", request=request)
        return response


def main() -> None:
    transport = DroppingTransport()
    run = run_id()
    ref_number = f"ND{run}"[:11]
    with make_client(http_client=httpx.Client(transport=transport), max_retries=2) as client:
        customer_id, item_id = invoice_parties(client)
        raw = client.qbd.invoices.with_raw_response.create(
            customer_id=customer_id,
            ref_number=ref_number,
            memo=f"network-drop example {run}",
            lines=[InvoiceLineCreateInput(item_id=item_id, quantity=1, rate=Decimal("10.00"))],
        )
        invoice = raw.parse()
        print(f"Attempt 2: {raw.status_code}, Daapi-Idempotent-Replayed: {raw.idempotent_replayed}")
        print(f"Invoice {invoice.id}, ref {invoice.ref_number}")
        print(f"Idempotency keys sent: {', '.join(transport.keys_sent)}")
        if len(transport.keys_sent) != 2 or transport.keys_sent[0] != transport.keys_sent[1]:
            raise SystemExit("expected two attempts with the same Idempotency-Key")

        matches = client.qbd.invoices.list(ref_numbers=[ref_number]).list_all()
        print(f"Invoices with ref {ref_number}: {len(matches)}")
        if len(matches) != 1:
            raise SystemExit(f"expected exactly one invoice, found {len(matches)}")

        client.qbd.invoices.void(invoice.id)
        print(f"Voided {invoice.id}. Exactly one invoice was created.")


if __name__ == "__main__":
    main()
