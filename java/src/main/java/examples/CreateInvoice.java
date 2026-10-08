package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.ApiResponse;
import com.desktopaccountingapi.quickbooksdesktop.core.RequestOptions;
import com.desktopaccountingapi.quickbooksdesktop.models.Customer;
import com.desktopaccountingapi.quickbooksdesktop.models.CustomerCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.Invoice;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceLineCreateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceUpdateInput;
import com.desktopaccountingapi.quickbooksdesktop.models.InvoiceVoided;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Creates a uniquely named customer and an invoice with a fixed idempotency key derived from a run
 * ID, updates the memo and voids the invoice. Running again with the same run ID replays the
 * creates and the void, and reads the invoice's current revision before updating, so a rerun never
 * sends a stale revisionNumber.
 *
 * <p>Arguments: [runId]. Uses DAAPI_ITEM_ID as the line item, else the first active service item.
 */
public final class CreateInvoice {
    private CreateInvoice() {}

    public static void main(String[] args) {
        Env.run(() -> {
            DesktopAccountingApiClient client = Env.client();
            String runId = Env.runId(args);
            System.out.println("Run ID: " + runId);

            Customer customer = client.qbd().customers().create(
                new CustomerCreateInput("SDK Example Customer " + runId).companyName("Example Co " + runId),
                RequestOptions.idempotent("example-customer-" + runId));
            System.out.println("Customer " + customer.id() + " " + customer.fullName());

            InvoiceCreateInput input = new InvoiceCreateInput(customer.id())
                .transactionDate(LocalDate.now())
                .memo("Created by the Java SDK example, run " + runId)
                .lines(List.of(new InvoiceLineCreateInput().itemId(Env.itemId(client)).quantity(2).rate(new BigDecimal("52.75"))));
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

            // A replayed create returns the invoice as it was created. Read the current revision: an
            // update with an older revisionNumber fails with 409 QBD_REVISION_NUMBER_STALE.
            Invoice current = client.qbd().invoices().retrieve(invoice.id());
            if (current.revisionNumber().equals(invoice.revisionNumber())) {
                Invoice updated = client.qbd().invoices().update(invoice.id(),
                    new InvoiceUpdateInput(current.revisionNumber()).memo("Updated memo, run " + runId));
                System.out.println("Updated memo: " + updated.memo() + " (revision " + updated.revisionNumber() + ")");
            } else {
                System.out.println("Skipped the update: an earlier run with this run ID already changed the invoice (revision "
                    + current.revisionNumber() + ")");
            }

            InvoiceVoided voided = client.qbd().invoices().voidTransaction(invoice.id(), RequestOptions.idempotent("example-void-" + runId));
            System.out.println("Voided " + voided.id() + ": " + voided.voided());
        });
    }
}
