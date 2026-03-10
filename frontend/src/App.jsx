const oauthProviders = [
  {
    name: "Google",
    href: "/oauth2/authorization/google"
  },
  {
    name: "Kakao",
    href: "/oauth2/authorization/kakao"
  }
];

export default function App() {
  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">Chart-first trading journal</p>
        <h1>Tratic</h1>
        <p className="lead">
          차트 수집과 거래 판단 분석을 한 곳에서 다루는 React 프론트엔드 초안입니다.
        </p>
      </section>

      <section className="panel">
        <h2>로그인</h2>
        <div className="actions">
          {oauthProviders.map((provider) => (
            <a key={provider.name} className="action" href={provider.href}>
              {provider.name}로 로그인
            </a>
          ))}
        </div>
      </section>

      <section className="panel panel-muted">
        <h2>구성</h2>
        <ul className="details">
          <li>개발 서버: `npm run dev`</li>
          <li>백엔드 API 프록시: `/api`, `/oauth2`, `/logout`</li>
          <li>배포 산출물: Spring Boot 정적 리소스로 패키징</li>
        </ul>
      </section>
    </main>
  );
}
