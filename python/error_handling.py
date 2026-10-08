"""error-handling: trigger and handle typed errors.

1. An unknown customer ID: the API answers with an IntegrationError (QBD_OBJECT_NOT_FOUND).
2. A mistyped API key: rejected locally with DaapiError before any request is sent.
3. A QuickBooks call without an end user: rejected locally with DaapiError before any request is sent.

Exits 1 when a step does not produce the error it demonstrates (for example a connection or setup
error instead of QBD_OBJECT_NOT_FOUND), so a misconfigured run never looks like a pass.

python error_handling.py
"""

from __future__ import annotations

import sys

from common import describe_error, make_client

from desktopaccountingapi import APIError, DaapiError, DesktopAccountingApi, ErrorCode, IntegrationError

failures = 0


def unexpected(what: str) -> None:
    global failures
    failures += 1
    print(f"  -> unexpected: {what}")


def main() -> None:
    print("1. Retrieve a customer that does not exist")
    with make_client() as client:
        try:
            client.qbd.customers.retrieve("80000000-0000000000")
            unexpected("found a customer")
        except IntegrationError as error:
            describe_error(error)
            if error.code == ErrorCode.QBD_OBJECT_NOT_FOUND:
                print("  -> the ID does not exist in this company file")
            else:
                unexpected("expected QBD_OBJECT_NOT_FOUND")
        except DaapiError as error:
            describe_error(error)
            unexpected("expected QBD_OBJECT_NOT_FOUND")

    print("2. Use a mistyped API key")
    try:
        DesktopAccountingApi(api_key="sk_test_" + "0" * 39 + "1")
        unexpected("the key was accepted")
    except DaapiError as error:
        describe_error(error)
        if isinstance(error, APIError):
            unexpected("expected a local DaapiError")

    print("3. Call QuickBooks without an end user")
    try:
        with DesktopAccountingApi() as no_end_user:  # reads DAAPI_SECRET_KEY; no end user set
            no_end_user.qbd.customers.list(limit=1).first_page()
        unexpected("the call succeeded")
    except DaapiError as error:
        describe_error(error)
        if isinstance(error, APIError):
            unexpected("expected a local DaapiError")
        else:
            print("  -> pass end_user_id=..., or use client.for_end_user('eu_...')")

    if failures:
        print("At least one step did not produce the error it demonstrates.")
        sys.exit(1)


if __name__ == "__main__":
    main()
