package examples;

import com.desktopaccountingapi.quickbooksdesktop.DesktopAccountingApiClient;
import com.desktopaccountingapi.quickbooksdesktop.core.Transport;

/** Shared configuration for the examples: everything comes from environment variables. */
final class Env {
    private Env() {}

    static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            System.err.println("Set " + name + " (see README.md).");
            System.exit(2);
        }
        return v;
    }

    /** Client for DAAPI_SECRET_KEY / DAAPI_BASE_URL, bound to DAAPI_END_USER_ID. */
    static DesktopAccountingApiClient client() {
        return builder().build();
    }

    static DesktopAccountingApiClient client(Transport transport) {
        return builder().transport(transport).build();
    }

    private static DesktopAccountingApiClient.Builder builder() {
        require("DAAPI_SECRET_KEY");
        return DesktopAccountingApiClient.builder().endUserId(require("DAAPI_END_USER_ID"));
    }

    /** A short run ID: the first argument, else RUN_ID, else the current time in seconds. */
    static String runId(String[] args) {
        for (String a : args) if (!a.startsWith("--")) return a;
        String env = System.getenv("RUN_ID");
        return env != null && !env.isEmpty() ? env : Long.toString(System.currentTimeMillis() / 1000);
    }
}
