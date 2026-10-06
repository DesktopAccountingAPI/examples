# Node.js examples

TypeScript programs that run directly with Node.js type stripping (no build step), using the [`@desktopaccountingapi/quickbooks-desktop`](https://github.com/DesktopAccountingAPI/quickbooks-desktop-node) SDK.

## Prerequisites

- Node.js 22.18 or later (type stripping on by default). The examples root `mise.toml` pins the version: `mise install`.
- The environment variables from the [examples README](../README.md#setup): `DAAPI_SECRET_KEY`, `DAAPI_END_USER_ID`, optionally `DAAPI_BASE_URL` (staging: `https://api-staging.desktopaccountingapi.com`), `DAAPI_WEBHOOK_SECRET` and `PORT`.
- For `create-invoice`, `network-drop` and `async-webhooks`: an active customer and an active service item in the company file. The examples use the first ones they find; set `DAAPI_CUSTOMER_ID` and `DAAPI_ITEM_ID` to choose.

## Use the SDK from npm

```sh
cd node
npm install
```

## Or use the SDK checked out next to the examples

The examples CI uses the SDK repository checked out at `$SDK_REPOS_DIR/quickbooks-desktop-node` (default `<examples root>/sdk/quickbooks-desktop-node`):

```sh
(cd "${SDK_REPOS_DIR:-../sdk}/quickbooks-desktop-node" && npm ci)   # installs and builds the SDK's dist/
node scripts/use-local-sdk.mjs                                        # links it into node_modules
```

`npm install` switches back to the npm package.

## Run

```sh
node quickstart.ts
node create-invoice.ts                 # RUN_ID=<id> repeats a run: the same idempotency keys replay instead of duplicating
node network-drop.ts
node sync-customers.ts
node sync-customers.ts --slow          # SLOW_SECONDS=15 per page by default
node async-webhooks.ts                 # async-mode create, waits on the request handle
node async-webhooks.ts --receive       # webhook receiver on PORT (default 8080)
node async-webhooks.ts --self-test     # signs and verifies a sample event locally; needs no API key
node error-handling.ts
```

`create-invoice`, `network-drop` and `async-webhooks` create records and void the invoices they create. Use a test company file.

## Check

```sh
node scripts/check.mjs
```

Links the local SDK checkout (see above), type-checks every example with the SDK's TypeScript in strict mode, and runs the webhook self-test. It makes no API calls.
