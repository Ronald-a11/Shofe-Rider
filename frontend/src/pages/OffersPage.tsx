import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { Star } from 'lucide-react'
import { motion } from 'framer-motion'
import { api } from '../lib/api'
import { labelStatus, money, stars } from '../lib/format'
import { useSocket } from '../context/SocketContext'
import { useToast } from '../context/ToastContext'
import { Badge, Button, PageTitle, Skeleton } from '../components/ui'
import { useState } from 'react'
import type { Ride, RideRequest } from '../types'

export function OffersPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [picking, setPicking] = useState<string | null>(null)

  const request = useQuery({
    queryKey: ['request', id],
    queryFn: () => api<RideRequest>(`/api/rides/requests/${id}`),
    refetchInterval: 4000,
  })

  useSocket((event) => {
    if (event.destination.endsWith('/offers') || event.destination.endsWith('/rides')) {
      void queryClient.invalidateQueries({ queryKey: ['request', id] })
    }
  })

  async function choose(offerId: string) {
    setPicking(offerId)
    try {
      const ride = await api<Ride>(`/api/rides/offers/${offerId}/select`, { method: 'POST' })
      navigate(`/rides/${ride.id}`)
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Could not select driver', 'err')
    } finally {
      setPicking(null)
    }
  }

  async function cancel() {
    await api(`/api/rides/requests/${id}/cancel`, { method: 'POST' })
    navigate('/')
  }

  const data = request.data
  const offers = data?.offers.filter((offer) => offer.status === 'PENDING' || offer.status === 'SELECTED') ?? []

  return (
    <div>
      <PageTitle eyebrow="Your request" title="Choose a driver" subtitle="Compare fare, rating, ETA, and vehicle." />

      {data && (
        <div className="panel animate-fade-up p-4">
          <div className="flex items-center justify-between gap-2">
            <Badge>{labelStatus(data.status)}</Badge>
            {data.nightOwl && <Badge tone="ink">Night Owl</Badge>}
          </div>
          <p className="mt-3 font-bold leading-snug">{data.pickupAddress}</p>
          <p className="mt-0.5 text-sm text-muted">to {data.destinationAddress}</p>
          <p className="mt-3 text-sm">
            Your offer <span className="font-extrabold text-brand">{money(data.proposedFare, data.currency)}</span>
            <span className="text-muted"> · suggested {money(data.recommendedFare, data.currency)}</span>
          </p>
        </div>
      )}

      <div className="mt-4 grid gap-3">
        {request.isLoading && <><Skeleton className="h-36" /><Skeleton className="h-36" /></>}

        {data && offers.length === 0 && (
          <div className="relative grid place-items-center overflow-hidden rounded-sheet bg-white py-14 shadow-soft">
            <div className="relative h-24 w-24">
              <span className="radar" /><span className="radar delay" />
              <span className="absolute inset-6 rounded-full bg-brand shadow-soft" />
            </div>
            <p className="font-display mt-5 text-lg font-extrabold">Waiting for drivers</p>
            <p className="mt-1 text-sm text-muted">Offers appear here as they arrive.</p>
          </div>
        )}

        {offers.map((offer, index) => (
          <motion.article
            key={offer.id}
            initial={{ opacity: 0, y: 14, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ delay: index * 0.06, duration: 0.35, ease: [0.22, 0.8, 0.22, 1] }}
            className="rounded-sheet border border-line bg-white p-4 shadow-soft"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-display text-lg font-extrabold tracking-tight">{offer.driverName}</p>
                <p className="truncate text-sm text-muted">
                  {offer.vehicleColor} {offer.vehicleMake} {offer.vehicleModel}
                </p>
                <p className="mt-1.5 flex items-center gap-1 text-sm">
                  <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
                  {stars(offer.driverRating, offer.driverRatingCount)} · {offer.tripCount} trips
                </p>
              </div>
              <div className="text-right">
                <p className="font-display text-2xl font-extrabold tracking-tight">{money(offer.amount, data?.currency)}</p>
                <p className="text-xs font-bold text-muted">{offer.etaMinutes} min away</p>
              </div>
            </div>
            {offer.message && <p className="mt-3 rounded-2xl bg-brand-soft px-3 py-2 text-sm">{offer.message}</p>}
            <div className="mt-3">
              <Badge tone={offer.type === 'ACCEPT' ? 'green' : 'amber'}>
                {offer.type === 'ACCEPT' ? 'Accepted your fare' : 'Counter offer'}
              </Badge>
            </div>
            <Button className="mt-4 w-full" loading={picking === offer.id} onClick={() => void choose(offer.id)}>
              Choose {offer.driverName.split(' ')[0]}
            </Button>
          </motion.article>
        ))}
      </div>

      <Button variant="ghost" className="mt-4 w-full" onClick={() => void cancel()}>Cancel request</Button>
    </div>
  )
}
