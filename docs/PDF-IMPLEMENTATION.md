# Implementing the Shofe PDF on this app

Source: `docs/Shofe-Night-Owl-Pesepay-APIs.pdf` (adapted onto Spring Boot + React).

## Mapped onto this repo

| PDF item | Status |
| --- | --- |
| Negotiation ride flow | Done |
| Night Owl PIN + SOS + trusted contacts | Done — `/night-owl` + Shell SOS |
| Check-in prompts (timed) | Done — `NightOwlCheckInScheduler` every ~5 min on Night Owl trips |
| Route-deviation alerts | Done — corridor check in `SafetyMonitorService` |
| Fatigue alerts | Done — online-hours gate + `/api/drivers/fatigue` |
| Quick-help + emergency numbers | Done (ZW defaults) |
| Pesepay + PaymentProvider | Done — live/sandbox initiate + status poll + result callback |
| Stripe | Provider port only; Zimbabwe checkout is Pesepay |
| Driver commission wallet + ledger | Done |
| Admin analytics | Done — `/api/admin/analytics` + SOS feed |
| Cursor rules | `.cursor/rules/shofe.mdc` |
| Flutter apps | Deferred — React PWA |
| Live Pesepay merchant HTTP | Done — documented initiate/check-payment/result callback |
| Real SMS/push for Guardians | Placeholder logs — plug Twilio/WhatsApp later |
| Multi-country i18n | Partial — ZW-first config |

## Deploy notes

- **Vercel** hosts the React frontend only. Set `VITE_API_URL` to your public Spring Boot URL.
- Backend can run as a Vercel container (`shofe-api`) or any Java host.
- CORS already allows browser origins (`allowedOriginPatterns: *`).
