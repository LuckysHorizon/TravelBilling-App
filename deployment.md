# TravelBilling CI/CD — Master Agent Prompt
> Copy this entire prompt into your AI coding agent (Claude Code, Cursor, Windsurf, etc.)
> Execute sequentially. Each phase builds on the previous one.

---

## 🧠 AGENT CONTEXT & IDENTITY

You are a **Senior DevOps Engineer + Full-Stack Architect** with deep expertise in:
- GitHub Actions CI/CD pipelines with path-based monorepo triggers
- Docker multi-stage builds optimized for minimal image size
- Free-tier cloud deployments: Vercel, Render, Railway
- Python FastAPI microservices and containerization
- Secret management, environment promotion, and zero-downtime deployments

You are setting up a **production-grade, fully automated CI/CD system** for a **TravelBilling SaaS application** — a billing and invoicing platform for travel agencies. The system has three independently deployable services hosted on three separate free cloud platforms. Every decision must optimize for:
1. **Zero cost** (free tier constraints respected at every step)
2. **Clean separation of concerns** (each service deploys independently)
3. **Developer experience** (one `git push` triggers the right pipeline automatically)
4. **Reliability** (proper health checks, rollback triggers, uptime monitoring)

---

## 📐 SYSTEM ARCHITECTURE OVERVIEW

```
travelbilling/                         ← GitHub Monorepo (root)
├── frontend/                          ← React/Vite or Next.js billing UI
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── vite.config.ts (or next.config.js)
│   └── Dockerfile                     ← only needed for local dev parity
│
├── backend/                           ← REST API (Node/Express OR Django OR Spring Boot)
│   ├── src/  (or app/)
│   ├── package.json (or requirements.txt / pom.xml)
│   ├── Dockerfile                     ← used by Render via GHCR
│   └── .env.example
│
├── pdf-service/                       ← Python FastAPI microservice
│   ├── app/
│   │   ├── main.py
│   │   ├── routers/
│   │   │   └── extract.py
│   │   └── services/
│   │       └── pdf_extractor.py
│   ├── tests/
│   ├── requirements.txt
│   ├── Dockerfile
│   └── railway.toml
│
├── .github/
│   └── workflows/
│       ├── frontend.yml               ← Vercel deploy pipeline
│       ├── backend.yml                ← Render deploy pipeline
│       ├── pdf-service.yml            ← Railway deploy pipeline
│       └── pr-checks.yml             ← Runs on all PRs regardless of path
│
├── docker-compose.yml                 ← Full local dev stack
├── docker-compose.override.yml        ← Local overrides (hot reload, dev DB)
├── Makefile                           ← Developer shortcuts
├── .env.example                       ← Master env template
└── README.md                          ← Setup guide
```

**Hosting map:**
| Service | Platform | URL Pattern |
|---|---|---|
| Frontend | Vercel | `https://travelbilling.vercel.app` |
| Backend API | Render | `https://travelbilling-api.onrender.com` |
| PDF Microservice | Railway | `https://pdf-service.up.railway.app` |
| Database | Supabase (or Neon) | PostgreSQL connection string via secret |
| File Storage | Cloudflare R2 | S3-compatible, 10GB free |
| Container Registry | GitHub Container Registry (GHCR) | `ghcr.io/[org]/travelbilling/[service]` |

---

## 🏗️ PHASE 1 — MONOREPO SCAFFOLD

**Task:** Create the complete monorepo directory structure with all scaffolding files.

### 1.1 — Initialize Git & Root Config

```bash
git init travelbilling
cd travelbilling
```

Create `.gitignore` at root:
```
# Node
node_modules/
.next/
dist/
build/
.env
.env.local
.env.*.local

# Python
__pycache__/
*.pyc
*.pyo
.pytest_cache/
.venv/
venv/
*.egg-info/
.mypy_cache/

# Docker
.dockerignore

# OS
.DS_Store
Thumbs.db

# IDE
.vscode/settings.json
.idea/

# Secrets (never commit these)
*.pem
*.key
secrets/
```

