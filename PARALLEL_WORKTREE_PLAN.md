# Parallel Worktree Plan

## Goal
- analyze 후속 작업을 서로 다른 쓰기 범위로 분리해 병렬 진행한다.
- 각 작업은 독립적으로 진행하되, 최종 병합 시 충돌이 적도록 책임 경계를 명확히 둔다.

## Shared Rules
- 모든 작업의 base branch는 `main`이다.
- 공통 정책 기준은 `AGENTS.md`와 `NEXT_WORK_PLAN.md`를 따른다.
- 다른 worktree의 파일을 미리 건드려 충돌을 만드는 수정은 피한다.
- 응답 계약 변경 여부는 `track 1`에서 먼저 결정한다.
- `track 2`, `track 3`은 그 결정이 내려지기 전까지 응답 필드명/형식을 임의 확정하지 않는다.

## Recommended Split
1. `track 1`: analyze 계약 및 분류 경계 결정
2. `track 2`: backend 테스트와 저장 재현성 정리
3. `track 3`: frontend analyze 플로우와 사용자 표시

## Setup Commands
```bash
git worktree add /home/lees/projects/worktrees/feat-analyze-contract-boundary -b feat/analyze-contract-boundary main
git worktree add /home/lees/projects/worktrees/feat-analyze-test-and-persistence -b feat/analyze-test-and-persistence main
git worktree add /home/lees/projects/worktrees/feat-frontend-analyze-flow -b feat/frontend-analyze-flow main
```

## Cleanup Commands
```bash
git worktree remove /home/lees/projects/worktrees/feat-simplify-analyze-contract
git branch -d feat/simplify-analyze-contract
```

## Track 1
### Branch / Worktree
- branch: `feat/analyze-contract-boundary`
- worktree: `/home/lees/projects/worktrees/feat-analyze-contract-boundary`

### Intent
- `/api/analyze` 외부 계약을 고정한다.
- classify가 도메인 책임인지 표현 책임인지 경계를 정리한다.
- 이후 트랙이 의존할 기준을 만든다.

### Owns
- `backend/src/main/java/app/leesh/tratic/analyze/controller/**`
- `backend/src/main/java/app/leesh/tratic/analyze/service/AnalyzeService.java`
- `backend/src/main/java/app/leesh/tratic/analyze/domain/classification/**`
- `backend/src/main/resources/config/analyze-engine.yml`
- `README.md`
- `AGENTS.md` 중 analyze 계약 관련 서술

### Avoids
- 프론트 구현 파일
- persistence 스키마 변경
- 테스트 파일 신설/대량 수정

### Deliverables
- 응답 계약을 `label` 유지 또는 `code + label`/`code only` 중 하나로 확정
- classify 위치 유지/이동 결정
- 결정 근거 문서화
- 다른 트랙이 참조할 응답 예시 제공

### Done Criteria
- `README.md`에 현재 analyze 요청/응답 예시가 있다.
- `AGENTS.md`와 실제 응답 계약이 어긋나지 않는다.
- track 2, track 3이 따를 “고정 계약”이 문서와 코드에 반영돼 있다.

### Instruction Set
```text
목표:
analyze API의 외부 계약과 classify 책임 경계를 확정한다. 이 트랙은 다른 병렬 작업이 의존하는 기준선을 만드는 역할이다.

집중 범위:
- AnalyzeResponseDto와 AnalyzeController 기준으로 외부 응답 계약 정리
- AnalyzeService 내부 classify 위치 유지/이동 검토
- README.md, AGENTS.md 문서 정합성 반영

지켜야 할 것:
- 새 축 추가 금지
- 해석 문장/summary/scenario 부활 금지
- persistence 스키마 변경 금지
- 프론트 파일 수정 금지

반드시 남길 결과:
- 최종 응답 JSON 예시
- 왜 이 계약을 선택했는지 짧은 근거
- classify 책임 위치에 대한 명시적 결정
```

## Track 2
### Branch / Worktree
- branch: `feat/analyze-test-and-persistence`
- worktree: `/home/lees/projects/worktrees/feat-analyze-test-and-persistence`

### Intent
- 현재 analyze 계산과 실패 매핑을 테스트로 고정한다.
- 저장 재현성 범위를 문서 또는 코드로 정리한다.

### Owns
- `backend/src/test/**`
- `backend/src/main/java/app/leesh/tratic/analyze/domain/AnalyzeEngine.java`
- `backend/src/main/java/app/leesh/tratic/analyze/domain/AnalyzeResult.java`
- `backend/src/main/java/app/leesh/tratic/analyze/infra/persistence/**`
- `backend/src/main/java/app/leesh/tratic/analyze/service/error/**`
- 필요 시 테스트 지원용 설정 파일

