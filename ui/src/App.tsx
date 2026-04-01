import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { OpsPage } from './pages/OpsPage.tsx'
import { ValidatePage } from './pages/ValidatePage.tsx'

export default function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>FHIR Validator UI</h1>
        <nav>
          <Link to="/validate">Validate</Link>
          <Link to="/ops">Operations</Link>
        </nav>
      </header>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/validate" replace />} />
          <Route path="/validate" element={<ValidatePage />} />
          <Route path="/ops" element={<OpsPage />} />
        </Routes>
      </main>
    </div>
  )
}
