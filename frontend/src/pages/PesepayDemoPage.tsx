import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { Button, PageTitle } from '../components/ui'

export function PesepayDemoPage() {
  const [params] = useSearchParams()
  const ref = params.get('ref') || ''
  const toast = useToast()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)

  async function confirm() {
    if (!ref) return
    setBusy(true)
    try {
      const status = await api<{ status: string; success: boolean }>(`/api/payments/demo/confirm/${ref}`, { method: 'POST' })
      toast(status.success ? 'Pesepay payment confirmed' : `Status: ${status.status}`)
      navigate('/activity')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Confirm failed', 'err')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-10">
      <PageTitle
        title="Pesepay demo checkout"
        subtitle="Keys stay on the server. This page simulates the redirect/return flow from the PDF."
      />
      <div className="panel p-5">
        <p className="text-sm text-muted">Reference</p>
        <p className="font-display mt-1 break-all text-lg font-extrabold">{ref || 'missing'}</p>
        <Button className="mt-5 w-full" loading={busy} disabled={!ref} onClick={() => void confirm()}>
          Confirm payment
        </Button>
        <Button variant="ghost" className="mt-2 w-full" onClick={() => navigate(-1)}>Cancel</Button>
      </div>
    </div>
  )
}
