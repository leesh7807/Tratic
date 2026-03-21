package app.leesh.tratic.chart.infra.binance;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.BinanceProps;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BinanceApiClient {

    private final RestClient client;
    private final ObjectMapper om;
    private final BinanceRateLimiter rateLimiter;
    private final int maxCandlesPerCall;
    private final Clock clock;

    public BinanceApiClient(
            RestClient.Builder builder,
            BinanceProps props,
            ObjectMapper om,
            BinanceRateLimiter rateLimiter,
            Clock clock) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Client-Name", "binance")
                .build();
        this.om = om;
        this.rateLimiter = rateLimiter;
        this.maxCandlesPerCall = props.maxCandlesPerCall();
        this.clock = clock;
        if (this.maxCandlesPerCall <= 0) {
            throw new IllegalArgumentException("clients.binance.max-candles-per-call must be positive");
        }
    }

    public Result<BinanceCandleResponse[], ChartFetchFailure> fetchCandlesTo(ChartSignature sig, String symbol,
            String interval, long to, int limit) {
        if (limit <= 0 || limit > maxCandlesPerCall) {
            return Result.err(new ChartFetchFailure.InvalidRequest(Market.BINANCE));
        }

        int requestWeight = calculateWeight(limit);
        return rateLimiter.acquire(requestWeight)
                .flatMap(ignored -> fetchFromApi(symbol, interval, to, limit));
    }

    private Result<BinanceCandleResponse[], ChartFetchFailure> fetchFromApi(
            String symbol, String interval, long to, int limit) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("endTime", to)
                        .queryParam("limit", limit)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange((req, res) -> {
                    int status = res.getStatusCode().value();
                    if (!res.getStatusCode().isError()) {
                        BinanceCandleResponse[] body = om.readValue(res.getBody(), BinanceCandleResponse[].class);
                        return Result.ok(body);
                    }
                    String rawMessage = null;
                    try {
                        BinanceErrorEnvelope env = om.readValue(res.getBody(), BinanceErrorEnvelope.class);
                        if (env != null && env.msg() != null) {
                            rawMessage = env.msg();
                        }
                    } catch (IOException ignore) {
                        rawMessage = "failed to parse binance error body";
                    }
                    BinanceErrorType binanceType = BinanceErrorType.from(status, rawMessage);
                    Duration retryAfter = null;
                    if (binanceType == BinanceErrorType.RATE_LIMITED) {
                        retryAfter = resolveRetryAfter(res.getHeaders().getFirst("Retry-After"),
                                rateLimiter.estimateRetryAfter(calculateWeight(limit)), clock.instant());
                    }
                    ChartFetchFailure failure = binanceType.toFailure(Market.BINANCE, retryAfter);
                    log.debug("OUT ERR [binance] status={} uri={} rawMessage={}", status, req.getURI(), rawMessage);
                    return Result.err(failure);
                });
    }

    static Duration resolveRetryAfter(String retryAfterHeader, Duration fallbackRetryAfter, Instant now) {
        Duration headerRetryAfter = parseRetryAfterHeader(retryAfterHeader, now);
        return headerRetryAfter != null ? headerRetryAfter : fallbackRetryAfter;
    }

    private static Duration parseRetryAfterHeader(String retryAfterHeader, Instant now) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return null;
        }

        try {
            long seconds = Long.parseLong(retryAfterHeader.trim());
            return seconds < 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignore) {
            // Fall through to RFC-1123 parsing.
        }

        try {
            Instant retryAt = ZonedDateTime.parse(retryAfterHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            Duration retryAfter = Duration.between(now, retryAt);
            return retryAfter.isNegative() ? Duration.ZERO : retryAfter;
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    static int calculateWeight(int limit) {
        if (limit < 100) {
            return 1;
        }
        if (limit < 500) {
            return 2;
        }
        if (limit <= 1000) {
            return 5;
        }
        return 10;
    }

    public record BinanceErrorEnvelope(int code, String msg) {
    }
}
