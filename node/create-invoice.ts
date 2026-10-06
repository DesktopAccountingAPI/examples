// create-invoice: create a uniquely named customer, create an invoice idempotently, update its
// memo, then void it. Re-running with the same RUN_ID replays the same customer and invoice
// instead of creating duplicates (fixed idempotency keys derived from the run ID).
//
//   node create-invoice.ts
//   RUN_ID=abc123 node create-invoice.ts

import { invoiceParties, makeClient, runId } from "./common.ts";

const client = makeClient();
const run = runId();
console.log(`Run ID ${run}`);

const customer = await client.qbd.customers.create({ name: `DAAPI Example ${run}` }, { idempotencyKey: `examples-customer-${run}` });
console.log(`Customer  ${customer.id}  ${customer.fullName}`);

const { itemId } = await invoiceParties(client);
const { data: invoice, response } = await client.qbd.invoices
  .create(
    {
      customerId: customer.id,
      transactionDate: new Date().toISOString().slice(0, 10),
      memo: `Created by the Node.js example, run ${run}`,
      lines: [{ itemId, quantity: 2, rate: "52.75", description: "Example line" }],
    },
    { idempotencyKey: `examples-invoice-${run}` },
  )
  .withResponse();
const replayed = response.headers.get("daapi-idempotent-replayed") === "true";
console.log(`Invoice   ${invoice.id}  ref ${invoice.refNumber}  subtotal ${invoice.subtotal}${replayed ? "  (replayed: this run ID was used before)" : ""}`);

const updated = await client.qbd.invoices.update(invoice.id, { revisionNumber: invoice.revisionNumber, memo: `Updated memo, run ${run}` });
console.log(`Updated   ${updated.id}  ref ${updated.refNumber}  memo "${updated.memo}"  revision ${updated.revisionNumber}`);

const voided = await client.qbd.invoices.void(invoice.id);
console.log(`Voided    ${voided.id}  ref ${voided.refNumber}  voided=${voided.voided}`);
