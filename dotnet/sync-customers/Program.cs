// sync-customers: read every customer, 10 per page, and check that no ID repeats.
//
//   dotnet run --project sync-customers              fast: each page is requested well inside the cursor idle window
//   dotnet run --project sync-customers -- --slow    sleeps past the cursor idle window after each page
//                                                    (SLOW_PAGE_SECONDS, default 15) to provoke
//                                                    CursorExpiredException, then restarts the list and
//                                                    skips IDs it already has. It does not resume from
//                                                    LastUpdatedAt: QuickBooks returns records in its own
//                                                    order, so unread records can be older than the last one.
using DesktopAccountingApi.QuickBooksDesktop;
using DesktopAccountingApi.QuickBooksDesktop.Models;
using Examples;

return await Env.Run(async () =>
{
    var slow = args.Contains("--slow");
    var pause = TimeSpan.FromSeconds(int.Parse(Env.Optional("SLOW_PAGE_SECONDS") ?? "15", System.Globalization.CultureInfo.InvariantCulture));
    using var client = Env.Client();

    var seen = new HashSet<string>();
    var duplicates = 0;
    var pages = 0;

    void Process(Customer customer)
    {
        if (!seen.Add(customer.Id)) duplicates++;
    }

    try
    {
        await foreach (var page in client.Qbd.Customers.ListAsync(new CustomerListParams { Limit = 10 }).PagesAsync())
        {
            pages++;
            foreach (var customer in page.Data) Process(customer);
            Console.WriteLine($"page {pages}: {page.Data.Count} customers, {seen.Count} so far, remaining {page.RemainingCount?.ToString() ?? "-"}, cursor expires {page.CursorExpiresAt?.ToString("O") ?? "-"}");
            if (slow && page.HasMore)
            {
                Console.WriteLine($"  sleeping {pause.TotalSeconds} s (past the cursor idle window)");
                await Task.Delay(pause);
            }
        }
    }
    catch (CursorExpiredException ex)
    {
        Console.WriteLine($"CursorExpiredException ({ex.Reason}): {ex.Message}");
        Console.WriteLine($"  itemsYielded={ex.ItemsYielded} pagesServed={ex.PagesServed} lastId={ex.LastId} lastUpdatedAt={ex.LastUpdatedAt}");
        Console.WriteLine($"  requestId={ex.RequestId}");
        foreach (var fix in ex.Fixes) Console.WriteLine($"  fix ({fix.Actor}): {fix.Action}");

        // Restart the same query (an incremental sync would restart from its saved UpdatedAfter
        // watermark) and skip the IDs already seen.
        var before = seen.Count;
        await foreach (var customer in client.Qbd.Customers.ListAsync(new CustomerListParams { Limit = 10 })) seen.Add(customer.Id);
        Console.WriteLine($"Restarted the list: {seen.Count - before} more customers");
    }

    Console.WriteLine($"Total customers: {seen.Count}, duplicate IDs within one pass: {duplicates}");
    if (duplicates > 0) throw new DaapiException("Duplicate customer IDs in one pass.");
});
