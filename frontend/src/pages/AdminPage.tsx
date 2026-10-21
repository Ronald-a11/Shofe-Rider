import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { labelStatus, money, when } from '../lib/format'
import { useToast } from '../context/ToastContext'
import { Badge, Button, PageTitle, Skeleton } from '../components/ui'
import { Tabs } from '../components/Tabs'
import { useState } from 'react'
import type { AdminStats, Ride, User } from '../types'

type Analytics = {
  completedRevenue: number
  estimatedCommission: number
  completedRides: number
  cancelledRides: number
  nightOwlRides: number
  sosEvents: number
  last7Days: { day: string; rides: number; revenue: number }[]
}

type SosRow = {
  id: string
  userName: string
  userPhone: string
  rideId?: string
  lat: number
  lng: number
  note?: string
  createdAt: string
}

export function AdminPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [tab, setTab] = useState<'overview' | 'sos' | 'drivers'>('overview')
  const stats = useQuery({ queryKey: ['admin-stats'], queryFn: () => api<AdminStats>('/api/admin/stats') })
  const analytics = useQuery({ queryKey: ['admin-analytics'], queryFn: () => api<Analytics>('/api/admin/analytics') })
  const sos = useQuery({ queryKey: ['admin-sos'], queryFn: () => api<SosRow[]>('/api/admin/sos') })
  const users = useQuery({ queryKey: ['admin-users'], queryFn: () => api<User[]>('/api/admin/users') })
  const rides = useQuery({ queryKey: ['admin-rides'], queryFn: () => api<Ride[]>('/api/admin/rides') })

  async function verify(userId: string, status: 'APPROVED' | 'REJECTED') {
    try {
      await api(`/api/admin/drivers/${userId}/verification`, {
        method: 'PATCH',
        body: JSON.stringify({ status, reason: status === 'REJECTED' ? 'Documents need a clearer photo' : '' }),
      })
      toast(status === 'APPROVED' ? 'Driver approved' : 'Driver rejected')
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] })
      await queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed', 'err')
    }
  }

  const cards = stats.data
    ? [
        ['People', stats.data.users],
        ['Pending drivers', stats.data.driversPending],
        ['Open requests', stats.data.openRequests],
        ['Active trips', stats.data.activeRides],
        ['Completed', stats.data.completedRides],
        ['SOS alerts', stats.data.sosEvents],
      ]
    : []

  return (
    <div>
      <PageTitle title="Admin desk" subtitle="Zimbabwe analytics, verification, and SOS feed." />
      <Tabs
        value={tab}
        onChange={setTab}
        items={[
          { id: 'overview', label: 'Overview' },
          { id: 'sos', label: 'SOS' },
          { id: 'drivers', label: 'Drivers' },
        ]}
      />

      {tab === 'overview' && (
        <>
          <div className="mt-4 grid grid-cols-2 gap-2.5 md:grid-cols-3">
            {stats.isLoading && <Skeleton className="col-span-2 h-24" />}
            {cards.map(([label, value]) => (
              <div key={String(label)} className="animate-scale-in panel p-4">
                <p className="text-[11px] font-bold uppercase tracking-[0.08em] text-muted">{label}</p>
                <p className="font-display mt-1 text-3xl font-extrabold tracking-tight">{value}</p>
              </div>
            ))}
          </div>

          {analytics.data && (
            <div className="panel mt-4 p-4">
              <p className="font-display text-lg font-extrabold">Revenue &amp; commission</p>
              <div className="mt-3 grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
                <div>
                  <p className="text-muted">Completed fares</p>
                  <p className="font-display text-xl font-extrabold">{money(analytics.data.completedRevenue)}</p>
                </div>
                <div>
                  <p className="text-muted">Est. commission</p>
                  <p className="font-display text-xl font-extrabold">{money(analytics.data.estimatedCommission)}</p>
                </div>
                <div>
                  <p className="text-muted">Night Owl trips</p>
                  <p className="font-display text-xl font-extrabold">{analytics.data.nightOwlRides}</p>
                </div>
                <div>
                  <p className="text-muted">Cancelled</p>
                  <p className="font-display text-xl font-extrabold">{analytics.data.cancelledRides}</p>
                </div>
              </div>
              <p className="mt-4 text-[11px] font-bold uppercase tracking-[0.08em] text-muted">Last 7 days</p>
              <div className="mt-2 grid gap-1.5">
                {analytics.data.last7Days.map((day) => (
                  <div key={day.day} className="flex items-center justify-between rounded-xl bg-brand-soft px-3 py-2 text-sm">
                    <span className="font-semibold">{day.day}</span>
                    <span className="text-muted">{day.rides} trips · {money(day.revenue)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <h2 className="font-display mt-7 text-lg font-extrabold">Recent rides</h2>
          <div className="mt-2 grid gap-2">
            {(rides.data ?? []).slice(0, 12).map((ride) => (
              <div key={ride.id} className="rounded-2xl border border-line bg-white/90 px-4 py-3 text-sm">
                <span className="font-bold">{labelStatus(ride.status)}</span> · {money(ride.fare, ride.currency)} · {ride.rider.fullName} → {ride.driver.fullName}
                {ride.nightOwl && (
                  <span className="ml-2 inline-block">
                    <Badge tone="ink">Night Owl</Badge>
                  </span>
                )}
              </div>
            ))}
          </div>
        </>
      )}

      {tab === 'sos' && (
        <div className="mt-4 grid gap-2.5">
          {sos.isLoading && <Skeleton className="h-24" />}
          {(sos.data ?? []).length === 0 && <p className="text-sm text-muted">No SOS events yet.</p>}
          {(sos.data ?? []).map((event) => (
            <article key={event.id} className="panel p-4">
              <div className="flex items-center justify-between gap-2">
                <Badge tone="amber">SOS</Badge>
                <span className="text-xs text-muted">{when(event.createdAt)}</span>
              </div>
              <p className="font-display mt-2 font-extrabold">{event.userName}</p>
              <p className="text-sm text-muted">{event.userPhone}</p>
              <p className="mt-1 text-sm">{event.note || 'No note'}</p>
              <p className="mt-1 text-xs text-muted">{event.lat.toFixed(4)}, {event.lng.toFixed(4)}</p>
            </article>
          ))}
        </div>
      )}

      {tab === 'drivers' && (
        <div className="mt-4 grid gap-2.5">
          {(users.data ?? []).filter((user) => user.driver).map((user) => (
            <article key={user.id} className="panel p-4">
              <div className="flex items-center justify-between gap-2">
                <div>
                  <p className="font-display font-extrabold">{user.fullName}</p>
                  <p className="text-sm text-muted">{user.driver?.vehicleMake} {user.driver?.vehicleModel} · {user.driver?.plateNumber}</p>
                </div>
                <Badge tone={user.driver?.verificationStatus === 'APPROVED' ? 'green' : 'amber'}>{user.driver?.verificationStatus}</Badge>
              </div>
              <div className="mt-2 flex flex-wrap gap-2 text-xs font-bold text-brand">
                {user.driver?.licenseDocUrl && <a href={user.driver.licenseDocUrl} target="_blank" rel="noreferrer">Licence</a>}
                {user.driver?.registrationDocUrl && <a href={user.driver.registrationDocUrl} target="_blank" rel="noreferrer">Registration</a>}
                {user.driver?.insuranceDocUrl && <a href={user.driver.insuranceDocUrl} target="_blank" rel="noreferrer">Insurance</a>}
              </div>
              {user.driver?.verificationStatus === 'PENDING' && (
                <div className="mt-3 grid grid-cols-2 gap-2">
                  <Button onClick={() => void verify(user.id, 'APPROVED')}>Approve</Button>
                  <Button variant="ghost" onClick={() => void verify(user.id, 'REJECTED')}>Reject</Button>
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
