"""async-webhooks: an async-mode create returning a request handle, plus a small webhook receiver
that verifies Standard Webhooks signatures with the SDK helper.

python async_webhooks.py               # async-mode create, waits on the request handle, voids the invoice
python async_webhooks.py --receive     # webhook receiver on PORT (default 8080)
python async_webhooks.py --self-test   # signs and verifies a sample event locally; needs no API key
"""

from __future__ import annotations

import json
import os
import sys
import time
from decimal import Decimal
from http.server import BaseHTTPRequestHandler, HTTPServer

from common import invoice_parties, make_client, require_env, run_id

from desktopaccountingapi import WebhookVerificationError, webhooks
from desktopaccountingapi.types import InvoiceLineCreateInput


def enqueue_invoice() -> None:
    run = run_id()
    with make_client() as client:
        customer_id, item_id = invoice_parties(client)
        handle = client.qbd.invoices.enqueue.create(
            customer_id=customer_id,
            memo=f"async-webhooks example {run}",
            lines=[InvoiceLineCreateInput(item_id=item_id, quantity=1, rate=Decimal("25.00"))],
            queue_ttl=3600,
        )
        status = handle.request.status if handle.request else "-"
        print(f"Queued request {handle.id} (status {status}); waiting for QuickBooks...")
        invoice = handle.wait(timeout=600)
        print(f"Invoice {invoice.id}: ref {invoice.ref_number}, subtotal {invoice.subtotal}")
        client.qbd.invoices.void(invoice.id)
        print(f"Voided {invoice.id}")


class WebhookHandler(BaseHTTPRequestHandler):
    secret = ""

    def do_POST(self) -> None:
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        headers = {name.lower(): value for name, value in self.headers.items()}
        try:
            event = webhooks.verify(body, headers, self.secret)
        except WebhookVerificationError as error:
            print(f"Rejected delivery: {error}")
            self.send_response(400)
            self.end_headers()
            return
        print(f"{event.type} {event.id}: {json.dumps(event.data)}")
        self.send_response(204)
        self.end_headers()

    def log_message(self, format: str, *args: object) -> None:
        return


def receive() -> None:
    WebhookHandler.secret = require_env("DAAPI_WEBHOOK_SECRET")
    port = int(os.environ.get("PORT", "8080"))
    server = HTTPServer(("0.0.0.0", port), WebhookHandler)
    print(f"Listening for webhooks on http://0.0.0.0:{port}/ (Ctrl+C to stop)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()


# Self-test secret when DAAPI_WEBHOOK_SECRET is not set (base64 of 32 bytes, Standard Webhooks format).
SAMPLE_SECRET = "whsec_c2VsZi10ZXN0LXNlY3JldC1ub3QtZm9yLXByb2R1Y3Rpb24="


def self_test() -> None:
    secret = os.environ.get("DAAPI_WEBHOOK_SECRET") or SAMPLE_SECRET
    body = json.dumps(
        {
            "id": "evt_selftest",
            "type": webhooks.EventType.REQUEST_SUCCEEDED,
            "timestamp": "2026-10-05T16:04:01.311Z",
            "projectId": "proj_selftest",
            "data": {"objectType": "request", "id": "req_selftest", "status": "succeeded"},
        }
    )
    timestamp = int(time.time())
    signature = webhooks.sign(body, msg_id="evt_selftest", timestamp=timestamp, secret=secret)
    headers = {"Webhook-Id": "evt_selftest", "Webhook-Timestamp": str(timestamp), "Webhook-Signature": signature}
    event = webhooks.verify(body, headers, secret)
    print(f"Verified {event.type} {event.id} for request {event.data['id']}")
    try:
        webhooks.verify(body.replace("succeeded", "failed"), headers, secret)
    except WebhookVerificationError:
        print("A tampered body is rejected")
    else:
        raise SystemExit("a tampered body was accepted")


if __name__ == "__main__":
    if "--self-test" in sys.argv[1:]:
        self_test()
    elif "--receive" in sys.argv[1:]:
        receive()
    else:
        enqueue_invoice()
