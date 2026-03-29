# TravelBilling Monorepo

TravelBilling is a billing and invoicing platform for travel agencies with a monorepo structure:

- `frontend/`: React + Vite user interface
- `backend/`: Spring Boot API and business logic
- `pdf-extractor/`: Python FastAPI microservice for PDF extraction

## Architecture

- Frontend talks to backend API.
- Backend persists data in PostgreSQL and uses Redis for caching/rate-limit support.
- Backend calls PDF extractor for OCR/vision extraction.

## Local Setup

1. Copy `.env.example` values as needed.
2. Start stack:

```bash
make dev
```

3. Service URLs:
- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Backend health: `http://localhost:8080/health`
- PDF extractor health: `http://localhost:8000/health`

## CI/CD Workflows

- `.github/workflows/frontend.yml`: Vercel build/deploy
- `.github/workflows/backend.yml`: Maven test, Docker build (GHCR), Render deploy
- `.github/workflows/pdf-extractor.yml`: Python quality checks, Docker build (GHCR), Railway deploy
- `.github/workflows/pr-checks.yml`: Security and dependency checks for PRs

## Branch Protection Hardening

- Apply required status checks and review rules using `scripts/enable-branch-protection.ps1`.
- Rollout guide: `docs/rollout-checklist.md`

## Required GitHub Secrets

- `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`
- `VITE_API_BASE_URL`, `VITE_PDF_SERVICE_URL`
- `RENDER_DEPLOY_HOOK_URL`
- `RAILWAY_TOKEN`
- `DATABASE_URL`
- `PDF_SERVICE_API_KEY`
- `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`, `R2_PUBLIC_URL`

## Troubleshooting

- Render cold starts: set UptimeRobot ping every 5 minutes to `/health`.
- If Vercel preview comment fails, ensure PR write permissions are enabled for workflow token.
- If Docker build fails in CI, confirm each service Dockerfile exists and dependencies are reachable.
