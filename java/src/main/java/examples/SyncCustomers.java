package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.Page;
import com.desktopaccountingapi.quickbooksdesktop.errors.CursorExpiredException;
import com.desktopaccountingapi.quickbooksdesktop.models.Customer;
import com.desktopaccountingapi.quickbooksdesktop.models.CustomerListParams;
import java.util.HashSet;
import java.util.Set;

/**
 * Auto-paginates every customer, 10 per page, and checks that no ID repeats.
 *
 * <p>{@code --slow [seconds]} waits after each page (default 15 s, longer than the cursor idle
 * window) to provoke {@link CursorExpiredException}, prints its progress fields and resumes from an
 * {@code updatedAfter} watermark.
 */
public final class SyncCustomers {
    private SyncCustomers() {}

    public static void main(String[] args) throws InterruptedException {
        DesktopAccountingApiClient client = Env.client();
        int slowSeconds = 0;
        for (int i = 0; i < args.length; i++) {
            if ("--slow".equals(args[i])) {
                slowSeconds = i + 1 < args.length && args[i + 1].matches("\\d+") ? Integer.parseInt(args[i + 1]) : 15;
            }
        }

        if (slowSeconds == 0) {
            Set<String> ids = new HashSet<>();
            int count = 0;
            for (Customer c : client.qbd().customers().list(new CustomerListParams().limit(10))) {
                count++;
                if (!ids.add(c.id())) throw new IllegalStateException("duplicate customer ID " + c.id());
            }
            System.out.println(count + " customers, " + ids.size() + " unique IDs, no duplicates");
            return;
        }

        Set<String> ids = new HashSet<>();
        try {
            for (Page<Customer> page : client.qbd().customers().list(new CustomerListParams().limit(10)).pages()) {
                for (Customer c : page.data()) ids.add(c.id());
                System.out.println("page of " + page.data().size() + " (" + ids.size() + " so far, cursor expires " + page.cursorExpiresAt()
                    + "); waiting " + slowSeconds + " s");
                Thread.sleep(slowSeconds * 1000L);
            }
            System.out.println("The cursor did not expire; " + ids.size() + " customers. Try a longer --slow value.");
        } catch (CursorExpiredException e) {
            System.out.println("CursorExpiredException: " + e.reason());
            System.out.println("  itemsYielded  " + e.itemsYielded());
            System.out.println("  pagesServed   " + e.pagesServed());
            System.out.println("  lastId        " + e.lastId());
            System.out.println("  lastUpdatedAt " + e.lastUpdatedAt());
            System.out.println("  fixes         " + e.fixes());
            // Resume: everything modified since the last item we saw. Records may repeat; dedupe by ID.
            CustomerListParams resume = new CustomerListParams().limit(10);
            if (e.lastUpdatedAt() != null) resume.updatedAfter(e.lastUpdatedAt());
            int before = ids.size();
            for (Customer c : client.qbd().customers().list(resume)) ids.add(c.id());
            System.out.println("Resumed from " + e.lastUpdatedAt() + ": " + (ids.size() - before) + " more, " + ids.size() + " unique customers");
        }
    }
}
