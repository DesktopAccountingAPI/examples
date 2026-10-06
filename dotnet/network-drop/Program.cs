// network-drop: the first attempt of an invoice create reaches the API, but the response is
// "lost" (the transport throws after the request was sent). The SDK retries with the same
// Idempotency-Key, the API replays the stored result, and exactly one invoice exists.
using System.Net.Http;
using DesktopAccountingApi.QuickBooksDesktop.Models;
using Examples;

return await Env.Run(async () =>
{
    var runId = Env.RunId(args);
    var drop = new DropFirstResponseHandler { InnerHandler = new HttpClientHandler() };
    using var client = Env.Client(drop);

    var customer = await client.Qbd.Customers.CreateAsync(new CustomerCreateInput { Name = $"DAAPI Network Drop {runId}" });
    Console.WriteLine($"Customer {customer.Name}: {customer.Id}");
    var line = await Env.SampleLineAsync(client, "Network drop example");

    drop.Armed = true;
    var response = await client.Qbd.Invoices.CreateWithResponseAsync(new InvoiceCreateInput
    {
        CustomerId = customer.Id,
        Memo = "Created by the .NET network-drop example",
        Lines = new[] { line },
    });
    Console.WriteLine($"Idempotency-Key on each attempt: {string.Join(", ", drop.Keys)}");
    Console.WriteLine($"Invoice {response.Data.RefNumber}: {response.Data.Id} (replayed: {(response.Headers.TryGetValue("Daapi-Idempotent-Replayed", out var r) ? r : "false")})");

    var invoices = await client.Qbd.Invoices.ListAsync(new InvoiceListParams { CustomerIds = new[] { customer.Id }, IncludeLineItems = false }).ListAllAsync();
    Console.WriteLine($"Invoices for this customer: {invoices.Count} (expected 1)");
    if (invoices.Count != 1) throw new InvalidOperationException("Expected exactly one invoice.");
});

/// <summary>Sends the first write to the API, then throws as if the connection dropped before the response arrived.</summary>
internal sealed class DropFirstResponseHandler : DelegatingHandler
{
    public bool Armed { get; set; }

    public List<string> Keys { get; } = new();

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        if (request.Headers.TryGetValues("Idempotency-Key", out var key) && Armed) Keys.Add(key.First());
        var response = await base.SendAsync(request, cancellationToken);
        if (Armed && request.Method == HttpMethod.Post && Keys.Count == 1)
        {
            response.Dispose();
            Console.WriteLine($"Simulating a dropped connection after {request.Method} {request.RequestUri?.AbsolutePath} reached the API");
            throw new HttpRequestException("Simulated network failure after the request was sent.");
        }
        return response;
    }
}
