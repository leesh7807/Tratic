# Next Work Plan

## Current State
- `/api/analyze`는 현재 `trend`, `location`, `pressure` 3축만 외부 응답으로 반환한다.
- analyze 저장 모델은 `direction`, `trend_score`, `location_score`, `pressure_score` 중심으로 단순화돼 있다.
- classify 단계는 여전히 서비스 내부에 남아 있고, 응답 DTO는 한국어 표시 문자열을 직접 반환한다.
- 프론트는 아직 analyze 입력/결과 플로우가 아니라 로그인 초안 화면만 제공한다.
- 백엔드 테스트 코드는 사실상 비어 있어 현재 계약을 고정해 주는 안전망이 없다.

## Goal
- 현재 단순화된 analyze 계약을 흔들리지 않는 기본 제품 기준으로 고정한다.
- 분류 표현, 저장 재현성, 프론트 연결, 테스트를 현재 철학에 맞게 정리한다.

## Working Decisions
- 다음 작업은 새 축을 늘리거나 해석 문장을 되살리는 방향이 아니라, 현재 3축 계약을 안정화하는 방향으로 진행한다.
- 외부 응답 값은 표시 문자열 하드코딩이 맞는지, enum/code 기반 계약으로 바꿀지 먼저 확정한다.
- 저장 모델은 재현 가능한 최소 입력 + 핵심 score를 유지하되, `pressure_raw`, `pressure_view`, 내부 volatility를 실제로 저장할지 여부를 결정해야 한다.
- 프론트는 별도 해석 문장 없이 입력 폼과 3축 결과 표시를 기본 기능으로 연결한다.

## Work Items
1. API 계약 고정
- `AnalyzeResponseDto`가 한국어 라벨을 직접 반환하는 현재 방식의 장단점을 정리한다.
- 프론트/외부 계약 기준으로 `display label` 대신 안정적인 code 응답이 필요한지 결정한다.
- 결정 후 OpenAPI 산출물과 README 설명을 같은 기준으로 맞춘다.

2. classify 경계 재점검
- `AnalyzeService`가 `AnalyzeResult`를 만든 뒤 다시 classify 하는 현재 구조를 유지할지 점검한다.
- 분류가 외부 표현 책임이면 controller/adapter 쪽으로 내리고, 도메인 책임이면 현재 위치를 유지하되 근거를 문서화한다.

3. 저장 재현성 정리
- 현재 DB에는 `trend_score`, `location_score`, `pressure_score`만 저장된다.
- AGENTS 기준에 맞춰 `pressure_raw`, `pressure_view`, 내부 volatility를 저장 재현에 포함할지 결정한다.
- 저장 컬럼을 늘리지 않는다면, 왜 현재 스키마만으로 충분한지 문서에 명시한다.

4. 테스트 기반 추가
- `AnalyzeEngine` 점수 계산 테스트를 추가한다.
- `AnalyzeService`의 lookahead 방지와 부족한 캔들 처리 테스트를 추가한다.
- `AnalyzeController`의 200/400/422/503 응답 계약 테스트를 추가한다.

5. 프론트 기본 흐름 연결
- analyze 요청 입력 폼을 만든다.
- 응답 3축 결과를 최소 UI로 표시한다.
- 로그인 없이도 analyze 호출 가능한 흐름과 로그인 사용자의 저장 흐름 안내를 정리한다.

6. 문서 정합성 정리
- `README.md`에 현재 analyze 요청/응답 예시를 추가한다.
- `AGENTS.md`와 실제 저장 모델 사이에 남은 차이가 있으면 문서를 업데이트한다.
- OpenAPI 생성 절차와 `openapi.json` 비추적 원칙을 개발 흐름 문서에 반영한다.

## Suggested Order
1. 응답 계약 기준(code vs label) 확정
2. 테스트 추가로 현재 동작 고정
3. 저장 재현성 범위 결정
4. 프론트 입력/결과 연결
5. 문서와 OpenAPI 정리

## Risks
- 지금처럼 한국어 표시 문자열을 응답으로 고정하면 다국어/프론트 리디자인 시 API 변경 비용이 커질 수 있다.
- 테스트 없이 프론트 연결을 먼저 진행하면 추후 classify 또는 응답 형식 수정 때 회귀가 커진다.
- 저장 재현성 범위를 늦게 결정하면 DB 스키마 변경이 다시 발생할 수 있다.
