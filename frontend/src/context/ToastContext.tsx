import { createContext, useContext, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'

type Toast = { id: number; text: string; tone: 'ok' | 'err' }
const ToastContext = createContext<(text: string, tone?: 'ok' | 'err') => void>(() => {})

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  function push(text: string, tone: 'ok' | 'err' = 'ok') {
    const id = Date.now() + Math.random()
    setToasts((list) => [...list, { id, text, tone }])
    setTimeout(() => setToasts((list) => list.filter((item) => item.id !== id)), 3200)
  }
  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 top-3 z-[80] flex flex-col items-center gap-2 px-4">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ y: -16, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: -12, opacity: 0 }}
              className={`pointer-events-auto max-w-sm rounded-2xl px-4 py-3.5 text-sm font-semibold shadow-float ${toast.tone === 'err' ? 'bg-rose-600 text-white' : 'bg-ink text-white'}`}
            >
              {toast.text}
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
