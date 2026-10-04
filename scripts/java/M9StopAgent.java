import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;

/** Owned M9 fixture control only. No network listener or application privilege. */
public final class M9StopAgent {
    public static void premain(String ignored, Instrumentation instrumentation) {
        Thread control = new Thread(() -> {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                if ("qixu-m9-graceful-stop".equals(input.readLine())) {
                    System.out.println("M9_GRACEFUL_STOP_REQUESTED");
                    System.out.flush();
                    System.exit(0); // normal JVM shutdown, including Spring's registered hook
                }
            } catch (Exception ignoredFailure) {
                // A broken control pipe is not authorization to close the application.
            }
        }, "qixu-m9-owned-stdin");
        control.setDaemon(true);
        control.start();
    }
}
