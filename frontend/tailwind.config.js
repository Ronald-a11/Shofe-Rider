/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#1C9B42',
          dark: '#0F7A30',
          deep: '#0A2F16',
          mist: '#E8F6ED',
          soft: '#F2FBF5',
        },
        ink: '#0F1F16',
        muted: '#5A6F62',
        line: 'rgba(15, 31, 22, 0.08)',
        surface: {
          DEFAULT: '#FFFFFF',
          dim: '#F4F8F5',
          glass: 'rgba(255, 255, 255, 0.82)',
        },
      },
      fontFamily: {
        display: ['Sora', 'Manrope', 'ui-sans-serif', 'sans-serif'],
        sans: ['Manrope', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 8px 28px rgba(15, 31, 22, 0.08)',
        float: '0 18px 48px rgba(15, 31, 22, 0.12)',
        dock: '0 -8px 32px rgba(15, 31, 22, 0.08)',
      },
      borderRadius: {
        panel: '1.5rem',
        sheet: '1.75rem',
      },
      keyframes: {
        floaty: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-6px)' },
        },
        fadeUp: {
          from: { opacity: '0', transform: 'translateY(14px)' },
          to: { opacity: '1', transform: 'none' },
        },
        scaleIn: {
          from: { opacity: '0', transform: 'scale(0.96)' },
          to: { opacity: '1', transform: 'scale(1)' },
        },
      },
      animation: {
        floaty: 'floaty 3.2s ease-in-out infinite',
        'fade-up': 'fadeUp 0.45s cubic-bezier(.22,.8,.22,1) both',
        'scale-in': 'scaleIn 0.35s cubic-bezier(.22,.8,.22,1) both',
      },
    },
  },
  plugins: [],
}
