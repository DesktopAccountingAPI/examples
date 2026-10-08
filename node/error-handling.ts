// error-handling: trigger typed errors and print the fields developers act on.
//
//   node error-handling.ts
//
// Exits 1 when a step does not produce the error it demonstrates (for example a connection or
// setup error instead of QBD_OBJECT_NOT_FOUND), so a misconfigured run never looks like a pass.

import { ApiError, DaapiError, DesktopAccountingApi, ErrorCode, IntegrationError, InvalidRequestError } from "@desktopaccountingapi/quickbooks-desktop";
import { describeError, makeClient } from "./common.ts";

const client = makeClient();

function unexpected(what: string): void {
  console.log(`  -> unexpected: ${what}`);
  process.exitCode = 1;
}

console.log("1. Unknown customer ID (QuickBooks rejects it):");
try {
  await client.qbd.customers.retrieve("80000099-1000000000");
  unexpected("found a customer");
} catch (err) {
  describeError(err);
  if (err instanceof IntegrationError && err.code === ErrorCode.QBD_OBJECT_NOT_FOUND) console.log("  -> handled as 'not found'");
  else unexpected("expected QBD_OBJECT_NOT_FOUND");
}

console.log("\n2. Invalid request (a decimal field with a non-decimal value):");
try {
  await client.qbd.invoices.create({ customerId: "80000099-1000000000", lines: [{ rate: "ten dollars" }] });
  unexpected("the invoice was created");
} catch (err) {
  describeError(err);
  if (err instanceof InvalidRequestError) console.log(`  -> fix the field at ${err.param ?? "(no param)"}`);
  else unexpected("expected an InvalidRequestError");
}

console.log("\n3. Malformed API key (rejected locally, before any request):");
try {
  new DesktopAccountingApi({ apiKey: "sk_test_0000000000000000000000000000000000000000" });
  unexpected("the key was accepted");
} catch (err) {
  describeError(err);
  if (!(err instanceof DaapiError) || err instanceof ApiError) unexpected("expected a local DaapiError");
}

console.log("\n4. QuickBooks operation without an end user (rejected locally):");
try {
  await new DesktopAccountingApi({ endUserId: null }).qbd.healthCheck();
  unexpected("the call succeeded");
} catch (err) {
  describeError(err);
  if (err instanceof DaapiError && !(err instanceof ApiError)) console.log("  -> use client.forEndUser(id) or pass { endUserId }");
  else unexpected("expected a local DaapiError");
}

if (process.exitCode) console.log("\nAt least one step did not produce the error it demonstrates.");
