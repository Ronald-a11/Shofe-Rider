import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Button, Field } from '../components/ui'
import type { AuthResponse } from '../types'

export function StaffPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setLoading(true)
    try {
      const response = await api<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: email.trim(), password }),
      })
      login(response)
      navigate(response.user.role === 'ADMIN' ? '/admin' : '/')
    } catch (error) {
      toast(error instanceof ApiError ? error.message : 'Could not sign in', 'err')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-stage relative min-h-dvh overflow-hidden text-white">
      <div className="relative mx-auto flex min-h-dvh max-w-md flex-col justify-center px-4 pb-7 pt-12">
        <p className="font-display text-4xl font-extrabold tracking-tight">Shofe staff</p>
        <p className="mt-2 text-sm text-white/70">Operations login for approved admin accounts.</p>
        <form onSubmit={submit} className="glass-strong mt-8 grid gap-3.5 rounded-[28px] p-5 text-ink">
          <Field label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Field label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <Button loading={loading} type="submit" className="mt-1 w-full">Continue</Button>
        </form>
      </div>
    </div>
  )
}
