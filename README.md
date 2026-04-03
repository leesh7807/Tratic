Tratic
=====================================

차트 기반 매매 성격 기록 앱

### 배경

매매 일지를 작성하는게 좋다는 것은 누구나 아는데 왜 작성하지 않을까?  
매매 일지를 작성하는건 귀찮고, 매매 일지 작성의 효과가 눈에 드러나지 않기 때문에  

### 앱의 목표

기술 지표 분석을 통해 한 매매의 성격을 빠르게 드러내는 것

### 흐름

1. 유저 로그인
2. 매매기록 입력
3. 차트 호출
4. 지표 생성
5. 매매 성격 확인

### 구현

차트를 일일이 저장하지 않고, 요청이 들어오면 해당 시점 기준으로 과거 캔들을 분석에 필요한 만큼 조회한다.
현재 입력 모델은 특정 진입이 어떤 자리였는지 판단하는 데 필요한 최소 항목으로 구성한다.

#### 사용자에게 받는 입력
- 마켓
- 심볼
- 사용한 시간봉
- 진입 시각
- 진입가
- 방향(Long/Short)

심볼은 거래소에서 바로 호출 가능한 포맷(예: `KRW-BTC`, `BTCUSDT`)을 사용한다.

#### 사용자가 받는 출력
한 매매의 성격을 드러내는 최소 결과

현재 결과는 3축으로 구성한다.
- 추세(trend)
- 위치(location)
- 수급 압력(pressure)

변동성(volatility)은 내부 환경값으로 사용한다.

#### 보장하는 해상도
일봉까지. 단, 일봉 아래 존재하는 해상도는 사용자가 직접 확인해야함

#### 레이트리밋
마켓 API 레이트 리밋 또는 연속된 내부 서버 에러 발생 시 요청을 일정시간 잠굴 것

### 디렉토리 구조

- `frontend/`: React + Vite 프론트엔드
- `backend/`: Spring Boot 백엔드
- `backend/build/generated/frontend-resources/main/static`: 프론트 빌드 산출물이 합쳐지는 생성 디렉토리

### 로컬 실행

1. `fnm install --lts`
2. `cd frontend`
3. `fnm exec --using=.node-version npm ci`
4. 프론트 개발 서버: `fnm exec --using=.node-version npm run dev`
5. 백엔드: `cd ../backend && ./gradlew bootRun`

### 패키징

- 전체 자동 빌드: `cd backend && ./gradlew build`
- 프론트만 갱신: `cd backend && ./gradlew syncFrontendAssets`
- React 산출물은 빌드 시 Spring Boot 정적 리소스로 포함된다.
