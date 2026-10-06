// network-drop: the first create attempt reaches the API, but its response is thrown away and the
// client sees a network error. The SDK retries with the same Idempotency-Key, the API replays the
// stored result, and exactly one invoice exists afterwards.
//
//   node network-drop.ts

import type { Fetch } from "@desktopaccountingapi/quickbooks-desktop";
import { invoiceParties, makeClient, runId } from "./common.ts";

let dropped = false;
const keysSent: string[] = [];

/** Sends every request for real; for the first POST it discards the response and fails like a dropped connection. */
const droppingFetch: Fetch = async (url, init) => {
  const response = await fetch(url, init);
  if (init.method === "POST" && url.includes("/invoices")) {
    keysSent.push(new Headers(init.headers).get("idempotency-key") ?? "(none)");
    if (!dropped) {
      dropped = true;
      await response.arrayBuffer();
      console.log(`Attempt 1: the API answered ${response.status}; dropping the response to simulate a lost connection`);
      throw new TypeError("simulated network failure after the request was sent");
    }
  }
  return response;
};

const client = makeClient({ fetch: droppingFetch, maxRetries: 2 });
const run = runId();
const refNumber = `ND${run}`.slice(0, 11);
const { customerId, itemId } = await invoiceParties(client);

const { data: invoice, response } = await client.qbd.invoices
  .create({ customerId, refNumber, memo: `network-drop example ${run}`, lines: [{ itemId, quantity: 1, rate: "10.00" }] })
  .withResponse();
console.log(`Attempt 2: ${response.status}, Daapi-Idempotent-Replayed: ${response.headers.get("daapi-idempotent-replayed") ?? "false"}`);
console.log(`Invoice ${invoice.id}, ref ${invoice.refNumber}`);
console.log(`Idempotency keys sent: ${keysSent.join(", ")}`);
if (keysSent.length !== 2 || keysSent[0] !== keysSent[1]) throw new Error("expected two attempts with the same Idempotency-Key");

const matches = await client.qbd.invoices.list({ refNumbers: [refNumber] }).listAll();
console.log(`Invoices with ref ${refNumber}: ${matches.length}`);
if (matches.length !== 1) throw new Error(`expected exactly one invoice, found ${matches.length}`);

await client.qbd.invoices.void(invoice.id);
console.log(`Voided ${invoice.id}. Exactly one invoice was created.`);
