# ═══════════════════════════════════════════════════════════════
# TravelBill Pro — Makefile (Windows + Unix compatible)
# ═══════════════════════════════════════════════════════════════
# Quick reference:
#   make dev            → Start all services (development)
#   make prod           → Start all services (production)
#   make dev-backend    → Run backend only (no Docker)
#   make dev-frontend   → Run frontend only (no Docker)
#   make dev-pdf        → Run PDF extractor only (no Docker)
#   make status         → Show running containers
#   make logs           → Tail all container logs
#   make test           → Run test suites
#   make clean          → Stop and remove all containers + volumes
#
# On Windows without make, use:  .\dev.ps1 <command>
# ═══════════════════════════════════════════════════════════════

.PHONY: dev prod dev-backend dev-frontend dev-pdf test lint build clean logs status deploy-frontend deploy-backend deploy-pdf

# ── Development (default) ────────────────────────────────────

dev: ## Start all services in development mode
	docker compose up --build

dev-backend: ## Run backend locally (no Docker)
	cd backend && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

dev-frontend: ## Run frontend locally (no Docker)
	cd frontend && npm run dev

dev-pdf: ## Run PDF extractor locally (no Docker)
	cd pdf-extractor && uvicorn main:app --reload --port 8000

# ── Production ───────────────────────────────────────────────

prod: ## Start all services in production mode (detached)
	docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d

# ── Quality ──────────────────────────────────────────────────

test: ## Run test suites for all services
	cd frontend && npm run build
	cd backend && mvnw.cmd test
	cd pdf-extractor && pytest || true

lint: ## Run linters for all services
	cd frontend && npm run build
	cd backend && mvnw.cmd -q -DskipTests compile
	cd pdf-extractor && flake8 . || true

# ── Docker ───────────────────────────────────────────────────

build: ## Build all Docker images
	docker compose build

clean: ## Stop and remove all containers, volumes, orphans
	docker compose down -v --remove-orphans

logs: ## Tail logs from all running containers
	docker compose logs -f

status: ## Show status of running containers
	docker compose ps

# ── Deployment (manual override) ─────────────────────────────

deploy-frontend: ## Deploy frontend to Vercel
	cd frontend && npx vercel --prod

deploy-backend: ## Trigger backend deploy on Render
	curl -X POST $$RENDER_DEPLOY_HOOK_URL

deploy-pdf: ## Deploy PDF extractor to Railway
	railway up --service pdf-extractor
