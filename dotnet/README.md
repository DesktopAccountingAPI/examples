# .NET examples

Console programs that use the [Desktop Accounting API .NET SDK](https://github.com/DesktopAccountingAPI/quickbooks-desktop-dotnet) against a real QuickBooks Desktop connection.

| Folder | What it does |
| --- | --- |
| `quickstart` | Health check, then the first 10 invoices. |
| `create-invoice` | Creates a uniquely named customer and an invoice with an idempotency key derived from the run ID, updates the memo, voids the invoice. |
| `network-drop` | Throws a network error after the first create reached the API. The SDK retries with the same `Idempotency-Key`; the program then checks that exactly one invoice exists. |
| `sync-customers` | Reads every customer 10 per page and checks for duplicate IDs. `--slow` sleeps past the cursor idle window to show `CursorExpiredException` and resumes from an `updatedAfter` watermark. |
| `async-webhooks` | Queues an invoice create in async mode and waits on the `RequestHandle`. `--receive` runs a webhook receiver; `--self-test` signs and verifies a sample event locally. |
| `error-handling` | Prints the fields of an `IntegrationException` for an unknown customer, and the local errors for a malformed key and a missing end user. |

## Prerequisites

- .NET 8 SDK (`mise install` at the repository root installs the pinned version).
- The SDK source in `../sdk/quickbooks-desktop-dotnet` (see the repository README), or `SDK_REPOS_DIR` pointing at the directory that contains `quickbooks-desktop-dotnet`.
- Environment variables:

| Variable | Used by |
| --- | --- |
| `DAAPI_SECRET_KEY` | all (`sk_test_...` for a test project) |
| `DAAPI_END_USER_ID` | all (`eu_...`) |
| `DAAPI_BASE_URL` | optional; staging is `https://api-staging.desktopaccountingapi.com` |
| `DAAPI_WEBHOOK_SECRET` | `async-webhooks --receive` and `--self-test` |
| `PORT` | `async-webhooks --receive` (default 8080) |
| `SLOW_PAGE_SECONDS` | `sync-customers --slow` (default 15) |
| `DAAPI_DEBUG` | optional; any value prints the SDK's log lines to stderr |

The examples create, update and void records. Use a sample or test company file.

## SDK source or NuGet package

`Directory.Build.props` decides how the examples reference the SDK:

- Default: a project reference to `$(SDK_REPOS_DIR)/quickbooks-desktop-dotnet/src/DesktopAccountingApi.QuickBooksDesktop/DesktopAccountingApi.QuickBooksDesktop.csproj`. Without `SDK_REPOS_DIR` the path is `../sdk/quickbooks-desktop-dotnet` relative to this folder. `-p:SdkReposDir=/path` overrides both.
- `-p:UseNuGetPackage=true`: the `DesktopAccountingAPI.QuickBooksDesktop` package from NuGet (version `-p:DaapiPackageVersion=0.1.0`).

## Build

```sh
cd dotnet
dotnet build examples.sln -c Release                              # against ../sdk or $SDK_REPOS_DIR
dotnet build examples.sln -c Release -p:UseNuGetPackage=true      # against the NuGet package
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
