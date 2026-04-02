# Frontend Redesign Prep

## 목적
- 현재 프론트 구현 진행 상태를 기준안으로 보지 않고, 새 디자인 구축 전에 유지 계약과 폐기 범위를 명확히 한다.
- 세부 시각안이 아니라 재구축 전에 고정해야 할 제품 흐름과 구현 준비 항목만 정리한다.

## 유지할 기준
- 제품 정체성은 `분석 저널`이며, 실시간 매매 실행 UI처럼 보이지 않게 유지한다.
- 기본 흐름은 `거래 기록 입력 -> 해석 결과 확인`이며 비로그인 사용자도 완결 가능해야 한다.
- 결과는 `scenario`, `summary` 중심으로 읽혀야 한다.
- `scenario`는 안정 식별자이고 저장/통계/분기 기준값으로 직접 사용한다.
- 입력 계약은 현재 브랜치의 임시 목업이 아니라 메인 브랜치 `README.md`와 백엔드 변경을 기준으로 다시 맞춘다.
- 현 시점 우선 후보 입력은 `market, symbol, entryAt, entryPrice, direction`이며, 결과 영역은 4축 라벨링 확장 가능성을 열어둔다.

## 폐기 대상으로 보는 현재 진행분
- 현재 [App.jsx](/home/lees/projects/tratic-frontend-work/frontend/src/App.jsx)에 섞여 있던 더미 입력/결과 연출
- 현재 [styles.css](/home/lees/projects/tratic-frontend-work/frontend/src/styles.css)의 임시 비주얼 스타일과 레이아웃 결정
- 로그인 팝오버와 결과 카드의 임시 시각 표현
- 이전 입력 계약(`stopLossPrice`, `positionPct` 기반 가정)에 묶인 프론트 문서와 화면 설명

## 재구축 전 준비 항목
1. 제품 컨셉과 화면 철학은 [FRONTEND_GUIDE.md](/home/lees/projects/tratic-frontend-work/frontend/FRONTEND_GUIDE.md)를 단일 기준으로 유지한다.
2. 메인 브랜치 `README.md`의 변경(`direction` 입력, 4축 라벨링 노출 가능성)을 현재 프론트 준비 문서에 선반영한다.
3. 새 디자인 작업에서는 입력, 결과, 로그인/저장 확장을 별도 구조로 나눠 다시 설계한다.
4. 더미 시나리오 표현을 다시 넣더라도 `scenario` 원본과 사용자 표시 문구를 분리한다.
5. 심볼 입력은 최종적으로 검색형 선택으로 확장될 수 있게 상태 모델을 잡는다.
6. 새 시각안은 정보 위계와 흐름 검증이 끝난 뒤에 올린다.
