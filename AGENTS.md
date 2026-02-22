# AGENTS

## 1) 모듈/패키지
- 루트 패키지: `app.leesh.tratic`
- 모듈 구성: `auth(controller/domain/service/infra)`, `chart(domain/service/infra)`, `user(domain/infra)`, `shared(config/logging/time/Result)`
- 진입점: `TraticApplication`, `auth.controller.ErrorPageController`
- 보안 구성: `shared.config.SecurityConfig`, `shared.logging.TraceIdFilter`
- 공통 HTTP 구성: `shared.config.CommonRestClientConfig`, `shared.logging.OutboundLoggingInterceptor`

## 2) 포트/어댑터
- chart 포트: `chart.service.ChartFetcher`
- chart 어댑터: `chart.infra.upbit.UpbitChartFetcher`, `chart.infra.binance.BinanceChartFetcher`
- auth 포트: `auth.service.OAuthAccountLinkRepository`
- auth 어댑터: `auth.infra.adapter.OAuthAccountLinkRepositoryJpaAdapter`
- DB/JPA 어댑터: `auth.infra.{dao,entity}.*`, `user.infra.{dao,entity}.*`
- 외부 API 어댑터: `chart.infra.upbit.*`, `chart.infra.binance.*`

## 3) 도메인 경계
- chart 도메인: `Chart`, `Candle`, `CandleSeries`, `ChartSignature`, `Symbol`, `Market`, `TimeResolution`
- auth 도메인: `OAuthIdentity`, `OAuthProvider`
- user 도메인: `User`, `UserId`
- 의존 규칙: domain -> infra 참조 없음
- 분석 영역: 전용 패키지/유스케이스 없음

## 4) 차트 수집
- 엔트리: `ChartService.collectChart(ChartFetchRequest) -> Result<Chart, ChartFetchFailure>`
- 리졸버 동작: `ChartFetcherResolver`가 `Market -> ChartFetcher` 매핑, 중복/미지원 시 `IllegalArgumentException`
- 실패 타입 위치: `chart.service.error.ChartFetchFailure`

## 5) 거래소 구현/제약
- Upbit 페이지 크기: `clients.upbit.max-candle-count-per-request`
- Upbit 선검증: `requiredCalls=ceil(count/max)` 계산
- Upbit fail-fast: `requiredCalls > 10`이면 `InvalidRequest` 반환(API 호출 전)
- Upbit API 호출 구조: `UpbitApiClient.fetchCandles(...)` 내부에서 `rateLimiter.acquire(1).flatMap(...)` 체인 후 HTTP 호출(`fetchFromApi(...)`)
- Upbit 페이지 이동: `nextTo = earliest - resolutionDuration`, 빈 배치/진전 없음 시 중단
- Upbit 후처리: 시간 정렬 + 동일 시각 deduplicate
- Upbit 중간 실패 처리: 배치 누적 중 `RateLimited` 발생 시 partial chart 반환 없이 즉시 `Err(ChartFetchFailure.RateLimited)`로 종료
- Upbit 해상도 매핑: `M1/M3/M5/M15/M30/H1/H4` (D1은 day endpoint)
- Binance 분할 상한: `clients.binance.max-candles-per-call`
- Binance 페이지 이동: `endTime = earliest - resolutionDuration`, 빈 배치 시 중단
- Binance 후처리: 시간 정렬 + `time` deduplicate
- Binance interval 매핑: `M1/M3/M5/M15/M30/H1/H4/D1 -> 1m/3m/5m/15m/30m/1h/4h/1d`
- Binance API 호출 구조: `BinanceApiClient.fetchCandlesTo`는 `rateLimiter.acquire(...).flatMap(...)` 체인, HTTP 로직은 `fetchFromApi(...)`

## 6) 레이트리밋/결과모델
- Upbit limiter 규칙: 초당 10req, `requestCount<=0 || >10`이면 `InvalidRequest`, `Remaining-Req(sec)` 동기화, 인터럽트 시 `RateLimited`
- Binance limiter 규칙: 분당 6000 weight, `requestWeight<=0 || >6000`이면 `InvalidRequest`, 인터럽트 시 `RateLimited`
- fast-fail 임계값 키: `clients.{upbit,binance}.fast-fail-wait-threshold`
- 결과 모델: `shared.Result<T,E>` (`Ok`/`Err`, `map`, `flatMap`)
- 실패 모델: `Temporary`, `RateLimited`, `InvalidRequest`, `Unauthorized`, `NotFound`
- API 에러 매핑: `UpbitApiClient`, `BinanceApiClient`가 HTTP 오류를 `ChartFetchFailure`로 변환

## 7) 설정/테스트
- 클라이언트 설정 키: `clients.upbit.base-url`, `clients.upbit.max-candle-count-per-request`, `clients.upbit.fast-fail-wait-threshold`, `clients.binance.base-url`, `clients.binance.max-candles-per-call`, `clients.binance.fast-fail-wait-threshold`
- 설정 바인딩: `chart.infra.shared.ClientPropsConfig`
- 테스트 존재(domain): `ChartTest`, `CandleTest`, `CandleSeriesTest`
- 테스트 존재(service): `ChartServiceTest`, `ChartFetcherResolverTest`
- 테스트 존재(fetcher): `UpbitChartFetcherPaginationTest`, `BinanceChartFetcherPaginationTest` (Upbit fetcher의 limiter 모킹/검증 제거)
- 테스트 존재(limiter): `UpbitRateLimiterTest`, `BinanceRateLimiterTest`
- 테스트 존재(api client): `UpbitApiClientTest`, `BinanceApiClientTest` (`@Tag("external")`)
- 테스트 존재(boot): `TraticApplicationTests`
- 테스트 부재: `auth`, `user`, `shared`
