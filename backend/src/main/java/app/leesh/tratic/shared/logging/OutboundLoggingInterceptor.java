package app.leesh.tratic.shared.logging;

import java.io.IOException;
import java.net.URI;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OutboundLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Request-Id";
    private static final String CLIENT_NAME_HEADER = "X-Client-Name";

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution) throws IOException {

        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            request.getHeaders().set(TRACE_ID_HEADER, traceId);
        }

        String clientName = request.getHeaders()
                .getFirst(CLIENT_NAME_HEADER);

        long start = System.nanoTime();
        URI uri = request.getURI();

        log.debug("OUT -> [{}] {} {}", clientName, request.getMethod(), uri);

        try {
            ClientHttpResponse res = execution.execute(request, body);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            log.debug(
                    "OUT <- [{}] {} {} ({}ms)",
                    clientName,
                    res.getStatusCode().value(),
                    uri,
                    elapsedMs);
            return res;

        } catch (IOException e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            log.warn(
                    "OUT !! [{}] {} {} failed ({}ms)",
                    clientName,
                    request.getMethod(),
                    uri,
                    elapsedMs,
                    e);
            throw e;
        }
    }

}
