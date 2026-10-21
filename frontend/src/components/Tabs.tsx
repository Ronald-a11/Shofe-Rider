import { motion } from 'framer-motion'

type TabItem<T extends string> = { id: T; label: string }

export function Tabs<T extends string>({
  value,
  onChange,
  items,
  className = '',
}: {
  value: T
  onChange: (id: T) => void
  items: TabItem<T>[]
  className?: string
}) {
  return (
    <div
      className={`relative grid rounded-2xl bg-brand-mist/90 p-1 text-sm font-bold ${className}`}
      style={{ gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }}
      role="tablist"
    >
      {items.map((item) => {
        const active = value === item.id
        return (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(item.id)}
            className={`relative z-10 rounded-xl py-2.5 transition ${active ? 'text-ink' : 'text-muted'}`}
          >
            {active && (
              <motion.span
                layoutId="shofe-tab"
                className="absolute inset-0 -z-10 rounded-xl bg-white shadow-soft"
                transition={{ type: 'spring', stiffness: 420, damping: 32 }}
              />
            )}
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
