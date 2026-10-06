package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.Page;
import com.desktopaccountingapi.quickbooksdesktop.errors.ApiException;
import com.desktopaccountingapi.quickbooksdesktop.models.HealthCheck;
import com.desktopaccountingapi.quickbooksdesktop.models.Invoice;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceListParams;

/** Health check of the end user's QuickBooks Desktop connection, then the first 10 invoices. */
public final class Quickstart {
    private Quickstart() {}

    public static void main(String[] args) {
        DesktopAccountingApiClient client = Env.client();
        try {
            HealthCheck health = client.qbd().healthCheck();
            System.out.println("Health check: " + health.status() + " (" + health.duration() + " ms round trip)");
            if (health.quickbooks() != null) {
                System.out.println("Company file: " + health.quickbooks().companyName() + ", " + health.quickbooks().product());
            }

            Page<Invoice> page = client.qbd().invoices().list(new InvoiceListParams().limit(10)).firstPage();
            System.out.println("First " + page.data().size() + " invoices:");
            for (Invoice invoice : page.data()) {
                System.out.printf("  %-20s %-12s %-10s %12s  %s%n", invoice.id(), invoice.refNumber(), invoice.transactionDate(),
                    invoice.subtotal(), invoice.customer() == null ? "" : invoice.customer().fullName());
            }
            if (page.hasMore()) System.out.println("  ... " + page.remainingCount() + " more");
        } catch (ApiException e) {
            System.err.println(e.code() + ": " + e.getMessage());
            System.err.println("For the end user: " + e.userFacingMessage());
            System.err.println("Request ID: " + e.requestId());
            System.exit(1);
        }
    }
}
