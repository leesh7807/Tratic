# AGENTS

## 문서 목적
- 이 문서는 코드 탐색만으로 즉시 확인 가능한 정보(디렉토리 구조, 클래스 나열, 테스트 파일 목록)를 반복하지 않는다.
- 에이전트가 구현/수정 시 반드시 지켜야 할 **도메인 의사결정과 운영 규칙**만 기록한다.
- 외부 API/저장 포맷도 단순 스펙 나열이 아니라, 왜 그 계약을 유지하는지가 구현만으로 충분히 복원되지 않는 경우에는 이 문서에 남긴다.
- 구현 변경으로 정책이 바뀌면 본문을 직접 갱신한다. 별도 append-only 섹션은 두지 않는다.

## 공통 아키텍처 결정
- 도메인 계층은 인프라를 참조하지 않는다.
- 외부 연동 실패는 도메인 실패 모델(`Result<T, E>`, 실패 타입)로 변환해 상위 계층으로 전달한다.
- 거래소(Upbit/Binance)별 제약은 각 어댑터 내부에서 흡수하고, 서비스 계층은 시장별 분기 로직을 최소화한다.
- 구현/수정 전에는 현재 도메인 내부만 보지 말고 `src/main/java/app/leesh/tratic/shared/**`와 해당 도메인의 `infra/shared/**`를 함께 확인한다.
- 별도 도메인 결정이 없더라도 공통 HTTP 클라이언트, 시간/슬립, 보안, 설정 프로퍼티 바인딩은 `shared/config`와 각 도메인 `infra/shared`를 기준으로 재사용/확장 여부를 먼저 판단한다.

## 코드 작성 규약
- 코드 작성을 마친 뒤에는 변경한 객체들 기준으로 불필요한 import 제거 및 fqcn을 import로 분리한다.
- git 커밋 메시지는 `type(scope): summary` 형태의 컨벤션을 따른다. scope가 불명확하면 생략 가능하지만, 가능한 한 변경 도메인을 드러낸다.

## 차트 수집 도메인 결정
- 차트 수집 엔트리는 `ChartService.collectChart(...)`이며 결과는 `Result<Chart, ChartFetchFailure>`를 사용한다.
- Fetcher 선택은 `Market -> ChartFetcher` 단일 매핑을 강제한다(중복/미지원은 예외 처리).
- 분석/차트 수집 윈도우 크기는 내부 정책으로 `256` 고정한다(외부 요청으로 가변 count를 열지 않음).
- 롤링 윈도우 확장 가능성은 유지하되, 확장 지점은 내부 정책 객체로 한정한다.
- Upbit는 요청 전 필요 호출 수를 선검증하며, 임계 초과 시 API 호출 없이 `InvalidRequest`로 실패시킨다.
- Upbit는 `max-candle-count-per-request`를 설정으로 강제하며, 단일 호출로 상한 초과 요청을 보내지 않는다.
- Upbit는 배치 호출이 필요할 때 `requiredCalls=ceil(count/max)`를 계산해 `rateLimiter.acquire(requiredCalls)`로 선점 후 전체 배치를 하나의 작업으로 실행한다.
- Upbit/Binance 모두 페이지 수집 후 **시간 오름차순 정렬 + 타임스탬프 deduplicate**를 수행한다.
- Binance는 `max-candles-per-call`을 설정으로 강제하며, 요청 `count`가 상한을 초과하면 즉시 `InvalidRequest`로 하드 fail-fast 한다.
- 페이지 수집 중 `RateLimited`가 발생하면 partial chart를 반환하지 않고 즉시 실패로 종료한다.
- 거래소 `429`의 `retryAfter`는 응답 `Retry-After` 헤더를 우선 사용하고, 헤더가 없으면 내부 레이트리미터 계산값으로 보강한다.
- 배치 실행 중 중간 실패가 발생해도 선점한 limiter 용량은 소진된 것으로 간주한다(롤백 없음).
- 레이트리미터는 거래소별 요청 단위(Upbit: req/sec, Binance: weight/min)를 검증하며, 인터럽트는 `RateLimited`로 처리한다.
- fast-fail 기준은 거래소 공통으로 `대기시간 3초 이상이면 실패`로 정렬한다.

