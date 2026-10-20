import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { labelStatus, money, when } from '../lib/format'
import { Badge, Empty, PageTitle, Skeleton } from '../components/ui'
import { Tabs } from '../components/Tabs'
import type { Ride } from '../types'

type Filter = 'all' | 'active' | 'done'

const ACTIVE = new Set(['ACCEPTED', 'DRIVER_ARRIVED', 'IN_PROGRESS'])

export function ActivityPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<Filter>('all')
  const history = useQuery({ queryKey: ['history'], queryFn: () => api<Ride[]>('/api/rides/history') })

  const rows = useMemo(() => {
    const list = history.data ?? []
    if (filter === 'active') return list.filter((ride) => ACTIVE.has(ride.status))
    if (filter === 'done') return list.filter((ride) => ride.status === 'COMPLETED' || ride.status === 'CANCELLED')
    return list
  }, [history.data, filter])

  if (history.isLoading) {
    return <div className="grid gap-3"><Skeleton className="h-28" /><Skeleton className="h-28" /></div>
  }

  return (
    <div>
      <PageTitle title="Trips" subtitle="Fares you proposed, and trips you drove." />
      <Tabs
        value={filter}
        onChange={setFilter}
        className="mb-4"
        items={[
          { id: 'all', label: 'All' },
          { id: 'active', label: 'Active' },
          { id: 'done', label: 'Past' },
        ]}
      />
      <div className="grid gap-3">
        {rows.length === 0 && <Empty title="No trips here" body="Request a ride and it will show up in this tab." />}
        {rows.map((ride, index) => (
          <button
            key={ride.id}
            onClick={() => navigate(`/rides/${ride.id}`)}
            className="animate-rise panel p-4 text-left transition hover:border-brand/25"
            style={{ animationDelay: `${index * 40}ms` }}
          >
            <div className="flex items-center justify-between gap-2">
              <Badge>{labelStatus(ride.status)}</Badge>
              <span className="font-display text-lg font-extrabold">{money(ride.fare, ride.currency)}</span>
            </div>
            <p className="mt-2.5 font-bold leading-snug">{ride.pickupAddress}</p>
            <p className="text-sm text-muted">to {ride.destinationAddress}</p>
            <p className="mt-2 text-xs text-muted">{when(ride.createdAt)} · {ride.driver.fullName}</p>
          </button>
        ))}
      </div>
    </div>
  )
}
