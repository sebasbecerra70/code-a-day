import java.time.Duration;
import java.util.Map;

public class SolutionTest {
    private static int passed = 0;

    interface TestBody {
        void run() throws Exception;
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void expectThrows(Class<? extends Throwable> type, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " to be thrown");
    }

    static void run(String name, TestBody body) {
        try {
            body.run();
        } catch (Throwable t) {
            throw new AssertionError(name + " failed: " + t.getMessage(), t);
        }
        passed++;
    }

    public static void main(String[] args) {
        run("defaults", () -> {
            HttpRequest r = HttpRequest.builder("https://api.example.com/items").build();
            check(r.method() == HttpRequest.Method.GET, "GET default");
            check(r.headers().isEmpty() && r.body() == null, "no headers/body");
            check(r.timeout().equals(Duration.ofSeconds(30)) && r.maxRetries() == 0, "timeout/retries");
        });

        run("fluentChain", () -> {
            HttpRequest r = HttpRequest.builder("https://api.example.com/items")
                    .method(HttpRequest.Method.POST)
                    .header("Authorization", "Bearer t")
                    .jsonBody("{\"name\":\"x\"}")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            check(r.method() == HttpRequest.Method.POST, "method");
            check(r.headers().equals(Map.of("authorization", "Bearer t", "content-type", "application/json")),
                    r.headers().toString());
            check(r.body().equals("{\"name\":\"x\"}") && r.timeout().getSeconds() == 5, "body/timeout");
        });

        run("headersCaseInsensitiveLastWins", () -> {
            HttpRequest r = HttpRequest.builder("http://h").header("X-Id", "1").header("x-id", "2").build();
            check(r.headers().size() == 1 && r.headers().get("x-id").equals("2"), r.headers().toString());
        });

        run("requiredAndNullChecks", () -> {
            expectThrows(NullPointerException.class, () -> HttpRequest.builder(null));
            expectThrows(NullPointerException.class, () -> HttpRequest.builder("http://h").method(null));
            expectThrows(NullPointerException.class, () -> HttpRequest.builder("http://h").header("a", null));
        });

        run("validationOnBuild", () -> {
            expectThrows(IllegalStateException.class, () -> HttpRequest.builder("ftp://h").build());
            expectThrows(IllegalStateException.class, () -> HttpRequest.builder("http://h").jsonBody("{}").build());
            expectThrows(IllegalStateException.class,
                    () -> HttpRequest.builder("http://h").timeout(Duration.ZERO).build());
            expectThrows(IllegalStateException.class, () -> HttpRequest.builder("http://h").maxRetries(-1).build());
            expectThrows(IllegalStateException.class, () -> HttpRequest.builder("http://h").maxRetries(11).build());
        });

        run("retriedPostNeedsIdempotencyKey", () -> {
            expectThrows(IllegalStateException.class,
                    () -> HttpRequest.builder("http://h").method(HttpRequest.Method.POST).maxRetries(3).build());
            HttpRequest ok = HttpRequest.builder("http://h").method(HttpRequest.Method.POST)
                    .header("Idempotency-Key", "abc").maxRetries(3).build();
            check(ok.maxRetries() == 3, "allowed with key");
            check(HttpRequest.builder("http://h").maxRetries(3).build().maxRetries() == 3, "GET retries fine");
        });

        run("immutability", () -> {
            HttpRequest r = HttpRequest.builder("http://h").header("a", "1").build();
            expectThrows(UnsupportedOperationException.class, () -> r.headers().put("b", "2"));
        });

        run("builderReuseDoesNotLeak", () -> {
            HttpRequest.Builder b = HttpRequest.builder("http://h").header("a", "1");
            HttpRequest first = b.build();
            b.header("b", "2");
            HttpRequest second = b.build();
            check(first.headers().size() == 1 && second.headers().size() == 2, "independent snapshots");
        });

        run("toBuilderCopiesAndEquality", () -> {
            HttpRequest base = HttpRequest.builder("http://h").method(HttpRequest.Method.PUT)
                    .header("a", "1").jsonBody("{}").maxRetries(2).build();
            HttpRequest same = base.toBuilder().build();
            check(same.equals(base) && same.hashCode() == base.hashCode() && same != base, "equal copy");
            HttpRequest changed = base.toBuilder().timeout(Duration.ofSeconds(1)).build();
            check(!changed.equals(base) && changed.headers().equals(base.headers()), "copy with change");
            check(base.timeout().getSeconds() == 30, "original untouched");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
