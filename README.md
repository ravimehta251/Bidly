# BidFlare — Real-Time Auction Platform

BidFlare is a full-stack auction application with JWT authentication, scheduled auction lifecycles,
concurrency-safe bidding, and live bid updates over STOMP/WebSockets. The React client is served as a
production image behind the same Nginx gateway as the horizontally scaled Spring Boot API.

## Features

- Register and authenticate users with JWT access tokens.
- Browse auctions and filter by `LIVE`, `SCHEDULED`, or `ENDED` status.
- Create and delete auctions as the seller.
- Place validated bids while an auction is live.
- View bid history and receive live price/auction-close events without refreshing.
- Protect concurrent bids with a Redisson distributed lock and JPA optimistic locking.
- Share events between backend instances using Redis pub/sub.

## Architecture

```text
Browser
  │
  ▼
Nginx :80 ───────────────► React/Vite frontend container
  │
  ├── /api, /actuator, /swagger-ui, /ws ─► app-1 :8080
  │                                      └► app-2 :8080
  │
  ├── PostgreSQL 16 (Flyway-managed schema)
  └── Redis 7 (distributed locks and pub/sub)
```

Nginx routes the SPA separately from backend paths and supports WebSocket upgrades on `/ws`. The API
instances share PostgreSQL and Redis, so a bid received by one instance can update subscribers connected
to another instance.

## Quick start with Docker

**Prerequisites:** Docker Engine and Docker Compose v2.

```bash
docker compose up --build
```

Open the application at <http://localhost>. Useful endpoints:

- Application: <http://localhost>
- API health: <http://localhost/actuator/health>
- Swagger UI: <http://localhost/swagger-ui.html>
- Frontend health: internal `frontend:80/health`

Stop the stack with `docker compose down`. Add `-v` if you also want to remove the PostgreSQL volume.
Set a strong JWT secret before sharing a deployment:

```bash
JWT_SECRET="replace-with-a-long-random-secret" docker compose up --build
```

## Local frontend development

The frontend uses relative `/api` and `/ws` paths. Start the backend stack first, then run Vite:

```bash
cd frontend
npm install
npm run dev
```

The Vite development server is available at <http://localhost:5173> and proxies API/WebSocket requests
to the gateway at `http://localhost:80`. Production builds can be checked with `npm run build` and
`npm run preview`.

## Tests and checks

```bash
# Backend tests; Testcontainers requires a running Docker daemon
./mvnw test

# Frontend lint and production build
cd frontend
npm run lint
npm run build
```

## API overview

All API routes are prefixed with `/api`.

| Method | Path | Auth | Purpose |
|---|---|---:|---|
| POST | `/auth/register` | No | Create an account |
| POST | `/auth/login` | No | Return a JWT access token |
| GET | `/auctions` | No | List and optionally filter auctions |
| GET | `/auctions/{id}` | No | Get auction details |
| POST | `/auctions` | Yes | Create an auction |
| DELETE | `/auctions/{id}` | Yes | Delete an owned auction |
| GET | `/auctions/{id}/bids` | No | Read bid history |
| POST | `/auctions/{id}/bids` | Yes | Place a bid |

The complete request/response schema is available in Swagger UI. The STOMP SockJS endpoint is `/ws`; bid
updates are published to `/topic/auctions/{id}`.

## Tech stack

- **Backend:** Spring Boot 3.3, Java 21, Spring Web, WebSocket/STOMP, Spring Data JPA, Spring Security, Bean Validation
- **Data:** PostgreSQL 16, Redis 7, Redisson, Flyway
- **Frontend:** React 19, Vite, React Router, Axios, STOMP.js, SockJS, Lucide React
- **Testing:** JUnit 5, Spring Security Test, Testcontainers
- **Infrastructure:** Docker Compose, Nginx, multi-stage Docker builds

## Project structure

- `src/main/java` — Spring Boot API, authentication, auctions, bids, scheduling, and messaging
- `src/main/resources/db/migration` — Flyway database migrations
- `frontend/src` — React pages, components, API client, and STOMP client
- `Dockerfile` — backend image
- `frontend/Dockerfile` — production frontend image
- `docker-compose.yml` — complete local stack
- `nginx/nginx.conf` — public routing and backend load balancing

