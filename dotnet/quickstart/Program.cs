// quickstart: check the end user's QuickBooks Desktop connection, then list the first 10 invoices.
using DesktopAccountingApi.QuickBooksDesktop.Models;
using Examples;

return await Env.Run(async () =>
{
    using var client = Env.Client();

    var health = await client.Qbd.HealthCheckAsync();
    Console.WriteLine($"QuickBooks: {health.Quickbooks.CompanyName} ({health.Quickbooks.Product}), round trip {health.Duration} ms");

    var page = await client.Qbd.Invoices.ListAsync(new InvoiceListParams { Limit = 10, IncludeLineItems = false }).GetFirstPageAsync();
    Console.WriteLine($"First {page.Data.Count} invoices (request {page.RequestId}):");
    foreach (var invoice in page.Data)
    {
        Console.WriteLine($"  {invoice.RefNumber,-12} {invoice.TransactionDate,-12} {invoice.Customer?.FullName,-30} {invoice.Subtotal,12}  {invoice.Id}");
    }
    Console.WriteLine(page.HasMore ? $"  ... {page.RemainingCount?.ToString() ?? "more"} more" : "  (end of list)");
});
