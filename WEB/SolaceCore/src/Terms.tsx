export default function Terms() {
	const today = new Date().toISOString().slice(0, 10)
	return (
		<div className="container" style={{ marginInline: 'auto', maxWidth: 1200 }}>
			<header className="header" style={{ marginBottom: 8 }}>
				<h1>Terms of Service</h1>
			</header>
			<div className="muted" style={{ marginBottom: 16 }}>Last updated: {today}</div>

				<p>
					These Terms of Service govern your access to and use of the SolaceCore website and plugin.
					By accessing or using the Services, you agree to be bound by these Terms.
				</p>

				<h3>1. Services</h3>
				<p>
					SolaceCore provides a community server and related web interface to view player
					statistics and moderation records. Some features may require a compatible game client.
				</p>

				<h3>2. Eligibility</h3>
				<p>
					You may use the Services only if you have the legal capacity to form a binding contract
					in your country of residence. If you are under the age of digital consent in your
					country, you must have permission from a parent or legal guardian.
				</p>

				<h3>3. Rules of Conduct</h3>
				<ul>
					<li>No harassment, hate speech, or unlawful content.</li>
					<li>No cheating, exploiting, or attempting to disrupt the service.</li>
					<li>Follow all moderator instructions and community guidelines.</li>
				</ul>

				<h3>4. Moderation</h3>
				<p>
					Moderators may take actions including warnings, mutes, kicks, or bans to protect the
					community. Moderation decisions are discretionary. You may contact us to appeal a
					decision; appeals do not guarantee reversal.
				</p>

				<h3>5. Content and Licensing</h3>
				<p>
					You retain rights to content you submit but grant us a non-exclusive, worldwide,
					royalty-free license to host and display it as necessary to operate the Services. Do not
					submit content you do not have the right to share.
				</p>

				<h3>6. Privacy</h3>
				<p>
					Our processing of personal data is described in the <a className="link" href="/privacy">Privacy Policy</a>,
					which forms part of these Terms. We operate in compliance with applicable EU law, including the GDPR.
				</p>

				<h3>7. Disclaimers</h3>
				<p>
					The Services are provided “as is” and “as available.” To the maximum extent permitted by
					applicable law, we disclaim all warranties, express or implied, including merchantability,
					fitness for a particular purpose, and non-infringement. We do not guarantee uninterrupted
					or error-free operation.
				</p>

				<h3>8. Liability</h3>
				<p>
					To the extent permitted by applicable law, we shall not be liable for any indirect,
					incidental, special, consequential, or punitive damages, or any loss of data, profits, or
					revenue, arising from or related to your use of the Services.
				</p>

				<h3>9. Suspension and Termination</h3>
				<p>
					We may suspend or terminate access to the Services at any time if we reasonably believe
					you have violated these Terms or pose a risk to the community or the operation of the
					Services.
				</p>

				<h3>10. Changes</h3>
				<p>
					We may modify these Terms. Material changes will be indicated by the “Last updated” date
					above. Continued use after changes indicates acceptance of the updated Terms.
				</p>

				<h3>11. Governing Law and Disputes</h3>
				<p>
					If you are an EU resident, mandatory consumer protection rules of your country may
					apply. Otherwise, these Terms are governed by the laws of the country where the operator
					is established, without regard to conflict of laws principles. Competent courts in that
					country shall have jurisdiction, subject to any mandatory consumer forum rights.
				</p>

				<h3>12. Contact</h3>
				<p>
					Questions about these Terms can be sent to <a className="link" href="mailto:admin@example.com">admin@example.com</a>.
				</p>

			<footer className="footer">
				<div className="footer-brand">SolaceCore</div>
				<div className="footer-links">
					<a className="link" href="/">← Back to Home</a>
					<span>•</span>
					<a className="link" href="/privacy">Privacy Policy</a>
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
