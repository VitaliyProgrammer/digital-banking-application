# Taborovets Universal Bank — web client

React + TypeScript + Vite. Served by the bank itself under `/app`, so the client and the API share one origin.

## Run locally

```bash
# 1. The bank (from the repository root)
./mvnw spring-boot:run

# 2. The client, with hot reload: http://localhost:5173/app/
cd frontend
npm install
npm run dev
```

The dev server forwards every API call to `http://localhost:8080`.

## Scripts

| Command                | What it does                                                       |
| ---------------------- | ------------------------------------------------------------------ |
| `npm run dev`          | Dev server with hot reload                                         |
| `npm run build`        | Type check + production build into `src/main/resources/static/app` |
| `npm test`             | Unit and component tests (Vitest + Testing Library)                |
| `npm run lint`         | ESLint                                                             |
| `npm run typecheck`    | TypeScript without emitting files                                  |
| `npm run format:check` | Prettier check (CI runs it)                                        |

## Layout

```
src/
├── app/            the application shell
├── shared/
│   ├── lib/        small helpers
│   ├── styles/     design tokens and global styles
│   └── ui/         reusable components (Logo, …)
└── test/           test setup
```

All colours, sizes, radii and shadows come from `shared/styles/tokens.css`, which follows the approved mockup.
Text that must sit exactly on a line uses the global `.text-trim` class.
