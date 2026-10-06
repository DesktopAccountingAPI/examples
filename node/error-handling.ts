// error-handling: trigger typed errors and print the fields developers act on.
//
//   node error-handling.ts

import { DaapiError, DesktopAccountingApi, ErrorCode, IntegrationError, InvalidRequestError } from "@desktopaccountingapi/quickbooks-desktop";
import { describeError, makeClient } from "./common.ts";

const client = makeClient();

console.log("1. Unknown customer ID (QuickBooks rejects it):");
try {
  await client.qbd.customers.retrieve("80000099-1000000000");
  console.log("  unexpectedly found a customer");
} catch (err) {
  describeError(err);
  if (err instanceof IntegrationError && err.code === ErrorCode.QBD_OBJECT_NOT_FOUND) console.log("  -> handled as 'not found'");
}

console.log("\n2. Invalid request (a decimal field with a non-decimal value):");
try {
  await client.qbd.invoices.create({ customerId: "80000099-1000000000", lines: [{ rate: "ten dollars" }] });
} catch (err) {
  describeError(err);
  if (err instanceof InvalidRequestError) console.log(`  -> fix the field at ${err.param ?? "(no param)"}`);
}

console.log("\n3. Malformed API key (rejected locally, before any request):");
try {
  new DesktopAccountingApi({ apiKey: "sk_test_0000000000000000000000000000000000000000" });
} catch (err) {
  describeError(err);
}

console.log("\n4. QuickBooks operation without an end user (rejected locally):");
try {
  await new DesktopAccountingApi({ endUserId: null }).qbd.healthCheck();
} catch (err) {
  describeError(err);
  if (err instanceof DaapiError) console.log("  -> use client.forEndUser(id) or pass { endUserId }");
}
