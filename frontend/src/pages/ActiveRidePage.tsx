import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageCircle, Share2, Star } from 'lucide-react'
import { MapCanvas } from '../components/MapCanvas'
import { Badge, Button } from '../components/ui'
import { api, apiUrl } from '../lib/api'
import { labelStatus, money } from '../lib/format'
import { useAuth } from '../context/AuthContext'
import { useSocket } from '../context/SocketContext'
import { useToast } from '../context/ToastContext'
import type { ChatMessage, Ride } from '../types'

const STEPS = ['ACCEPTED', 'DRIVER_ARRIVED', 'IN_PROGRESS', 'COMPLETED']

export function ActiveRidePage() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [chatOpen, setChatOpen] = useState(false)
  const [text, setText] = useState('')
  const [pin, setPin] = useState('')
  const [driverPoint, setDriverPoint] = useState<{ lat: number; lng: number } | null>(null)
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)
  const [safetyAlert, setSafetyAlert] = useState<string | null>(null)

  const ride = useQuery({
    queryKey: ['ride', id],
    queryFn: () => api<Ride>(`/api/rides/${id}`),
    refetchInterval: 4000,
  })
  const chat = useQuery({
    queryKey: ['chat', id],
    queryFn: () => api<ChatMessage[]>(`/api/rides/${id}/chat`),
    enabled: chatOpen,
  })

  useSocket((event) => {
    if (String(event.destination).includes('/rides')) void queryClient.invalidateQueries({ queryKey: ['ride', id] })
  })

  useEffect(() => {
    const token = localStorage.getItem('shofe_token')
    if (!token || !id) return
    let client: { deactivate: () => void; subscribe: Function; activate: () => void } | null = null
    let stop = false
    ;(async () => {
      const { Client } = await import('@stomp/stompjs')
      const SockJS = (await import('sockjs-client')).default
      if (stop) return
      const stomp = new Client({
        webSocketFactory: () => new SockJS(apiUrl('/ws')),
        connectHeaders: { Authorization: `Bearer ${token}` },
        onConnect: () => {
          stomp.subscribe(`/topic/rides/${id}/location`, (message) => setDriverPoint(JSON.parse(message.body)))
          stomp.subscribe(`/topic/rides/${id}/chat`, () => void queryClient.invalidateQueries({ queryKey: ['chat', id] }))
          stomp.subscribe(`/topic/rides/${id}`, () => void queryClient.invalidateQueries({ queryKey: ['ride', id] }))
          stomp.subscribe(`/topic/rides/${id}/safety`, (message) => {
            const payload = JSON.parse(message.body) as { message?: string; type?: string }
            const text = payload.message || payload.type || 'Safety alert'
            setSafetyAlert(text)
            toast(text, 'err')
          })
        },
      })
      stomp.activate()
      client = stomp
    })()
    return () => { stop = true; void client?.deactivate() }
  }, [id, queryClient])

  const data = ride.data
  const isDriver = user?.id === data?.driver.id
  const step = STEPS.indexOf(data?.status ?? '')

  async function act(path: string, body?: unknown) {
    setBusy(true)
    try {
      await api(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
      await queryClient.invalidateQueries({ queryKey: ['ride', id] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  async function startPayment() {
    if (!id) return
    setBusy(true)
    try {
      const started = await api<{ redirectUrl: string }>(`/api/payments/rides/${id}/start`, {
        method: 'POST',
        body: JSON.stringify({ provider: 'PESEPAY' }),
      })
      if (started.redirectUrl) {
        window.location.href = started.redirectUrl
        return
      }
      toast('Payment started')
      await queryClient.invalidateQueries({ queryKey: ['ride', id] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Payment failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  async function sendChat() {
    if (!text.trim()) return
    await api(`/api/rides/${id}/chat`, { method: 'POST', body: JSON.stringify({ body: text }) })
    setText('')
    await queryClient.invalidateQueries({ queryKey: ['chat', id] })
  }

  if (!data) return <div className="skeleton h-80" />

  const pins = [
    { point: { lat: data.pickupLat, lng: data.pickupLng }, kind: 'pickup' as const },
    { point: { lat: data.destinationLat, lng: data.destinationLng }, kind: 'drop' as const },
    { point: driverPoint ?? { lat: data.driver.lat ?? data.pickupLat, lng: data.driver.lng ?? data.pickupLng }, kind: 'driver' as const },
  ]

  return (
    <div className="grid gap-3">
      <div className="h-64 overflow-hidden rounded-sheet border border-line shadow-soft md:h-80">
        <MapCanvas center={pins[2].point} pins={pins} />
      </div>
      <div className="panel p-4">
        {safetyAlert && (
          <div className="mb-3 rounded-2xl bg-rose-50 px-3.5 py-3 text-sm font-bold text-rose-800">
            {safetyAlert}
          </div>
        )}
        <div className="flex items-center justify-between gap-2">
          <Badge>{labelStatus(data.status)}</Badge>
          <p className="font-display text-2xl font-extrabold tracking-tight">{money(data.fare, data.currency)}</p>
        </div>
        <div className="mt-4 grid grid-cols-4 gap-1.5">
          {STEPS.map((item, index) => (
            <div key={item} className={`h-1.5 rounded-md transition ${index <= step ? 'bg-brand' : 'bg-black/10'}`} />
          ))}
        </div>
        <p className="font-display mt-4 text-lg font-extrabold">{isDriver ? data.rider.fullName : data.driver.fullName}</p>
        <p className="text-sm text-muted">{isDriver ? data.rider.phone : `${data.driver.vehicleColor ?? ''} ${data.driver.vehicleMake ?? ''} ${data.driver.vehicleModel ?? ''} · ${data.driver.plateNumber ?? ''}`}</p>
        <p className="mt-3 text-sm font-semibold">{data.pickupAddress}</p>
        <p className="text-sm text-muted">to {data.destinationAddress}</p>
        {data.nightOwl && !isDriver && data.pinCode && (
          <div className="mt-4 rounded-2xl bg-ink px-4 py-4 text-white">
            <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-white/70">Night Owl PIN</p>
            <p className="font-display mt-1 text-3xl font-extrabold tracking-[0.28em]">{data.pinCode}</p>
            <p className="mt-1 text-xs text-white/70">Show this to your driver before the trip starts.</p>
          </div>
        )}
        {isDriver && data.nightOwl && !data.pinVerified && data.status !== 'CANCELLED' && data.status !== 'COMPLETED' && (
          <div className="mt-4 flex gap-2">
            <input value={pin} onChange={(e) => setPin(e.target.value)} maxLength={4} placeholder="PIN" className="w-28 rounded-2xl bg-brand-soft px-4 py-3 text-center font-display text-xl font-extrabold tracking-widest outline-none focus:ring-4 focus:ring-brand/15" />
            <Button loading={busy} onClick={() => void act(`/api/rides/${id}/verify-pin`, { pin })}>Verify PIN</Button>
          </div>
        )}
        <div className="mt-4 grid gap-2">
          {isDriver && data.status === 'ACCEPTED' && <Button loading={busy} onClick={() => void act(`/api/rides/${id}/status`, { status: 'DRIVER_ARRIVED' })}>I've arrived</Button>}
          {isDriver && data.status === 'DRIVER_ARRIVED' && <Button loading={busy} onClick={() => void act(`/api/rides/${id}/status`, { status: 'IN_PROGRESS' })}>Start trip</Button>}
          {isDriver && data.status === 'IN_PROGRESS' && <Button loading={busy} onClick={() => void act(`/api/rides/${id}/status`, { status: 'COMPLETED' })}>Complete trip</Button>}
          {data.status !== 'COMPLETED' && data.status !== 'CANCELLED' && data.status !== 'IN_PROGRESS' && (
            <Button variant="ghost" onClick={() => void act(`/api/rides/${id}/status`, { status: 'CANCELLED', reason: 'Cancelled in app' })}>Cancel trip</Button>
          )}
          {!isDriver && data.paymentMethod === 'PESEPAY' && data.paymentStatus !== 'PAID' && (
            <Button
              variant="dark"
              loading={busy}
              onClick={() => void startPayment()}
            >
              Pay with Pesepay
            </Button>
          )}
        </div>
        <div className="mt-3 flex gap-2">
          <Button variant="ghost" className="flex-1" onClick={() => setChatOpen(true)}><MessageCircle className="h-4 w-4" /> Chat</Button>
          <Button variant="ghost" className="flex-1" onClick={() => { void navigator.clipboard.writeText(`${location.origin}${data.sharePath}`); toast('Live link copied') }}><Share2 className="h-4 w-4" /> Share</Button>
        </div>
      </div>
      {data.status === 'COMPLETED' && (
        <div className="panel p-4">
          <p className="font-display font-extrabold">Rate this trip</p>
          <div className="mt-2 flex gap-1">
            {[1, 2, 3, 4, 5].map((score) => (
              <button key={score} onClick={() => setRating(score)}><Star className={`h-7 w-7 ${score <= rating ? 'fill-amber-400 text-amber-400' : 'text-black/20'}`} /></button>
            ))}
          </div>
          <textarea value={comment} onChange={(e) => setComment(e.target.value)} className="mt-3 w-full rounded-2xl bg-brand-soft p-3 outline-none focus:ring-4 focus:ring-brand/15" placeholder="Optional note" />
          <Button className="mt-3" loading={busy} onClick={() => void act(`/api/rides/${id}/rate`, { score: rating, comment })}>Submit rating</Button>
        </div>
      )}
      {chatOpen && (
        <div className="fixed inset-0 z-50 grid place-items-end bg-ink/40 p-3 backdrop-blur-sm md:place-items-center">
          <div className="sheet flex h-[70dvh] w-full max-w-md flex-col rounded-sheet bg-white p-4 shadow-float">
            <div className="flex items-center justify-between">
              <p className="font-display font-extrabold">Chat</p>
              <button onClick={() => setChatOpen(false)} className="text-sm font-bold text-muted">Close</button>
            </div>
            <div className="mt-3 flex-1 space-y-2 overflow-auto">
              {(chat.data ?? []).map((message) => (
                <div key={message.id} className={`max-w-[80%] rounded-2xl px-3.5 py-2.5 text-sm ${message.senderId === user?.id ? 'ml-auto bg-brand text-white' : 'bg-brand-mist'}`}>
                  <p className="text-[10px] font-bold opacity-70">{message.senderName}</p>
                  {message.body}
                </div>
              ))}
            </div>
            <div className="mt-3 flex gap-2">
              <input value={text} onChange={(e) => setText(e.target.value)} className="flex-1 rounded-2xl bg-brand-soft px-3 py-3 outline-none focus:ring-4 focus:ring-brand/15" placeholder="Message" />
              <Button onClick={() => void sendChat()}>Send</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
