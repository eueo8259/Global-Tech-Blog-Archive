import { buildApiUrl } from '../shared/api/client';

const installationSteps = [
  {
    number: '01',
    title: '워크스페이스에 설치',
    description: 'Add to Slack을 누르고 TechPort를 사용할 워크스페이스를 선택해 권한을 승인하세요.',
  },
  {
    number: '02',
    title: '구독할 채널에서 실행',
    description: '기술 소식을 받아볼 Slack 채널에서 /subscribe 명령어를 입력하세요.',
  },
  {
    number: '03',
    title: '관심 기업 선택',
    description: '열리는 설정 창에서 구독할 기술 기업을 고르면 매일 새 글을 모아 보내드려요.',
  },
];

function SlackMark() {
  return (
    <span className="slack-mark" aria-hidden="true">
      <span className="slack-mark-piece slack-mark-blue" />
      <span className="slack-mark-piece slack-mark-green" />
      <span className="slack-mark-piece slack-mark-yellow" />
      <span className="slack-mark-piece slack-mark-red" />
    </span>
  );
}

export function SlackInstallPage() {
  return (
    <div className="slack-page-shell">
      <header className="slack-page-header">
        <a className="site-logo slack-page-logo" href="/" aria-label="TechPort 홈">
          <img className="site-logo-icon" src="/techport-icon.png" alt="" width="40" height="40" />
          TechPort
        </a>
        <a className="slack-back-link" href="/">
          아티클 둘러보기
          <span aria-hidden="true">↗</span>
        </a>
      </header>

      <main>
        <section className="slack-hero" aria-labelledby="slack-page-title">
          <div className="slack-hero-copy">
            <p className="slack-eyebrow">
              <SlackMark />
              TECHPORT FOR SLACK
            </p>
            <h1 id="slack-page-title">
              팀이 놓치면 안 될 기술 이야기,
              <br />
              이제 Slack에서 만나보세요.
            </h1>
            <p className="slack-hero-description">
              TechPort가 세계적인 기술 기업의 새 아티클을 모아 구독한 채널로 전해드려요. 흩어진 기술
              소식을 찾는 시간은 줄이고, 팀의 대화는 더 빠르게 시작하세요.
            </p>
            <a className="add-to-slack-button" href={buildApiUrl('/slack/oauth/authorize')}>
              <SlackMark />
              Add to Slack
            </a>
            <p className="slack-install-note">
              설치 과정에서 Slack 워크스페이스 로그인과 앱 권한 승인이 필요합니다.
            </p>
          </div>

          <div className="slack-preview" aria-label="TechPort Slack 메시지 예시">
            <div className="slack-preview-toolbar">
              <span className="slack-preview-dot" />
              <span className="slack-preview-dot" />
              <span className="slack-preview-dot" />
              <strong># tech-news</strong>
            </div>
            <div className="slack-preview-message">
              <img src="/techport-icon.png" alt="" width="44" height="44" />
              <div>
                <p className="slack-preview-sender">
                  TechPort <span>앱 · 오전 9:00</span>
                </p>
                <p className="slack-preview-greeting">오늘의 기술 아티클이 도착했어요 👋</p>
                <div className="slack-preview-card">
                  <span>NETFLIX TECHBLOG</span>
                  <strong>대규모 시스템을 더 단순하게 만드는 방법</strong>
                  <p>팀과 함께 읽을 새로운 엔지니어링 이야기를 확인해 보세요.</p>
                </div>
                <div className="slack-preview-card">
                  <span>OPENAI</span>
                  <strong>프로덕션 AI 시스템을 위한 새로운 설계 원칙</strong>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="slack-benefits" aria-labelledby="benefits-title">
          <p className="slack-section-label">WHY TECHPORT</p>
          <h2 id="benefits-title">검색하지 않아도, 좋은 기술 글은 찾아옵니다.</h2>
          <div className="slack-benefit-grid">
            <article>
              <span aria-hidden="true">01</span>
              <h3>한 채널에 모아보기</h3>
              <p>여러 기업의 엔지니어링 블로그를 매번 찾아가지 않고 Slack에서 확인하세요.</p>
            </article>
            <article>
              <span aria-hidden="true">02</span>
              <h3>우리 팀 관심사만</h3>
              <p>팀이 주목하는 기업을 직접 골라 필요한 소식만 깔끔하게 구독할 수 있어요.</p>
            </article>
            <article>
              <span aria-hidden="true">03</span>
              <h3>읽고 바로 대화하기</h3>
              <p>좋은 글을 발견한 순간 같은 채널의 동료와 공유하고 이야기를 시작하세요.</p>
            </article>
          </div>
        </section>

        <section className="slack-steps" aria-labelledby="installation-title">
          <div className="slack-steps-heading">
            <p className="slack-section-label">GET STARTED</p>
            <h2 id="installation-title">설치부터 구독까지, 3단계면 충분해요.</h2>
          </div>
          <ol className="slack-step-list">
            {installationSteps.map((step) => (
              <li key={step.number}>
                <span>{step.number}</span>
                <div>
                  <h3>{step.title}</h3>
                  <p>{step.description}</p>
                </div>
              </li>
            ))}
          </ol>
          <div className="slack-command-callout">
            <div>
              <p>구독 설정 명령어</p>
              <code>/subscribe</code>
            </div>
            <p>
              앱을 설치한 뒤 원하는 채널에서 명령어를 실행하세요. 구독 기업은 같은 명령어로 언제든
              다시 변경할 수 있습니다.
            </p>
          </div>
        </section>

        <section className="slack-final-cta" aria-labelledby="slack-cta-title">
          <SlackMark />
          <h2 id="slack-cta-title">팀의 기술 레이더를 오늘부터 켜보세요.</h2>
          <p>TechPort가 새로운 엔지니어링 이야기를 매일 팀 가까이 가져다드립니다.</p>
          <a
            className="add-to-slack-button add-to-slack-button-light"
            href={buildApiUrl('/slack/oauth/authorize')}
          >
            <SlackMark />
            Add to Slack
          </a>
        </section>
      </main>

      <footer className="slack-page-footer">
        <a className="site-logo" href="/" aria-label="TechPort 홈">
          <img className="site-logo-icon" src="/techport-icon.png" alt="" width="32" height="32" />
          TechPort
        </a>
        <p>Global engineering stories, delivered closer.</p>
      </footer>
    </div>
  );
}
