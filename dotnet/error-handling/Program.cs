// error-handling: trigger typed errors and print their fields.
//   1. Retrieve a customer ID that does not exist: IntegrationException (QBD_* code) from QuickBooks.
//   2. A malformed API key: DaapiException before any request is sent.
//   3. A QuickBooks call without an end user: DaapiException before any request is sent.
// Exits 1 when a step does not produce the error it demonstrates (for example a connection or setup
// error instead of QBD_OBJECT_NOT_FOUND), so a misconfigured run never looks like a pass.
using DesktopAccountingApi.QuickBooksDesktop;
using Examples;

var failures = 0;
void Unexpected(string what)
{
    failures++;
    Console.WriteLine($"   -> unexpected: {what}");
}

var exitCode = await Env.Run(async () =>
{
    using var client = Env.Client();

    Console.WriteLine("1. Unknown customer ID");
    try
    {
        await client.Qbd.Customers.RetrieveAsync("80000999-1000000000");
        Unexpected("found a customer");
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
        Unexpected("expected QBD_OBJECT_NOT_FOUND");
    }

    Console.WriteLine("2. Malformed API key");
    try
    {
        using var bad = new DesktopAccountingApiClient(new ClientOptions { ApiKey = "sk_test_this-is-not-a-real-key", EndUserId = "eu_example" });
        Unexpected("the key was accepted");
    }
    catch (DaapiException ex) when (ex is not ApiException)
    {
        Console.WriteLine($"   {ex.GetType().Name} (no request sent): {ex.Message}");
    }

    Console.WriteLine("3. QuickBooks operation without an end user");
    try
    {
        using var noEndUser = new DesktopAccountingApiClient(new ClientOptions());
        await noEndUser.Qbd.HealthCheckAsync();
        Unexpected("the call succeeded");
    }
    catch (DaapiException ex) when (ex is not ApiException)
    {
        Console.WriteLine($"   {ex.GetType().Name} (no request sent): {ex.Message}");
    }
});

if (failures > 0)
{
    Console.WriteLine("At least one step did not produce the error it demonstrates.");
    return 1;
}
return exitCode;

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
