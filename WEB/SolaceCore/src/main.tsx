import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles.css'
import App from './App.tsx'
import Privacy from './Privacy.tsx'
import Terms from './Terms.tsx'
import PlayerDetailsPage from './PlayerDetails.tsx'

function Root() {
  const path = typeof window !== 'undefined' ? window.location.pathname : '/'
  if (path === '/privacy' || path === '/privacy.html') {
    return <Privacy />
  }
  if (path === '/terms' || path === '/terms.html') {
    return <Terms />
  }
  const detailsMatch = path.match(/^\/players\/([^/]+)$/)
  if (detailsMatch) {
    return <PlayerDetailsPage playerName={decodeURIComponent(detailsMatch[1])} />
  }
  return <App />
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
)
