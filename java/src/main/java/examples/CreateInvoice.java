package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.ApiResponse;
import com.desktopaccountingapi.quickbooksdesktop.core.Page;
import com.desktopaccountingapi.quickbooksdesktop.core.RequestOptions;
import com.desktopaccountingapi.quickbooksdesktop.models.Customer;
import com.desktopaccountingapi.quickbooksdesktop.models.CustomerCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.Invoice;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceLineCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceUpdateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceVoided;
import com.desktopaccountingapi.quickbooksdesktop.models.ServiceItem;
import com.desktopaccountingapi.quickbooksdesktop.models.ServiceItemListParams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Creates a uniquely named customer and an invoice with a fixed idempotency key derived from a run
 * ID, updates the memo and voids the invoice.
 *
 * <p>Arguments: [runId]. Uses DAAPI_ITEM_ID as the line item, else the first service item.
 */
public final class CreateInvoice {
    private CreateInvoice() {}

    public static void main(String[] args) {
        DesktopAccountingApiClient client = Env.client();
        String runId = Env.runId(args);
        System.out.println("Run ID: " + runId);

        Customer customer = client.qbd().customers().create(
            new CustomerCreateInput("SDK Example Customer " + runId).companyName("Example Co " + runId),
            RequestOptions.idempotent("example-customer-" + runId));
        System.out.println("Customer " + customer.id() + " " + customer.fullName());

        String itemId = System.getenv("DAAPI_ITEM_ID");
        if (itemId == null || itemId.isEmpty()) {
            Page<ServiceItem> items = client.qbd().serviceItems().list(new ServiceItemListParams().limit(1)).firstPage();
            if (items.data().isEmpty()) {
                System.err.println("No service item found; set DAAPI_ITEM_ID.");
                System.exit(2);
            }
            itemId = items.data().get(0).id();
        }

        InvoiceCreateInput input = new InvoiceCreateInput(customer.id())
            .transactionDate(LocalDate.now())
            .memo("Created by the Java SDK example, run " + runId)
            .lines(List.of(new InvoiceLineCreateInput().itemId(itemId).quantity(2).rate(new BigDecimal("52.75"))));
        // The same key on every run with this run ID: repeating the call returns the stored invoice
        // instead of creating a second one.
        RequestOptions once = RequestOptions.idempotent("example-invoice-" + runId);
        ApiResponse<Invoice> created = client.qbd().invoices().createWithResponse(input, once);
        Invoice invoice = created.data();
        System.out.println("Invoice " + invoice.id() + " ref " + invoice.refNumber() + " subtotal " + invoice.subtotal()
            + " (request " + created.requestId() + ")");
        ApiResponse<Invoice> again = client.qbd().invoices().createWithResponse(input, once);
        System.out.println("Repeated create returned " + again.data().id() + ", replayed="
            + again.headers().get("Daapi-Idempotent-Replayed"));

        Invoice updated = client.qbd().invoices().update(invoice.id(),
            new InvoiceUpdateInput(invoice.revisionNumber()).memo("Updated memo, run " + runId));
        System.out.println("Updated memo: " + updated.memo() + " (revision " + updated.revisionNumber() + ")");

        InvoiceVoided voided = client.qbd().invoices().voidTransaction(invoice.id());
        System.out.println("Voided " + voided.id() + ": " + voided.voided());
    }
}
