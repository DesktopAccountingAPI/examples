// async-webhooks: an async-mode create that returns a request handle, and a webhook receiver.
//
//   node async-webhooks.ts                 # enqueue an invoice create, wait on the handle, print the result
//   node async-webhooks.ts --receive       # webhook receiver on PORT (default 8080), verifies signatures
//   node async-webhooks.ts --self-test     # sign a sample event with DAAPI_WEBHOOK_SECRET and verify it (no API calls)

import { createServer } from "node:http";
import { signWebhook, verifyWebhook, WebhookEventType, WebhookVerificationError, type WebhookEvent } from "@desktopaccountingapi/quickbooks-desktop";
import { invoiceParties, makeClient, runId } from "./common.ts";

// Self-test secret when DAAPI_WEBHOOK_SECRET is not set (base64 of 32 bytes, Standard Webhooks format).
const SAMPLE_SECRET = "whsec_c2VsZi10ZXN0LXNlY3JldC1ub3QtZm9yLXByb2R1Y3Rpb24=";

function describe(event: WebhookEvent): string {
  const data = event.data;
  return `${event.type} ${event.id} at ${event.timestamp}: ${String(data["objectType"] ?? "")} ${String(data["id"] ?? "")} status=${String(data["status"] ?? "")}`;
}

if (process.argv.includes("--self-test")) {
  const secret = process.env["DAAPI_WEBHOOK_SECRET"] ?? SAMPLE_SECRET;
  const body = JSON.stringify({
    id: "evt_selftest",
    type: WebhookEventType.REQUEST_SUCCEEDED,
    timestamp: new Date().toISOString(),
    projectId: "proj_selftest",
    data: { objectType: "request", id: "req_selftest", status: "succeeded", operationId: "qbd.invoices.create" },
  });
  const headers = await signWebhook(body, secret, { id: "evt_selftest" });
  const event = await verifyWebhook(body, headers, secret);
  console.log(`Self-test verified: ${describe(event)}`);
  try {
    await verifyWebhook(body.replace("succeeded", "failed"), headers, secret);
    throw new Error("a tampered body was accepted");
  } catch (err) {
    if (!(err instanceof WebhookVerificationError)) throw err;
    console.log(`Self-test rejected a tampered body: ${err.message}`);
  }
} else if (process.argv.includes("--receive")) {
  const secret = process.env["DAAPI_WEBHOOK_SECRET"];
  if (!secret) {
    console.error("Set DAAPI_WEBHOOK_SECRET to the endpoint's signing secret (whsec_...).");
    process.exit(2);
  }
  const port = Number(process.env["PORT"] ?? "8080");
  createServer((req, res) => {
    const chunks: Buffer[] = [];
    req.on("data", (c: Buffer) => chunks.push(c));
    req.on("end", () => {
      // Verify the raw bytes exactly as received.
      verifyWebhook(Buffer.concat(chunks), req.headers, secret)
        .then((event) => {
          console.log(describe(event));
          res.writeHead(204).end();
        })
        .catch((err: unknown) => {
          console.log(`Rejected delivery: ${err instanceof Error ? err.message : String(err)}`);
          res.writeHead(err instanceof WebhookVerificationError ? 400 : 500).end();
        });
    });
  }).listen(port, () => console.log(`Webhook receiver listening on http://localhost:${port}/`));
} else {
  const client = makeClient();
  const run = runId();
  const { customerId, itemId } = await invoiceParties(client);
  const handle = await client.qbd.invoices.create(
    { customerId, memo: `async example ${run}`, lines: [{ itemId, quantity: 1, rate: "25.00" }] },
    { async: true, idempotencyKey: `examples-async-${run}`, queueTtl: 3600 },
  );
  console.log(`Accepted: request ${handle.id}, status ${handle.request.status}${handle.request.waitingReason ? ` (${handle.request.waitingReason})` : ""}`);
  const invoice = await handle.wait({ timeout: 300_000 });
  console.log(`Succeeded: invoice ${invoice.id}, ref ${invoice.refNumber}, subtotal ${invoice.subtotal}`);
  await client.qbd.invoices.void(invoice.id);
  console.log(`Voided ${invoice.id}.`);
}
