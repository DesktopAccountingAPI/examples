# .NET examples

Console programs that use the [Desktop Accounting API .NET SDK](https://github.com/DesktopAccountingAPI/quickbooks-desktop-dotnet) against a real QuickBooks Desktop connection.

| Folder | What it does |
| --- | --- |
| `quickstart` | Health check, then the first 10 invoices. |
| `create-invoice` | Creates a uniquely named customer and an invoice with an idempotency key derived from the run ID, reads the invoice's current revision and updates the memo, voids the invoice. |
| `network-drop` | Throws a network error after the first create reached the API. The SDK retries with the same `Idempotency-Key`; the program then checks that exactly one invoice exists and voids it. |
| `sync-customers` | Reads every customer 10 per page and checks for duplicate IDs. `--slow` sleeps past the cursor idle window to show `CursorExpiredException` with its request ID, then restarts the list and skips the IDs it already has. |
| `async-webhooks` | Queues an invoice create in async mode, waits on the `RequestHandle` and voids the invoice. `--receive` runs a webhook receiver; `--self-test` signs and verifies a sample event locally. |
| `error-handling` | Prints the fields of an `IntegrationException` for an unknown customer, and the local errors for a malformed key and a missing end user. Exits 1 if a step gets a different error than the one it demonstrates. |

## Prerequisites

- .NET 8 SDK (`mise install` at the repository root installs the pinned version).
- Environment variables:

| Variable | Used by |
| --- | --- |
| `DAAPI_SECRET_KEY` | all (`sk_test_...` for a test project) |
| `DAAPI_END_USER_ID` | all (`eu_...`) |
| `DAAPI_BASE_URL` | optional; staging is `https://api-staging.desktopaccountingapi.com` |
| `DAAPI_CUSTOMER_ID`, `DAAPI_ITEM_ID` | optional; the customer and service item the write examples use (default: the first active ones) |
| `DAAPI_WEBHOOK_SECRET` | `async-webhooks --receive` and `--self-test` |
| `PORT` | `async-webhooks --receive` (default 8080) |
| `SLOW_PAGE_SECONDS` | `sync-customers --slow` (default 15) |
| `DAAPI_DEBUG` | optional; any value prints the SDK's log lines to stderr |

The examples create, update and void invoices; `create-invoice` also creates one customer per run. Use a sample or test company file.

## SDK source or NuGet package

`Directory.Build.props` decides how the examples reference the SDK:

- Default: the released `DesktopAccountingAPI.QuickBooksDesktop` package from NuGet, version 0.5.3. `-p:DaapiPackageVersion=<version>` picks another version.
- `-p:UseNuGetPackage=false` (for SDK development): a project reference to `$(SDK_REPOS_DIR)/quickbooks-desktop-dotnet/src/DesktopAccountingApi.QuickBooksDesktop/DesktopAccountingApi.QuickBooksDesktop.csproj`. Without `SDK_REPOS_DIR` the path is `../sdk/quickbooks-desktop-dotnet` relative to this folder. `-p:SdkReposDir=/path` overrides both.

## Build

```sh
cd dotnet
dotnet build examples.sln -c Release                              # against the NuGet package
dotnet build examples.sln -c Release -p:UseNuGetPackage=false     # against ../sdk or $SDK_REPOS_DIR
```

## Run

```sh
cd dotnet
export DAAPI_SECRET_KEY=sk_test_... DAAPI_END_USER_ID=eu_...

dotnet run --project quickstart
dotnet run --project create-invoice
dotnet run --project create-invoice -- --run-id 20261005-120000    # repeat a run: the creates are replayed
dotnet run --project network-drop
dotnet run --project sync-customers
dotnet run --project sync-customers -- --slow
dotnet run --project async-webhooks
DAAPI_WEBHOOK_SECRET=whsec_... dotnet run --project async-webhooks -- --self-test
DAAPI_WEBHOOK_SECRET=whsec_... PORT=8080 dotnet run --project async-webhooks -- --receive
dotnet run --project error-handling
```

On Windows PowerShell set variables with `$env:DAAPI_SECRET_KEY = "sk_test_..."`.
