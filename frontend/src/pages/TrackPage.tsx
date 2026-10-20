import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { api } from '../lib/api'
import { labelStatus } from '../lib/format'
import { Badge } from '../components/ui'
import { MapCanvas } from '../components/MapCanvas'
import { DEMO, type Track } from '../types'

export function TrackPage() {
  const { token } = useParams()
  const track = useQuery({
    queryKey: ['track', token],
    queryFn: () => api<Track>(`/api/public/track/${token}`),
    refetchInterval: 5000,
  })
  const data = track.data
  const point = data?.driverLat ? { lat: data.driverLat, lng: data.driverLng! } : DEMO
  return (
    <div className="min-h-dvh">
      <div className="mx-auto max-w-lg px-4 py-5">
        <div className="flex items-center gap-3">
          <img src="/logo.jpg" alt="" className="h-12 w-12 rounded-2xl shadow-soft" />
          <div>
            <p className="font-display text-xl font-extrabold tracking-tight">Shofe</p>
            <p className="text-xs text-muted">Live trip share</p>
          </div>
        </div>
        <h1 className="font-display mt-5 text-3xl font-extrabold tracking-tight">Live trip</h1>
        {!data && <div className="skeleton mt-4 h-64 rounded-[1.75rem]" />}
        {data && (
          <>
            <div className="mt-4 h-72 overflow-hidden rounded-[1.75rem] border border-line shadow-soft">
              <MapCanvas center={point} pins={[{ point, kind: 'driver' }]} />
            </div>
            <div className="panel mt-4 p-4">
              <Badge>{labelStatus(data.status)}</Badge>
              {data.nightOwl && <span className="ml-2"><Badge tone="ink">Night Owl</Badge></span>}
              <p className="font-display mt-3 text-lg font-extrabold">{data.driverName} · {data.vehicle}</p>
              <p className="text-sm text-muted">{data.plateNumber}</p>
              <p className="mt-3 text-sm font-semibold">{data.pickupAddress}</p>
              <p className="text-sm text-muted">to {data.destinationAddress}</p>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
