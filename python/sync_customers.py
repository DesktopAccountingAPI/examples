"""sync-customers: auto-paginate every customer, 10 per page, and check that no ID repeats.

--slow sleeps between pages past the cursor idle window (SLOW_SECONDS, default 15) to show
CursorExpiredError with its progress fields, then restarts the list and skips IDs it already has.
It does not resume from last_updated_at: QuickBooks returns records in its own order, not by
updated_at, so records not read yet can be older than the last one read.

python sync_customers.py
python sync_customers.py --slow
"""

from __future__ import annotations

import os
import sys
import time

from common import make_client

from desktopaccountingapi import CursorExpiredError


def main(slow: bool) -> None:
    pause = float(os.environ.get("SLOW_SECONDS", "15"))
    seen: set[str] = set()
    duplicates = 0
    with make_client() as client:
        try:
            for page_number, page in enumerate(client.qbd.customers.list(limit=10).iter_pages(), start=1):
                for customer in page.data:
                    if customer.id in seen:
                        duplicates += 1
                    seen.add(customer.id)
                print(f"Page {page_number}: {len(page.data)} customers, {page.remaining_count or 0} remaining")
                if slow and page.has_more:
                    print(f"  sleeping {pause:g} s (cursor expires at {page.cursor_expires_at})")
                    time.sleep(pause)
        except CursorExpiredError as error:
            print("CursorExpiredError:")
            print(f"  reason:          {error.reason}")
            print(f"  items_yielded:   {error.items_yielded}")
            print(f"  pages_served:    {error.pages_served}")
            print(f"  last_id:         {error.last_id}")
            print(f"  last_updated_at: {error.last_updated_at}")
            print(f"  request_id:      {error.request_id}")
            # Restart the same query (an incremental sync would restart from its saved
            # updated_after watermark) and skip the IDs already seen.
            print("Restarting the list and skipping IDs already seen")
            resumed = 0
            for customer in client.qbd.customers.list(limit=10):
                if customer.id not in seen:
                    seen.add(customer.id)
                    resumed += 1
            print(f"Restarted list added {resumed} customers")
    print(f"{len(seen)} customers, {duplicates} duplicate IDs")
    if duplicates:
        raise SystemExit("duplicate customer IDs")


if __name__ == "__main__":
    main("--slow" in sys.argv[1:])
