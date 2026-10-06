package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.errors.ApiException;
import com.desktopaccountingapi.quickbooksdesktop.errors.DaapiException;
import com.desktopaccountingapi.quickbooksdesktop.errors.IntegrationConnectionException;
import com.desktopaccountingapi.quickbooksdesktop.errors.IntegrationException;
import com.desktopaccountingapi.quickbooksdesktop.models.ErrorCode;

/** Triggers typed errors and prints the fields an application uses to handle them. */
public final class ErrorHandling {
    private ErrorHandling() {}

    public static void main(String[] args) {
        DesktopAccountingApiClient client = Env.client();

        System.out.println("1. Unknown customer ID");
        try {
            client.qbd().customers().retrieve("80000099-1700000000");
            System.out.println("   unexpectedly found a customer");
        } catch (IntegrationException e) {
            print(e);
            if (ErrorCode.QBD_OBJECT_NOT_FOUND.equals(e.code())) System.out.println("   -> handled as not found");
        } catch (IntegrationConnectionException e) {
            print(e);
            System.out.println("   -> the end user has to act; show userFacingMessage");
        } catch (ApiException e) {
            print(e);
        }

        System.out.println("2. Malformed secret key (rejected locally, nothing is sent)");
        try {
            DesktopAccountingApiClient.builder().apiKey("sk_test_not-a-real-key").build();
        } catch (DaapiException e) {
            System.out.println("   " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println("3. QuickBooks call without an end user (rejected locally)");
        try {
            DesktopAccountingApiClient.builder().build().qbd().healthCheck();
        } catch (DaapiException e) {
            System.out.println("   " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
