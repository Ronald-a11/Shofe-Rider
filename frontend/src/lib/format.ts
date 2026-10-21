export function money(amount: number | string, currency = 'USD') {
  return new Intl.NumberFormat('en-ZW', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(Number(amount))
}

export function stars(rating: number, count: number) {
  if (!count) return 'New'
  return `${rating.toFixed(1)} · ${count}`
}

export function when(iso?: string) {
  if (!iso) return ''
  return new Intl.DateTimeFormat('en-ZW', {
    hour: '2-digit',
    minute: '2-digit',
    day: 'numeric',
    month: 'short',
    timeZone: 'Africa/Harare',
  }).format(new Date(iso))
}

export function labelStatus(status: string) {
  return status.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase())
}
