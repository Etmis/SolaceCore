export default function Privacy() {
  const today = new Date().toISOString().slice(0, 10)
  return (
    <div className="container" style={{ marginInline: 'auto', maxWidth: 1200 }}>
      <header className="header" style={{ marginBottom: 8 }}>
        <h1>Privacy Policy</h1>
      </header>
      <div className="muted" style={{ marginBottom: 16 }}>Last updated: {today}</div>
        <p>
          This Privacy Policy explains how the SolaceCore website and plugin collect and
          process personal data of users/players. We aim to be transparent about what data we
          collect, why we collect it, on which legal basis under the GDPR, and what rights you have.
        </p>

        <h3>Controller</h3>
        <p>
          The data controller is the operator of this project (contact: <a className="link" href="mailto:admin@example.com">admin@example.com</a>).
        </p>

        <h3>What data we process</h3>
        <ul>
          <li><b>Website/API</b>: IP addresses in server logs and technical request data (URL, timestamp, user agent).</li>
          <li><b>Plugin/Database</b>: player name, UUID, IP address, last login; moderation records (type, reason, operator, start, end/duration, status).</li>
          <li><b>External services</b>: rendering player skins/avatars (technical requests only; no marketing cookies).
          </li>
        </ul>

        <h3>Purposes and legal bases</h3>
        <ul>
          <li><b>Operating and securing</b> the website/API – legitimate interests (Article 6(1)(f) GDPR).</li>
          <li><b>Moderation</b> and keeping order on the server – legitimate interests and/or performance of a service requested by users (Article 6(1)(b) and (f) GDPR).</li>
        </ul>

        <h3>Retention</h3>
        <ul>
          <li>Website/API logs: typically 30–90 days or until a security incident is resolved.</li>
          <li>Moderation records: for the period necessary to protect the community and enforce rules, then deletion or anonymisation.</li>
        </ul>

        <h3>Recipients</h3>
        <p>
          Hosting and infrastructure providers (e.g., Docker/Nginx), database hosting, and potential
          backup/monitoring providers where applicable.
        </p>

        <h3>International transfers</h3>
        <p>
          If data is transferred outside the EU/EEA, we rely on appropriate safeguards such as
          Standard Contractual Clauses (SCC). Otherwise, data is not transferred internationally.
        </p>

        <h3>Your rights</h3>
        <p>
          Under the GDPR, you have the right to access, rectification, erasure, restriction,
          objection, and to lodge a complaint with your local supervisory authority. To exercise
          your rights, contact us at the email above.
        </p>

        <h3>Cookies and local storage</h3>
        <p>
          This website uses only strictly necessary technical storage (e.g., theme preference and
          cookie consent) and does not use analytics or marketing cookies by default. If we enable
          optional analytics in the future, you will be offered a clear choice to consent or decline.
        </p>

        <h3>Updates to this policy</h3>
        <p>
          We may update this policy from time to time. Material changes will be reflected by the
          “Last updated” date above.
        </p>

      <footer className="footer">
        <div className="footer-brand">SolaceCore</div>
        <div className="footer-links">
          <a className="link" href="/">← Back to Home</a>
          <span>•</span>
          <a className="link" href="/terms">Terms of Use</a>
        </div>
        <div className="footer-meta">
          <span>© 2025</span>
          <span>•</span>
          <span>Made with <span className="heart">♥</span> by</span>
          <a className="link" href="https://github.com/Etmis" target="_blank" rel="noreferrer">Etmis</a>
        </div>
      </footer>
    </div>
  )
}
