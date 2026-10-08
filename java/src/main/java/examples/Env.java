package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.Transport;
import com.desktopaccountingapi.quickbooksdesktop.errors.ApiException;
import com.desktopaccountingapi.quickbooksdesktop.errors.DaapiException;
import com.desktopaccountingapi.quickbooksdesktop.models.ActiveStatus;
import com.desktopaccountingapi.quickbooksdesktop.models.Customer;
import com.desktopaccountingapi.quickbooksdesktop.models.CustomerListParams;
import com.desktopaccountingapi.quickbooksdesktop.models.ServiceItem;
import com.desktopaccountingapi.quickbooksdesktop.models.ServiceItemListParams;
import java.util.List;

/** Shared configuration for the examples: everything comes from environment variables. */
final class Env {
    private Env() {}

    /** The body of an example's main method. */
    interface Body {
        void run() throws Exception;
    }

    /**
     * Runs an example. An API error prints its code, message, user-facing message, fixes and
     * request ID (quote it to support) and exits 1, instead of a bare stack trace without the ID.
     */
    static void run(Body body) {
        try {
            body.run();
        } catch (ApiException e) {
            System.err.println(e.getClass().getSimpleName() + " " + e.status() + " " + e.code() + ": " + e.getMessage());
            if (e.userFacingMessage() != null) System.err.println("  user-facing: " + e.userFacingMessage());
            e.fixes().forEach(f -> System.err.println("  fix (" + f.actor() + "): " + f.action()));
            if (e.docsUrl() != null) System.err.println("  docs: " + e.docsUrl());
            System.err.println("  request ID: " + e.requestId());
            System.exit(1);
        } catch (DaapiException e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e);
            System.exit(1);
        }
    }

    static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            System.err.println("Set " + name + " (see README.md).");
            System.exit(2);
        }
        return v;
    }

    static String optional(String name) {
        String v = System.getenv(name);
        return v == null || v.isEmpty() ? null : v;
    }

    /** Client for DAAPI_SECRET_KEY / DAAPI_BASE_URL, bound to DAAPI_END_USER_ID. */
    static DesktopAccountingApiClient client() {
        return builder().build();
    }

    static DesktopAccountingApiClient client(Transport transport) {
        return builder().transport(transport).build();
    }

    private static DesktopAccountingApiClient.Builder builder() {
        require("DAAPI_SECRET_KEY");
        return DesktopAccountingApiClient.builder().endUserId(require("DAAPI_END_USER_ID"));
    }

    /** The customer for invoices: DAAPI_CUSTOMER_ID, else the first active customer. */
    static String customerId(DesktopAccountingApiClient client) {
        String id = optional("DAAPI_CUSTOMER_ID");
        if (id != null) return id;
        List<Customer> customers = client.qbd().customers().list(new CustomerListParams().limit(1).status(ActiveStatus.ACTIVE)).firstPage().data();
        if (customers.isEmpty()) {
            System.err.println("The company file needs an active customer (or set DAAPI_CUSTOMER_ID).");
            System.exit(2);
        }
        return customers.get(0).id();
    }

    /** The service item for invoice lines: DAAPI_ITEM_ID, else the first active service item. */
    static String itemId(DesktopAccountingApiClient client) {
        String id = optional("DAAPI_ITEM_ID");
        if (id != null) return id;
        List<ServiceItem> items = client.qbd().serviceItems().list(new ServiceItemListParams().limit(1).status(ActiveStatus.ACTIVE)).firstPage().data();
        if (items.isEmpty()) {
            System.err.println("The company file needs an active service item (or set DAAPI_ITEM_ID).");
            System.exit(2);
        }
        return items.get(0).id();
    }

    /** A short run ID: the first argument, else RUN_ID, else the current time in seconds. */
    static String runId(String[] args) {
        for (String a : args) if (!a.startsWith("--")) return a;
        String env = System.getenv("RUN_ID");
        return env != null && !env.isEmpty() ? env : Long.toString(System.currentTimeMillis() / 1000);
    }
}
