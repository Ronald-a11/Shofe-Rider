# Implementing the Shofe PDF on this app

Source: `docs/Shofe-Night-Owl-Pesepay-APIs.pdf`

The PDF’s master prompt targets NestJS + Flutter. **This repository already ships Spring Boot + React**, so features from the PDF are adapted onto that stack.

## Mapped onto this repo

| PDF item | Status in Shofe-Rider |
| --- | --- |
| Negotiation ride flow | Done (request → offers → select → trip) |
| Night Owl PIN + SOS + trusted contacts | Done — APIs + `/night-owl` UI (auto window, check-ins, quick-help, emergency) |
| Pesepay + PaymentProvider abstraction | Done — ride pay + wallet top-up + `/payments/pesepay-demo` |
| Stripe stub | Done (card pay on active ride) |
| Driver commission wallet + ledger | Done — Profile wallet + go-online gate |
| Cursor rules | `.cursor/rules/shofe.mdc` |
| Flutter passenger/driver apps | Deferred — React PWA is the client for now |

## Env for Pesepay

```bash
PESEPAY_INTEGRATION_KEY=...
PESEPAY_ENCRYPTION_KEY=...
PESEPAY_RESULT_URL=https://your-api/api/payments/pesepay/result
PESEPAY_RETURN_URL=https://your-app/rides/payment-return
PESEPAY_DEMO=true   # mock initiate/poll when keys are empty
```

## Suggested next phases (from the PDF)

4. Live Pesepay keys + webhook/poll job  
5. Full Night Owl fatigue / route-deviation alerts  
6. Admin analytics  
7. Multi-country config + i18n  
