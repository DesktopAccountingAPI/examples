// sync-customers: auto-paginate every customer, 10 per page, and check there are no duplicates.
//
//   node sync-customers.ts
//   node sync-customers.ts --slow          # sleep SLOW_SECONDS (default 15) per page to let the cursor expire
//
// In --slow mode the cursor's idle window passes, the iteration throws CursorExpiredError with its
// progress fields, and the example resumes from an updatedAfter watermark, skipping IDs it already has.

import { setTimeout as sleep } from "node:timers/promises";
import { CursorExpiredError, type Customer } from "@desktopaccountingapi/quickbooks-desktop";
import { makeClient } from "./common.ts";

const client = makeClient();
const slow = process.argv.includes("--slow");
const pause = Number(process.env["SLOW_SECONDS"] ?? "15") * 1000;
const seen = new Map<string, Customer>();
let duplicates = 0;

function take(customer: Customer): void {
  if (seen.has(customer.id)) duplicates++;
  seen.set(customer.id, customer);
}

if (!slow) {
  for await (const customer of client.qbd.customers.list({ limit: 10 })) take(customer);
  console.log(`Customers: ${seen.size}, duplicate IDs: ${duplicates}`);
  if (duplicates > 0) process.exit(1);
} else {
  try {
    let pageNumber = 0;
    for await (const page of client.qbd.customers.list({ limit: 10 }).pages()) {
      pageNumber++;
      page.data.forEach(take);
      console.log(`Page ${pageNumber}: ${page.data.length} customers (cursor expires at ${page.cursorExpiresAt ?? "n/a"}); sleeping ${pause / 1000} s`);
      if (page.hasMore) await sleep(pause);
    }
    console.log(`Finished without expiry. Customers: ${seen.size}. Increase SLOW_SECONDS to see CursorExpiredError.`);
  } catch (err) {
    if (!(err instanceof CursorExpiredError)) throw err;
    console.log("CursorExpiredError:");
    console.log(`  reason:        ${err.reason}`);
    console.log(`  itemsYielded:  ${err.itemsYielded}`);
    console.log(`  pagesServed:   ${err.pagesServed}`);
    console.log(`  lastId:        ${err.lastId}`);
    console.log(`  lastUpdatedAt: ${err.lastUpdatedAt}`);
    console.log(`  requestId:     ${err.requestId}`);
    // Resume: everything changed at or after the watermark, skipping what we already have.
    const before = seen.size;
    const watermark = err.lastUpdatedAt ?? undefined;
    for await (const customer of client.qbd.customers.list({ limit: 10, ...(watermark ? { updatedAfter: watermark } : {}) })) {
      if (!seen.has(customer.id)) take(customer);
    }
    console.log(`Resumed from updatedAfter=${watermark ?? "(start)"}: ${seen.size - before} more customers, ${seen.size} in total.`);
  }
}
