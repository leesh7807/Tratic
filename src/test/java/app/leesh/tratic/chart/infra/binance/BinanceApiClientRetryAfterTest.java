package app.leesh.tratic.chart.infra.binance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class BinanceApiClientRetryAfterTest {
    @Test
    void resolveRetryAfter_prefersRetryAfterHeaderSeconds() {
        Duration resolved = BinanceApiClient.resolveRetryAfter("7", Duration.ofSeconds(59),
                Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(Duration.ofSeconds(7), resolved);
    }

    @Test
    void resolveRetryAfter_usesFallbackWhenHeaderMissing() {
        Duration resolved = BinanceApiClient.resolveRetryAfter(null, Duration.ofSeconds(59),
                Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(Duration.ofSeconds(59), resolved);
    }

    @Test
    void resolveRetryAfter_returnsNullWhenHeaderInvalidAndFallbackMissing() {
        Duration resolved = BinanceApiClient.resolveRetryAfter("not-a-duration", null,
                Instant.parse("2026-01-01T00:00:00Z"));

        assertNull(resolved);
    }
}
