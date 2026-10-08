package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.JavaHttpTransport;
import com.desktopaccountingapi.quickbooksdesktop.core.Transport;
import com.desktopaccountingapi.quickbooksdesktop.models.Invoice;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceLineCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceListParams;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Loses the response of an invoice create on purpose: the transport sends the request to the real
 * API, then throws a network error instead of returning the response. The SDK retries with the same
 * Idempotency-Key, the API replays the stored result, and exactly one invoice exists afterwards. The
 * invoice is voided at the end.
 *
 * <p>Arguments: [runId]. Uses DAAPI_CUSTOMER_ID / DAAPI_ITEM_ID, else the first active ones.
 */
public final class NetworkDrop {
    private NetworkDrop() {}

    /** Delegates to the real transport and drops the first response of an idempotent write. */
    static final class DropFirstResponse implements Transport {
        private final Transport real = new JavaHttpTransport();
        final List<String> keys = new ArrayList<>();
        boolean armed;
        private boolean dropped;

        @Override
        public Response send(Request request) throws IOException, InterruptedException {
            Response response = real.send(request);
            String key = request.headers().get("Idempotency-Key");
            if (!armed || key == null) return response;
            keys.add(key);
            if (!dropped) {
                dropped = true;
                System.out.println("  attempt 1: the API answered " + response.status() + ", dropping the response (simulated network failure)");
                throw new IOException("simulated connection reset after the request was sent");
            }
            System.out.println("  attempt " + keys.size() + ": " + response.status()
                + (response.headers().get("Daapi-Idempotent-Replayed") != null ? " (replayed)" : ""));
            return response;
        }
    }

    public static void main(String[] args) {
        Env.run(() -> {
            String runId = Env.runId(args);
            DropFirstResponse transport = new DropFirstResponse();
            DesktopAccountingApiClient client = Env.client(transport);
            // A reference number unique to this run, to count the invoices the create produced.
            String refNumber = "ND" + runId.substring(Math.max(0, runId.length() - 9));
            InvoiceCreateInput input = new InvoiceCreateInput(Env.customerId(client))
                .refNumber(refNumber)
                .memo("network-drop example, run " + runId)
                .lines(List.of(new InvoiceLineCreateInput().itemId(Env.itemId(client)).quantity(1).rate(new BigDecimal("10.00"))));

            System.out.println("Creating invoice " + refNumber);
            transport.armed = true;
            Invoice invoice = client.qbd().invoices().create(input);
            transport.armed = false;
            System.out.println("Created " + invoice.id());
            System.out.println("Idempotency-Key per attempt: " + transport.keys);
            if (transport.keys.size() < 2 || !transport.keys.stream().allMatch(transport.keys.get(0)::equals)) {
                throw new IllegalStateException("expected a retry with the same Idempotency-Key");
            }

            List<Invoice> matches = client.qbd().invoices().list(new InvoiceListParams().refNumbers(List.of(refNumber))).listAll();
            System.out.println("Invoices with ref " + refNumber + ": " + matches.size());
            if (matches.size() != 1) throw new IllegalStateException("expected exactly one invoice, found " + matches.size());

            client.qbd().invoices().voidTransaction(invoice.id());
            System.out.println("Voided " + invoice.id() + ". The dropped response was retried safely; exactly one invoice was created.");
        });
    }
}
