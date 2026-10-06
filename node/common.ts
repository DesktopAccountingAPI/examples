// Shared helpers for the examples: configuration from the environment and output formatting.

import { ApiError, DaapiError, DesktopAccountingApi, type ClientOptions } from "@desktopaccountingapi/quickbooks-desktop";

export function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    console.error(`Set ${name}. See README.md for the required environment variables.`);
    process.exit(2);
  }
  return value;
}

/** Client for the end user in DAAPI_END_USER_ID. The SDK reads DAAPI_SECRET_KEY and DAAPI_BASE_URL itself. */
export function makeClient(options: ClientOptions = {}): DesktopAccountingApi {
  requireEnv("DAAPI_SECRET_KEY");
  return new DesktopAccountingApi(options).forEndUser(requireEnv("DAAPI_END_USER_ID"));
}

/** Short ID that makes names and idempotency keys unique per run. Set RUN_ID to repeat a run. */
export function runId(): string {
  return process.env["RUN_ID"] ?? Date.now().toString(36).slice(-7);
}

/** A customer and a service item to put on invoices: DAAPI_CUSTOMER_ID / DAAPI_ITEM_ID, else the first active ones. */
export async function invoiceParties(client: DesktopAccountingApi): Promise<{ customerId: string; itemId: string }> {
  let customerId = process.env["DAAPI_CUSTOMER_ID"];
  let itemId = process.env["DAAPI_ITEM_ID"];
  if (!customerId) {
    const page = await client.qbd.customers.list({ limit: 1, status: "active" });
    customerId = page.data[0]?.id;
  }
  if (!itemId) {
    const page = await client.qbd.serviceItems.list({ limit: 1, status: "active" });
    itemId = page.data[0]?.id;
  }
  if (!customerId || !itemId) {
    console.error("The company file needs at least one active customer and one active service item (or set DAAPI_CUSTOMER_ID and DAAPI_ITEM_ID).");
    process.exit(2);
  }
  return { customerId, itemId };
}

/** Prints the fields developers act on. */
export function describeError(err: unknown): void {
  if (err instanceof ApiError) {
    console.log(`  class:             ${err.constructor.name}`);
    console.log(`  HTTP status:       ${err.status ?? "n/a"}`);
    console.log(`  type / code:       ${err.type} / ${err.code}`);
    console.log(`  message:           ${err.message}`);
    console.log(`  userFacingMessage: ${err.userFacingMessage}`);
    console.log(`  integrationCode:   ${err.integrationCode ?? "-"}`);
    console.log(`  outcome:           ${err.outcome}`);
    console.log(`  requestId:         ${err.requestId}`);
    console.log(`  docsUrl:           ${err.docsUrl}`);
    for (const fix of err.fixes) console.log(`  fix (${fix.actor}): ${fix.action}`);
  } else if (err instanceof DaapiError) {
    console.log(`  class:   ${err.constructor.name} (raised locally, nothing was sent)`);
    console.log(`  message: ${err.message}`);
  } else {
    console.log(`  unexpected: ${String(err)}`);
  }
}
