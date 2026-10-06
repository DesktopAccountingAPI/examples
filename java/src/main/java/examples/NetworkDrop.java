package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.JavaHttpTransport;
import com.desktopaccountingapi.quickbooksdesktop.core.Transport;
import com.desktopaccountingapi.quickbooksdesktop.models.Customer;
import com.desktopaccountingapi.quickbooksdesktop.models.CustomerCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.CustomerListParams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loses the response of a create on purpose: the transport sends the request to the real API, then
 * throws a network error instead of returning the response. The SDK retries with the same
 * Idempotency-Key, the API replays the stored result, and exactly one customer exists afterwards.
 *
 * <p>Arguments: [runId].
 */
public final class NetworkDrop {
    private NetworkDrop() {}

    /** Delegates to the real transport and drops the first response. */
    static final class DropFirstResponse implements Transport {
        private final Transport real = new JavaHttpTransport();
        final List<String> keys = new ArrayList<>();
        private boolean dropped;

        @Override
        public Response send(Request request) throws IOException, InterruptedException {
            Response response = real.send(request);
            String key = request.headers().get("Idempotency-Key");
            if (key != null) keys.add(key);
            if (key != null && !dropped) {
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
        String runId = Env.runId(args);
        String name = "SDK Network Drop " + runId;
        DropFirstResponse transport = new DropFirstResponse();
        DesktopAccountingApiClient client = Env.client(transport);

        System.out.println("Creating customer \"" + name + "\"");
        Customer customer = client.qbd().customers().create(new CustomerCreateInput(name));
        System.out.println("Created " + customer.id());
        System.out.println("Idempotency-Key per attempt: " + transport.keys);
        if (transport.keys.size() < 2 || !transport.keys.stream().allMatch(transport.keys.get(0)::equals)) {
            throw new IllegalStateException("expected a retry with the same Idempotency-Key");
        }

        List<Customer> matches = Env.client().qbd().customers().list(new CustomerListParams().fullNames(List.of(name))).listAll();
        System.out.println("Customers named \"" + name + "\": " + matches.size());
        if (matches.size() != 1) throw new IllegalStateException("expected exactly one customer, found " + matches.size());
        System.out.println("OK: the dropped response was retried safely; one record exists.");
    }
}
