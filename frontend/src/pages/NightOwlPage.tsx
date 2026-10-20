import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Phone, ShieldAlert } from 'lucide-react'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { Badge, Button, PageTitle, Skeleton } from '../components/ui'
import { DEMO } from '../types'

type NightStatus = {
  active: boolean
  manualOn: boolean
  scheduleWindow: boolean
  solarNight: boolean
  zone: string
  startHour: number
  endHour: number
  emergency: { countryCode: string; police: string; ambulance: string; fire: string; label: string }
  uiHint: string
}

type HelpPlace = { type: string; name: string; lat: number; lng: number; distanceKm: number; open24h: boolean }

export function NightOwlPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [manualOn, setManualOn] = useState(() => localStorage.getItem('shofe_night_owl') === '1')
  const [point, setPoint] = useState(DEMO)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition(
      (pos) => setPoint({ lat: pos.coords.latitude, lng: pos.coords.longitude, label: 'Current location' }),
      () => undefined,
      { timeout: 4000 },
    )
  }, [])

  const status = useQuery({
    queryKey: ['night-owl', point.lat, point.lng, manualOn],
    queryFn: () => api<NightStatus>(`/api/night-owl/status?lat=${point.lat}&lng=${point.lng}&manualOn=${manualOn}`),
  })
  const help = useQuery({
    queryKey: ['quick-help', point.lat, point.lng],
    queryFn: () => api<HelpPlace[]>(`/api/night-owl/quick-help?lat=${point.lat}&lng=${point.lng}`),
    enabled: Boolean(status.data?.active),
  })

  function toggleManual() {
    const next = !manualOn
    setManualOn(next)
    localStorage.setItem('shofe_night_owl', next ? '1' : '0')
  }

  async function checkIn(ok: boolean) {
    setBusy(true)
    try {
      const result = await api<{ message: string }>('/api/night-owl/check-in', {
        method: 'POST',
        body: JSON.stringify({ ok, lat: point.lat, lng: point.lng, note: ok ? 'All good' : 'Need help' }),
      })
      toast(result.message, ok ? 'ok' : 'err')
      await queryClient.invalidateQueries({ queryKey: ['night-owl'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Check-in failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  const data = status.data

  return (
    <div className={data?.active ? 'rounded-[1.75rem] bg-ink p-1 text-white' : ''}>
      <div className={data?.active ? 'rounded-[1.6rem] bg-[#0b1220] p-4' : ''}>
        <PageTitle
          eyebrow="Safety module"
          title="Night Owl"
          subtitle="Auto night window, Guardians, SOS, check-ins, and quick help — from the Shofe PDF."
        />

        {status.isLoading && <Skeleton className="h-28" />}
        {data && (
          <div className={`panel p-4 ${data.active ? '!bg-white/10 !border-white/10 !text-white' : ''}`}>
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="font-display text-lg font-extrabold">{data.active ? 'Night Owl is on' : 'Night Owl is off'}</p>
                <p className={`text-sm ${data.active ? 'text-white/70' : 'text-muted'}`}>
                  Schedule {data.startHour}:00–{data.endHour}:00 ({data.zone})
                  {data.solarNight ? ' · solar night' : ''}
                </p>
              </div>
              <button
                onClick={toggleManual}
                className={`relative h-9 w-16 rounded-2xl transition ${data.manualOn || data.active ? 'bg-brand' : 'bg-black/20'}`}
              >
                <span className={`absolute top-1 h-7 w-7 rounded-xl bg-white transition ${manualOn ? 'left-8' : 'left-1'}`} />
              </button>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              {data.scheduleWindow && <Badge tone="ink">Schedule window</Badge>}
              {data.solarNight && <Badge>Solar night</Badge>}
              {data.manualOn && <Badge tone="amber">Manual</Badge>}
            </div>
            <p className={`mt-3 text-sm ${data.active ? 'text-white/75' : 'text-muted'}`}>{data.uiHint}</p>
          </div>
        )}

        {data && (
          <div className={`panel mt-3 p-4 ${data.active ? '!bg-white/10 !border-white/10' : ''}`}>
            <p className="font-display font-extrabold">Emergency — {data.emergency.label}</p>
            <div className="mt-3 grid grid-cols-3 gap-2 text-center text-sm">
              {[
                ['Police', data.emergency.police],
                ['Ambulance', data.emergency.ambulance],
                ['Fire', data.emergency.fire],
              ].map(([label, number]) => (
                <a key={label} href={`tel:${number}`} className="rounded-2xl bg-brand-mist px-2 py-3 font-bold text-ink">
                  <Phone className="mx-auto mb-1 h-4 w-4" />
                  {label}
                  <span className="mt-1 block text-xs">{number}</span>
                </a>
              ))}
            </div>
          </div>
        )}

        <div className="mt-3 grid grid-cols-2 gap-2">
          <Button loading={busy} onClick={() => void checkIn(true)}>I am OK</Button>
          <Button variant="danger" loading={busy} onClick={() => void checkIn(false)}>
            <ShieldAlert className="h-4 w-4" /> Need help
          </Button>
        </div>

        {data?.active && (
          <div className="mt-4">
            <p className="font-display font-extrabold">Quick-help nearby</p>
            <div className="mt-2 grid gap-2">
              {(help.data ?? []).map((place) => (
                <div key={place.name} className={`rounded-2xl px-3.5 py-3 text-sm ${data.active ? 'bg-white/10' : 'bg-white border border-line'}`}>
                  <div className="flex justify-between gap-2">
                    <span className="font-bold">{place.name}</span>
                    <span className={data.active ? 'text-white/60' : 'text-muted'}>{place.distanceKm.toFixed(1)} km</span>
                  </div>
                  <p className={data.active ? 'text-white/60' : 'text-muted'}>
                    {place.type.replaceAll('_', ' ')}
                    {place.open24h ? ' · 24h' : ''}
                  </p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
