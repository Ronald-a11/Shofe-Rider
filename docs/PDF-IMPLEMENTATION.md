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
| Pesepay + PaymentProvider | Done (demo when keys empty) + background `PaymentPollJob` |
| Stripe stub | Done |
| Driver commission wallet + ledger | Done |
| Admin analytics | Done — `/api/admin/analytics` + SOS feed |
| Cursor rules | `.cursor/rules/shofe.mdc` |
| Flutter apps | Deferred — React PWA |
| Live Pesepay merchant HTTP | Partial — wire real endpoints when keys/docs confirmed |
| Real SMS/push for Guardians | Placeholder logs — plug Twilio/WhatsApp later |
| Multi-country i18n | Partial — ZW-first config |

## Deploy notes

- **Vercel** hosts the React frontend only. Set `VITE_API_URL` to your public Spring Boot URL.
- Backend needs a host that runs Java (Render, Railway, Fly, Azure, or a tunnel for demos).
- CORS already allows browser origins (`allowedOriginPatterns: *`).
