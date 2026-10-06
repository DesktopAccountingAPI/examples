"""error-handling: trigger and handle typed errors.

1. An unknown customer ID: the API answers with an IntegrationError (QBD_OBJECT_NOT_FOUND).
2. A mistyped API key: rejected locally with DaapiError before any request is sent.

python error_handling.py
"""

from __future__ import annotations

from common import describe_error, make_client

from desktopaccountingapi import DaapiError, DesktopAccountingApi, ErrorCode, IntegrationError


def main() -> None:
    print("1. Retrieve a customer that does not exist")
    with make_client() as client:
        try:
            client.qbd.customers.retrieve("80000000-0000000000")
        except IntegrationError as error:
            describe_error(error)
            if error.code == ErrorCode.QBD_OBJECT_NOT_FOUND:
                print("  -> the ID does not exist in this company file")
        except DaapiError as error:
            describe_error(error)

    print("2. Use a mistyped API key")
    try:
        DesktopAccountingApi(api_key="sk_test_" + "0" * 39 + "1")
    except DaapiError as error:
        describe_error(error)


if __name__ == "__main__":
    main()
