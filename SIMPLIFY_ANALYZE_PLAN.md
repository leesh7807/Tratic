# Simplify Analyze Plan

## Goal
- Tratic의 기본 동작을 `한 매매의 성격을 드러내는 최소 분석`으로 재정의한다.
- 사용자 입력은 `market, symbol, resolution, entryAt, entryPrice, direction`으로 유지한다.
- 외부 출력은 `trend, location, pressure` 3축만 제공한다.
- `volatility`는 외부 출력 축에서 제거하고 내부 환경값으로만 유지한다.
- `scenario`, `summary`, 해석 문장 렌더링, matrix 기반 시나리오 선택은 기본 흐름에서 제거한다.
- 단일 사용자 전제를 기준으로, 불친절하지만 일관된 출력 계약을 허용한다.

## Scope
- 백엔드 analyze API 계약 재정의
- analyze 서비스/도메인 결과 구조 단순화
- 해석 계층 및 관련 정책 제거
- 저장 모델 단순화
- 테스트와 문서 재정렬
- 프론트 준비 문서에서 해석 중심 설명 제거
- symbol search 관련 워크트리/브랜치는 병합 대상에서 제외

## Non-Goals
- 이번 작업에서 새로운 해석 문장, 시나리오 enum, 라벨 번역 레이어를 추가하지 않는다.
- 단일 사용자 전제를 깨는 멀티 유저 기능 확장은 다루지 않는다.
- symbol search, catalog, 자동완성은 이번 정리에서 제거 방향으로 본다.

## Target Contract
- request
  - `market`
  - `symbol`
  - `resolution`
  - `entryAt`
  - `entryPrice`
  - `direction`
- response
  - `trend`
  - `location`
  - `pressure`

## Working Decisions
- `AnalyzeResult`는 외부 응답과 저장 기준이 되는 최소 축만 중심에 둔다.
- `volatility` 계산은 내부 정책/보조값으로 남길 수 있지만 API와 기본 저장 계약의 주인공으로 두지 않는다.
- `AnalyzeService`는 더 이상 해석기를 거치지 않고 analyze 결과를 직접 반환한다.
- `AnalyzeController`는 렌더러 없이 3축 DTO만 매핑한다.
- 로그인 사용자 저장 레코드는 재현 가능한 입력과 3축 결과만 우선 저장한다.
- `scenario`, `policyVersion` 저장은 기본 계약에서 제거한다.
- 문장 출력이 없으므로 프론트도 원시 문장이 아니라 3축 결과 표시만 준비한다.

## Implementation Order
1. 문서 기준 변경
   - `AGENTS.md`, `README.md`에서 scenario/summary/4축 라벨링 전제를 제거한다.
   - 프론트 문서에서 해석 결과 중심 문구를 제거한다.
2. API 계약 변경
   - `AnalyzeResponseDto`를 3축 응답으로 교체한다.
   - OpenAPI 테스트와 예시를 새 계약에 맞춘다.
3. 서비스/도메인 단순화
   - `AnalyzeService` 반환형을 해석 결과가 아닌 분석 결과로 전환한다.
   - `AnalyzeResult`를 3축 중심으로 정리한다.
   - `AnalysisEngine`에서 volatility를 내부값으로 격하한다.
4. 해석 계층 제거
   - `AnalyzeInterpreter`
   - `AnalyzeInterpretation`
   - `AnalyzeInterpretationRenderer`
   - `MatrixAnalyzeInterpreter`
   - `MatrixInterpretationPolicy`
   - 관련 테스트
5. 저장 모델 정리
   - 엔티티/리포지토리에서 `scenario`, `policyVersion` 제거 여부를 확정한다.
   - 필요 시 migration 또는 ddl 변경 계획을 추가한다.
6. 테스트 복구
   - controller/service/domain 테스트를 3축 계약 기준으로 다시 작성한다.
7. merge 준비
   - 메인 대비 변경 의도를 다시 요약하고, 제거한 워크트리/브랜치를 정리한다.

## Removal Candidates
- 병합 대상으로 보지 않을 워크트리/브랜치
  - `feat/analyze-interpretation-mapper`
  - `feat/analyze-signal-labels`
  - `feat/frontend-work`
  - `feat/symbol-search-strategy`
- 위 작업축들은 현재 목표와 철학이 다르므로 참조만 하고 병합 소스로 사용하지 않는다.

## Risks To Handle
- DB 스키마가 `scenario`, `policyVersion`, `volatility_score`를 이미 전제하면 마이그레이션이 필요할 수 있다.
- 테스트와 OpenAPI가 해석 계층 타입을 직접 mock하고 있어 삭제 범위가 생각보다 넓다.
- 프론트는 결과 문장을 전제로 만든 임시 설명이 남아 있어, API 변경만 하고 문서를 안 바꾸면 다시 흔들린다.

## Branching
- base: `main`
- work branch: `feat/simplify-analyze-contract`
- worktree: `/home/lees/projects/worktrees/feat-simplify-analyze-contract`

## Merge Readiness Checklist
- `README.md`와 `AGENTS.md`가 새 제품 정의를 반영한다.
- `/api/analyze` 응답이 3축만 반환한다.
- 해석 계층 코드와 관련 테스트가 제거되거나 더 이상 기본 경로에 연결되지 않는다.
- persistence 계약이 새 기준과 맞는다.
- 프론트 문서가 `scenario/summary`가 아닌 3축 결과 기준으로 정리된다.
