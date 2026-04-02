const productPrinciples = [
  "Tratic은 실시간 실행 도구가 아니라 거래 기록을 해석하는 분석 저널이어야 한다.",
  "첫 화면의 핵심 가치는 즉각적 해석이며, 로그인 없이도 기본 흐름을 끝까지 사용할 수 있어야 한다.",
  "입력과 결과는 같은 흐름 안에서 이어져야 하고, scenario와 summary가 결과의 중심을 유지해야 한다."
];

const lockedInputs = [
  "market",
  "symbol",
  "entryAt",
  "entryPrice",
  "direction"
];

const resetTargets = [
  "현재 단일 파일에 섞여 있는 더미 폼, 결과 카드, 로그인 팝오버 연출을 새 디자인 전에 폐기 대상으로 본다.",
  "시각 스타일보다 제품 흐름과 입력 계약을 먼저 고정하고, 이후 화면 구조를 다시 설계한다.",
  "scenario는 안정 식별자로 유지하고, summary는 교체 가능한 표현 레이어로만 다룬다."
];

const rebuildTracks = [
  {
    title: "Input Flow",
    description: "거래 기록 입력을 즉시 시작하게 만드는 1차 진입 화면",
    items: [
      "검색형 심볼 선택을 전제로 상태 모델 분리",
      "표 중심 흐름에서 direction 입력을 최소 밀도로 배치",
      "과도한 보조 옵션 없이 기본 흐름 우선"
    ]
  },
  {
    title: "Result Flow",
    description: "scenario와 summary가 먼저 읽히는 결과 경험",
    items: [
      "raw 지표보다 해석 결과 위계 우선",
      "scenario는 저장과 분기 기준값으로 직접 사용",
      "summary는 렌더링 레이어에서만 교체 가능",
      "4축 라벨링 확장 영역을 분리 가능하게 유지"
    ]
  },
  {
    title: "Auth Layer",
    description: "기본 사용 흐름을 막지 않는 저장 확장 레이어",
    items: [
      "비로그인도 분석 가능",
      "로그인은 저장과 기록 누적 가치 추가",
      "저장 유도는 압박형이 아닌 복기 가치 중심"
    ]
  }
];

const deliverables = [
  "새 비주얼 방향 정의",
  "정보 위계가 반영된 레이아웃 재설계",
  "API 연결 전 임시 상태 모델 분리",
  "로그인/저장 흐름 재배치"
];

export default function App() {
  return (
    <main className="app-shell">
      <section className="hero-panel">
        <p className="eyebrow">Frontend reset</p>
        <h1>Tratic frontend is ready for a full design rebuild.</h1>
        <p className="hero-copy">
          현재 구현 진행 상태는 기준 화면으로 유지하지 않습니다. 제품 컨셉과 계약만 고정한 뒤,
          입력과 결과가 한 흐름으로 읽히는 새 디자인을 다시 구축하는 단계로 전환합니다.
        </p>
      </section>

      <section className="grid-layout">
        <article className="panel">
          <div className="panel-heading">
            <p className="section-label">Locked concept</p>
            <h2>디자인이 바뀌어도 유지할 기준</h2>
          </div>
          <ul className="stack-list">
            {productPrinciples.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </article>

        <article className="panel accent-panel">
          <div className="panel-heading">
            <p className="section-label">Reset scope</p>
            <h2>이번 리셋에서 우선 정리할 것</h2>
          </div>
          <ul className="stack-list">
            {resetTargets.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </article>
      </section>

      <section className="contract-panel">
        <div className="panel-heading">
          <p className="section-label">Input contract</p>
          <h2>유지할 기본 입력 계약</h2>
        </div>
        <div className="token-row" aria-label="locked input fields">
          {lockedInputs.map((field) => (
            <span key={field} className="token-chip">
              {field}
            </span>
          ))}
        </div>
      </section>

      <section className="tracks">
        {rebuildTracks.map((track) => (
          <article key={track.title} className="track-card">
            <div className="panel-heading">
              <p className="section-label">{track.title}</p>
              <h2>{track.description}</h2>
            </div>
            <ul className="stack-list compact-list">
              {track.items.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        ))}
      </section>

      <section className="deliverable-panel">
        <div className="panel-heading">
          <p className="section-label">Rebuild queue</p>
          <h2>새 디자인 착수 전 준비 완료 상태</h2>
        </div>

        <div className="deliverable-grid">
          {deliverables.map((item, index) => (
            <article key={item} className="deliverable-card">
              <span className="deliverable-index">0{index + 1}</span>
              <p>{item}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
