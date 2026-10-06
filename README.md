# Desktop Accounting API examples

Runnable QuickBooks Desktop integrations built on the Desktop Accounting API SDKs, in Node.js (TypeScript), Python, C# / .NET and Java. Every example exists in all four languages and does the same thing in each.

| Example | What it shows |
| --- | --- |
| `quickstart` | Health check of the end user's QuickBooks connection, then the first 10 invoices. |
| `create-invoice` | Create a customer and an invoice with a fixed idempotency key, update the memo, void the invoice. |
| `network-drop` | A create whose response is lost on the first attempt. The SDK retries with the same idempotency key and exactly one invoice exists afterwards. |
| `sync-customers` | Auto-pagination over every customer, 10 per page. `--slow` waits past the cursor idle window to show `CursorExpiredError` with its progress fields and how to resume from an `updatedAfter` watermark. |
| `async-webhooks` | An async-mode create returning a request handle, plus a small webhook receiver that verifies Standard Webhooks signatures with the SDK helper. |
| `error-handling` | Typed errors: `code`, `userFacingMessage`, `fixes`, `docsUrl` and `requestId`. |

## Setup

You need:

1. A Desktop Accounting API project and a secret key (`sk_test_...` for a test project).
2. An end user whose QuickBooks Desktop company file is connected through the Web Connector. Use a sample or test company file. The examples create and void records.
3. The SDK repositories checked out in `sdk/` next to the language folders, or anywhere you like with `SDK_REPOS_DIR` pointing at them:

   ```sh
   mkdir -p sdk
   git clone https://github.com/DesktopAccountingAPI/quickbooks-desktop-node.git sdk/quickbooks-desktop-node
   git clone https://github.com/DesktopAccountingAPI/quickbooks-desktop-python.git sdk/quickbooks-desktop-python
   git clone https://github.com/DesktopAccountingAPI/quickbooks-desktop-dotnet.git sdk/quickbooks-desktop-dotnet
   git clone https://github.com/DesktopAccountingAPI/quickbooks-desktop-java.git sdk/quickbooks-desktop-java
   ```

4. The toolchains pinned in `mise.toml`. With [mise](https://mise.jdx.dev): `mise install`.

Configuration comes from environment variables:

| Variable | Meaning |
| --- | --- |
| `DAAPI_SECRET_KEY` | Secret key. Required. |
| `DAAPI_BASE_URL` | API base URL. Defaults to `https://api.desktopaccountingapi.com`. |
| `DAAPI_END_USER_ID` | End user (`eu_...`) whose company file the examples use. Required. |
| `DAAPI_WEBHOOK_SECRET` | Webhook endpoint signing secret (`whsec_...`), for `async-webhooks`. |
| `PORT` | Port for the webhook receiver. Defaults to 8080. |

Each language folder's README has the build and run commands: [node](node/README.md), [python](python/README.md), [dotnet](dotnet/README.md), [java](java/README.md).

## Checks

- `mise run check:registry` builds or type-checks every example in every language against the released packages from npm, PyPI, NuGet and Maven Central. CI runs it on every push and weekly.
- `mise run check` does the same against SDK checkouts in `SDK_REPOS_DIR` (default `./sdk`).

Neither calls the API. The examples run against a real QuickBooks connection only when you run them.

---

QuickBooks is a registered trademark of Intuit Inc. Desktop Accounting API is an independent product and is not affiliated with, endorsed by, or approved by Intuit Inc.