Create `README.md` with:
- Project overview
- Architecture diagram (text-based)
- Local setup instructions
- Environment variables reference
- Deployment instructions for each platform
- Troubleshooting section (especially Render cold starts)

### 1.2 — Root `Makefile`

Create a `Makefile` with these targets:
```makefile
.PHONY: dev dev-backend dev-frontend dev-pdf test lint build clean logs

dev:
	docker-compose up --build

dev-backend:
	cd backend && npm run dev   # or: cd backend && uvicorn app.main:app --reload

dev-frontend:
	cd frontend && npm run dev

dev-pdf:
	cd pdf-service && uvicorn app.main:app --reload --port 8001

test:
	cd frontend && npm test
	cd backend && npm test       # or pytest
	cd pdf-service && pytest

lint:
	cd frontend && npm run lint
	cd backend && npm run lint
	cd pdf-service && flake8 app/ && mypy app/

build:
	docker-compose build

clean:
	docker-compose down -v --remove-orphans

logs:
	docker-compose logs -f

# Deploy shortcuts (for manual override)
deploy-frontend:
	cd frontend && vercel --prod

deploy-backend:
	curl -X POST $$RENDER_DEPLOY_HOOK_URL

deploy-pdf:
	railway up --service pdf-service
```

### 1.3 — Master `.env.example`

Create `.env.example` at root (never commit actual `.env`):
```bash
# ============================================================
# TravelBilling — Environment Variables Reference
# Copy this to .env and fill in your values
# ============================================================

# --- Database ---
DATABASE_URL=postgresql://user:password@host:5432/travelbilling
DATABASE_POOL_SIZE=10

# --- Backend ---
NODE_ENV=development
PORT=8000
JWT_SECRET=your-256-bit-secret-here
JWT_EXPIRY=7d
CORS_ORIGIN=http://localhost:3000

# --- PDF Service ---
PDF_SERVICE_URL=http://localhost:8001
PDF_SERVICE_API_KEY=your-internal-api-key

# --- Cloudflare R2 Storage ---
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET_NAME=travelbilling-pdfs
R2_PUBLIC_URL=https://pub-xxxxx.r2.dev

# --- Frontend ---
VITE_API_BASE_URL=http://localhost:8000
VITE_PDF_SERVICE_URL=http://localhost:8001

# --- Monitoring ---
UPTIME_ROBOT_API_KEY=

# --- Email (optional) ---
SMTP_HOST=
SMTP_PORT=587
SMTP_USER=
SMTP_PASS=
```

---

## 🐳 PHASE 2 — DOCKERFILES

Write production-ready, multi-stage Dockerfiles for each service. Optimize for smallest final image size.

### 2.1 — Frontend `Dockerfile`

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --frozen-lockfile
COPY . .
RUN npm run build

# Stage 2: Serve (used only for local parity; Vercel handles prod)
FROM nginx:alpine AS runner
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Create `frontend/nginx.conf`:
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback — all routes go to index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Health check endpoint
    location /health {
        return 200 'OK';
        add_header Content-Type text/plain;
    }
}
```

Create `frontend/.dockerignore`:
```
node_modules
.next
dist
.env
.env.local
*.md
.git
```

### 2.2 — Backend `Dockerfile`

```dockerfile
# Stage 1: Dependencies
FROM node:20-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --frozen-lockfile --only=production

# Stage 2: Builder
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --frozen-lockfile
COPY . .
RUN npm run build   # or tsc if TypeScript

# Stage 3: Runner
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

# Non-root user for security
RUN addgroup --system --gid 1001 nodejs \
 && adduser --system --uid 1001 appuser

COPY --from=deps --chown=appuser:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=appuser:nodejs /app/dist ./dist
COPY --chown=appuser:nodejs package.json ./

