import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { Moon, Navigation, MapPin, LocateFixed } from 'lucide-react'
import { MapCanvas } from '../components/MapCanvas'
import { Badge, Button } from '../components/ui'
import { Tabs } from '../components/Tabs'
import { ModeSwitch } from '../components/ModeSwitch'
import { api } from '../lib/api'
import { money } from '../lib/format'
import { filterPlaces, locate, reverseLabel } from '../lib/geo'
import { useToast } from '../context/ToastContext'
import { DEFAULT_CITY, type Place, type Quote, type Ride, type RideRequest } from '../types'

type Point = { lat: number; lng: number; label: string }

export function RiderHome() {
  const navigate = useNavigate()
  const toast = useToast()
  const [target, setTarget] = useState<'pickup' | 'drop'>('drop')
  const [pickup, setPickup] = useState<Point | null>(null)
  const [drop, setDrop] = useState<Point | null>(null)
  const [query, setQuery] = useState('')
  const [places, setPlaces] = useState<Place[]>(filterPlaces(''))
  const [nightOwl, setNightOwl] = useState(() => localStorage.getItem('shofe_night_owl') === '1')
  const [fare, setFare] = useState(5)
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
    void (async () => {
      const point = await locate()
      const label = await reverseLabel(point.lat, point.lng)
      const here = { ...point, label: label === 'Dropped pin' ? 'Current location' : label }
      setPickup(here)
      setFocus(here)
    })()
  }, [])

  useEffect(() => {
    const handle = setTimeout(() => {
      const local = filterPlaces(query)
      if (query.trim().length < 2) {
        setPlaces(local)
        return
      }
      api<Place[]>(`/api/geo/search?q=${encodeURIComponent(query)}`)
        .then((remote) => setPlaces(filterPlaces(query, remote)))
        .catch(() => setPlaces(local))
    }, 250)
    return () => clearTimeout(handle)
  }, [query])

  function choose(place: Place) {
    const point = { lat: place.lat, lng: place.lng, label: place.label }
    if (target === 'pickup') setPickup(point)
    else setDrop(point)
    setFocus(point)
    setQuery('')
    setPlaces(filterPlaces(''))
    if (target === 'pickup') setTarget('drop')
  }

  async function onMap(point: { lat: number; lng: number }) {
    const label = await reverseLabel(point.lat, point.lng)
    const next = { ...point, label }
    if (target === 'pickup') setPickup(next)
    else setDrop(next)
    setFocus(next)
  }

  async function useHere() {
    const point = await locate()
    const label = await reverseLabel(point.lat, point.lng)
    const here = { ...point, label: label === 'Dropped pin' ? 'Current location' : label }
    setPickup(here)
    setFocus(here)
    setTarget('drop')
  }

  async function requestRide() {
    if (!pickup || !drop) return
    if (pickup.lat === drop.lat && pickup.lng === drop.lng) {
      toast('Choose a different drop-off', 'err')
      return
    }
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
        center={pickup ?? DEFAULT_CITY}
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
          className="pointer-events-auto glass-strong w-full rounded-sheet p-4 md:w-[400px]"
        >
          <div className="mb-3 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <img src="/logo.jpg" alt="Shofe" className="h-9 w-9 rounded-xl object-cover" />
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
          <ModeSwitch compact />

          <div className="mt-3 grid gap-2">
            <button
              type="button"
              onClick={() => setTarget('pickup')}
              className={`flex items-center gap-3 rounded-2xl px-3.5 py-3 text-left text-sm ${
                target === 'pickup' ? 'bg-white ring-2 ring-brand/30' : 'bg-brand-soft'
              }`}
            >
              <span className="grid h-6 w-6 place-items-center rounded-lg bg-brand text-[11px] font-extrabold text-white">A</span>
              <span className="min-w-0 flex-1 truncate font-semibold">{pickup?.label || 'Pickup'}</span>
            </button>
            <button
              type="button"
              onClick={() => setTarget('drop')}
              className={`flex items-center gap-3 rounded-2xl px-3.5 py-3 text-left text-sm ${
                target === 'drop' ? 'bg-white ring-2 ring-brand/30' : 'bg-brand-soft'
              }`}
            >
              <span className="grid h-6 w-6 place-items-center rounded-lg bg-ink text-[11px] font-extrabold text-white">B</span>
              <span className="min-w-0 flex-1 truncate font-semibold text-muted">{drop?.label || 'Destination'}</span>
            </button>
          </div>

          <div className="relative mt-3">
            <MapPin className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-brand" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={target === 'pickup' ? 'Search pickup' : 'Search destination'}
              className="w-full rounded-2xl bg-brand-soft py-3.5 pl-10 pr-12 text-sm outline-none ring-brand/20 transition focus:bg-white focus:ring-4"
            />
            <button
              type="button"
              onClick={() => void useHere()}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-xl p-2 text-brand"
              aria-label="Use current location"
            >
              <LocateFixed className="h-4 w-4" />
            </button>
          </div>

          <div className="mt-2 max-h-40 overflow-auto rounded-2xl border border-line bg-white">
            {places.map((place) => (
              <button
                key={place.label}
                onClick={() => choose(place)}
                className="block w-full border-b border-line/60 px-3.5 py-2.5 text-left text-sm last:border-0 hover:bg-brand-mist"
              >
                {place.label}
              </button>
            ))}
          </div>
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
            <div className="glass-strong rounded-sheet p-4">
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
                      min={Math.max(2, Math.round(Number(quote.data.recommendedFare) * 0.6))}
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
                    <Navigation className="h-4 w-4" /> Request ride
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
