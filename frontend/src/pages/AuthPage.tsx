import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Button, Field } from '../components/ui'
import type { AuthResponse, PhoneCountry } from '../types'

type Step = 'phone' | 'code'
type OtpSent = {
  sent: boolean
  expiresInSeconds: number
  retryAfterSeconds: number
  newUser: boolean
}

export function AuthPage() {
  const [step, setStep] = useState<Step>('phone')
  const [countries, setCountries] = useState<PhoneCountry[]>([])
  const [countryIso, setCountryIso] = useState('ZW')
  const [nationalNumber, setNationalNumber] = useState('')
  const [code, setCode] = useState('')
  const [fullName, setFullName] = useState('')
  const [sent, setSent] = useState<OtpSent | null>(null)
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const country = countries.find((item) => item.iso === countryIso) ?? countries[0]

  useEffect(() => {
    api<PhoneCountry[]>('/api/auth/countries')
      .then(setCountries)
      .catch(() => setCountries([{ iso: 'ZW', name: 'Zimbabwe', dial: '+263', placeholder: '71 234 5678' }]))
  }, [])

  function go(response: AuthResponse) {
    login(response)
    navigate(response.user.role === 'ADMIN' ? '/admin' : '/')
  }

  async function requestCode(event: React.FormEvent) {
    event.preventDefault()
    setLoading(true)
    try {
      const response = await api<OtpSent>('/api/auth/otp/request', {
        method: 'POST',
        body: JSON.stringify({ countryIso, nationalNumber: nationalNumber.trim() }),
      })
      setSent(response)
      setStep('code')
      toast(response.newUser ? 'Code sent. New account will be created.' : 'Code sent to your phone.')
    } catch (error) {
      toast(formatAuthError(error), 'err')
    } finally {
      setLoading(false)
    }
  }

  async function verifyCode(event: React.FormEvent) {
    event.preventDefault()
    setLoading(true)
    try {
      const response = await api<AuthResponse>('/api/auth/otp/verify', {
        method: 'POST',
        body: JSON.stringify({
          countryIso,
          nationalNumber: nationalNumber.trim(),
          code: code.trim(),
          fullName: fullName.trim() || undefined,
        }),
      })
      go(response)
    } catch (error) {
      toast(formatAuthError(error), 'err')
    } finally {
      setLoading(false)
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
          <img src="/logo.jpg" alt="Shofe" className="h-[4.5rem] w-[4.5rem] rounded-[22px] object-cover shadow-float" />
          <p className="font-display mt-5 text-5xl font-extrabold tracking-tight">Shofe</p>
          <h1 className="font-display mt-3 max-w-[14ch] text-2xl font-semibold leading-snug tracking-tight text-white/95">
            Your fare. Your driver.
          </h1>
          <p className="mt-2 max-w-[28ch] text-[15px] leading-relaxed text-white/70">
            Enter your country number. We send an SMS code. One account switches between passenger and driver.
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 28 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.12, duration: 0.5, ease: [0.22, 0.8, 0.22, 1] }}
          className="glass-strong mt-8 rounded-[28px] p-5 text-ink"
        >
          {step === 'phone' ? (
              <form onSubmit={requestCode} className="grid gap-3.5">
                <label className="block">
                  <span className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.08em] text-muted">Country</span>
                  <select
                    value={countryIso}
                    onChange={(e) => setCountryIso(e.target.value)}
                    className="w-full rounded-2xl border border-line bg-brand-soft/60 px-4 py-3.5 outline-none transition duration-200 focus:border-brand/40 focus:bg-white focus:ring-4 focus:ring-brand/15"
                  >
                    {(countries.length ? countries : [{ iso: 'ZW', name: 'Zimbabwe', dial: '+263', placeholder: '' }]).map((item) => (
                      <option key={item.iso} value={item.iso}>
                        {item.name} ({item.dial})
                      </option>
                    ))}
                  </select>
                </label>
                <Field
                  label="Mobile number"
                  value={nationalNumber}
                  onChange={(e) => setNationalNumber(e.target.value)}
                  placeholder={country?.placeholder || '71 234 5678'}
                  inputMode="tel"
                  autoComplete="tel"
                  required
                />
                <Button loading={loading} type="submit" className="mt-1 w-full">
                  Send SMS code
                </Button>
              </form>
            ) : (
              <form onSubmit={verifyCode} className="grid gap-3.5">
                <p className="text-sm leading-relaxed text-muted">
                  Enter the 6-digit code sent to {country?.dial} {nationalNumber}.
                </p>
                <Field
                  label="SMS code"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  required
                  minLength={4}
                />
                {sent?.newUser && (
                  <Field
                    label="Your name"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                    autoComplete="name"
                  />
                )}
                <Button loading={loading} type="submit" className="mt-1 w-full">
                  {sent?.newUser ? 'Create account' : 'Continue'}
                </Button>
                <button
                  type="button"
                  className="text-sm font-bold text-muted"
                  onClick={() => { setStep('phone'); setCode('') }}
                >
                  Use a different number
                </button>
              </form>
            )}

        </motion.div>
      </div>
    </div>
  )
}

function formatAuthError(error: unknown) {
  if (error instanceof TypeError) {
    return 'Cannot reach Shofe. Check your connection and try again.'
  }
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Could not sign in'
}
