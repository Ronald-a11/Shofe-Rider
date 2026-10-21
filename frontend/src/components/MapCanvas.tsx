import { useEffect } from 'react'
import { MapContainer, Marker, Polyline, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import type { LatLng } from '../types'

const icon = (color: string, glyph: string) => L.divIcon({
  className: '',
  html: `<div style="width:38px;height:38px;border-radius:14px;background:${color};color:white;display:grid;place-items:center;font:800 13px Sora,Manrope,sans-serif;box-shadow:0 10px 22px rgba(15,31,22,.22);border:2.5px solid white;letter-spacing:-0.02em">${glyph}</div>`,
  iconSize: [38, 38],
  iconAnchor: [19, 36],
})

function Fly({ center }: { center: [number, number] }) {
  const map = useMap()
  useEffect(() => {
    map.flyTo(center, Math.max(map.getZoom(), 13), { duration: 0.65 })
  }, [center[0], center[1]])
  return null
}

function Clicks({ onClick }: { onClick?: (point: LatLng) => void }) {
  useMapEvents({
    click(event) {
      onClick?.({ lat: event.latlng.lat, lng: event.latlng.lng })
    },
  })
  return null
}

export function MapCanvas({
  center, pins, route, onClick, focus,
}: {
  center: LatLng
  pins: { point: LatLng; kind: 'pickup' | 'drop' | 'driver' }[]
  route?: LatLng[]
  onClick?: (point: LatLng) => void
  focus?: LatLng
}) {
  return (
    <MapContainer center={[center.lat, center.lng]} zoom={13} zoomControl={false} className="h-full w-full">
      <TileLayer
        attribution='&copy; OpenStreetMap'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {focus && <Fly center={[focus.lat, focus.lng]} />}
      <Clicks onClick={onClick} />
      {route && route.length > 1 && (
        <Polyline
          positions={route.map((point) => [point.lat, point.lng])}
          pathOptions={{ color: '#1C9B42', weight: 5, opacity: 0.9, lineCap: 'round', lineJoin: 'round' }}
        />
      )}
      {pins.map((pin, index) => (
        <Marker
          key={`${pin.kind}-${index}`}
          position={[pin.point.lat, pin.point.lng]}
          icon={icon(
            pin.kind === 'drop' ? '#0F1F16' : pin.kind === 'driver' ? '#0F766E' : '#1C9B42',
            pin.kind === 'drop' ? 'B' : pin.kind === 'driver' ? 'D' : 'A',
          )}
        />
      ))}
    </MapContainer>
  )
}
