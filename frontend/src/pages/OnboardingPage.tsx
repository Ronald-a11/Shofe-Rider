import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Button, Field } from '../components/ui'

export function OnboardingPage() {
  const toast = useToast()
  const { refresh } = useAuth()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({
    licenseNumber: '', vehicleMake: '', vehicleModel: '', vehicleColor: '', plateNumber: '', vehicleYear: '2022', roadsideOptIn: true,
  })
  const [files, setFiles] = useState<{ license?: File; registration?: File; insurance?: File }>({})

  function set(key: string, value: string | boolean) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    try {
      const data = new FormData()
      data.append('profile', JSON.stringify({ ...form, vehicleYear: Number(form.vehicleYear) }))
      if (files.license) data.append('license', files.license)
      if (files.registration) data.append('registration', files.registration)
      if (files.insurance) data.append('insurance', files.insurance)
      await api('/api/drivers/profile', { method: 'POST', body: data })
      await refresh()
      toast('Documents submitted for review')
      navigate('/profile')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Upload failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="grid gap-3">
      <h1 className="font-display text-3xl font-extrabold tracking-tight">Driver documents</h1>
      <p className="text-sm text-muted">Licence, vehicle registration, and insurance. An admin approves you before you can take trips.</p>
      <Field label="Licence number" value={form.licenseNumber} onChange={(e) => set('licenseNumber', e.target.value)} required />
      <div className="grid grid-cols-2 gap-2">
        <Field label="Make" value={form.vehicleMake} onChange={(e) => set('vehicleMake', e.target.value)} required />
        <Field label="Model" value={form.vehicleModel} onChange={(e) => set('vehicleModel', e.target.value)} required />
        <Field label="Colour" value={form.vehicleColor} onChange={(e) => set('vehicleColor', e.target.value)} required />
        <Field label="Plate" value={form.plateNumber} onChange={(e) => set('plateNumber', e.target.value)} required />
      </div>
      <Field label="Year" type="number" value={form.vehicleYear} onChange={(e) => set('vehicleYear', e.target.value)} required />
      {(['license', 'registration', 'insurance'] as const).map((key) => (
        <label key={key} className="block text-sm font-bold capitalize">
          {key}
          <input type="file" accept="image/*,.pdf" className="mt-1 block w-full text-sm font-medium" onChange={(e) => setFiles((current) => ({ ...current, [key]: e.target.files?.[0] }))} />
        </label>
      ))}
      <label className="flex items-center gap-2 text-sm font-bold">
        <input type="checkbox" checked={form.roadsideOptIn} onChange={(e) => set('roadsideOptIn', e.target.checked)} />
        Also take roadside assistance jobs
      </label>
      <Button loading={busy} type="submit">Submit for verification</Button>
    </form>
  )
}
