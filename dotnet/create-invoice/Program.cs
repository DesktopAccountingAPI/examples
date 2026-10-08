// create-invoice: create a uniquely named customer, create an invoice with a fixed idempotency key
// derived from the run ID, update its memo, then void it.
//
//   dotnet run --project create-invoice [-- --run-id <id>]
//
// Running again with the same --run-id replays the same creates and void instead of making
// duplicates, and reads the invoice's current revision before updating so it never sends a stale one.
using DesktopAccountingApi.QuickBooksDesktop;
using DesktopAccountingApi.QuickBooksDesktop.Models;
using Examples;

return await Env.Run(async () =>
{
    var runId = Env.RunId(args);
    using var client = Env.Client();

    var customer = await client.Qbd.Customers.CreateAsync(
        new CustomerCreateInput { Name = $"DAAPI Example {runId}" },
        new RequestOptions { IdempotencyKey = $"example-{runId}-customer" });
    Console.WriteLine($"Customer {customer.Name}: {customer.Id}");

    var line = await Env.SampleLineAsync(client, $"Example invoice line ({runId})");
    var created = await client.Qbd.Invoices.CreateWithResponseAsync(
        new InvoiceCreateInput
        {
            CustomerId = customer.Id,
            TransactionDate = DateOnly.FromDateTime(DateTime.Today),
            Memo = "Created by the .NET create-invoice example",
            Lines = new[] { line },
        },
        new RequestOptions { IdempotencyKey = $"example-{runId}-invoice" });
    var invoice = created.Data;
    var replayed = created.Headers.TryGetValue("Daapi-Idempotent-Replayed", out var r) && r == "true";
    Console.WriteLine($"Invoice {invoice.RefNumber}: {invoice.Id}, subtotal {invoice.Subtotal}{(replayed ? " (replayed: this run ID was used before)" : "")}");

    // A replayed create returns the invoice as it was created. Read the current revision: an update
    // with an older RevisionNumber fails with 409 QBD_REVISION_NUMBER_STALE.
    var current = await client.Qbd.Invoices.RetrieveAsync(invoice.Id);
    if (current.RevisionNumber == invoice.RevisionNumber)
    {
        var updated = await client.Qbd.Invoices.UpdateAsync(invoice.Id, new InvoiceUpdateInput
        {
            RevisionNumber = current.RevisionNumber,
            Memo = $"Memo updated by run {runId}",
        });
        Console.WriteLine($"Updated memo: \"{updated.Memo}\" (revision {updated.RevisionNumber})");
    }
    else
    {
        Console.WriteLine($"Skipped the update: an earlier run with this run ID already changed the invoice (revision {current.RevisionNumber})");
    }

    var voided = await client.Qbd.Invoices.VoidAsync(invoice.Id, new RequestOptions { IdempotencyKey = $"example-{runId}-void" });
    Console.WriteLine($"Voided invoice {voided.RefNumber} ({voided.Id}): voided={voided.Voided}");
});
