export type Role = 'RIDER' | 'DRIVER' | 'ADMIN'
export type ActiveMode = 'PASSENGER' | 'DRIVER'

export type DriverSummary = {
  id: string
  verificationStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
  vehicleMake?: string
  vehicleModel?: string
  vehicleColor?: string
  plateNumber?: string
  vehicleYear?: number
  tripCount: number
  online: boolean
  roadsideOptIn: boolean
  licenseDocUrl?: string
  registrationDocUrl?: string
  insuranceDocUrl?: string
  rejectionReason?: string
  lastLat?: number
  lastLng?: number
}

export type User = {
  id: string
  fullName: string
  email?: string | null
  phone: string
  countryIso?: string
  role: Role
  activeMode: ActiveMode
  phoneVerified: boolean
  ratingAvg: number
  ratingCount: number
  driver: DriverSummary | null
}

export function isDriving(user?: User | null) {
  if (!user) return false
  if (user.role === 'ADMIN') return true
  return user.activeMode === 'DRIVER'
}

export type PhoneCountry = { iso: string; name: string; dial: string; placeholder: string }

export type AuthResponse = { token: string; user: User }

export type LatLng = { lat: number; lng: number }

export type Quote = {
  distanceKm: number
  durationMin: number
  recommendedFare: number
  currency: string
  nightOwl: boolean
  route: LatLng[]
}

export type Offer = {
  id: string
  requestId: string
  driverId: string
  driverName: string
  driverRating: number
  driverRatingCount: number
  tripCount: number
  vehicleMake?: string
  vehicleModel?: string
  vehicleColor?: string
  plateNumber?: string
  amount: number
  type: 'ACCEPT' | 'COUNTER'
  etaMinutes: number
  status: string
  message?: string
  createdAt: string
}

export type RideRequest = {
  id: string
  status: string
  riderName: string
  pickupAddress: string
  destinationAddress: string
  pickupLat: number
  pickupLng: number
  destinationLat: number
  destinationLng: number
  distanceKm: number
  durationMin: number
  recommendedFare: number
  proposedFare: number
  currency: string
  nightOwl: boolean
  paymentMethod: string
  createdAt: string
  expiresAt: string
  rideId?: string
  offers: Offer[]
}

export type Party = {
  id: string
  fullName: string
  phone: string
  ratingAvg: number
  ratingCount: number
  vehicleMake?: string
  vehicleModel?: string
  vehicleColor?: string
  plateNumber?: string
  lat?: number
  lng?: number
}

export type Ride = {
  id: string
  requestId: string
  status: string
  fare: number
  currency: string
  nightOwl: boolean
  pinCode?: string | null
  pinVerified: boolean
  shareToken: string
  sharePath: string
  pickupAddress: string
  destinationAddress: string
  pickupLat: number
  pickupLng: number
  destinationLat: number
  destinationLng: number
  rider: Party
  driver: Party
  paymentMethod: string
  paymentStatus?: string
  cancelReason?: string
  createdAt: string
  startedAt?: string
  completedAt?: string
}

export type ChatMessage = {
  id: string
  rideId: string
  senderId: string
  senderName: string
  body: string
  sentAt: string
}

export type Contact = { id: string; name: string; phone: string }
export type Place = { label: string; lat: number; lng: number }

export type Roadside = {
  id: string
  type: string
  status: string
  address: string
  details?: string
  lat: number
  lng: number
  fare: number
  currency: string
  riderName: string
  providerName?: string
  providerLat?: number
  providerLng?: number
  createdAt: string
}

export type AdminStats = {
  users: number
  driversPending: number
  openRequests: number
  activeRides: number
  completedRides: number
  sosEvents: number
}

export type Track = {
  status: string
  driverName: string
  vehicle: string
  plateNumber?: string
  pickupAddress: string
  destinationAddress: string
  driverLat?: number
  driverLng?: number
  nightOwl: boolean
}

/** Default map centre when GPS is unavailable — Harare, Zimbabwe. */
export const DEFAULT_CITY = { lat: -17.8292, lng: 31.0522, label: 'Harare CBD' }

export const ROADSIDE = [
  { id: 'JUMP_START', label: 'Jump start', fare: 8 },
  { id: 'FLAT_TIRE', label: 'Flat tyre', fare: 10 },
  { id: 'FUEL', label: 'Fuel delivery', fare: 12 },
  { id: 'TOWING', label: 'Towing', fare: 40 },
  { id: 'LOCKOUT', label: 'Lockout', fare: 9 },
] as const
