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

    /// <summary>One invoice line: the first active service item, or a description-only line when the company file has none.</summary>
    public static async Task<InvoiceLineCreateInput> SampleLineAsync(DesktopAccountingApiClient client, string description)
    {
        var items = await client.Qbd.ServiceItems.ListAsync(new ServiceItemListParams { Limit = 1 }).GetFirstPageAsync();
        if (items.Data.Count == 0) return new InvoiceLineCreateInput { Description = description };
        var item = items.Data[0];
        Console.WriteLine($"Using service item {item.FullName} ({item.Id})");
        return new InvoiceLineCreateInput { ItemId = item.Id, Description = description, Quantity = 2, Rate = 52.75m };
    }

    /// <summary>A short ID for this run, used in names and idempotency keys. Pass --run-id to repeat a run.</summary>
    public static string RunId(string[] args)
    {
        var i = Array.IndexOf(args, "--run-id");
        return i >= 0 && i + 1 < args.Length ? args[i + 1] : DateTime.UtcNow.ToString("yyyyMMdd-HHmmss", System.Globalization.CultureInfo.InvariantCulture);
    }
}
