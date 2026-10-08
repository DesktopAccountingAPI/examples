// async-webhooks: queue an invoice create in async mode, wait on the request handle, and run a
// minimal webhook receiver that verifies Standard Webhooks signatures with the SDK.
//
//   dotnet run --project async-webhooks                 enqueue + wait, then void the invoice (DAAPI_SECRET_KEY, DAAPI_END_USER_ID)
//   dotnet run --project async-webhooks -- --receive    webhook receiver on PORT (default 8080), DAAPI_WEBHOOK_SECRET
//   dotnet run --project async-webhooks -- --self-test  sign a sample event with DAAPI_WEBHOOK_SECRET and verify it locally
using System.Net;
using System.Text;
using DesktopAccountingApi.QuickBooksDesktop;
using DesktopAccountingApi.QuickBooksDesktop.Models;
using Examples;

return await Env.Run(async () =>
{
    if (args.Contains("--self-test"))
    {
        SelfTest(Env.Require("DAAPI_WEBHOOK_SECRET"));
        return;
    }
    if (args.Contains("--receive"))
    {
        await ReceiveAsync(Env.Require("DAAPI_WEBHOOK_SECRET"), int.Parse(Env.Optional("PORT") ?? "8080", System.Globalization.CultureInfo.InvariantCulture));
        return;
    }

    var runId = Env.RunId(args);
    using var client = Env.Client();
    var customerId = await Env.CustomerIdAsync(client);
    var line = await Env.SampleLineAsync(client, "Async example");

    var handle = await client.Qbd.Invoices.Enqueue.CreateAsync(
        new InvoiceCreateInput { CustomerId = customerId, Memo = $"Async example {runId}", Lines = new[] { line } },
        new RequestOptions { IdempotencyKey = $"example-{runId}-async-invoice", QueueTtl = TimeSpan.FromHours(1) });
    Console.WriteLine($"Queued request {handle.Id}: status {handle.Request.Status}, waiting reason {handle.Request.WaitingReason ?? "-"}, queue position {handle.Request.QueuePosition?.ToString() ?? "-"}");

    try
    {
        var invoice = await handle.WaitAsync(TimeSpan.FromMinutes(2));
        Console.WriteLine($"Request {handle.Id} succeeded: invoice {invoice.RefNumber} ({invoice.Id}), subtotal {invoice.Subtotal}");
        await client.Qbd.Invoices.VoidAsync(invoice.Id);
        Console.WriteLine($"Voided {invoice.Id}.");
    }
    catch (RequestPendingException ex)
    {
        var status = await handle.StatusAsync();
        Console.WriteLine($"Still {status.Status} after 2 minutes ({ex.RequestId}). The webhook receiver (--receive) reports when it finishes.");
    }
});

static void SelfTest(string secret)
{
    var now = DateTimeOffset.UtcNow;
    var body = "{\"id\":\"evt_selftest\",\"type\":\"request.succeeded\",\"timestamp\":\"" + now.ToString("yyyy-MM-ddTHH:mm:ss.fffZ", System.Globalization.CultureInfo.InvariantCulture) +
        "\",\"projectId\":\"proj_selftest\",\"data\":{\"objectType\":\"request\",\"id\":\"req_selftest\",\"status\":\"succeeded\",\"operationId\":\"qbd.invoices.create\"}}";
    var headers = new Dictionary<string, string>
    {
        ["webhook-id"] = "evt_selftest",
        ["webhook-timestamp"] = now.ToUnixTimeSeconds().ToString(System.Globalization.CultureInfo.InvariantCulture),
        ["webhook-signature"] = WebhookVerifier.Sign(body, "evt_selftest", now, secret),
    };
    var ev = WebhookVerifier.Verify(body, headers, secret);
    Console.WriteLine($"Self-test OK: {ev.Type} {ev.Id}, data.id {ev.Data.GetProperty("id").GetString()}");
    try
    {
        WebhookVerifier.Verify(body.Replace("succeeded", "failed", StringComparison.Ordinal), headers, secret);
        throw new InvalidOperationException("A tampered body was accepted.");
    }
    catch (WebhookVerificationException ex)
    {
        Console.WriteLine($"Tampered body rejected: {ex.Message}");
    }
}

static async Task ReceiveAsync(string secret, int port)
{
    using var listener = new HttpListener();
    listener.Prefixes.Add($"http://+:{port}/");
    try
    {
        listener.Start();
    }
    catch (HttpListenerException)
    {
        // Binding all interfaces can need extra rights on Windows; fall back to localhost.
        listener.Prefixes.Clear();
        listener.Prefixes.Add($"http://localhost:{port}/");
        listener.Start();
    }
    Console.WriteLine($"Listening for webhooks on port {port} (POST any path). Ctrl+C to stop.");
    while (true)
    {
        var context = await listener.GetContextAsync();
        string body;
        using (var reader = new StreamReader(context.Request.InputStream, Encoding.UTF8)) body = await reader.ReadToEndAsync();
        try
        {
            var ev = WebhookVerifier.Verify(body, name => context.Request.Headers[name], secret);
            var status = ev.Data.ValueKind == System.Text.Json.JsonValueKind.Object && ev.Data.TryGetProperty("status", out var s) ? s.GetString() : null;
            Console.WriteLine($"{ev.Timestamp:O} {ev.Type} {ev.Id} data.id={(ev.Data.ValueKind == System.Text.Json.JsonValueKind.Object && ev.Data.TryGetProperty("id", out var id) ? id.GetString() : "-")} status={status ?? "-"}");
            context.Response.StatusCode = 204;
        }
        catch (WebhookVerificationException ex)
        {
            Console.WriteLine($"Rejected a webhook: {ex.Message}");
            context.Response.StatusCode = 400;
        }
        context.Response.Close();
    }
}
