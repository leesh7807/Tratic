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

## 8) 분석 도메인 설계 결정 (append-only)
- 신규 분석 도메인 도입: DB 저장 없이 동작하며, 비로그인 사용자도 분석 요청 가능하도록 설계
- 분석 파이프라인: `캔들 수집 -> 전처리(특징 추출) -> 분석 엔진` 순서 고정
- 외부 LLM API 연동은 분리 가능한 어댑터로 두고, 추후 로컬 LLM 엔진으로 교체/확장 가능해야 함
- 분석 출력 4축: `추세(trend)`, `변동성(volatility)`, `위치(location)`, `수급 압력(pressure)`
- 사용자 입력: `종목`, `진입 시점`, `롱/숏`, `전체 비중`, `손절 라인`, `익절 라인`
- 룩어헤드 방지 정책: 모든 분석 지표는 `현재 캔들 제외(exclude current candle)` 기준으로 계산
- 추세 계산 정책:
  - `trend = 0.5 * trend_lr + 0.5 * trend_ma`
  - `trend_lr = LinearRegressionSlope(C, N) / (ATR(N) + epsilon)`
  - `trend_ma = (EMA(C, N_f) - EMA(C, N_s)) / (ATR(N) + epsilon)`
  - `trend_score = 100 * clamp(trend, -1, 1)`
  - 안정성 보완: `ATR`가 과소일 때 급등 방지를 위해 `ATR floor` 적용
- 변동성 계산 정책:
  - 점수: `vol_score = 100 * clamp(ZScore(ATR(N)/(SMA(C, N)+epsilon), M) / 3, 0, 1)`
  - 라벨: `short_vol=ATR(14)`, `long_vol=ATR(100)`, `ratio=short_vol/(long_vol+epsilon)` 기반 `LOW/MID/HIGH`
  - 기본 경계: `ratio > 1.4 -> HIGH`, `ratio < 0.7 -> LOW`, 그 외 `MID`
  - 히스테리시스 적용: 경계값 인접 구간에서 라벨 진동(chattering) 방지
  - 최소 데이터 길이: `ATR(100)` 계산 가능한 길이 미만이면 라벨 미산출(`UNKNOWN` 등) 처리
- 위치 계산 정책:
  - `location_score = 100 * ((C - LowestLow(N)) / (HighestHigh(N) - LowestLow(N) + epsilon))`
  - 해석: `0=구조 하단`, `50=중앙`, `100=구조 상단`
- 수급 압력 계산 정책:
  - `raw = 0.6*pos_c + 0.3*body + 0.1*wick_diff`
  - `pressure = vol_w * raw`, `pressure_score = 100 * clamp(pressure, -1, 1)`
  - 포화 완화: clamp 포화 빈도 감소를 위해 `vol_w` 상한 또는 `raw` 가중치 조정 정책 반영
  - 노출 정책: `pressure_raw`(원본)와 `pressure_view=EMA(pressure_raw, 3~5)`(표시용 스무딩) 동시 운영
