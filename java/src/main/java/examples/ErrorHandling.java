package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.errors.ApiException;
import com.desktopaccountingapi.quickbooksdesktop.errors.DaapiException;
import com.desktopaccountingapi.quickbooksdesktop.errors.IntegrationConnectionException;
import com.desktopaccountingapi.quickbooksdesktop.errors.IntegrationException;
import com.desktopaccountingapi.quickbooksdesktop.models.ErrorCode;

/**
 * Triggers typed errors and prints the fields an application uses to handle them. Exits 1 when a
 * step does not produce the error it demonstrates (for example a connection or setup error instead
 * of QBD_OBJECT_NOT_FOUND), so a misconfigured run never looks like a pass.
 */
public final class ErrorHandling {
    private ErrorHandling() {}

    private static int failures;

    private static void unexpected(String what) {
        failures++;
        System.out.println("   -> unexpected: " + what);
    }

    public static void main(String[] args) {
        DesktopAccountingApiClient client = Env.client();

        System.out.println("1. Unknown customer ID");
        try {
            client.qbd().customers().retrieve("80000099-1700000000");
            unexpected("found a customer");
        } catch (IntegrationException e) {
            print(e);
            if (ErrorCode.QBD_OBJECT_NOT_FOUND.equals(e.code())) System.out.println("   -> handled as not found");
            else unexpected("expected QBD_OBJECT_NOT_FOUND");
        } catch (IntegrationConnectionException e) {
            print(e);
            System.out.println("   -> the end user has to act; show userFacingMessage");
            unexpected("expected QBD_OBJECT_NOT_FOUND");
        } catch (ApiException e) {
            print(e);
            unexpected("expected QBD_OBJECT_NOT_FOUND");
        }

        System.out.println("2. Malformed secret key (rejected locally, nothing is sent)");
        try {
            DesktopAccountingApiClient.builder().apiKey("sk_test_not-a-real-key").build();
            unexpected("the key was accepted");
        } catch (ApiException e) {
            print(e);
            unexpected("expected a local DaapiException");
        } catch (DaapiException e) {
            System.out.println("   " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println("3. QuickBooks call without an end user (rejected locally)");
        try {
            DesktopAccountingApiClient.builder().build().qbd().healthCheck();
            unexpected("the call succeeded");
        } catch (ApiException e) {
            print(e);
            unexpected("expected a local DaapiException");
        } catch (DaapiException e) {
            System.out.println("   " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        if (failures > 0) {
            System.out.println("At least one step did not produce the error it demonstrates.");
            System.exit(1);
        }
    }

    private static void print(ApiException e) {
        System.out.println("   " + e.getClass().getSimpleName() + " " + e.status() + " " + e.type() + " " + e.code());
        System.out.println("   message:           " + e.getMessage());
        System.out.println("   userFacingMessage: " + e.userFacingMessage());
        System.out.println("   integrationCode:   " + e.integrationCode());
        System.out.println("   cause:             " + e.errorCause());
        e.fixes().forEach(f -> System.out.println("   fix (" + f.actor() + "): " + f.action()));
        System.out.println("   docsUrl:           " + e.docsUrl());
        System.out.println("   requestId:         " + e.requestId());
        System.out.println("   retryable:         " + e.retryable() + ", outcome " + e.outcome());
    }
}
