# BidFlare frontend

This directory contains the React/Vite single-page application for BidFlare. It provides authentication,
auction discovery, auction creation, bid history, bidding, and live auction updates over SockJS/STOMP.

## Development

```bash
npm install
npm run dev
```

Vite runs on `http://localhost:5173` and proxies `/api` and `/ws` to the local gateway at
`http://localhost:80`. Start the Docker Compose stack before using the app.

## Commands

```bash
npm run dev      # Start the Vite development server
npm run lint     # Run Oxlint
npm run build    # Create a production build in dist/
npm run preview  # Preview the production build locally
```

## Production container

`Dockerfile` uses a two-stage build: Node compiles the app and Nginx serves the static files.
`nginx.conf` provides an `index.html` fallback so React Router routes work on refresh. The root Compose
stack places this container behind the public gateway at `http://localhost`.

The frontend uses relative `/api` and `/ws` URLs so the same build works with both the Vite development
proxy and the production gateway.

## Main routes

- `/` — auction listing and status filters
- `/login` and `/register` — authentication
- `/auctions/create` — create an auction for an authenticated user
- `/auctions/:id` — auction detail, bid history, bidding, countdown, and live updates






- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)



The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).



If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and Oxlint's TypeScript related rules in your project.

