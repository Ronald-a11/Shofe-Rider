import { Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react'

export function Button({
  children, loading, variant = 'primary', className = '', ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { loading?: boolean; variant?: 'primary' | 'ghost' | 'dark' | 'danger' }) {
  const styles = {
    primary: 'bg-brand text-white hover:bg-brand-dark shadow-soft',
    dark: 'bg-ink text-white hover:bg-black',
    ghost: 'bg-white/80 text-ink border border-line hover:bg-brand-mist',
    danger: 'bg-rose-600 text-white hover:bg-rose-700',
  }[variant]
  return (
    <button
      {...props}
      disabled={props.disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-3.5 text-sm font-bold tracking-tight transition duration-200 active:scale-[0.98] disabled:opacity-55 ${styles} ${className}`}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  )
}

export function Field({ label, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.08em] text-muted">{label}</span>
      <input
        {...props}
        className="w-full rounded-2xl border border-line bg-brand-soft/60 px-4 py-3.5 outline-none transition duration-200 placeholder:text-muted/60 focus:border-brand/40 focus:bg-white focus:ring-4 focus:ring-brand/15"
      />
    </label>
  )
}

export function Splash() {
  return (
    <div className="auth-stage relative grid min-h-dvh place-items-center overflow-hidden text-white">
      <div className="breathe pointer-events-none absolute -left-20 top-16 h-56 w-56 rounded-[40%] bg-white/10 blur-2xl" />
      <div className="breathe pointer-events-none absolute -right-16 bottom-24 h-64 w-64 rounded-[45%] bg-brand/40 blur-3xl" style={{ animationDelay: '1s' }} />
      <motion.div
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.22, 0.8, 0.22, 1] }}
        className="relative text-center"
      >
        <img src="/logo.jpg" alt="Shofe" className="car-drive mx-auto h-28 w-28 rounded-[30px] shadow-float" />
        <p className="font-display mt-7 text-4xl font-extrabold tracking-tight">Shofe</p>
        <p className="mt-2 text-sm text-white/75">Propose your fare</p>
        <div className="mx-auto mt-8 h-1 w-32 overflow-hidden rounded-full bg-white/20">
          <div className="h-full w-2/5 rounded-full bg-white animate-[shimmer_1.1s_infinite]" />
        </div>
      </motion.div>
    </div>
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`skeleton rounded-2xl ${className}`} />
}

export function Badge({ children, tone = 'green' }: { children: ReactNode; tone?: 'green' | 'amber' | 'ink' }) {
  const styles = {
    green: 'bg-brand-mist text-brand-dark',
    amber: 'bg-amber-50 text-amber-800 border border-amber-200/80',
    ink: 'bg-ink text-white',
  }[tone]
  return (
    <span className={`inline-flex items-center rounded-xl px-2.5 py-1 text-[11px] font-bold tracking-wide ${styles}`}>
      {children}
    </span>
  )
}

export function Empty({ title, body }: { title: string; body: string }) {
  return (
    <div className="rounded-[1.75rem] border border-dashed border-line bg-white/70 px-5 py-12 text-center">
      <p className="font-display text-lg font-bold">{title}</p>
      <p className="mt-1.5 text-sm text-muted">{body}</p>
    </div>
  )
}

export function PageTitle({ eyebrow, title, subtitle }: { eyebrow?: string; title: string; subtitle?: string }) {
  return (
    <header className="animate-fade-up mb-5">
      {eyebrow && <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-muted">{eyebrow}</p>}
      <h1 className="font-display mt-1 text-3xl font-extrabold tracking-tight text-ink">{title}</h1>
      {subtitle && <p className="mt-1.5 text-sm text-muted">{subtitle}</p>}
    </header>
  )
}
