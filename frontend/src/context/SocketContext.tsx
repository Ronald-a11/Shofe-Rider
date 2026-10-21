import { Client } from '@stomp/stompjs'
import { createContext, useContext, useEffect, useRef, type MutableRefObject } from 'react'
import SockJS from 'sockjs-client'
import { apiUrl } from '../lib/api'
import { useAuth } from './AuthContext'

type Handler = (event: { destination: string; body: unknown }) => void
const SocketContext = createContext<(handler: Handler) => () => void>(() => () => {})

export function SocketProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth()
  const handlers = useRef(new Set<Handler>())

  useEffect(() => {
    const token = localStorage.getItem('shofe_token')
    if (!user || !token) return
    const client = new Client({
      webSocketFactory: () => new SockJS(apiUrl('/ws')),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 4000,
      onConnect: () => {
        ;['/user/queue/offers', '/user/queue/requests', '/user/queue/rides', '/user/queue/roadside', '/user/queue/check-in', '/topic/requests'].forEach((destination) => {
          client.subscribe(destination, (message) => {
            const body = JSON.parse(message.body) as unknown
            handlers.current.forEach((handler) => handler({ destination, body }))
          })
        })
      },
    })
    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [user?.id, user?.activeMode])

  function subscribe(handler: Handler) {
    handlers.current.add(handler)
    return () => handlers.current.delete(handler)
  }

  return <SocketContext.Provider value={subscribe}>{children}</SocketContext.Provider>
}

export function useSocket(handler: Handler) {
  const subscribe = useContext(SocketContext)
  const ref = useRef(handler) as MutableRefObject<Handler>
  ref.current = handler
  useEffect(() => subscribe((event) => ref.current(event)), [subscribe])
}
