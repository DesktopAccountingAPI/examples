// error-handling: trigger typed errors and print their fields.
//   1. Retrieve a customer ID that does not exist: IntegrationException (QBD_* code) from QuickBooks.
//   2. A malformed API key: DaapiException before any request is sent.
//   3. A QuickBooks call without an end user: DaapiException before any request is sent.
using DesktopAccountingApi.QuickBooksDesktop;
using Examples;

return await Env.Run(async () =>
{
    using var client = Env.Client();

    Console.WriteLine("1. Unknown customer ID");
    try
    {
        await client.Qbd.Customers.RetrieveAsync("80000999-1000000000");
        Console.WriteLine("   unexpectedly found a customer");
    }
    catch (IntegrationException ex) when (ex.Code == ErrorCodes.QbdObjectNotFound)
    {
        Console.WriteLine("   IntegrationException, QuickBooks has no such record:");
        Print(ex);
    }
    catch (ApiException ex)
    {
        Console.WriteLine($"   {ex.GetType().Name}:");
        Print(ex);
    }

    Console.WriteLine("2. Malformed API key");
    try
    {
        using var bad = new DesktopAccountingApiClient(new ClientOptions { ApiKey = "sk_test_this-is-not-a-real-key", EndUserId = "eu_example" });
    }
    catch (DaapiException ex)
    {
        Console.WriteLine($"   {ex.GetType().Name} (no request sent): {ex.Message}");
    }

    Console.WriteLine("3. QuickBooks operation without an end user");
    try
    {
        using var noEndUser = new DesktopAccountingApiClient(new ClientOptions());
        await noEndUser.Qbd.HealthCheckAsync();
    }
    catch (DaapiException ex) when (ex is not ApiException)
    {
        Console.WriteLine($"   {ex.GetType().Name} (no request sent): {ex.Message}");
    }
});

static void Print(ApiException ex)
{
    Console.WriteLine($"   status:            {ex.Status}");
    Console.WriteLine($"   type / code:       {ex.Type} / {ex.Code}");
    Console.WriteLine($"   message:           {ex.Message}");
    Console.WriteLine($"   userFacingMessage: {ex.UserFacingMessage}");
    Console.WriteLine($"   integrationCode:   {ex.IntegrationCode ?? "-"}");
    Console.WriteLine($"   cause:             {ex.Cause}");
    foreach (var fix in ex.Fixes) Console.WriteLine($"   fix ({fix.Actor}):  {fix.Action}");
    Console.WriteLine($"   docsUrl:           {ex.DocsUrl}");
    Console.WriteLine($"   requestId:         {ex.RequestId}");
    Console.WriteLine($"   retryable:         {ex.Retryable}, outcome: {ex.Outcome}");
}
