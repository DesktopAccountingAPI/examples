package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.RequestHandle;
import com.desktopaccountingapi.quickbooksdesktop.core.RequestOptions;
import com.desktopaccountingapi.quickbooksdesktop.errors.WebhookVerificationException;
import com.desktopaccountingapi.quickbooksdesktop.models.CustomerListParams;
import com.desktopaccountingapi.quickbooksdesktop.models.Invoice;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceLineCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.ServiceItemListParams;
import com.desktopaccountingapi.quickbooksdesktop.webhooks.WebhookEvent;
import com.desktopaccountingapi.quickbooksdesktop.webhooks.Webhooks;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Async mode and webhooks.
 *
 * <ul>
 *   <li>No flag: enqueues an invoice create ({@code Prefer: respond-async}) for the first customer
 *       and service item (or DAAPI_CUSTOMER_ID / DAAPI_ITEM_ID), prints the request handle, waits on
 *       it and prints the result.
 *   <li>{@code --receive}: runs a webhook receiver on PORT (default 8080) that verifies every
 *       delivery with DAAPI_WEBHOOK_SECRET and prints the event.
 *   <li>{@code --self-test}: starts the receiver, posts a locally signed sample event to it and exits.
 * </ul>
 */
public final class AsyncWebhooks {
    private AsyncWebhooks() {}

    public static void main(String[] args) throws Exception {
        boolean receive = false;
        boolean selfTest = false;
        for (String a : args) {
            if ("--receive".equals(a)) receive = true;
            if ("--self-test".equals(a)) selfTest = true;
        }
        if (receive || selfTest) {
            runReceiver(selfTest);
            return;
        }

        DesktopAccountingApiClient client = Env.client();
        String runId = Env.runId(args);
        String customerId = System.getenv("DAAPI_CUSTOMER_ID");
        if (customerId == null || customerId.isEmpty()) {
            customerId = client.qbd().customers().list(new CustomerListParams().limit(1)).firstPage().data().get(0).id();
        }
        String itemId = System.getenv("DAAPI_ITEM_ID");
        if (itemId == null || itemId.isEmpty()) {
            itemId = client.qbd().serviceItems().list(new ServiceItemListParams().limit(1)).firstPage().data().get(0).id();
        }
        InvoiceCreateInput input = new InvoiceCreateInput(customerId)
            .memo("Async example, run " + runId)
            .lines(List.of(new InvoiceLineCreateInput().itemId(itemId).quantity(1).rate(new BigDecimal("10.00"))));
        RequestHandle<Invoice> handle = client.qbd().invoices().enqueue().create(input,
            RequestOptions.builder().idempotencyKey("example-async-" + runId).queueTtl(Duration.ofHours(1)).build());
        System.out.println("Queued request " + handle.id() + " (status " + handle.request().status() + ")");
        System.out.println("Current status: " + handle.status().status());
        Invoice invoice = handle.await(Duration.ofMinutes(5));
        System.out.println("Request " + handle.id() + " succeeded: invoice " + invoice.id() + " ref " + invoice.refNumber() + " subtotal " + invoice.subtotal());
    }

    private static void runReceiver(boolean selfTest) throws Exception {
        String secret = Env.require("DAAPI_WEBHOOK_SECRET");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> handle(exchange, secret));
        server.start();
        System.out.println("Webhook receiver listening on http://localhost:" + port + "/");
        if (!selfTest) return; // keeps running until the process is stopped

        try {
            String body = "{\"id\":\"evt_selftest\",\"type\":\"request.succeeded\",\"timestamp\":\"" + Instant.now() + "\","
                + "\"projectId\":\"proj_selftest\",\"data\":{\"objectType\":\"request\",\"id\":\"req_selftest\",\"status\":\"succeeded\"}}";
            long ts = Instant.now().getEpochSecond();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                .header("webhook-id", "evt_selftest")
                .header("webhook-timestamp", Long.toString(ts))
                .header("webhook-signature", Webhooks.sign("evt_selftest", ts, body, secret))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> res = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Self-test delivery answered " + res.statusCode());
            if (res.statusCode() != 204) throw new IllegalStateException("self-test failed");
        } finally {
            server.stop(0);
        }
    }

    private static void handle(HttpExchange exchange, String secret) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int status;
        try {
            // HttpExchange headers are already a case-insensitive Map<String, List<String>>.
            WebhookEvent event = Webhooks.verify(body, exchange.getRequestHeaders(), secret);
            System.out.println("Verified " + event.type() + " " + event.id() + " data.id=" + event.data().get("id") + " status=" + event.data().get("status"));
            status = 204;
        } catch (WebhookVerificationException e) {
            System.out.println("Rejected delivery: " + e.getMessage());
            status = 400;
        }
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