## 분석 도메인 결정
- 분석 파이프라인은 고정: `캔들 수집 -> 전처리(현재 버킷 제외) -> 분석 엔진`.
- `analyze`는 내부 도메인 책임으로 다룬다. analyze는 차트를 설명 가능한 수학적 상태로 규정하는 과정이며, 삭제/추가 변경보다 기존 규칙을 옵션과 임계값으로 보수적으로 조정하는 영역으로 취급한다.
- analyze 이후 해석은 `AnalyzeResult`를 입력으로 받아 안정된 scenario/meta snapshot을 산출하는 별도 단계로 다룬다.
- `AnalyzeScenario` 같은 안정 식별자와 해석 결과 snapshot 계약은 도메인 타입으로 유지한다.
- 매트릭스 기반 해석 정책과 그 정책을 사용해 scenario를 선택하는 해석기는 현재 구현체이며 인프라 책임으로 둔다. matrix는 유일한 해석 방식으로 고정하지 않는다.
- `interpret`는 scenario/meta snapshot을 사용자/도구별 표현으로 렌더링하는 외부 인프라 책임으로 둔다. 어떤 문장/포맷/툴 출력으로 변환할지는 교체 가능한 어댑터 경계 뒤에 둔다.
- analyze 이후 내부 해석 정책은 시나리오 매트릭스로 동작하며, 현재 정책은 조합 coverage 전수 검증 대신 마지막 fallback 시나리오로 항상 결과를 반환하는 쪽을 기본으로 한다.
- 시나리오는 문자열 상수 대신 안정적인 enum 식별자로 관리하고, 저장/통계/사후평가 기준값으로 사용한다.
- 해석 결과 snapshot은 분석 결과 저장 레코드에 함께 보존할 수 있다. 단, 사용자 문장(summary)처럼 변경 가능한 표현값은 저장 기준값으로 취급하지 않는다.
- 로그인 사용자 저장 레코드에는 최소한 `resolution`, `scenario`, `policyVersion`을 포함해 당시 해석 기준을 재현 가능하게 남긴다.
- 룩어헤드 방지: `entryAt` 버킷 캔들은 지표 계산에서 제외한다.
- 손절가는 분석 입력으로 유지한다. 손절가는 setup 무효화 지점과 리스크 크기를 규정하는 핵심 입력으로 본다.
- 익절가는 기본 분석 입력에서 제거한다. 익절가는 setup 자체의 성립보다 운영 전략 선택에 가까운 값으로 간주한다.
- 사용자 입력은 기본 모드 기준 `market, symbol, resolution, entryAt, entryPrice, stopLossPrice, positionPct`를 사용한다.
- 방향(`LONG/SHORT`)은 손절가와 진입가의 가격관계로 추론한다.
  - `stopLoss < entry` => LONG
  - `entry < stopLoss` => SHORT
  - 그 외는 `InvalidInput`
- 비로그인 사용자도 분석 요청 가능하다.
- 로그인 사용자 요청은 분석 결과를 저장한다(현재 구현은 DB persist 활성화).

## 분석 지표 정책
- 내부 analyze 출력 축은 `trend, volatility, location, pressure` 4개를 유지한다.
- 추세 점수:
  - `trend = 0.5 * trend_lr + 0.5 * trend_ma`
  - `trend_lr = LinearRegressionSlope(close tail) / (ATR + epsilon)`
  - `trend_ma = (EMA_fast - EMA_slow) / (ATR + epsilon)`
  - `ATR floor`(ratio/min)로 저변동 구간 과증폭을 완화한다.
- 변동성 점수:
  - 점수는 `ATR/SMA` 비율의 Z-score 기반 정규화 점수 사용.
- 위치 점수:
  - 최근 윈도우의 `LowestLow~HighestHigh` 대비 종가 상대 위치를 0~100으로 산출한다.
- 수급 압력:
  - `raw = w1*posClose + w2*body + w3*wickDiff`
  - 거래량 가중치를 곱한 `pressure_raw`를 계산하고, 표시값 `pressure_view`는 EMA 스무딩을 적용한다.
  - 내부 analyze 결과와 로그인 사용자 저장 레코드에는 `pressure_score`, `pressure_raw`, `pressure_view`를 유지한다.

## 분석 응답 계약 결정
- `/api/analyze`의 외부 응답은 raw 지표값 나열보다 해석 결과 전달을 우선한다.
- 현재 응답 계약은 `direction, scenario, summary, bias, confidence, riskLevel`을 기준으로 유지한다.
- `scenario`는 안정 식별자이며, `summary`는 교체 가능한 렌더러가 생성하는 표현값으로 본다.
- raw analyze 축 점수(`trend`, `volatility`, `location`, `pressure_*`)는 현재 외부 응답 계약에 직접 노출하지 않는다.
- 익절가를 입력으로 받지 않는 대신, 필요하면 결과/후속 UX에서 목표 구간이나 청산 시나리오를 제안하는 방향으로 확장한다.

## 설정/운영 결정
- 분석 엔진 파라미터는 설정값으로 주입하며, 해상도별 override를 허용한다.
- `analyze.fetch-candle-count`는 엔진 최소 필요 캔들 수 이상이어야 하며, 부팅 시 검증한다.
- 분석 실패는 `InvalidInput`, `InsufficientCandles`, `ChartDataUnavailable` 3분류를 기준으로 API 에러로 매핑한다.
- 백엔드 Gradle 빌드는 프론트 빌드를 포함한다. `processResources` 전에 프론트 산출물을 생성하고 Spring 정적 리소스 디렉토리로 sync하는 흐름을 전제로 유지한다.
- 프론트 변경 시에도 배포/패키징 경로는 별도 프론트 서버가 아니라 백엔드가 포함한 정적 리소스 서빙과 SPA forward 규칙을 기준으로 깨지지 않게 유지한다.
- 프론트용 API 스펙 JSON은 `cd backend && ./gradlew generateOpenApiSpec` 전용 태스크로 생성한다. 산출물은 저장소 루트 `openapi.json`이며 Git 추적 대상에 포함하지 않는다.

## 향후 확장 결정
- 외부 LLM 기반 `interpret`는 교체 가능한 어댑터 경계 뒤에 둔다.
- 도메인 `analyze` 결과와 사용자 대상 렌더링 결과를 동일 객체로 섞지 않는다. analyze snapshot과 scenario snapshot은 도메인에서 안정적으로 유지하고, matrix/rule/LLM 등 어떤 방식으로 scenario를 선택할지는 교체 가능한 해석 어댑터 경계 뒤에 둔다.
- 로컬 LLM/룰 엔진으로의 대체 가능성을 유지하기 위해, 분석 엔진 입력/출력 계약은 도메인 타입 중심으로 고정한다.