### Avoids
- `AnalyzeController`, `AnalyzeResponseDto`의 계약 결정
- 프론트 파일
- README의 최종 API 설명 문구

### Deliverables
- `AnalyzeEngine` 계산 테스트
- `AnalyzeService`의 부족한 캔들 / lookahead 방지 검증
- controller 레벨 200/400/422/503 계약 테스트
- 저장 스키마 유지 또는 확장 필요성에 대한 결론

### Dependencies
- 응답 계약 형식은 track 1 결정에 맞춘다.
- track 1이 늦어지면 우선 engine/service/persistence 테스트부터 진행하고 controller 응답 assertion은 마지막에 맞춘다.

### Done Criteria
- 현재 핵심 동작이 테스트로 고정된다.
- 저장 재현성 범위에 대해 “현 상태 유지” 또는 “추가 저장 필요” 중 하나가 명시된다.
- controller 계약 테스트가 track 1의 최종 응답 형식을 따른다.

### Instruction Set
```text
목표:
analyze 도메인 계산, 서비스 실패 처리, 저장 재현성을 테스트와 코드 기준으로 고정한다.

집중 범위:
- backend 테스트 추가
- AnalyzeEngine, service, persistence 검증
- 저장 컬럼이 현재로 충분한지 판단

지켜야 할 것:
- 응답 계약 자체를 새로 설계하지 말 것
- 프론트 수정 금지
- README 최종 서술 변경 금지

진행 순서 권장:
1. engine 테스트
2. service 테스트
3. persistence 판단
4. controller 테스트는 track 1 결정 반영 후 마무리

반드시 남길 결과:
- 실패/회귀를 잡아주는 테스트
- 저장 범위에 대한 명시적 판단과 근거
```

## Track 3
### Branch / Worktree
- branch: `feat/frontend-analyze-flow`
- worktree: `/home/lees/projects/worktrees/feat-frontend-analyze-flow`

### Intent
- 현재 로그인 초안 화면을 analyze 입력/결과 흐름으로 확장한다.
- 백엔드의 3축 결과를 최소 UI로 보여준다.

### Owns
- `frontend/src/App.jsx`
- `frontend/src/styles.css`
- `frontend/src/main.jsx`
- 필요 시 프론트 설정 파일

### Avoids
- 백엔드 응답 계약 결정
- JPA/entity 변경
- AGENTS.md 수정

### Dependencies
- track 1의 최종 응답 형식을 따른다.
- track 1 결정 전에는 프론트 내부 어댑터 함수로 응답 매핑을 분리해 충돌을 줄인다.

### Deliverables
- analyze 입력 폼
- API 호출 상태 처리(`idle/loading/success/error`)
- 3축 결과 표시
- 비로그인 호출 가능, 로그인 시 저장된다는 현재 제품 설명 반영

### Done Criteria
- 프론트에서 analyze 요청을 직접 보낼 수 있다.
- 성공/실패 상태가 보인다.
- 결과 UI가 현재 계약(`trend`, `location`, `pressure`)을 반영한다.

### Instruction Set
```text
목표:
frontend에 analyze 입력과 3축 결과 표시를 추가해 최소 동작 제품을 만든다.

집중 범위:
- App.jsx 중심으로 입력 폼, 요청 상태, 결과 렌더링 구현
- styles.css에서 현재 화면 구조 개선

지켜야 할 것:
- 백엔드 계약을 임의 변경하지 말 것
- 해석 문장/시나리오 UI 추가 금지
- 백엔드 파일 수정 금지

구현 팁:
- 응답 파싱은 별도 작은 함수로 분리해서 track 1 계약 변경에 대응
- 결과는 trend/location/pressure 3개만 표시
- 로그인 여부는 UX 안내 문구 수준으로만 다루고 인증 흐름은 건드리지 않음

반드시 남길 결과:
- 수동으로 호출 가능한 analyze 폼
- 성공/실패가 보이는 UI
```

## Merge Order
1. `feat/analyze-contract-boundary`
2. `feat/analyze-test-and-persistence`
3. `feat/frontend-analyze-flow`

## Notes
- `track 1`이 기준선이다. 응답 계약이 흔들리면 나머지 두 트랙의 수정량이 커진다.
- `track 2`와 `track 3`은 동시에 시작해도 되지만, 응답 형식과 표시값 확정은 `track 1` 머지 전 재확인한다.
- 기존 `feat/simplify-analyze-contract`는 `main`에 이미 포함된 상태라 새 병렬 분할 시작 전에 정리한다.
