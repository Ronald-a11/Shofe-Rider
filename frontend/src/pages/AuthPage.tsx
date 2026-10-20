import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Button, Field } from '../components/ui'
import { Tabs } from '../components/Tabs'
import type { AuthResponse } from '../types'

const demos = [
  { label: 'Rider', email: 'rider@shofe.app', password: 'Rider123!' },
  { label: 'Driver', email: 'driver@shofe.app', password: 'Driver123!' },
  { label: 'Admin', email: 'admin@shofe.app', password: 'Admin123!' },
]

export function AuthPage() {
  const [tab, setTab] = useState<'login' | 'register'>('login')
  const [loading, setLoading] = useState(false)
  const [demoLoading, setDemoLoading] = useState<string | null>(null)
  const [role, setRole] = useState<'RIDER' | 'DRIVER'>('RIDER')
  const [form, setForm] = useState({ fullName: '', email: 'rider@shofe.app', phone: '', password: 'Rider123!' })
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  function set<K extends keyof typeof form>(key: K, value: string) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function signIn(email: string, password: string, extras?: { fullName?: string; phone?: string; role?: 'RIDER' | 'DRIVER' }) {
    if (extras) {
      return api<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          fullName: extras.fullName,
          email,
          phone: extras.phone,
          password,
          role: extras.role,
        }),
      })
    }
    return api<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
  }

  function go(response: AuthResponse) {
    login(response)
    navigate(response.user.role === 'ADMIN' ? '/admin' : '/')
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setLoading(true)
    try {
      const response = tab === 'login'
        ? await signIn(form.email.trim(), form.password)
        : await signIn(form.email.trim(), form.password, {
            fullName: form.fullName.trim(),
            phone: form.phone.trim(),
            role,
          })
      go(response)
    } catch (error) {
      toast(formatAuthError(error), 'err')
    } finally {
      setLoading(false)
    }
  }

  async function demoLogin(demo: (typeof demos)[number]) {
    setTab('login')
    setForm((current) => ({ ...current, email: demo.email, password: demo.password }))
    setDemoLoading(demo.email)
    try {
      go(await signIn(demo.email, demo.password))
    } catch (error) {
      toast(formatAuthError(error), 'err')
    } finally {
      setDemoLoading(null)
    }
  }

  return (
    <div className="auth-stage relative min-h-dvh overflow-hidden text-white">
      <div className="breathe pointer-events-none absolute -left-24 top-10 h-72 w-72 rounded-[42%] bg-white/10 blur-3xl" />
      <div className="pointer-events-none absolute right-[-4rem] top-40 h-80 w-80 rounded-full border border-white/10" />
      <div className="pointer-events-none absolute bottom-32 left-8 h-40 w-40 rounded-full border border-white/10" />

      <div className="relative mx-auto flex min-h-dvh max-w-md flex-col justify-end px-4 pb-7 pt-12">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.55, ease: [0.22, 0.8, 0.22, 1] }}
        >
          <img src="/logo.jpg" alt="Shofe" className="h-[4.5rem] w-[4.5rem] rounded-[22px] shadow-float" />
          <p className="font-display mt-5 text-5xl font-extrabold tracking-tight">Shofe</p>
          <h1 className="font-display mt-3 max-w-[14ch] text-2xl font-semibold leading-snug tracking-tight text-white/95">
            Your fare. Your driver.
          </h1>
          <p className="mt-2 max-w-[28ch] text-[15px] leading-relaxed text-white/70">
            Name a price. Nearby drivers accept or counter. You choose who shows up.
          </p>
        </motion.div>

        <motion.form
          onSubmit={submit}
          initial={{ opacity: 0, y: 28 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.12, duration: 0.5, ease: [0.22, 0.8, 0.22, 1] }}
          className="glass-strong mt-8 rounded-[28px] p-5 text-ink"
        >
          <Tabs
            value={tab}
            onChange={setTab}
            items={[
              { id: 'login', label: 'Log in' },
              { id: 'register', label: 'Register' },
            ]}
          />

          <AnimatePresence mode="wait">
            <motion.div
              key={tab}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.22 }}
              className="mt-5 grid gap-3.5"
            >
              {tab === 'register' && (
                <>
                  <Field label="Full name" value={form.fullName} onChange={(e) => set('fullName', e.target.value)} required />
                  <Field label="Phone" value={form.phone} onChange={(e) => set('phone', e.target.value)} required />
                  <Tabs
                    value={role}
                    onChange={setRole}
                    items={[
                      { id: 'RIDER', label: 'I need a ride' },
                      { id: 'DRIVER', label: 'I drive' },
                    ]}
                  />
                </>
              )}
              <Field label="Email" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} required autoComplete="username" />
              <Field label="Password" type="password" value={form.password} onChange={(e) => set('password', e.target.value)} required minLength={8} autoComplete={tab === 'login' ? 'current-password' : 'new-password'} />
              <Button loading={loading} type="submit" className="mt-1 w-full">
                {tab === 'login' ? 'Continue' : 'Create account'}
              </Button>
            </motion.div>
          </AnimatePresence>

          <div className="mt-5 border-t border-line pt-4">
            <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-muted">One-tap demo</p>
            <div className="mt-2 grid grid-cols-3 gap-2">
              {demos.map((demo) => (
                <Button
                  key={demo.email}
                  type="button"
                  variant="ghost"
                  loading={demoLoading === demo.email}
                  disabled={loading || Boolean(demoLoading)}
                  onClick={() => void demoLogin(demo)}
                  className="px-2 py-2.5 text-xs"
                >
                  {demo.label}
                </Button>
              ))}
            </div>
            <p className="mt-2 text-[11px] leading-relaxed text-muted">
              Rider · rider@shofe.app / Rider123!
            </p>
          </div>
        </motion.form>
      </div>
    </div>
  )
}

function formatAuthError(error: unknown) {
  if (error instanceof TypeError) {
    return 'Cannot reach the Shofe API. Start the backend with: mvn spring-boot:run -Dspring-boot.run.profiles=local'
  }
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Could not sign in'
}
