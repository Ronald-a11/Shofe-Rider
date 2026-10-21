import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Button, Field } from '../components/ui'
import { ModeSwitch } from '../components/ModeSwitch'

export function OnboardingPage() {
  const toast = useToast()
  const { user, refresh } = useAuth()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({
    licenseNumber: '',
    vehicleMake: '',
    vehicleModel: '',
    vehicleColor: '',
    plateNumber: '',
    vehicleYear: '2022',
    roadsideOptIn: true,
  })
  const [files, setFiles] = useState<{ license?: File; registration?: File; insurance?: File }>({})

  function set(key: string, value: string | boolean) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!files.license || !files.registration || !files.insurance) {
      toast('Upload licence, registration, and insurance photos', 'err')
      return
    }
    setBusy(true)
    try {
      const data = new FormData()
      data.append('profile', JSON.stringify({ ...form, vehicleYear: Number(form.vehicleYear) }))
      data.append('license', files.license)
      data.append('registration', files.registration)
      data.append('insurance', files.insurance)
      await api('/api/drivers/profile', { method: 'POST', body: data })
      await refresh()
      toast('You are registered. Go online to see ride requests.')
      navigate('/')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Upload failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="grid gap-3">
      <ModeSwitch compact />
      <h1 className="font-display text-3xl font-extrabold tracking-tight">Register as a driver</h1>
      <p className="text-sm text-muted">
        Add your vehicle and three documents. After that you can go online and incoming rides will pop up.
      </p>
      {user?.driver?.verificationStatus === 'APPROVED' && (
        <p className="rounded-2xl bg-brand-mist px-3.5 py-2.5 text-sm font-semibold text-brand-dark">Already approved — you can update details anytime.</p>
      )}
      <Field label="Licence number" value={form.licenseNumber} onChange={(e) => set('licenseNumber', e.target.value)} required />
      <div className="grid grid-cols-2 gap-2">
        <Field label="Make" value={form.vehicleMake} onChange={(e) => set('vehicleMake', e.target.value)} placeholder="Toyota" required />
        <Field label="Model" value={form.vehicleModel} onChange={(e) => set('vehicleModel', e.target.value)} placeholder="Aqua" required />
        <Field label="Colour" value={form.vehicleColor} onChange={(e) => set('vehicleColor', e.target.value)} placeholder="White" required />
        <Field label="Plate" value={form.plateNumber} onChange={(e) => set('plateNumber', e.target.value)} placeholder="AEE 1234" required />
      </div>
      <Field label="Year" type="number" value={form.vehicleYear} onChange={(e) => set('vehicleYear', e.target.value)} required />
      {([
        ['license', 'Driver licence photo'],
        ['registration', 'Vehicle registration'],
        ['insurance', 'Insurance disc'],
      ] as const).map(([key, label]) => (
        <label key={key} className="block text-sm font-bold">
          {label}
          <input
            type="file"
            accept="image/*,.pdf"
            required
            capture="environment"
            className="mt-1 block w-full rounded-2xl bg-brand-soft px-3 py-3 text-sm font-medium"
            onChange={(e) => setFiles((current) => ({ ...current, [key]: e.target.files?.[0] }))}
          />
        </label>
      ))}
      <label className="flex items-center gap-2 text-sm font-bold">
        <input type="checkbox" checked={form.roadsideOptIn} onChange={(e) => set('roadsideOptIn', e.target.checked)} />
        Also take roadside assistance jobs
      </label>
      <Button loading={busy} type="submit">Finish registration</Button>
    </form>
  )
}
