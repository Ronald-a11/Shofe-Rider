import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import { Shell } from './components/Shell'
import { Splash } from './components/ui'
import { AuthPage } from './pages/AuthPage'
import { isDriving } from './types'
import { RiderHome } from './pages/RiderHome'
import { DriverHome } from './pages/DriverHome'
import { OffersPage } from './pages/OffersPage'
import { ActiveRidePage } from './pages/ActiveRidePage'
import { ActivityPage } from './pages/ActivityPage'
import { AssistPage } from './pages/AssistPage'
import { ProfilePage } from './pages/ProfilePage'
import { AdminPage } from './pages/AdminPage'
import { TrackPage } from './pages/TrackPage'
import { OnboardingPage } from './pages/OnboardingPage'
import { NightOwlPage } from './pages/NightOwlPage'
import { StaffPage } from './pages/StaffPage'

function Home() {
  const { user } = useAuth()
  if (user?.role === 'ADMIN') return <Navigate to="/admin" replace />
  if (isDriving(user)) return <DriverHome />
  return <RiderHome />
}

function Guard({ children }: { children: React.ReactNode }) {
  const { user, ready } = useAuth()
  if (!ready) return <Splash />
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  const { user, ready } = useAuth()
  return (
    <Routes>
      <Route path="/track/:token" element={<TrackPage />} />
      <Route path="/login" element={ready && user ? <Navigate to="/" replace /> : <AuthPage />} />
      <Route path="/staff" element={ready && user ? <Navigate to="/" replace /> : <StaffPage />} />
      <Route element={<Guard><Shell /></Guard>}>
        <Route path="/" element={<Home />} />
        <Route path="/activity" element={<ActivityPage />} />
        <Route path="/assist" element={<AssistPage />} />
        <Route path="/night-owl" element={<NightOwlPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/requests/:id" element={<OffersPage />} />
        <Route path="/rides/:id" element={<ActiveRidePage />} />
        <Route path="/drive/setup" element={<OnboardingPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
