# Shofe

Shofe is an inDrive-style ride app for **Zimbabwe**: the passenger proposes a fare (USD), nearby drivers accept or counter, and the passenger picks a driver. There is no surge algorithm.

The React app is mobile-first and installable as a PWA. The API is Spring Boot 3.5 on Java 21 bytecode, with JWT roles `RIDER`, `DRIVER`, and `ADMIN`.

## Layout

- `backend` — Spring Boot API, STOMP/WebSocket, JPA
- `frontend` — React 18, TypeScript, Vite, Tailwind, React Query, Leaflet
- `docker-compose.yml` — PostGIS PostgreSQL and the API

Screens are split so a later React Native client can reuse the same REST and STOMP contracts in `frontend/src/types.ts`.

## Features

- Register and login with JWT (no seeded demo accounts)
- Rider map centred on Harare, Zimbabwe place search, recommended fare, proposed price
- Driver online toggle and incoming requests
- Accept or counter-offer, then the rider chooses
- Ride statuses: requested, offers, accepted, arrived, in progress, completed, cancelled
- Live driver location, in-ride chat, and a public share link
- Ratings both ways, cash, Pesepay, and a Stripe card placeholder
- Night Owl (22:00–05:00 Africa/Harare): PIN before start, trusted contacts, SOS, Zimbabwe emergency numbers
- Roadside assistance request and accept flow
- Driver document upload and admin approval
- Admin desk for people, rides, and SOS events

Nearby drivers are matched with a haversine radius. On PostgreSQL the API also enables PostGIS for later `ST_DWithin` queries.

## Run it

Requirements: Docker (optional), JDK 21+, Maven, Node 20+.

```bash
docker compose up -d postgres
```

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses a file-backed H2 database under `backend/data`, so the API runs without Docker. For PostgreSQL, start `docker compose up -d postgres` and run `mvn spring-boot:run` with no profile.

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Create a rider or driver account from **Register**. The Vite server proxies `/api` and `/ws` to http://localhost:8080.

API docs: http://localhost:8080/swagger-ui.html

### Optional bootstrap admin

No users are seeded by default. To create the first admin on startup:

```bash
set SHOFE_BOOTSTRAP_ADMIN_EMAIL=admin@yourdomain.co.zw
set SHOFE_BOOTSTRAP_ADMIN_PASSWORD=choose-a-strong-password
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Configuration

| Variable | Default |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/shofe` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `shofe` / `shofe` |
| `JWT_SECRET` | dev secret in `application.yml` — replace this |
| `SHOFE_DEFAULT_COUNTRY` | `ZW` |
| `SHOFE_DEV_TOOLS` | `false` |

Fares are in **USD**: base $2 + $0.80 per km, 15% Night Owl uplift, minimum $2.50. Maps and geocoding default to Zimbabwe (`countrycodes=zw`, timezone `Africa/Harare`).
