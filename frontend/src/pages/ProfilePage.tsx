import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Moon } from 'lucide-react'
import { api } from '../lib/api'
import { money } from '../lib/format'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Badge, Button, Field } from '../components/ui'
import { ModeSwitch } from '../components/ModeSwitch'
import { isDriving, type Contact } from '../types'

type WalletView = {
  balance: number
  currency: string
  minBalance: number
  commissionPercent: number
  canReceiveRequests: boolean
  recent: { id: string; type: string; amount: number; currency: string; note?: string; createdAt: string }[]
}

export function ProfilePage() {
  const { user, logout, refresh } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [topUp, setTopUp] = useState('20')
  const [busy, setBusy] = useState(false)
  const contacts = useQuery({ queryKey: ['contacts'], queryFn: () => api<Contact[]>('/api/account/contacts') })
  const wallet = useQuery({
    queryKey: ['wallet'],
    queryFn: () => api<WalletView>('/api/payments/wallet'),
    enabled: Boolean(user?.driver) || isDriving(user),
  })

  async function addContact(event: React.FormEvent) {
    event.preventDefault()
    try {
      await api('/api/account/contacts', { method: 'POST', body: JSON.stringify({ name, phone }) })
      setName(''); setPhone('')
      await queryClient.invalidateQueries({ queryKey: ['contacts'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Could not save contact', 'err')
    }
  }

  async function topUpWallet() {
    setBusy(true)
    try {
      const started = await api<{ redirectUrl: string }>(`/api/payments/wallet/top-up`, {
        method: 'POST',
        body: JSON.stringify({ amount: Number(topUp) }),
      })
      if (started.redirectUrl) {
        window.location.href = started.redirectUrl
        return
      }
      toast('Top-up started')
      await queryClient.invalidateQueries({ queryKey: ['wallet'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Top-up failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  if (!user) return null
  return (
    <div className="grid gap-4">
      <div className="auth-stage relative overflow-hidden rounded-sheet p-6 text-white shadow-float">
        <div className="breathe pointer-events-none absolute -right-10 -top-8 h-36 w-36 rounded-full bg-white/10 blur-2xl" />
        <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-white/70">
          {user.activeMode === 'DRIVER' ? 'Driver mode' : 'Passenger mode'}
        </p>
        <h1 className="font-display mt-1 text-3xl font-extrabold tracking-tight">{user.fullName}</h1>
        <p className="mt-1 text-sm text-white/80">{user.phone}{user.email ? ` · ${user.email}` : ''}</p>
        <p className="mt-4 text-sm font-semibold text-white/90">
          {user.ratingCount ? `${user.ratingAvg.toFixed(1)} from ${user.ratingCount} ratings` : 'No ratings yet'}
        </p>
        <div className="mt-4 rounded-2xl bg-white/15 p-1">
          <ModeSwitch compact />
        </div>
      </div>

      {user.driver && (
        <div className="panel p-4">
          <div className="flex items-center justify-between gap-2">
            <p className="font-display font-extrabold">
              {user.driver.vehicleColor} {user.driver.vehicleMake} {user.driver.vehicleModel}
            </p>
            <Badge tone={user.driver.verificationStatus === 'APPROVED' ? 'green' : 'amber'}>
              {user.driver.verificationStatus}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-muted">{user.driver.plateNumber} · {user.driver.tripCount} trips</p>
          <Button variant="ghost" className="mt-3" onClick={() => navigate('/drive/setup')}>Update documents</Button>
        </div>
      )}

      {user.driver && wallet.data && (
        <div className="panel p-4">
          <p className="font-display text-lg font-extrabold">Commission wallet</p>
          <p className="mt-1 text-sm text-muted">
            {wallet.data.commissionPercent}% per completed trip · min {money(wallet.data.minBalance, wallet.data.currency)} to go online
          </p>
          <p className="font-display mt-3 text-3xl font-extrabold tracking-tight">
            {money(wallet.data.balance, wallet.data.currency)}
          </p>
          {!wallet.data.canReceiveRequests && (
            <p className="mt-2 text-sm font-semibold text-rose-600">Top up before going online.</p>
          )}
          <div className="mt-4 grid grid-cols-[1fr_auto] items-end gap-2">
            <Field label="Top-up amount" value={topUp} onChange={(e) => setTopUp(e.target.value)} type="number" min="1" step="1" />
            <Button loading={busy} onClick={() => void topUpWallet()}>Pesepay</Button>
          </div>
          <div className="mt-3 space-y-2">
            {wallet.data.recent.slice(0, 5).map((line) => (
              <div key={line.id} className="flex justify-between rounded-2xl bg-brand-soft px-3.5 py-2.5 text-sm">
                <span className="font-semibold">{line.type.replaceAll('_', ' ')}</span>
                <span>{money(line.amount, line.currency)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <Button variant="ghost" onClick={() => navigate('/night-owl')}>
        <Moon className="h-4 w-4" /> Night Owl safety
      </Button>

      {user.role === 'ADMIN' && <Button onClick={() => navigate('/admin')}>Open admin desk</Button>}

      <form onSubmit={addContact} className="panel p-4">
        <p className="font-display text-lg font-extrabold">Trusted contacts</p>
        <p className="mt-1 text-sm text-muted">Night Owl rides and SOS alerts are shared with this list.</p>
        <div className="mt-4 grid gap-2.5">
          {(contacts.data ?? []).map((contact) => (
            <div key={contact.id} className="flex items-center justify-between rounded-2xl bg-brand-soft px-3.5 py-3 text-sm">
              <span className="font-semibold">{contact.name} · {contact.phone}</span>
              <button
                type="button"
                className="font-bold text-rose-600"
                onClick={() => void api(`/api/account/contacts/${contact.id}`, { method: 'DELETE' }).then(() => queryClient.invalidateQueries({ queryKey: ['contacts'] }))}
              >
                Remove
              </button>
            </div>
          ))}
          <Field label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
          <Field label="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} required />
          <Button type="submit" variant="dark">Add contact</Button>
        </div>
      </form>

      <Button variant="ghost" onClick={() => { logout(); void refresh(); navigate('/login') }}>Log out</Button>
    </div>
  )
}