USER appuser
EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:8000/health || exit 1

CMD ["node", "dist/server.js"]
```

**IMPORTANT:** Add a `/health` endpoint to your backend that returns:
```json
{ "status": "ok", "service": "travelbilling-backend", "timestamp": "ISO8601" }
```

Create `backend/.dockerignore`:
```
node_modules
dist
.env
*.test.ts
*.spec.ts
coverage/
.git
```

### 2.3 — PDF Service `Dockerfile`

```dockerfile
# Stage 1: Dependencies
FROM python:3.11-slim AS deps
WORKDIR /app
RUN apt-get update && apt-get install -y \
    poppler-utils \
    libpoppler-cpp-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip \
 && pip install --no-cache-dir -r requirements.txt

# Stage 2: Runner
FROM python:3.11-slim AS runner
WORKDIR /app

# Runtime deps only
RUN apt-get update && apt-get install -y \
    poppler-utils \
    && rm -rf /var/lib/apt/lists/*

# Non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=deps /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=deps /usr/local/bin /usr/local/bin
COPY --chown=appuser:appgroup . .

USER appuser
EXPOSE 8001

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/health')" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "2"]
```

Create `pdf-service/requirements.txt`:
```
fastapi==0.111.0
uvicorn[standard]==0.29.0
pdfplumber==0.11.0
pypdf2==3.0.1
python-multipart==0.0.9
boto3==1.34.0          # for R2 storage
httpx==0.27.0
pydantic==2.7.0
pydantic-settings==2.2.1
pytest==8.2.0
pytest-asyncio==0.23.6
httpx==0.27.0
flake8==7.0.0
mypy==1.10.0
```

Create `pdf-service/app/main.py`:
```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.routers import extract
import time

@asynccontextmanager
async def lifespan(app: FastAPI):
    print("PDF Service starting up...")
    yield
    print("PDF Service shutting down...")

app = FastAPI(
    title="TravelBilling PDF Extractor",
    description="Microservice for extracting data from travel invoices and receipts",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Tighten in production with specific origins
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)

app.include_router(extract.router, prefix="/api/v1", tags=["extraction"])

@app.get("/health")
async def health_check():
    return {
        "status": "ok",
        "service": "pdf-extractor",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    }
```

Create `pdf-service/railway.toml`:
```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile"

[deploy]
startCommand = "uvicorn app.main:app --host 0.0.0.0 --port $PORT --workers 2"
healthcheckPath = "/health"
healthcheckTimeout = 30
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 3
```

---

## 🐙 PHASE 3 — GITHUB ACTIONS WORKFLOWS

Create all workflow files under `.github/workflows/`. These are the heart of the CI/CD system.

### 3.1 — Frontend Pipeline `.github/workflows/frontend.yml`

```yaml
name: Frontend — Build & Deploy to Vercel

on:
  push:
    branches: [main]
    paths:
      - 'frontend/**'
      - '.github/workflows/frontend.yml'
  pull_request:
    branches: [main]
    paths:
      - 'frontend/**'

env:
  NODE_VERSION: '20'
  WORKING_DIR: frontend

jobs:
  # ─── Job 1: Quality Gate ────────────────────────────────────────
  quality:
    name: Lint & Test
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ${{ env.WORKING_DIR }}

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci --frozen-lockfile

      - name: Run ESLint
        run: npm run lint

      - name: Run TypeScript check
        run: npm run type-check
        continue-on-error: false

      - name: Run unit tests
        run: npm run test -- --coverage --reporter=verbose

      - name: Upload coverage report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: frontend-coverage
          path: frontend/coverage/
          retention-days: 7

  # ─── Job 2: Build Check ─────────────────────────────────────────
  build:
    name: Build Check
    runs-on: ubuntu-latest
    needs: quality
    defaults:
      run:
        working-directory: ${{ env.WORKING_DIR }}

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - run: npm ci --frozen-lockfile

      - name: Build production bundle
        run: npm run build
        env:
          VITE_API_BASE_URL: ${{ secrets.VITE_API_BASE_URL }}
          VITE_PDF_SERVICE_URL: ${{ secrets.VITE_PDF_SERVICE_URL }}

      - name: Check bundle size
        run: |
          BUILD_SIZE=$(du -sh dist/ | cut -f1)
          echo "📦 Build size: $BUILD_SIZE"
          # Fail if build exceeds 10MB
          BUILD_BYTES=$(du -sb dist/ | cut -f1)
          if [ "$BUILD_BYTES" -gt "10485760" ]; then
            echo "❌ Build exceeds 10MB limit!"
            exit 1
          fi

      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: frontend-build
          path: frontend/dist/
          retention-days: 1

  # ─── Job 3: Deploy to Vercel ────────────────────────────────────
  deploy:
    name: Deploy to Vercel
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    environment:
      name: production
      url: https://travelbilling.vercel.app

    steps:
      - uses: actions/checkout@v4

      - name: Deploy to Vercel (Production)
        uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: ./frontend
          vercel-args: '--prod'

      - name: Notify on success
        if: success()
        run: |
          echo "✅ Frontend deployed successfully to Vercel!"
          echo "🌐 URL: https://travelbilling.vercel.app"

      - name: Notify on failure
        if: failure()
        run: echo "❌ Frontend deployment failed. Check logs above."

  # ─── Job 4: PR Preview Deploy ───────────────────────────────────
  preview:
    name: Preview Deploy (PR)
    runs-on: ubuntu-latest
    needs: build
    if: github.event_name == 'pull_request'

    steps:
      - uses: actions/checkout@v4

      - name: Deploy Preview to Vercel
        uses: amondnet/vercel-action@v25
        id: vercel-preview
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: ./frontend

      - name: Comment preview URL on PR
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '🚀 **Frontend Preview Deployed!**\n\n🌐 Preview URL: ${{ steps.vercel-preview.outputs.preview-url }}\n\n_This preview will be promoted to production when the PR is merged._'
            })
```

### 3.2 — Backend Pipeline `.github/workflows/backend.yml`

```yaml
name: Backend — Build, Test & Deploy to Render

on:
  push:
    branches: [main]
    paths:
      - 'backend/**'
      - '.github/workflows/backend.yml'
  pull_request:
    branches: [main]
    paths:
      - 'backend/**'

env:
  NODE_VERSION: '20'
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/backend

jobs:
  # ─── Job 1: Lint & Test ─────────────────────────────────────────
  quality:
    name: Lint & Test
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
          POSTGRES_DB: travelbilling_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: backend/package-lock.json

      - name: Install dependencies
        working-directory: backend
        run: npm ci --frozen-lockfile

      - name: Run linter
        working-directory: backend
        run: npm run lint

      - name: Run type check
        working-directory: backend
        run: npm run type-check

      - name: Run tests with coverage
        working-directory: backend
        run: npm run test:coverage
        env:
          DATABASE_URL: postgresql://test:test@localhost:5432/travelbilling_test
          JWT_SECRET: test-secret-for-ci-only
          NODE_ENV: test

      - name: Upload coverage
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: backend-coverage
          path: backend/coverage/
          retention-days: 7

  # ─── Job 2: Docker Build & Push ─────────────────────────────────
  docker:
    name: Build & Push Docker Image
    runs-on: ubuntu-latest
    needs: quality
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    permissions:
      contents: read
      packages: write

    outputs:
      image-tag: ${{ steps.meta.outputs.tags }}
      image-digest: ${{ steps.push.outputs.digest }}

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=sha-
            type=raw,value=latest,enable={{is_default_branch}}
            type=raw,value={{date 'YYYY.MM.DD'}}-{{sha}}

      - name: Build and push Docker image
        id: push
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
          platforms: linux/amd64

      - name: Image built summary
        run: |
          echo "### 🐳 Docker Image Built" >> $GITHUB_STEP_SUMMARY
          echo "**Image:** \`${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}\`" >> $GITHUB_STEP_SUMMARY
          echo "**Digest:** \`${{ steps.push.outputs.digest }}\`" >> $GITHUB_STEP_SUMMARY

  # ─── Job 3: Deploy to Render ────────────────────────────────────
  deploy:
    name: Deploy to Render
    runs-on: ubuntu-latest
    needs: docker
    environment:
      name: production
      url: https://travelbilling-api.onrender.com

    steps:
      - name: Trigger Render Deploy Hook
        run: |
          RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST "${{ secrets.RENDER_DEPLOY_HOOK_URL }}")
          if [ "$RESPONSE" -ne 200 ] && [ "$RESPONSE" -ne 201 ]; then
            echo "❌ Render deploy hook failed with status: $RESPONSE"
            exit 1
          fi
          echo "✅ Render deploy triggered (HTTP $RESPONSE)"

      - name: Wait for Render health check
        run: |
          echo "⏳ Waiting 60s for Render to deploy..."
          sleep 60
          MAX_RETRIES=10
          RETRY=0
          until curl -sf https://travelbilling-api.onrender.com/health; do
            RETRY=$((RETRY+1))
            if [ "$RETRY" -ge "$MAX_RETRIES" ]; then
              echo "❌ Backend health check failed after $MAX_RETRIES attempts"
              exit 1
            fi
            echo "⏳ Attempt $RETRY/$MAX_RETRIES — waiting 15s..."
            sleep 15
          done
          echo "✅ Backend is healthy!"
```

### 3.3 — PDF Service Pipeline `.github/workflows/pdf-service.yml`

```yaml
name: PDF Service — Build, Test & Deploy to Railway

on:
  push:
    branches: [main]
    paths:
      - 'pdf-service/**'
      - '.github/workflows/pdf-service.yml'
  pull_request:
    branches: [main]
    paths:
      - 'pdf-service/**'

env:
  PYTHON_VERSION: '3.11'
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/pdf-service

jobs:
  # ─── Job 1: Quality Gate ────────────────────────────────────────
  quality:
    name: Lint, Type-check & Test
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}
          cache: 'pip'
          cache-dependency-path: pdf-service/requirements.txt

      - name: Install dependencies
        working-directory: pdf-service
        run: pip install -r requirements.txt

      - name: Run flake8 linter
        working-directory: pdf-service
        run: flake8 app/ tests/ --max-line-length=100 --ignore=E501,W503

      - name: Run mypy type checker
        working-directory: pdf-service
        run: mypy app/ --ignore-missing-imports --strict

      - name: Run pytest with coverage
        working-directory: pdf-service
        run: pytest tests/ -v --cov=app --cov-report=xml --cov-report=term-missing
        env:
          ENVIRONMENT: test

      - name: Upload coverage
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: pdf-service-coverage
          path: pdf-service/coverage.xml
          retention-days: 7

  # ─── Job 2: Docker Build & Push ─────────────────────────────────
  docker:
    name: Build & Push Docker Image
    runs-on: ubuntu-latest
    needs: quality
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - uses: docker/setup-buildx-action@v3

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=sha-
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./pdf-service
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
          platforms: linux/amd64

  # ─── Job 3: Deploy to Railway ───────────────────────────────────
  deploy:
    name: Deploy to Railway
    runs-on: ubuntu-latest
    needs: docker
    environment:
      name: production
      url: https://pdf-service.up.railway.app

    steps:
      - uses: actions/checkout@v4

      - name: Install Railway CLI
        run: npm install -g @railway/cli@latest

      - name: Deploy to Railway
        working-directory: pdf-service
        run: railway up --service pdf-service --detach
        env:
          RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}

      - name: Wait & verify health
        run: |
          echo "⏳ Waiting 45s for Railway to deploy..."
          sleep 45
          MAX_RETRIES=8
          RETRY=0
          until curl -sf https://pdf-service.up.railway.app/health; do
            RETRY=$((RETRY+1))
            if [ "$RETRY" -ge "$MAX_RETRIES" ]; then
              echo "❌ PDF service health check failed"
              exit 1
            fi
            echo "⏳ Attempt $RETRY/$MAX_RETRIES..."
            sleep 10
          done
          echo "✅ PDF Service is healthy!"
```

### 3.4 — PR Checks (All Services) `.github/workflows/pr-checks.yml`

```yaml
name: PR Checks — Security & Quality Gate

on:
  pull_request:
    branches: [main]

jobs:
  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'table'
          exit-code: '0'       # Set to '1' to fail on HIGH/CRITICAL vulns
          severity: 'CRITICAL,HIGH'

  secrets-scan:
    name: Detect Leaked Secrets
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0      # Full history for gitleaks

      - name: Run Gitleaks
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  dependency-review:
    name: Dependency Review
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/dependency-review-action@v4
        with:
          fail-on-severity: high
```

---

## 🔐 PHASE 4 — SECRETS CONFIGURATION

### 4.1 — GitHub Repository Secrets

Go to: `GitHub Repo → Settings → Secrets and variables → Actions → New repository secret`

Add every secret from this list — do not skip any:

```
# Vercel
VERCEL_TOKEN              ← vercel.com → Account Settings → Tokens → Create
VERCEL_ORG_ID             ← vercel.com → Team Settings → General → Team ID
VERCEL_PROJECT_ID         ← vercel.com → Project → Settings → General → Project ID

# Frontend env vars (passed at build time)
VITE_API_BASE_URL         ← https://travelbilling-api.onrender.com
VITE_PDF_SERVICE_URL      ← https://pdf-service.up.railway.app

# Render
RENDER_DEPLOY_HOOK_URL    ← render.com → Service → Settings → Deploy Hooks → Create

# Railway
RAILWAY_TOKEN             ← railway.app → Account → Tokens → New Token

# Database
DATABASE_URL              ← Supabase: Project → Settings → Database → Connection string

# Internal service auth
PDF_SERVICE_API_KEY       ← generate: openssl rand -hex 32

# Cloudflare R2
R2_ACCOUNT_ID             ← cloudflare.com → R2 → Overview
R2_ACCESS_KEY_ID          ← cloudflare.com → R2 → Manage R2 API tokens
R2_SECRET_ACCESS_KEY      ← same as above
R2_BUCKET_NAME            ← travelbilling-pdfs
R2_PUBLIC_URL             ← cloudflare.com → R2 → Bucket → Public URL
```

### 4.2 — Render Environment Variables

In Render dashboard → Your service → Environment → Add these:
```
NODE_ENV=production
DATABASE_URL=[from Supabase]
JWT_SECRET=[openssl rand -hex 64]
JWT_EXPIRY=7d
CORS_ORIGIN=https://travelbilling.vercel.app
PDF_SERVICE_URL=https://pdf-service.up.railway.app
PDF_SERVICE_API_KEY=[same key as GitHub secret]
R2_ACCESS_KEY_ID=[from Cloudflare]
R2_SECRET_ACCESS_KEY=[from Cloudflare]
R2_BUCKET_NAME=travelbilling-pdfs
R2_ACCOUNT_ID=[from Cloudflare]
```

### 4.3 — Railway Environment Variables

In Railway dashboard → Your service → Variables → Add these:
```
ENVIRONMENT=production
PDF_SERVICE_API_KEY=[same key]
R2_ACCESS_KEY_ID=[from Cloudflare]
R2_SECRET_ACCESS_KEY=[from Cloudflare]
R2_BUCKET_NAME=travelbilling-pdfs
R2_ACCOUNT_ID=[from Cloudflare]
PORT=8001
```

---

## 🐳 PHASE 5 — LOCAL DEVELOPMENT STACK

### 5.1 — Root `docker-compose.yml`

```yaml
version: '3.9'

services:
  # ─── Database ──────────────────────────────────────────────────
  db:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_USER: travelbilling
      POSTGRES_PASSWORD: devpassword
      POSTGRES_DB: travelbilling_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backend/db/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U travelbilling"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ─── Backend ───────────────────────────────────────────────────
  backend:
    build:
      context: ./backend
      target: runner
    restart: unless-stopped
    ports:
      - "8000:8000"
    environment:
      NODE_ENV: development
      DATABASE_URL: postgresql://travelbilling:devpassword@db:5432/travelbilling_dev
      JWT_SECRET: dev-local-secret-not-for-production
      CORS_ORIGIN: http://localhost:3000
      PDF_SERVICE_URL: http://pdf-service:8001
      PDF_SERVICE_API_KEY: dev-pdf-key
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - ./backend/src:/app/src    # Hot reload in dev override

  # ─── PDF Service ───────────────────────────────────────────────
  pdf-service:
    build:
      context: ./pdf-service
      target: runner
    restart: unless-stopped
    ports:
      - "8001:8001"
    environment:
      ENVIRONMENT: development
      PDF_SERVICE_API_KEY: dev-pdf-key
    volumes:
      - ./pdf-service/app:/app/app   # Hot reload in dev override

  # ─── Frontend ──────────────────────────────────────────────────
  frontend:
    build:
      context: ./frontend
      target: builder
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      VITE_API_BASE_URL: http://localhost:8000
      VITE_PDF_SERVICE_URL: http://localhost:8001
    depends_on:
      - backend

volumes:
  postgres_data:

networks:
  default:
    name: travelbilling-network
```

### 5.2 — `docker-compose.override.yml` (dev hot-reload)

```yaml
version: '3.9'

services:
  backend:
    command: npm run dev           # nodemon / ts-node-dev
    volumes:
      - ./backend/src:/app/src
      - ./backend/package.json:/app/package.json

  pdf-service:
    command: uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
    volumes:
      - ./pdf-service/app:/app/app
      - ./pdf-service/tests:/app/tests

  frontend:
    command: npm run dev -- --host
    volumes:
      - ./frontend/src:/app/src
      - ./frontend/public:/app/public
```

---

## 📡 PHASE 6 — UPTIME MONITORING & ALERTS

### 6.1 — UptimeRobot Setup (Free)

Go to `uptimerobot.com` → Create free account → Add these 3 monitors:

| Monitor Name | URL | Type | Interval | Alert on |
|---|---|---|---|---|
| TravelBilling Frontend | `https://travelbilling.vercel.app` | HTTPS | 5 min | Down, Recovery |
| TravelBilling Backend | `https://travelbilling-api.onrender.com/health` | HTTPS | 5 min | Down, Recovery |
| TravelBilling PDF Service | `https://pdf-service.up.railway.app/health` | HTTPS | 5 min | Down, Recovery |

**Critical:** Enable email alerts + set alert contacts. The backend Render ping every 5 minutes also prevents cold start spin-down.

### 6.2 — GitHub Actions Failure Notifications

Add this job to all three workflow files to get Slack/email alerts on failures:

```yaml
  notify-failure:
    name: Notify on Failure
    runs-on: ubuntu-latest
    needs: [quality, docker, deploy]
    if: failure()

    steps:
      - name: Send failure notification
        uses: actions/github-script@v7
        with:
          script: |
            const { owner, repo } = context.repo;
            const run_url = `https://github.com/${owner}/${repo}/actions/runs/${context.runId}`;
            await github.rest.issues.create({
              owner,
              repo,
              title: `🚨 CI/CD Pipeline Failed — ${context.workflow}`,
              body: `**Branch:** ${context.ref}\n**Commit:** ${context.sha.slice(0,7)}\n**Run:** ${run_url}\n\nPlease investigate immediately.`,
              labels: ['ci-failure', 'urgent']
            });
```

---

## ✅ PHASE 7 — VERIFICATION CHECKLIST

Run through this checklist after setup to confirm everything works end-to-end:

### Local Development
- [ ] `make dev` brings up all 3 services + DB without errors
- [ ] Frontend loads at `http://localhost:3000`
- [ ] Backend health check responds: `curl http://localhost:8000/health`
- [ ] PDF service health check responds: `curl http://localhost:8001/health`
- [ ] Frontend can call backend API successfully
- [ ] Backend can call PDF service successfully
- [ ] Hot reload works for all 3 services (file change → auto-reload)

### CI/CD Pipelines
- [ ] Pushing to `frontend/` triggers only `frontend.yml`
- [ ] Pushing to `backend/` triggers only `backend.yml`
- [ ] Pushing to `pdf-service/` triggers only `pdf-service.yml`
- [ ] Opening a PR creates a Vercel preview URL automatically
- [ ] PR comment is posted with the preview URL
- [ ] All linting/test jobs must pass before deploy runs
- [ ] Failed test blocks deployment (confirm by introducing a failing test)
- [ ] Docker images appear in GitHub Packages (GHCR) after main push

### Production Deployments
- [ ] Vercel production URL loads: `https://travelbilling.vercel.app`
- [ ] Render backend health check: `curl https://travelbilling-api.onrender.com/health`
- [ ] Railway PDF service health: `curl https://pdf-service.up.railway.app/health`
- [ ] UptimeRobot shows all 3 monitors as UP (green)
- [ ] Environment variables are set correctly on all 3 platforms
- [ ] CORS is configured — frontend can call backend without errors
- [ ] Secrets are not exposed in any workflow logs

### Security
- [ ] No `.env` files committed to git
- [ ] Gitleaks secret scan passes on all PRs
- [ ] Docker images run as non-root users
- [ ] All services behind HTTPS (enforced by platforms)
- [ ] Internal API key set between backend ↔ PDF service

---

## ⚠️ FREE TIER CONSTRAINTS & WORKAROUNDS

| Platform | Constraint | Workaround |
|---|---|---|
| **Render** | Spins down after 15min idle | UptimeRobot pings every 5min = always warm |
| **Render** | 750 hrs/month free | = 31 days × 24 hrs — enough for 1 service |
| **Railway** | $5 credit/month | Small Python service uses ~$1-2/month |
| **Vercel** | 100GB bandwidth/month | More than enough for billing app |
| **GHCR** | Public packages free, private needs plan | Set repo to private + GHCR works free |
| **Supabase** | 500MB DB, 2 projects free | Sufficient for travel billing MVP |
| **Cloudflare R2** | 10GB storage, 10M reads free | No egress fees unlike AWS S3 |

---

## 🚀 QUICK REFERENCE — DEPLOYMENT COMMANDS

```bash
# Local development
make dev                          # Start everything
make test                         # Run all tests
make lint                         # Lint all services
make logs                         # Follow logs

# Manual deploys (emergency use only — normally handled by CI/CD)
make deploy-frontend              # Push frontend to Vercel
make deploy-backend               # Trigger Render deploy hook
make deploy-pdf                   # Push pdf-service to Railway

# Docker operations
docker-compose build --no-cache   # Full rebuild
docker-compose down -v            # Wipe volumes (reset DB)
docker system prune -af           # Clean all images

# Check production health
curl https://travelbilling-api.onrender.com/health | jq
curl https://pdf-service.up.railway.app/health | jq
```

---

*End of TravelBilling CI/CD Agent Prompt — v1.0*
*Execute phases sequentially. Do not skip the verification checklist.*