export function SlackInstallSuccessPage() {
  return (
    <div className="slack-success-page">
      <main className="slack-success-card">
        <a className="site-logo slack-success-logo" href="/" aria-label="TechPort 홈">
          <img src="/techport-icon.png" alt="" width="40" height="40" />
          TechPort
        </a>

        <div className="slack-success-check" aria-hidden="true">
          ✓
        </div>
        <p className="slack-section-label">SLACK CONNECTED</p>
        <h1>TechPort 설치가 완료되었습니다.</h1>
        <p className="slack-success-description">
          이제 원하는 Slack 채널에서 구독할 기업을 선택하고 기술 아티클을 받아보세요.
        </p>

        <div className="slack-success-command">
          <span>Slack 채널에서 다음 명령어를 입력하세요.</span>
          <code>/subscribe</code>
        </div>

        <div className="slack-success-actions">
          <a className="slack-success-primary" href="/">
            TechPort 홈으로
          </a>
          <a className="slack-success-secondary" href="/slack">
            설치 안내 다시 보기
          </a>
        </div>
      </main>
    </div>
  );
}
