const sampleCompanies = ['Netflix', 'Google', 'Meta', 'Uber', 'Airbnb'];

function App() {
  return (
    <main className="app-shell">
      <section className="hero">
        <p className="eyebrow">Global Tech Blog Archive</p>
        <h1>해외 엔지니어링 블로그를 한곳에서 모아봅니다.</h1>
        <p className="description">
          MVP에서는 글로벌 기술 기업의 개발 블로그 글을 수집하고 탐색하는
          기본 경험에 집중합니다.
        </p>
      </section>

      <section className="company-section" aria-labelledby="company-heading">
        <h2 id="company-heading">Initial Sources</h2>
        <ul className="company-list">
          {sampleCompanies.map((company) => (
            <li key={company}>{company}</li>
          ))}
        </ul>
      </section>
    </main>
  );
}

export default App;
