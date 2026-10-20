# Shofe

Shofe is an inDrive-style ride app: the passenger proposes a fare, nearby drivers accept or counter, and the passenger picks a driver. There is no surge algorithm.

The React app is mobile-first and installable as a PWA. The API is Spring Boot 3.5 on Java 21 bytecode, with JWT roles `RIDER`, `DRIVER`, and `ADMIN`.

## Layout

- `backend` — Spring Boot API, STOMP/WebSocket, JPA
- `frontend` — React 18, TypeScript, Vite, Tailwind, React Query, Leaflet
- `docker-compose.yml` — PostGIS PostgreSQL and the API

Screens are split so a later React Native client can reuse the same REST and STOMP contracts in `frontend/src/types.ts`.

## Features in this MVP

- Register and login with JWT
- Rider map, search, recommended fare, and a proposed price
- Driver online toggle and incoming requests
- Accept or counter-offer, then the rider chooses
- Ride statuses: requested, offers, accepted, arrived, in progress, completed, cancelled
- Live driver location, in-ride chat, and a public share link
- Ratings both ways, cash, and a Stripe card placeholder
- Night Owl toggle (22:00–05:00 Africa/Johannesburg also turns it on): PIN before start, trusted-contact share placeholder, SOS
- Roadside assistance request and accept flow
- Driver document upload and admin approval
- Admin counts for people, rides, and SOS events
- Tabs, skeleton loaders, and motion on offers, sheets, and the splash screen

Nearby drivers are matched with a haversine radius. On startup the API also enables PostGIS and keeps a `geography` point plus a GIST index on `driver_profiles.geog` for a later `ST_DWithin` query.

With dev tools on, two seeded drivers near Johannesburg automatically bid so the offer screen is easy to demo.

## Run it

Requirements: Docker, JDK 21+, Maven, Node 20+.

```bash
docker compose up -d postgres
```

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses a file-backed H2 database under `backend/data`, so login works without Docker. For PostgreSQL instead, start `docker compose up -d postgres` and run `mvn spring-boot:run` with no profile.

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite server proxies `/api` and `/ws` to http://localhost:8080.

API docs: http://localhost:8080/swagger-ui.html

To run the API in Docker as well: `docker compose up --build`.

### Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Rider | rider@shofe.app | Rider123! |
| Driver | driver@shofe.app | Driver123! |
| Admin | admin@shofe.app | Admin123! |

On the rider map, tap **Use Johannesburg demo area**, then **Find drivers**. Amara and Johan (seeded, online) send an accept and a counter a few seconds later. Log in as the driver in a second browser, tap **Demo city**, and you can bid too.

## Configuration

| Variable | Default |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/shofe` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `shofe` / `shofe` |
| `JWT_SECRET` | dev secret in `application.yml` — replace this |
| `SHOFE_DEV_TOOLS` | `true` (demo auto-offers and driver teleport) |

Fares are in ZAR: base 20 + 9 per km, 15% Night Owl uplift, minimum 25.

Push notifications and card charges log a Firebase / Stripe placeholder. Swap `NotificationService` and `POST /api/rides/{id}/pay` when those keys exist.
