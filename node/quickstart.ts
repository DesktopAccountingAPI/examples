// quickstart: health check of the end user's QuickBooks connection, then the first 10 invoices.
//
//   node quickstart.ts

import { makeClient } from "./common.ts";

const client = makeClient();

const health = await client.qbd.healthCheck();
console.log(`QuickBooks connection: ${health.status} in ${health.duration} ms`);
console.log(`  company: ${health.quickbooks.companyName ?? "(unknown)"}, product: ${health.quickbooks.product ?? "(unknown)"}, qbXML ${health.quickbooks.qbxmlVersion ?? "?"}`);

const page = await client.qbd.invoices.list({ limit: 10 });
console.log(`\nFirst ${page.data.length} invoices:`);
for (const invoice of page.data) {
  console.log(`  ${invoice.refNumber ?? "(no ref)"}  ${invoice.transactionDate ?? "          "}  ${invoice.customer?.fullName ?? ""}  subtotal ${invoice.subtotal ?? "0.00"}  id ${invoice.id}`);
}
console.log(page.hasMore ? `\n${page.remainingCount ?? "More"} more invoices not shown.` : "\nNo more invoices.");
