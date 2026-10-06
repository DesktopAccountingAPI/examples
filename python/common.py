"""Shared helpers for the examples: configuration from the environment and output formatting."""

from __future__ import annotations

import os
import sys
import time
from typing import Any, NoReturn

from desktopaccountingapi import APIError, DaapiError, DesktopAccountingApi


def fail(message: str) -> NoReturn:
    print(message, file=sys.stderr)
    sys.exit(2)


def require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        fail(f"Set {name}. See README.md for the required environment variables.")
    return value


def make_client(**options: Any) -> DesktopAccountingApi:
    """Client for the end user in DAAPI_END_USER_ID. The SDK reads DAAPI_SECRET_KEY and DAAPI_BASE_URL itself."""
    require_env("DAAPI_SECRET_KEY")
    return DesktopAccountingApi(end_user_id=require_env("DAAPI_END_USER_ID"), **options)


def run_id() -> str:
    """Short ID that makes names and idempotency keys unique per run. Set RUN_ID to repeat a run."""
    return os.environ.get("RUN_ID") or format(int(time.time() * 1000), "x")[-7:]


def invoice_parties(client: DesktopAccountingApi) -> tuple[str, str]:
    """A customer and a service item for invoices: DAAPI_CUSTOMER_ID / DAAPI_ITEM_ID, else the first active ones."""
    customer_id = os.environ.get("DAAPI_CUSTOMER_ID")
    item_id = os.environ.get("DAAPI_ITEM_ID")
    if not customer_id:
        customers = client.qbd.customers.list(limit=1, status="active").first_page().data
        customer_id = customers[0].id if customers else None
    if not item_id:
        items = client.qbd.service_items.list(limit=1, status="active").first_page().data
        item_id = items[0].id if items else None
    if not customer_id or not item_id:
        fail(
            "The company file needs at least one active customer and one active service item "
            "(or set DAAPI_CUSTOMER_ID and DAAPI_ITEM_ID)."
        )
    return customer_id, item_id


def describe_error(error: BaseException) -> None:
    """Prints the fields developers act on."""
    if isinstance(error, APIError):
        print(f"  class:               {type(error).__name__}")
        print(f"  HTTP status:         {error.status if error.status is not None else 'n/a'}")
        print(f"  type / code:         {error.type} / {error.code}")
        print(f"  message:             {error.message}")
        print(f"  user_facing_message: {error.user_facing_message}")
        print(f"  integration_code:    {error.integration_code or '-'}")
        print(f"  outcome:             {error.outcome}")
        print(f"  request_id:          {error.request_id}")
        print(f"  docs_url:            {error.docs_url}")
        for fix in error.fixes:
            print(f"  fix ({fix.actor}): {fix.action}")
    elif isinstance(error, DaapiError):
        print(f"  class:   {type(error).__name__} (raised locally, nothing was sent)")
        print(f"  message: {error.message}")
    else:
        print(f"  unexpected {type(error).__name__}: {error}")
