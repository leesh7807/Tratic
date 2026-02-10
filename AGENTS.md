# AGENTS

## 1) 전체 아키텍처 개요
- 패키지 구분: `app.leesh.tratic.{auth,chart,user,shared}`에 `domain/service/infra` 하위 패키지가 존재함.
- 포트/어댑터 형태로 보이는 인터페이스/구현 쌍:
  - `chart.service.ChartFetcher` 인터페이스 + `chart.infra.upbit.UpbitChartFetcher`, `chart.infra.binance.BinanceChartFetcher` 구현.
  - `auth.service.OAuthAccountLinkRepository` 인터페이스 + `auth.infra.adapter.OAuthAccountLinkRepositoryJpaAdapter` 구현.
- 주요 도메인/모듈 패키지 목록:
  - `auth`(controller/domain/service/infra)
  - `chart`(domain/service/infra)
  - `user`(domain/infra)
  - `shared`(config/logging/Result)
- inbound adapter 위치:
  - `auth.controller.ErrorPageController`
  - 보안 필터 체인/필터: `shared.config.SecurityConfig`, `shared.logging.TraceIdFilter`
- outbound adapter 위치:
  - 외부 API: `chart.infra.upbit.*`, `chart.infra.binance.*`
  - DB/JPA: `auth.infra.adapter.*`, `auth.infra.dao.*`, `user.infra.dao.*`, `auth.infra.entity.*`, `user.infra.entity.*`

## 2) 도메인별 책임 정리
- chart
  - 엔티티/VO: `Chart`, `Candle`, `CandleSeries`, `ChartSignature`, `Symbol`, `Market`, `TimeResolution`
  - 서비스: `chart.service.ChartService`, `ChartFetcherResolver`
  - 외부 의존 Port: `chart.service.ChartFetcher`
  - 알고 있는 것: `chart.domain` 클래스들은 `java.*`와 `chart.domain` 타입만 참조함.
  - 알지 않는 것: `chart.domain`에서 `chart.service`/`chart.infra` 참조 없음.
- auth
  - 엔티티/VO: `OAuthIdentity`, `OAuthProvider`
  - 서비스: `CustomOidcUserService`, `OAuthAccountResolver`
  - 외부 의존 Port: `auth.service.OAuthAccountLinkRepository`
  - 알고 있는 것: `auth.domain`은 `OAuthProvider/Identity`만 포함.
  - 알지 않는 것: `auth.domain`에서 `infra` 참조 없음.
- user
  - 엔티티/VO: `User`, `UserId`
  - 외부 의존 Port: 없음(도메인 레벨 인터페이스 없음)
  - 알고 있는 것: `user.domain`은 `User/UserId`만 포함.
  - 알지 않는 것: `user.domain`에서 `infra` 참조 없음.
- shared
  - 공통 유틸/모델: `shared.Result`
  - 공통 인프라 구성: `shared.config.*`, `shared.logging.*`

## 3) 차트 수집 관련 구조
- `ChartService` 역할과 public 메서드 시그니처:
  - 역할: `ChartFetcherResolver`로 마켓별 fetcher를 선택하고, fetcher가 반환한 `Result`를 그대로 반환.
  - 시그니처: `public Result<Chart, ChartFetchFailure> collectChart(ChartFetchRequest req)`
- `ChartFetcher / Resolver` 구조:
  - `ChartFetcherResolver`는 `List<ChartFetcher>`를 받아 `Market`별로 매핑하고, 중복/미지원 마켓 시 `IllegalArgumentException` 발생.
  - `ChartFetcher`는 `Result<Chart, ChartFetchFailure> fetch(ChartFetchRequest req)` 및 `Market market()` 제공.
- 루프 호출 / pagination / rate-limit 관련 코드 존재 여부:
  - `UpbitChartFetcher`는 요청 수량(`remaining`)이 0이 될 때까지 반복 호출하며 페이지네이션을 수행함.
  - 페이지 크기는 고정 상수(200)가 아니라 `clients.upbit.max-candle-count-per-request` 설정값 사용.
  - 각 페이지의 가장 이른 캔들 시각에서 해상도(step)만큼 뺀 시각을 다음 `to`로 사용하며, 시각이 진전되지 않으면 무한 루프 방지를 위해 중단.
  - 병합 후 시간순 정렬을 수행하고 동일 시각 캔들은 deduplicate 처리.
  - `BinanceChartFetcher`는 `clients.binance.max-candles-per-call` 값을 상한으로 분할해 루프 호출함.
  - `BinanceChartFetcher`는 배치별 `endTime`을 `earliestReturnedCandle.openTime - resolutionDuration`으로 갱신하며, 빈 배열(`[]`) 응답 시 조기 종료함.
  - `BinanceChartFetcher`는 배치 병합 후 `time` 기준 중복 제거를 수행해 도메인(`CandleSeries`) 중복 검증 실패를 방어함.
  - `ChartFetchFailure.RateLimited`가 존재하나, 재시도/대기 로직은 없음.
- 마켓별 제약이 코드에 직접 등장하는 위치:
  - `UpbitApiClient` 주석에 `count <= 200`, 분 단위 `unit`(1,3,5,10,15,30,60,240) 목록 명시.
  - `UpbitChartFetcher.parseMinuteUnit`에서 분 단위 매핑(M1/M3/M5/M15/M30/H1/H4).
  - `BinanceChartFetcher.parseTimeResolution`에서 interval 매핑("1m", "3m", …, "1d").
  - `application.yml`의 `clients.binance.max-candles-per-call` 및 `ClientPropsConfig.BinanceProps.maxCandlesPerCall`로 Binance 1회 호출 상한을 주입받음.

## 4) 분석 관련 구조
- 분석 도메인/패키지/클래스/컨트롤러/유스케이스가 `src/main/java`에 존재하지 않음.
- lookback(캔들 개수) 결정 로직이 존재하지 않음.
- 분석 로직이 차트 도메인에 의존하는 코드가 존재하지 않음.

## 5) 실패/에러 모델
- `shared.Result<T,E>` 존재(Ok/Err sealed interface).
- 차트 수집 실패 모델: `chart.service.ChartFetchFailure`(Temporary/RateLimited/InvalidRequest/Unauthorized/NotFound).
- 에러 흐름:
  - `UpbitApiClient`/`BinanceApiClient`에서 HTTP 에러를 `ChartFetchFailure`로 변환해 `Result.err` 반환.
  - `ChartService.collectChart`는 fetcher의 `Result`를 그대로 반환.

## 6) 테스트 및 미완성 흔적
- 테스트 존재:
  - 차트 도메인: `ChartTest`, `CandleTest`, `CandleSeriesTest`
  - 차트 서비스: `ChartServiceTest`, `ChartFetcherResolverTest`
  - 차트 fetcher 분할/페이지네이션: `UpbitChartFetcherSplitTest`, `BinanceChartFetcherPaginationTest`
  - 외부 API 호출 테스트: `UpbitApiClientTest`, `BinanceApiClientTest` (@Tag("external"))
  - 컨텍스트 로드: `TraticApplicationTests`
- 최근 테스트 변경:
  - `UpbitChartFetcherSplitTest` 삭제.
  - `UpbitChartFetcherPaginationTest` 추가(페이지네이션, partial 응답, empty 응답, 중복 캔들 제거 시나리오).
- 테스트가 없는 영역(테스트 파일 부재 기준):
  - `auth` 패키지 전반(컨트롤러/서비스/도메인/infra) 테스트 없음.
  - `user` 패키지 전반 테스트 없음.
  - `shared` 패키지(config/logging/Result) 테스트 없음.