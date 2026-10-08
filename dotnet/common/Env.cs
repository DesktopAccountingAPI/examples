using DesktopAccountingApi.QuickBooksDesktop;
using DesktopAccountingApi.QuickBooksDesktop.Models;

namespace Examples;

/// <summary>Configuration from environment variables and small helpers shared by the examples.</summary>
internal static class Env
{
    public static string Require(string name) =>
        Environment.GetEnvironmentVariable(name) is { Length: > 0 } value
            ? value
            : throw new DaapiException($"Set the {name} environment variable (see README.md).");

    public static string? Optional(string name) =>
        Environment.GetEnvironmentVariable(name) is { Length: > 0 } value ? value : null;

    /// <summary>A client for DAAPI_END_USER_ID. The SDK reads DAAPI_SECRET_KEY and DAAPI_BASE_URL itself.</summary>
    public static DesktopAccountingApiClient Client(HttpMessageHandler? handler = null) => new(new ClientOptions
    {
        EndUserId = Require("DAAPI_END_USER_ID"),
        HttpMessageHandler = handler,
        Logger = Optional("DAAPI_DEBUG") is null ? null : (level, message) => Console.Error.WriteLine($"[{level}] {message}"),
    });

    /// <summary>Runs an example and prints SDK errors readably.</summary>
    public static async Task<int> Run(Func<Task> body)
    {
        try
        {
            await body();
            return 0;
        }
        catch (ApiException ex)
        {
            PrintError(ex);
            return 1;
        }
        catch (DaapiException ex)
        {
            Console.Error.WriteLine($"{ex.GetType().Name}: {ex.Message}");
            return 1;
        }
    }

    public static void PrintError(ApiException ex)
    {
        Console.Error.WriteLine($"{ex.GetType().Name} {ex.Status} {ex.Code}: {ex.Message}");
        if (!string.IsNullOrEmpty(ex.UserFacingMessage)) Console.Error.WriteLine($"  user-facing: {ex.UserFacingMessage}");
        foreach (var fix in ex.Fixes) Console.Error.WriteLine($"  fix ({fix.Actor}): {fix.Action}");
        if (!string.IsNullOrEmpty(ex.DocsUrl)) Console.Error.WriteLine($"  docs: {ex.DocsUrl}");
        Console.Error.WriteLine($"  request ID: {ex.RequestId}");
    }

    /// <summary>The customer for invoices: DAAPI_CUSTOMER_ID, else the first active customer.</summary>
    public static async Task<string> CustomerIdAsync(DesktopAccountingApiClient client)
    {
        if (Optional("DAAPI_CUSTOMER_ID") is { } id) return id;
        var customers = await client.Qbd.Customers.ListAsync(new CustomerListParams { Limit = 1, Status = ActiveStatus.Active }).GetFirstPageAsync();
        if (customers.Data.Count == 0) throw new DaapiException("The company file needs an active customer (or set DAAPI_CUSTOMER_ID).");
        Console.WriteLine($"Using customer {customers.Data[0].FullName} ({customers.Data[0].Id})");
        return customers.Data[0].Id;
    }

    /// <summary>One invoice line for the service item in DAAPI_ITEM_ID, else the first active service item.</summary>
    public static async Task<InvoiceLineCreateInput> SampleLineAsync(DesktopAccountingApiClient client, string description)
    {
        var itemId = Optional("DAAPI_ITEM_ID");
        if (itemId is null)
        {
            var items = await client.Qbd.ServiceItems.ListAsync(new ServiceItemListParams { Limit = 1, Status = ActiveStatus.Active }).GetFirstPageAsync();
            if (items.Data.Count == 0) throw new DaapiException("The company file needs an active service item (or set DAAPI_ITEM_ID).");
            itemId = items.Data[0].Id;
            Console.WriteLine($"Using service item {items.Data[0].FullName} ({itemId})");
        }
        return new InvoiceLineCreateInput { ItemId = itemId, Description = description, Quantity = 2, Rate = 52.75m };
    }

    /// <summary>A short ID for this run, used in names and idempotency keys. Pass --run-id to repeat a run.</summary>
    public static string RunId(string[] args)
    {
        var i = Array.IndexOf(args, "--run-id");
        return i >= 0 && i + 1 < args.Length ? args[i + 1] : DateTime.UtcNow.ToString("yyyyMMdd-HHmmss", System.Globalization.CultureInfo.InvariantCulture);
    }
}
