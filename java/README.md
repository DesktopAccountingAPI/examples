# Java examples

Runnable programs for the [Desktop Accounting API Java SDK](https://github.com/DesktopAccountingAPI/quickbooks-desktop-java) (`com.desktopaccountingapi:quickbooks-desktop:0.5.1`).

| Class | What it does |
| --- | --- |
| `examples.Quickstart` | Health check, then the first 10 invoices. |
| `examples.CreateInvoice` | Creates a uniquely named customer and an invoice with a fixed idempotency key (`example-invoice-<runId>`), repeats the create to show the replay, reads the current revision and updates the memo, then voids the invoice (`example-void-<runId>`). Re-running with the same run ID replays instead of duplicating. Arguments: `[runId]`. Uses `DAAPI_ITEM_ID` as the line item, else the first active service item. |
| `examples.NetworkDrop` | A transport wrapper sends an invoice create to the API and then throws a network error instead of returning the response. The SDK retries with the same `Idempotency-Key`; the program checks that exactly one invoice with the run's reference number exists, then voids it. Arguments: `[runId]`. |
| `examples.SyncCustomers` | Iterates every customer 10 per page and checks for duplicate IDs. `--slow [seconds]` (default 15) waits after each page to provoke `CursorExpiredException`, prints `itemsYielded`, `pagesServed`, `lastId`, `lastUpdatedAt` and `requestId`, then restarts the list and skips the IDs it already has. |
| `examples.AsyncWebhooks` | Enqueues an invoice create in async mode, prints the request handle, waits for the result and voids the invoice. `--receive` runs a webhook receiver (JDK `HttpServer`, port `PORT`, default 8080) that verifies signatures with `DAAPI_WEBHOOK_SECRET`. `--self-test` starts the receiver, posts a locally signed sample event to it and exits; it needs no API key. |
| `examples.ErrorHandling` | An unknown customer ID (`IntegrationException`, `QBD_*` code), a malformed key and a missing end user (`DaapiException`, raised locally). Prints `code`, `userFacingMessage`, `fixes`, `docsUrl` and `requestId`, and exits 1 if a step gets a different error than the one it demonstrates. |

## Prerequisites

- Java 11 or later and Maven 3.9 (the examples repository's `mise.toml` pins both: `mise install`).
- Environment: `DAAPI_SECRET_KEY`, `DAAPI_END_USER_ID`, optional `DAAPI_BASE_URL` (staging: `https://api-staging.desktopaccountingapi.com`), `DAAPI_WEBHOOK_SECRET` for webhooks. Optional: `DAAPI_ITEM_ID`, `DAAPI_CUSTOMER_ID`.

## The SDK

`pom.xml` depends on `com.desktopaccountingapi:quickbooks-desktop:0.5.1`. Maven resolves it from Maven Central, or from your local repository (`~/.m2`) after you install the SDK checkout next to this repository:

```sh
SDK_REPOS_DIR="${SDK_REPOS_DIR:-$PWD/sdk}"          # from the examples repository root
mvn -B -ntp -f "$SDK_REPOS_DIR/quickbooks-desktop-java/pom.xml" install -DskipTests
```

## Build

From the examples repository root:

```sh
mvn -B -ntp -f java/pom.xml compile
```

## Run

From the `java/` directory:

```sh
mvn -q compile exec:java -Dexec.mainClass=examples.Quickstart
mvn -q compile exec:java -Dexec.mainClass=examples.CreateInvoice -Dexec.args="run42"
mvn -q compile exec:java -Dexec.mainClass=examples.NetworkDrop
mvn -q compile exec:java -Dexec.mainClass=examples.SyncCustomers
mvn -q compile exec:java -Dexec.mainClass=examples.SyncCustomers -Dexec.args="--slow 15"
mvn -q compile exec:java -Dexec.mainClass=examples.AsyncWebhooks
mvn -q compile exec:java -Dexec.mainClass=examples.AsyncWebhooks -Dexec.args="--receive"
mvn -q compile exec:java -Dexec.mainClass=examples.AsyncWebhooks -Dexec.args="--self-test"
mvn -q compile exec:java -Dexec.mainClass=examples.ErrorHandling
```

The examples create invoices and void every invoice they create; `CreateInvoice` also creates one customer per run. An API error that an example does not handle prints its code, message and request ID and exits 1. Use a test company file.
