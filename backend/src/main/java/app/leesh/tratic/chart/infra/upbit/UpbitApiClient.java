package app.leesh.tratic.chart.infra.upbit;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.UpbitProps;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UpbitApiClient {

    private final RestClient client;
    private final ObjectMapper om;
    private final UpbitRateLimiter rateLimiter;
    private final Clock clock;

    public UpbitApiClient(
            RestClient.Builder builder,
            UpbitProps props,
            ObjectMapper om,
            UpbitRateLimiter rateLimiter,
            Clock clock) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Client-Name", "upbit")
                .build();
        this.om = om;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    /**
     * upbit 분봉 캔들 요청
     *
     * @param unit 원하는 분봉 (1,3,5,10,15,30,60,240)
     * @param market 조회 요청하는 페어 ex) KRW-BTC
     * @param to 조회 기간의 종료 시각
     * @param count 캔들 개수 <= 200
     */
    public Result<UpbitCandleResponse[], ChartFetchFailure> fetchMinuteCandles(long unit, String market, String to,
            long count) {
        return fetchCandles("/candles/minutes/{unit}", market, to, count, unit);
    }

    /**
     * upbit 일봉 캔들 요청
     *
     * @param market 조회 요청하는 페어 ex) KRW-BTC
     * @param to 조회 기간의 종료 시각
     * @param count 캔들 개수 <= 200
     */
    public Result<UpbitCandleResponse[], ChartFetchFailure> fetchDayCandles(String market, String to, long count) {
        return fetchCandles("/candles/days", market, to, count, null);
    }

    private Result<UpbitCandleResponse[], ChartFetchFailure> fetchCandles(@NonNull String path, String market,
            String to, long count, Long unit) {
        return fetchFromApi(path, market, to, count, unit);
    }

    private Result<UpbitCandleResponse[], ChartFetchFailure> fetchFromApi(@NonNull String path, String market,
            String to, long count, Long unit) {
        return client.get()
                .uri(uriBuilder -> {
                    if (unit != null) {
                        return uriBuilder
                                .path(path)
                                .queryParam("market", market)
                                .queryParam("to", to)
                                .queryParam("count", count)
                                .build(unit);
                    }

                    return uriBuilder
                            .path(path)
                            .queryParam("market", market)
                            .queryParam("to", to)
                            .queryParam("count", count)
                            .build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .exchange((req, res) -> {
                    String remainingReq = res.getHeaders().getFirst("Remaining-Req");
                    rateLimiter.syncRemainingReqHeader(remainingReq);

                    int status = res.getStatusCode().value();
                    if (!res.getStatusCode().isError()) {
                        UpbitCandleResponse[] body = om.readValue(res.getBody(), UpbitCandleResponse[].class);
                        return Result.ok(body);
                    }

                    String rawMessage = null;
                    try {
                        UpbitErrorEnvelope env = om.readValue(res.getBody(), UpbitErrorEnvelope.class);
                        if (env != null && env.error() != null) {
                            rawMessage = env.error().message();
                        }
                    } catch (IOException ignore) {
                        rawMessage = "failed to parse upbit error body";
                    }

                    UpbitErrorType upbitType = UpbitErrorType.from(status, rawMessage);
                    Duration retryAfter = null;
                    if (upbitType == UpbitErrorType.RATE_LIMITED) {
                        retryAfter = resolveRetryAfter(res.getHeaders().getFirst("Retry-After"),
                                rateLimiter.estimateRetryAfter(1), clock.instant());
                    }
                    ChartFetchFailure failure = upbitType.toFailure(Market.UPBIT, retryAfter);

                    log.debug("OUT ERR [upbit] status={} uri={} rawMessage={}", status, req.getURI(), rawMessage);
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

    public record UpbitErrorEnvelope(UpbitError error) {
    }

    public record UpbitError(int name, String message) {
    }
}
