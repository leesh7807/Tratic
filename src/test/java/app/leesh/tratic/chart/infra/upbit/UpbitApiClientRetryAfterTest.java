package app.leesh.tratic.chart.infra.upbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class UpbitApiClientRetryAfterTest {
    @Test
    void resolveRetryAfter_prefersRetryAfterHeaderSeconds() {
        Duration resolved = UpbitApiClient.resolveRetryAfter("2", Duration.ofMillis(700),
                Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(Duration.ofSeconds(2), resolved);
    }

    @Test
    void resolveRetryAfter_usesFallbackWhenHeaderMissing() {
        Duration resolved = UpbitApiClient.resolveRetryAfter(null, Duration.ofMillis(700),
                Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(Duration.ofMillis(700), resolved);
    }

    @Test
    void resolveRetryAfter_returnsNullWhenHeaderInvalidAndFallbackMissing() {
        Duration resolved = UpbitApiClient.resolveRetryAfter("invalid", null,
                Instant.parse("2026-01-01T00:00:00Z"));

        assertNull(resolved);
    }
}
