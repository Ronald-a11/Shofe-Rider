import { useState } from 'react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import type { AuthResponse } from '../types'

export function ModeSwitch({ compact = false }: { compact?: boolean }) {
  const { user, login } = useAuth()
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  if (!user || user.role === 'ADMIN') return null

  const mode = user.activeMode === 'DRIVER' ? 'DRIVER' : 'PASSENGER'

  async function setMode(next: 'PASSENGER' | 'DRIVER') {
    if (next === mode || busy) return
    setBusy(true)
    try {
      const response = await api<AuthResponse>('/api/account/mode', {
        method: 'POST',
        body: JSON.stringify({ mode: next }),
      })
      login(response)
      toast(next === 'DRIVER' ? 'Driver mode on' : 'Passenger mode on')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Could not switch mode', 'err')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className={`grid grid-cols-2 rounded-2xl bg-black/5 p-1 ${compact ? '' : 'mt-4'}`}>
      <button
        type="button"
        disabled={busy}
        onClick={() => void setMode('PASSENGER')}
        className={`rounded-xl px-3 py-2 text-xs font-bold transition ${
          mode === 'PASSENGER' ? 'bg-white text-ink shadow-soft' : 'text-muted'
        }`}
      >
        Passenger
      </button>
      <button
        type="button"
        disabled={busy}
        onClick={() => void setMode('DRIVER')}
        className={`rounded-xl px-3 py-2 text-xs font-bold transition ${
          mode === 'DRIVER' ? 'bg-white text-ink shadow-soft' : 'text-muted'
        }`}
      >
        Driver
      </button>
    </div>
  )
}
