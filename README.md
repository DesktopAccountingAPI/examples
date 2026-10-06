# Desktop Accounting API examples

Runnable QuickBooks Desktop integrations built on the [Desktop Accounting API](https://www.desktopaccountingapi.com/) SDKs, in Node.js (TypeScript), Python, C# / .NET and Java. Every example exists in all four languages and does the same thing in each.

| Example | What it shows |
| --- | --- |
| `quickstart` | Health check of the end user's QuickBooks connection, then the first 10 invoices. |
| `create-invoice` | Create a customer and an invoice with a fixed idempotency key, update the memo with the invoice's `revisionNumber`, void the invoice. |
| `network-drop` | A create whose response is lost on the first attempt. The SDK retries with the same idempotency key and exactly one invoice exists afterwards. |
| `sync-customers` | Auto-pagination over every customer, 10 per page. `--slow` waits past the cursor idle window to show `CursorExpiredError` with its progress fields and how to resume from an `updatedAfter` watermark. |
| `async-webhooks` | An async-mode create returning a request handle, plus a small webhook receiver that verifies Standard Webhooks signatures with the SDK helper. |
| `error-handling` | Typed errors: `code`, `userFacingMessage`, `fixes`, `docsUrl` and `requestId`. |

## Packages

The examples install the released SDKs from their public registries, at version **0.1.1**:

| Language | Folder | Package |
| --- | --- | --- |
| Node.js / TypeScript | [node](node/README.md) | [`@desktopaccountingapi/quickbooks-desktop`](https://www.npmjs.com/package/@desktopaccountingapi/quickbooks-desktop) on npm |
| Python | [python](python/README.md) | [`desktopaccountingapi-quickbooks-desktop`](https://pypi.org/project/desktopaccountingapi-quickbooks-desktop/) on PyPI |
| C# / .NET | [dotnet](dotnet/README.md) | [`DesktopAccountingAPI.QuickBooksDesktop`](https://www.nuget.org/packages/DesktopAccountingAPI.QuickBooksDesktop) on NuGet |
| Java | [java](java/README.md) | [`com.desktopaccountingapi:quickbooks-desktop`](https://central.sonatype.com/artifact/com.desktopaccountingapi/quickbooks-desktop) on Maven Central |

Each SDK's README documents its full API: [Node.js](https://github.com/DesktopAccountingAPI/quickbooks-desktop-node), [Python](https://github.com/DesktopAccountingAPI/quickbooks-desktop-python), [.NET](https://github.com/DesktopAccountingAPI/quickbooks-desktop-dotnet), [Java](https://github.com/DesktopAccountingAPI/quickbooks-desktop-java).

## Setup

You need:

1. A secret key from the [dashboard](https://www.desktopaccountingapi.com/dashboard) (**API keys**). Use a test project and its `sk_test_...` key.
2. An end user whose QuickBooks Desktop company file is connected through the Web Connector, and QuickBooks open on that computer. Use a sample or test company file: the examples create and void records.
3. The toolchains pinned in `mise.toml` (Node.js, Python with uv, .NET 8, Java 21 with Maven). With [mise](https://mise.jdx.dev): `mise install`. Other installs of the supported versions work too.

Configuration comes from environment variables:

| Variable | Meaning |
| --- | --- |
| `DAAPI_SECRET_KEY` | Secret key. Required. Keep it out of source control. |
| `DAAPI_END_USER_ID` | End user (`eu_...`) whose company file the examples use. Required. |
| `DAAPI_BASE_URL` | API base URL. Defaults to `https://api.desktopaccountingapi.com`. |
| `DAAPI_WEBHOOK_SECRET` | Webhook endpoint signing secret (`whsec_...`), for `async-webhooks`. |
| `PORT` | Port for the webhook receiver. Defaults to 8080. |
| `DAAPI_CUSTOMER_ID`, `DAAPI_ITEM_ID` | Customer and service item the write examples use. Defaults to the first active ones. |

## Run the quickstart

```sh
export DAAPI_SECRET_KEY="sk_test_..." DAAPI_END_USER_ID="eu_..."

# Pick your language:
(cd node && npm install && node quickstart.ts)                                   # Node.js 22.18+
(cd python && python -m venv .venv && .venv/bin/pip install -r requirements.txt && .venv/bin/python quickstart.py)
(cd dotnet && dotnet run --project quickstart -p:UseNuGetPackage=true)
(cd java && mvn -q compile exec:java -Dexec.mainClass=examples.Quickstart)
```

Each language folder's README lists every example's run command and options.

## Checks

- `mise run check:registry` builds or type-checks every example in every language against the released packages from npm, PyPI, NuGet and Maven Central, and runs the webhook self-tests. CI runs it on every push and weekly.
- `mise run check` does the same against SDK repository checkouts in `SDK_REPOS_DIR` (default `./sdk`), which is how SDK changes are tested before a release.

Neither calls the API. The examples reach a real QuickBooks company file only when you run them.

## Support

- [Documentation](https://www.desktopaccountingapi.com/docs/), [API reference](https://www.desktopaccountingapi.com/docs/api/reference/) and [status page](https://status.desktopaccountingapi.com).
- Problems with an example: [GitHub issues](https://github.com/DesktopAccountingAPI/examples/issues). Questions about your account or a connection: [contact us](https://www.desktopaccountingapi.com/contact).

## License

MIT. See [LICENSE](LICENSE).

QuickBooks is a registered trademark of Intuit Inc. Desktop Accounting API is an independent product and is not affiliated with, endorsed by, or approved by Intuit Inc.
