import { api } from './api'
import { DEFAULT_CITY, type Place } from '../types'

export const HARARE_PLACES: Place[] = [
  { label: 'Harare CBD — First Street', lat: -17.8292, lng: 31.0522 },
  { label: 'Joina City', lat: -17.8314, lng: 31.0528 },
  { label: 'Eastgate Mall', lat: -17.832, lng: 31.147 },
  { label: 'Avondale Shopping Centre', lat: -17.791, lng: 31.037 },
  { label: 'Sam Levy Village, Borrowdale', lat: -17.776, lng: 31.087 },
  { label: 'Robert Gabriel Mugabe International Airport', lat: -17.9318, lng: 31.0928 },
  { label: 'Mbare Musika', lat: -17.859, lng: 31.033 },
  { label: 'University of Zimbabwe', lat: -17.784, lng: 31.053 },
  { label: 'Westgate Shopping Centre', lat: -17.784, lng: 30.985 },
  { label: 'Newlands Shopping Centre', lat: -17.804, lng: 31.066 },
]

export function locate(): Promise<{ lat: number; lng: number }> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve(DEFAULT_CITY)
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => resolve(DEFAULT_CITY),
      { enableHighAccuracy: true, timeout: 5000 },
    )
  })
}

export async function reverseLabel(lat: number, lng: number) {
  try {
    const place = await api<Place>(`/api/geo/reverse?lat=${lat}&lng=${lng}`)
    return place.label || 'Current location'
  } catch {
    return 'Current location'
  }
}

export function filterPlaces(query: string, extras: Place[] = []) {
  const needle = query.trim().toLowerCase()
  const pool = [...HARARE_PLACES, ...extras]
  const seen = new Set<string>()
  const out: Place[] = []
  for (const place of pool) {
    if (seen.has(place.label)) continue
    if (needle && !place.label.toLowerCase().includes(needle) && extras.indexOf(place) < 0) continue
    seen.add(place.label)
    out.push(place)
  }
  return out.slice(0, 8)
}
