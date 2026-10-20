import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { Moon, Navigation, MapPin } from 'lucide-react'
import { MapCanvas } from '../components/MapCanvas'
import { Badge, Button } from '../components/ui'
import { Tabs } from '../components/Tabs'
import { api } from '../lib/api'
import { money } from '../lib/format'
import { useToast } from '../context/ToastContext'
import { DEMO, type Place, type Quote, type Ride, type RideRequest } from '../types'

type Point = { lat: number; lng: number; label: string }

export function RiderHome() {
  const navigate = useNavigate()
  const toast = useToast()
  const [target, setTarget] = useState<'pickup' | 'drop'>('pickup')
  const [pickup, setPickup] = useState<Point | null>(null)
  const [drop, setDrop] = useState<Point | null>(null)
  const [query, setQuery] = useState('')
  const [places, setPlaces] = useState<Place[]>([])
  const [nightOwl, setNightOwl] = useState(() => localStorage.getItem('shofe_night_owl') === '1')
  const [fare, setFare] = useState(80)
  const [touched, setTouched] = useState(false)
  const [pay, setPay] = useState<'CASH' | 'PESEPAY' | 'CARD'>('CASH')
  const [busy, setBusy] = useState(false)
  const [focus, setFocus] = useState<Point | undefined>()

  const active = useQuery({ queryKey: ['active-ride'], queryFn: () => api<Ride | null>('/api/rides/active') })

  const quote = useQuery({
    queryKey: ['quote', pickup?.lat, pickup?.lng, drop?.lat, drop?.lng, nightOwl],
    enabled: Boolean(pickup && drop),
    queryFn: () => api<Quote>('/api/rides/quote', {
      method: 'POST',
      body: JSON.stringify({
        pickupLat: pickup!.lat, pickupLng: pickup!.lng,
        destinationLat: drop!.lat, destinationLng: drop!.lng, nightOwl,
      }),
    }),
  })

  useEffect(() => {
    if (quote.data && !touched) setFare(Number(quote.data.recommendedFare))
  }, [quote.data, touched])

  useEffect(() => {
    const handle = setTimeout(() => {
      if (query.trim().length < 3) { setPlaces([]); return }
      api<Place[]>(`/api/geo/search?q=${encodeURIComponent(query)}`).then(setPlaces).catch(() => setPlaces([]))
    }, 350)
    return () => clearTimeout(handle)
  }, [query])

  function choose(place: Place) {
    const point = { lat: place.lat, lng: place.lng, label: place.label }
    if (target === 'pickup') setPickup(point)
    else setDrop(point)
    setFocus(point)
    setPlaces([])
    setQuery('')
    setTarget(target === 'pickup' ? 'drop' : 'drop')
  }

  function onMap(point: { lat: number; lng: number }) {
    const next = { ...point, label: `${point.lat.toFixed(4)}, ${point.lng.toFixed(4)}` }
    if (target === 'pickup') setPickup(next)
    else setDrop(next)
  }

  function useDemo() {
    const start = { ...DEMO, label: DEMO.label }
    const end = { lat: DEMO.lat + 0.045, lng: DEMO.lng + 0.03, label: 'Sandton City' }
    setPickup(start)
    setDrop(end)
    setFocus(start)
    setTarget('drop')
  }

  async function requestRide() {
    if (!pickup || !drop) return
    setBusy(true)
    try {
      const created = await api<RideRequest>('/api/rides/requests', {
        method: 'POST',
        body: JSON.stringify({
          pickupLat: pickup.lat, pickupLng: pickup.lng, pickupAddress: pickup.label,
          destinationLat: drop.lat, destinationLng: drop.lng, destinationAddress: drop.label,
          proposedFare: fare, paymentMethod: pay, nightOwl,
        }),
      })
      navigate(`/requests/${created.id}`)
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Could not send request', 'err')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="relative h-dvh">
      <MapCanvas
        center={pickup ?? DEMO}
        focus={focus}
        onClick={onMap}
        route={quote.data?.route}
        pins={[
          ...(pickup ? [{ point: pickup, kind: 'pickup' as const }] : []),
          ...(drop ? [{ point: drop, kind: 'drop' as const }] : []),
        ]}
      />

      <div className="pointer-events-none absolute inset-x-0 top-0 z-[500] p-3 md:flex md:justify-end">
        <motion.div
          initial={{ opacity: 0, y: -12 }}
          animate={{ opacity: 1, y: 0 }}
          className="pointer-events-auto glass-strong w-full rounded-[1.75rem] p-4 md:w-[400px]"
        >
          <div className="mb-3 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <img src="/logo.jpg" alt="" className="h-9 w-9 rounded-xl" />
              <div>
                <p className="font-display text-sm font-extrabold leading-none">Shofe</p>
                <p className="mt-0.5 text-[11px] font-semibold text-muted">Where to?</p>
              </div>
            </div>
            <button
              onClick={() => {
                const next = !nightOwl
                setNightOwl(next)
                localStorage.setItem('shofe_night_owl', next ? '1' : '0')
              }}
              className={`flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-bold transition ${
                nightOwl ? 'bg-ink text-white' : 'bg-brand-mist text-brand-dark'
              }`}
            >
              <Moon className="h-3.5 w-3.5" /> Night Owl
            </button>
          </div>

          <Tabs
            value={target}
            onChange={setTarget}
            items={[
              { id: 'pickup', label: 'Pickup' },
              { id: 'drop', label: 'Drop-off' },
            ]}
          />

          <div className="relative mt-3">
            <MapPin className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-brand" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={target === 'pickup' ? 'Search pickup' : 'Search destination'}
              className="w-full rounded-2xl bg-brand-soft py-3.5 pl-10 pr-4 text-sm outline-none ring-brand/20 transition focus:bg-white focus:ring-4"
            />
          </div>

          <AnimatePresence>
            {places.length > 0 && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="mt-2 max-h-40 overflow-auto rounded-2xl border border-line bg-white"
              >
                {places.map((place) => (
                  <button
                    key={place.label}
                    onClick={() => choose(place)}
                    className="block w-full border-b border-line/60 px-3.5 py-2.5 text-left text-sm last:border-0 hover:bg-brand-mist"
                  >
                    {place.label}
                  </button>
                ))}
              </motion.div>
            )}
          </AnimatePresence>

          <p className="mt-3 truncate text-xs text-muted">
            <span className="font-semibold text-ink">{pickup ? pickup.label : 'Tap map or search'}</span>
            {' → '}
            {drop ? drop.label : 'choose drop-off'}
          </p>
          <button onClick={useDemo} className="mt-2 text-xs font-bold text-brand hover:underline">
            Use Johannesburg demo area
          </button>
        </motion.div>
      </div>

      {active.data && (
        <motion.button
          initial={{ opacity: 0, scale: 0.94 }}
          animate={{ opacity: 1, scale: 1 }}
          onClick={() => navigate(`/rides/${active.data!.id}`)}
          className="absolute left-3 top-52 z-[500] rounded-2xl bg-ink px-4 py-2.5 text-xs font-bold text-white shadow-float md:top-6 md:left-[unset] md:right-[430px]"
        >
          Trip in progress
        </motion.button>
      )}

      <AnimatePresence>
        {pickup && drop && (
          <motion.div
            initial={{ y: 40, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: 24, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 280, damping: 26 }}
            className="absolute inset-x-0 bottom-20 z-[500] px-3 md:bottom-4 md:left-auto md:w-[400px]"
          >
            <div className="glass-strong rounded-[1.75rem] p-4">
              {quote.isLoading && <div className="skeleton mb-3 h-16 rounded-2xl" />}
              {quote.data && (
                <>
                  <div className="flex items-end justify-between gap-3">
                    <div>
                      <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-muted">Recommended</p>
                      <p className="font-display text-3xl font-extrabold tracking-tight">
                        {money(quote.data.recommendedFare, quote.data.currency)}
                      </p>
                    </div>
                    <div className="text-right text-sm text-muted">
                      <p className="font-semibold text-ink">{Number(quote.data.distanceKm).toFixed(1)} km</p>
                      <p>{quote.data.durationMin} min</p>
                      {quote.data.nightOwl && <div className="mt-1"><Badge tone="ink">Night Owl +15%</Badge></div>}
                    </div>
                  </div>

                  <label className="mt-4 block">
                    <div className="flex items-center justify-between text-sm font-bold">
                      <span>Your offer</span>
                      <span className="font-display text-lg text-brand">{money(fare, quote.data.currency)}</span>
                    </div>
                    <input
                      className="range-brand mt-3 w-full"
                      type="range"
                      min={Math.max(25, Math.round(Number(quote.data.recommendedFare) * 0.6))}
                      max={Math.round(Number(quote.data.recommendedFare) * 2)}
                      value={fare}
                      onChange={(e) => { setTouched(true); setFare(Number(e.target.value)) }}
                    />
                  </label>

                  <Tabs
                    className="mt-4"
                    value={pay}
                    onChange={setPay}
                    items={[
                      { id: 'CASH', label: 'Cash' },
                      { id: 'PESEPAY', label: 'Pesepay' },
                      { id: 'CARD', label: 'Card' },
                    ]}
                  />

                  <Button className="mt-4 w-full" loading={busy} onClick={() => void requestRide()}>
                    <Navigation className="h-4 w-4" /> Find drivers
                  </Button>
                </>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
