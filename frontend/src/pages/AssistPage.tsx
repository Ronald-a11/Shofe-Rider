import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Moon } from 'lucide-react'
import { api } from '../lib/api'
import { labelStatus, money } from '../lib/format'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Badge, Button, Empty, PageTitle } from '../components/ui'
import { DEMO, ROADSIDE, type Roadside } from '../types'

export function AssistPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [type, setType] = useState<(typeof ROADSIDE)[number]['id']>('JUMP_START')
  const [details, setDetails] = useState('')
  const [busy, setBusy] = useState(false)
  const selected = ROADSIDE.find((item) => item.id === type)!
  const driver = user?.role === 'DRIVER'

  const mine = useQuery({ queryKey: ['roadside-mine'], queryFn: () => api<Roadside[]>('/api/roadside/mine') })
  const open = useQuery({ queryKey: ['roadside-open'], queryFn: () => api<Roadside[]>('/api/roadside/open'), enabled: driver })

  async function create() {
    setBusy(true)
    try {
      await api('/api/roadside', {
        method: 'POST',
        body: JSON.stringify({ type, lat: DEMO.lat, lng: DEMO.lng, address: DEMO.label, details, fare: selected.fare }),
      })
      toast('Assistance request sent')
      await queryClient.invalidateQueries({ queryKey: ['roadside-mine'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Could not request help', 'err')
    } finally {
      setBusy(false)
    }
  }

  async function accept(id: string) {
    try {
      await api(`/api/roadside/${id}/accept`, { method: 'POST' })
      toast('Job accepted')
      await queryClient.invalidateQueries({ queryKey: ['roadside-open'] })
      await queryClient.invalidateQueries({ queryKey: ['roadside-mine'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Could not accept', 'err')
    }
  }

  return (
    <div>
      <PageTitle
        title="Roadside assistance"
        subtitle="Jump starts, tyres, fuel, towing, and lockouts — same nearby-driver flow."
      />

      <button
        onClick={() => navigate('/night-owl')}
        className="mb-4 flex w-full items-center gap-3 rounded-[1.75rem] bg-ink px-4 py-4 text-left text-white shadow-soft"
      >
        <Moon className="h-5 w-5 shrink-0" />
        <div>
          <p className="font-display font-extrabold">Night Owl</p>
          <p className="text-xs text-white/70">Schedule window, check-ins, emergency numbers, quick-help</p>
        </div>
      </button>

      {!driver && (
        <div className="panel p-4">
          <div className="grid grid-cols-2 gap-2">
            {ROADSIDE.map((item) => (
              <button
                key={item.id}
                onClick={() => setType(item.id)}
                className={`rounded-2xl px-3.5 py-3.5 text-left text-sm font-bold transition ${
                  type === item.id ? 'bg-brand text-white shadow-soft' : 'bg-brand-mist text-ink'
                }`}
              >
                {item.label}
                <span className="mt-1 block text-xs opacity-80">{money(item.fare)}</span>
              </button>
            ))}
          </div>
          <textarea
            value={details}
            onChange={(e) => setDetails(e.target.value)}
            placeholder="What happened?"
            className="mt-3 w-full rounded-2xl bg-brand-soft p-3.5 outline-none focus:ring-4 focus:ring-brand/15"
          />
          <Button className="mt-3 w-full" loading={busy} onClick={() => void create()}>
            Request {selected.label}
          </Button>
          <p className="mt-2 text-xs text-muted">Demo requests use Johannesburg so seeded drivers can see them.</p>
        </div>
      )}

      {driver && (
        <div className="grid gap-3">
          <p className="text-sm font-bold">Open jobs</p>
          {(open.data ?? []).length === 0 && <Empty title="No open jobs" body="Go online near a request to see it here." />}
          {(open.data ?? []).map((job) => (
            <article key={job.id} className="panel p-4">
              <Badge>{labelStatus(job.type)}</Badge>
              <p className="font-display mt-2 text-xl font-extrabold">{money(job.fare, job.currency)}</p>
              <p className="text-sm text-muted">{job.address}</p>
              <Button className="mt-3" onClick={() => void accept(job.id)}>Accept job</Button>
            </article>
          ))}
        </div>
      )}

      <div className="mt-6 grid gap-3">
        <p className="text-sm font-bold">Your jobs</p>
        {(mine.data ?? []).map((job) => (
          <article key={job.id} className="panel p-4">
            <div className="flex justify-between gap-2">
              <Badge>{labelStatus(job.status)}</Badge>
              <span className="font-display font-extrabold">{money(job.fare, job.currency)}</span>
            </div>
            <p className="mt-2 font-bold">{labelStatus(job.type)}</p>
            <p className="text-sm text-muted">{job.address}</p>
            {job.providerName && <p className="mt-1 text-sm">Provider: {job.providerName}</p>}
          </article>
        ))}
      </div>
    </div>
  )
}
