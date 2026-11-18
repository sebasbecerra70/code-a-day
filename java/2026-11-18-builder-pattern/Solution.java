import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable HTTP request value object. The only way to make one is through
 * {@link Builder}, which collects optional fields fluently and validates
 * cross-field rules once, in {@link Builder#build()}.
 */
final class HttpRequest {
    enum Method { GET, POST, PUT, PATCH, DELETE }

    private static final Set<Method> BODY_ALLOWED = Set.of(Method.POST, Method.PUT, Method.PATCH);

    private final String url;
    private final Method method;
    private final Map<String, String> headers;
    private final String body;
    private final Duration timeout;
    private final int maxRetries;

    private HttpRequest(Builder b) {
        this.url = b.url;
        this.method = b.method;
        // Defensive copy: later changes to the builder must not leak into this object.
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));
        this.body = b.body;
        this.timeout = b.timeout;
        this.maxRetries = b.maxRetries;
    }

    /** The URL is the one required field, so it goes in the entry point. */
    static Builder builder(String url) {
        return new Builder(url);
    }

    /** A builder pre-filled with this request's values, for "copy with changes". */
    Builder toBuilder() {
        Builder b = new Builder(url).method(method).timeout(timeout).maxRetries(maxRetries);
        b.headers.putAll(headers);
        b.body = body;
        return b;
    }

    String url() { return url; }
    Method method() { return method; }
    Map<String, String> headers() { return headers; }
    String body() { return body; }
    Duration timeout() { return timeout; }
    int maxRetries() { return maxRetries; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpRequest r)) return false;
        return maxRetries == r.maxRetries && url.equals(r.url) && method == r.method
                && headers.equals(r.headers) && Objects.equals(body, r.body) && timeout.equals(r.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method, headers, body, timeout, maxRetries);
    }

    @Override
    public String toString() {
        return method + " " + url + " " + headers + (body == null ? "" : " body=" + body.length() + "B");
    }

    static final class Builder {
        private final String url;
        private Method method = Method.GET;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String body;
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetries = 0;

        private Builder(String url) {
            this.url = Objects.requireNonNull(url, "url");
        }

        Builder method(Method m) {
            this.method = Objects.requireNonNull(m, "method");
            return this;
        }

        /** Header names are case-insensitive, so they're normalized to lowercase. */
        Builder header(String name, String value) {
            headers.put(name.toLowerCase(Locale.ROOT), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** Convenience: sets the body and its content type together. */
        Builder jsonBody(String json) {
            this.body = Objects.requireNonNull(json, "json");
            return header("Content-Type", "application/json");
        }

        Builder timeout(Duration d) {
            this.timeout = Objects.requireNonNull(d, "timeout");
            return this;
        }

        Builder maxRetries(int n) {
            this.maxRetries = n;
            return this;
        }

        /** Validates everything at once; the built object is always valid. */
        HttpRequest build() {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new IllegalStateException("url must be http(s): " + url);
            }
            if (body != null && !BODY_ALLOWED.contains(method)) {
                throw new IllegalStateException(method + " requests cannot have a body");
            }
            if (timeout.isNegative() || timeout.isZero()) throw new IllegalStateException("timeout must be positive");
            if (maxRetries < 0 || maxRetries > 10) throw new IllegalStateException("maxRetries must be in [0, 10]");
            // Retrying a non-idempotent POST can duplicate side effects; require an idempotency key.
            if (maxRetries > 0 && method == Method.POST && !headers.containsKey("idempotency-key")) {
                throw new IllegalStateException("retried POST needs an Idempotency-Key header");
            }
            return new HttpRequest(this);
        }
    }
}
