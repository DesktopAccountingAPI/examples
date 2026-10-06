# Python examples

Python programs using the [`desktopaccountingapi-quickbooks-desktop`](https://github.com/DesktopAccountingAPI/quickbooks-desktop-python) SDK (`import desktopaccountingapi`).

## Prerequisites

- Python 3.9 or later. The examples root `mise.toml` pins Python and uv: `mise install`.
- The environment variables from the [examples README](../README.md#setup): `DAAPI_SECRET_KEY`, `DAAPI_END_USER_ID`, optionally `DAAPI_BASE_URL` (staging: `https://api-staging.desktopaccountingapi.com`), `DAAPI_WEBHOOK_SECRET` and `PORT`.
- For `create_invoice`, `network_drop` and `async_webhooks`: an active customer and an active service item in the company file. The examples use the first ones they find; set `DAAPI_CUSTOMER_ID` and `DAAPI_ITEM_ID` to choose.

## Use the SDK from PyPI

```sh
cd python
python -m venv .venv && . .venv/bin/activate      # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

## Or use the SDK checked out next to the examples

The examples CI uses the SDK repository checked out at `$SDK_REPOS_DIR/quickbooks-desktop-python` (default `<examples root>/sdk/quickbooks-desktop-python`):

```sh
cd python
python -m venv .venv && . .venv/bin/activate
pip install "${SDK_REPOS_DIR:-../sdk}/quickbooks-desktop-python"
```

## Run

```sh
python quickstart.py
python create_invoice.py               # RUN_ID=<id> repeats a run: the same idempotency keys replay instead of duplicating
python network_drop.py
python sync_customers.py
python sync_customers.py --slow        # SLOW_SECONDS=15 per page by default
python async_webhooks.py               # async-mode create, waits on the request handle
python async_webhooks.py --receive     # webhook receiver on PORT (default 8080)
python async_webhooks.py --self-test   # signs and verifies a sample event locally; needs no API key
python error_handling.py
```

| Program | Example |
| --- | --- |
| `quickstart.py` | `quickstart` |
| `create_invoice.py` | `create-invoice` |
| `network_drop.py` | `network-drop`: an `httpx` transport that sends the first invoice create for real and then raises a network error, so the SDK retries with the same idempotency key |
| `sync_customers.py` | `sync-customers` |
| `async_webhooks.py` | `async-webhooks` |
| `error_handling.py` | `error-handling` |

`create_invoice`, `network_drop` and `async_webhooks` create records and void the invoices they create. Use a test company file.

## Check

```sh
python check.py
```

Installs the SDK from `$SDK_REPOS_DIR/quickbooks-desktop-python` into `python/.venv` with uv, type-checks every example with `mypy --strict`, and runs the webhook self-test. It makes no API calls.
