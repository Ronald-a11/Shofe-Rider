import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { MapCanvas } from '../components/MapCanvas'
import { Badge, Button } from '../components/ui'
import { api } from '../lib/api'
import { money } from '../lib/format'
import { useAuth } from '../context/AuthContext'
import { useSocket } from '../context/SocketContext'
import { useToast } from '../context/ToastContext'
import { DEMO, type Ride, type RideRequest } from '../types'

export function DriverHome() {
  const { user, refresh } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [busy, setBusy] = useState(false)
  const [counterFor, setCounterFor] = useState<RideRequest | null>(null)
  const [amount, setAmount] = useState(0)
  const profile = user?.driver
  const online = Boolean(profile?.online)

  const requests = useQuery({
    queryKey: ['open-requests'],
    queryFn: () => api<RideRequest[]>('/api/rides/requests/open'),
    enabled: online,
    refetchInterval: 5000,
  })
  const active = useQuery({ queryKey: ['active-ride'], queryFn: () => api<Ride | null>('/api/rides/active') })

  useSocket((event) => {
    if (event.destination.endsWith('/requests')) void queryClient.invalidateQueries({ queryKey: ['open-requests'] })
  })

  async function setOnline(next: boolean, atDemo = false) {
    setBusy(true)
    try {
      const point = atDemo ? DEMO : await locate()
      if (next) {
        await api('/api/drivers/online', { method: 'POST', body: JSON.stringify({ lat: point.lat, lng: point.lng, roadsideOptIn: true }) })
        toast('You are online')
      } else {
        await api('/api/drivers/offline', { method: 'POST' })
        toast('You are offline')
      }
      await refresh()
      await queryClient.invalidateQueries({ queryKey: ['open-requests'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Could not update status', 'err')
    } finally {
      setBusy(false)
    }
  }

  async function offer(request: RideRequest, type: 'ACCEPT' | 'COUNTER', value = request.proposedFare) {
    setBusy(true)
    try {
      await api(`/api/rides/requests/${request.id}/offers`, {
        method: 'POST',
        body: JSON.stringify({ amount: value, type, etaMinutes: 6, message: type === 'ACCEPT' ? 'On my way at your fare' : 'Counter offer' }),
      })
      toast(type === 'ACCEPT' ? 'Fare accepted' : 'Counter sent')
      setCounterFor(null)
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Offer failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  if (profile && profile.verificationStatus !== 'APPROVED') {
    return (
      <div className="px-1 pt-2">
        <div className="panel p-6">
          <Badge tone="amber">{profile.verificationStatus}</Badge>
          <h1 className="font-display mt-3 text-2xl font-extrabold tracking-tight">Documents needed</h1>
          <p className="mt-2 text-sm leading-relaxed text-muted">
            {profile.rejectionReason || 'Upload your licence, registration, and insurance. An admin will approve you before you can go online.'}
          </p>
          <Button className="mt-5" onClick={() => navigate('/drive/setup')}>Upload documents</Button>
        </div>
      </div>
    )
  }

  const center = profile?.lastLat ? { lat: profile.lastLat, lng: profile.lastLng! } : DEMO

  return (
    <div className="relative h-dvh">
      <MapCanvas center={center} pins={[{ point: center, kind: 'driver' }]} />

      <div className="absolute inset-x-0 top-0 z-[500] p-3 md:flex md:justify-end">
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass-strong w-full rounded-[1.75rem] p-4 md:w-[420px]"
        >
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <span className={`h-2.5 w-2.5 rounded-md ${online ? 'bg-brand' : 'bg-black/20'}`} />
              <div>
                <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-muted">Driver</p>
                <p className="font-display text-lg font-extrabold leading-tight">
                  {online ? 'You are online' : 'You are offline'}
                </p>
              </div>
            </div>
            <button
              onClick={() => void setOnline(!online)}
              className={`relative h-9 w-16 rounded-2xl transition ${online ? 'bg-brand' : 'bg-black/10'}`}
              aria-label="Toggle online"
            >
              <span className={`absolute top-1 h-7 w-7 rounded-xl bg-white shadow-soft transition ${online ? 'left-8' : 'left-1'}`} />
            </button>
          </div>
          <div className="mt-3.5 flex gap-2">
            <Button variant="ghost" className="flex-1" loading={busy} onClick={() => void setOnline(true, true)}>Demo city</Button>
            {active.data && <Button className="flex-1" onClick={() => navigate(`/rides/${active.data!.id}`)}>Open trip</Button>}
          </div>
        </motion.div>
      </div>

      <div className="absolute inset-x-0 bottom-20 z-[500] max-h-[46%] space-y-2.5 overflow-auto px-3 md:bottom-4 md:left-auto md:w-[420px]">
        {requests.isLoading && <div className="skeleton h-28 rounded-[1.75rem]" />}
        {(requests.data ?? []).map((request, index) => (
          <motion.article
            key={request.id}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.05 }}
            className="glass-strong rounded-[1.75rem] p-4"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-display text-2xl font-extrabold tracking-tight">{money(request.proposedFare, request.currency)}</p>
                <p className="mt-1 truncate text-sm font-semibold">{request.pickupAddress}</p>
                <p className="truncate text-sm text-muted">to {request.destinationAddress}</p>
              </div>
              {request.nightOwl && <Badge tone="ink">Night Owl</Badge>}
            </div>
            <p className="mt-2 text-xs text-muted">
              {Number(request.distanceKm).toFixed(1)} km · {request.durationMin} min · {request.riderName}
            </p>
            <div className="mt-3.5 grid grid-cols-2 gap-2">
              <Button loading={busy} onClick={() => void offer(request, 'ACCEPT')}>Accept</Button>
              <Button variant="ghost" onClick={() => { setCounterFor(request); setAmount(Number(request.proposedFare) + 15) }}>Counter</Button>
            </div>
          </motion.article>
        ))}
        {online && requests.data?.length === 0 && (
          <div className="glass rounded-[1.75rem] p-4 text-sm text-muted">Listening for nearby requests…</div>
        )}
      </div>

      <AnimatePresence>
        {counterFor && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[600] grid place-items-end bg-ink/40 p-4 backdrop-blur-sm"
          >
            <motion.div
              initial={{ y: 36 }}
              animate={{ y: 0 }}
              exit={{ y: 24 }}
              className="w-full max-w-md rounded-[1.75rem] bg-white p-5 shadow-float"
            >
              <p className="font-display text-lg font-extrabold">Counter offer</p>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="mt-4 w-full rounded-2xl bg-brand-soft px-4 py-4 font-display text-3xl font-extrabold outline-none focus:ring-4 focus:ring-brand/15"
              />
              <div className="mt-4 grid grid-cols-2 gap-2">
                <Button variant="ghost" onClick={() => setCounterFor(null)}>Cancel</Button>
                <Button loading={busy} onClick={() => void offer(counterFor, 'COUNTER', amount)}>Send</Button>
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
    if (!navigator.geolocation) return resolve(DEMO)
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => resolve(DEMO),
      { timeout: 4000 },
    )
  })
}
