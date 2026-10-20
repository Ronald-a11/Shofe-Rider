import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { labelStatus, money } from '../lib/format'
import { useToast } from '../context/ToastContext'
import { Badge, Button, PageTitle, Skeleton } from '../components/ui'
import type { AdminStats, Ride, User } from '../types'

export function AdminPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const stats = useQuery({ queryKey: ['admin-stats'], queryFn: () => api<AdminStats>('/api/admin/stats') })
  const users = useQuery({ queryKey: ['admin-users'], queryFn: () => api<User[]>('/api/admin/users') })
  const rides = useQuery({ queryKey: ['admin-rides'], queryFn: () => api<Ride[]>('/api/admin/rides') })

  async function verify(userId: string, status: 'APPROVED' | 'REJECTED') {
    try {
      await api(`/api/admin/drivers/${userId}/verification`, { method: 'PATCH', body: JSON.stringify({ status, reason: status === 'REJECTED' ? 'Documents need a clearer photo' : '' }) })
      toast(status === 'APPROVED' ? 'Driver approved' : 'Driver rejected')
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] })
      await queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed', 'err')
    }
  }

  const cards = stats.data ? [
    ['People', stats.data.users],
    ['Pending drivers', stats.data.driversPending],
    ['Open requests', stats.data.openRequests],
    ['Active trips', stats.data.activeRides],
    ['Completed', stats.data.completedRides],
    ['SOS alerts', stats.data.sosEvents],
  ] : []

  return (
    <div>
      <PageTitle title="Admin desk" subtitle="People, verification, and recent rides." />
      <div className="grid grid-cols-2 gap-2.5 md:grid-cols-3">
        {stats.isLoading && <Skeleton className="col-span-2 h-24" />}
        {cards.map(([label, value]) => (
          <div key={String(label)} className="animate-scale-in panel p-4">
            <p className="text-[11px] font-bold uppercase tracking-[0.08em] text-muted">{label}</p>
            <p className="font-display mt-1 text-3xl font-extrabold tracking-tight">{value}</p>
          </div>
        ))}
      </div>
      <h2 className="font-display mt-7 text-lg font-extrabold">Drivers</h2>
      <div className="mt-2 grid gap-2.5">
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
      <h2 className="font-display mt-7 text-lg font-extrabold">Recent rides</h2>
      <div className="mt-2 grid gap-2">
        {(rides.data ?? []).slice(0, 12).map((ride) => (
          <div key={ride.id} className="rounded-2xl border border-line bg-white/90 px-4 py-3 text-sm">
            <span className="font-bold">{labelStatus(ride.status)}</span> · {money(ride.fare, ride.currency)} · {ride.rider.fullName} → {ride.driver.fullName}
          </div>
        ))}
      </div>
    </div>
  )
}
