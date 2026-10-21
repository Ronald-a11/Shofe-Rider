import { CarFront, LifeBuoy, Moon, ShieldAlert, UserRound, WalletCards } from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { useState } from 'react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Button } from './ui'
import { ModeSwitch } from './ModeSwitch'
import { DEFAULT_CITY } from '../types'

const links = [
  { to: '/', label: 'Home', icon: CarFront },
  { to: '/activity', label: 'Trips', icon: WalletCards },
  { to: '/assist', label: 'Assist', icon: LifeBuoy },
  { to: '/profile', label: 'Account', icon: UserRound },
]

const desktopExtra = [
  { to: '/night-owl', label: 'Night Owl', icon: Moon },
]

export function Shell() {
  const { pathname } = useLocation()
  const bare = pathname === '/'
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [sos, setSos] = useState(false)
  const [sending, setSending] = useState(false)

  async function sendSos() {
    setSending(true)
    try {
      const point = await locate()
      const result = await api<{ message: string }>('/api/safety/sos', {
        method: 'POST',
        body: JSON.stringify({ lat: point.lat, lng: point.lng, note: 'SOS from Shofe app' }),
      })
      toast(result.message)
      setSos(false)
    } catch (error) {
      toast(error instanceof Error ? error.message : 'SOS failed', 'err')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="min-h-dvh md:grid md:grid-cols-[272px_1fr]">
      <aside className="hidden border-r border-line bg-white/70 backdrop-blur-xl md:flex md:flex-col md:px-5 md:py-7">
        <button onClick={() => navigate('/')} className="flex items-center gap-3 px-1 text-left">
          <img src="/logo.jpg" alt="Shofe" className="h-12 w-12 rounded-2xl object-cover shadow-soft" />
          <div>
            <p className="font-display text-xl font-extrabold tracking-tight">Shofe</p>
            <p className="text-xs text-muted">{user?.fullName}</p>
          </div>
        </button>
        <ModeSwitch />
        <nav className="mt-8 grid gap-1.5">
          {[...links, ...desktopExtra].map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-2xl px-3.5 py-3 text-sm font-bold transition ${
                  isActive ? 'bg-brand text-white shadow-soft' : 'text-ink hover:bg-brand-mist'
                }`
              }
            >
              <link.icon className="h-5 w-5" /> {link.label}
            </NavLink>
          ))}
          {user?.role === 'ADMIN' && (
            <NavLink
              to="/admin"
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-2xl px-3.5 py-3 text-sm font-bold transition ${
                  isActive ? 'bg-brand text-white shadow-soft' : 'text-ink hover:bg-brand-mist'
                }`
              }
            >
              Admin desk
            </NavLink>
          )}
        </nav>
        <div className="mt-auto rounded-[1.75rem] bg-brand-soft p-4">
          <p className="font-display text-sm font-bold">Propose. Choose. Ride.</p>
          <p className="mt-1 text-xs leading-relaxed text-muted">Built for Zimbabwe — no surge, just a fair offer with a nearby driver.</p>
        </div>
      </aside>

      <div className="relative min-h-dvh">
        <main className={bare ? '' : 'mx-auto w-full max-w-3xl px-4 pb-28 pt-5 md:pb-10'}>
          <Outlet />
        </main>

        <motion.button
          whileTap={{ scale: 0.94 }}
          onClick={() => setSos(true)}
          className="fixed bottom-24 right-4 z-40 grid h-14 w-14 place-items-center rounded-2xl bg-rose-600 text-white shadow-float md:bottom-6"
          aria-label="Emergency SOS"
        >
          <ShieldAlert className="h-5 w-5" />
        </motion.button>

        <nav className="fixed inset-x-3 bottom-3 z-40 md:hidden" style={{ paddingBottom: 'max(env(safe-area-inset-bottom), 0px)' }}>
          <div className="glass-strong grid grid-cols-4 rounded-[22px] px-1.5 py-1.5 shadow-dock">
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.to === '/'}
                className={({ isActive }) =>
                  `relative flex flex-col items-center gap-0.5 rounded-2xl py-2.5 text-[10px] font-bold tracking-wide transition ${
                    isActive ? 'text-brand' : 'text-muted'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    {isActive && (
                      <motion.span
                        layoutId="nav-pill"
                        className="absolute inset-1 -z-10 rounded-xl bg-brand-mist"
                        transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                      />
                    )}
                    <link.icon className="h-5 w-5" strokeWidth={isActive ? 2.4 : 2} />
                    {link.label}
                  </>
                )}
              </NavLink>
            ))}
          </div>
        </nav>
      </div>

      <AnimatePresence>
        {sos && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 grid place-items-end bg-ink/45 p-4 backdrop-blur-sm md:place-items-center"
          >
            <motion.div
              initial={{ y: 40, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 24, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 320, damping: 28 }}
              className="w-full max-w-md rounded-[1.75rem] bg-white p-6 shadow-float"
            >
              <p className="font-display text-xl font-extrabold">Send an emergency alert?</p>
              <p className="mt-2 text-sm leading-relaxed text-muted">
                Shofe stores your location and notifies trusted contacts. This does not replace calling local emergency services.
              </p>
              <div className="mt-6 grid grid-cols-2 gap-2.5">
                <Button variant="ghost" onClick={() => setSos(false)}>Not now</Button>
                <Button variant="danger" loading={sending} onClick={() => void sendSos()}>Send SOS</Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

function locate() {
  return new Promise<{ lat: number; lng: number }>((resolve) => {
    if (!navigator.geolocation) {
      resolve(DEFAULT_CITY)
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => resolve(DEFAULT_CITY),
      { enableHighAccuracy: true, timeout: 4000 },
    )
  })
}
