<#
.SYNOPSIS
    TravelBill Pro — Development & Deployment Commands (PowerShell)

.DESCRIPTION
    Windows-native alternative to the Makefile.
    Run: .\dev.ps1 <command>

.EXAMPLE
    .\dev.ps1 dev              # Start all services (development)
    .\dev.ps1 prod             # Start all services (production)
    .\dev.ps1 dev-backend      # Run backend only (no Docker)
    .\dev.ps1 dev-frontend     # Run frontend only (no Docker)
    .\dev.ps1 dev-pdf          # Run PDF extractor only (no Docker)
    .\dev.ps1 status           # Show running containers
    .\dev.ps1 logs             # Tail all container logs
    .\dev.ps1 clean            # Stop and remove all containers
    .\dev.ps1 help             # Show all available commands
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet(
        "dev", "prod",
        "dev-backend", "dev-frontend", "dev-pdf",
        "test", "lint", "build",
        "clean", "logs", "status",
        "deploy-frontend", "deploy-backend", "deploy-pdf",
        "help"
    )]
    [string]$Command = "help"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

function Write-Header($text) {
    Write-Host ""
    Write-Host "  $text" -ForegroundColor Cyan
    Write-Host "  $('─' * 50)" -ForegroundColor DarkGray
}

switch ($Command) {

    # ── Development ──────────────────────────────────────────

    "dev" {
        Write-Header "Starting ALL services (development)"
        docker compose up --build
    }

    "dev-backend" {
        Write-Header "Starting Backend (Spring Boot — dev profile)"
        $env:SPRING_PROFILES_ACTIVE = "dev"
        Push-Location "$ProjectRoot\backend"
        try { & .\mvnw.cmd spring-boot:run }
        finally { Pop-Location; Remove-Item Env:\SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue }
    }

    "dev-frontend" {
        Write-Header "Starting Frontend (Vite — development)"
        Push-Location "$ProjectRoot\frontend"
        try { npm run dev }
        finally { Pop-Location }
    }

    "dev-pdf" {
        Write-Header "Starting PDF Extractor (FastAPI — development)"
        $env:APP_ENV = "development"
        Push-Location "$ProjectRoot\pdf-extractor"
        try { uvicorn main:app --reload --port 8000 }
        finally { Pop-Location; Remove-Item Env:\APP_ENV -ErrorAction SilentlyContinue }
    }

    # ── Production ───────────────────────────────────────────

    "prod" {
        Write-Header "Starting ALL services (production)"
        if (-not (Test-Path "$ProjectRoot\.env.production")) {
            Write-Host "  ERROR: .env.production not found!" -ForegroundColor Red
            Write-Host "  Copy .env.production template and fill with real secrets first." -ForegroundColor Yellow
            exit 1
        }
        docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
    }

    # ── Quality ──────────────────────────────────────────────

    "test" {
        Write-Header "Running test suites"
        Push-Location "$ProjectRoot\frontend"; npm run build; Pop-Location
        Push-Location "$ProjectRoot\backend"; & .\mvnw.cmd test; Pop-Location
    }

    "lint" {
        Write-Header "Running linters"
        Push-Location "$ProjectRoot\frontend"; npm run build; Pop-Location
        Push-Location "$ProjectRoot\backend"; & .\mvnw.cmd -q -DskipTests compile; Pop-Location
    }

    # ── Docker ───────────────────────────────────────────────

    "build" {
        Write-Header "Building all Docker images"
        docker compose build
    }

    "clean" {
        Write-Header "Stopping and removing all containers"
        docker compose down -v --remove-orphans
    }

    "logs" {
        Write-Header "Tailing container logs"
        docker compose logs -f
    }

    "status" {
        Write-Header "Container status"
        docker compose ps
    }

    # ── Deployment ───────────────────────────────────────────

    "deploy-frontend" {
        Write-Header "Deploying frontend to Vercel"
        Push-Location "$ProjectRoot\frontend"
        try { npx vercel --prod }
        finally { Pop-Location }
    }

    "deploy-backend" {
        Write-Header "Triggering backend deploy on Render"
        if (-not $env:RENDER_DEPLOY_HOOK_URL) {
            Write-Host "  ERROR: Set RENDER_DEPLOY_HOOK_URL env var first" -ForegroundColor Red
            exit 1
        }
        Invoke-RestMethod -Method Post -Uri $env:RENDER_DEPLOY_HOOK_URL
    }

    "deploy-pdf" {
        Write-Header "Deploying PDF extractor to Railway"
        Push-Location "$ProjectRoot\pdf-extractor"
        try { railway up --service pdf-extractor }
        finally { Pop-Location }
    }

    # ── Help ─────────────────────────────────────────────────

    "help" {
        Write-Host ""
        Write-Host "  TravelBill Pro — Dev Commands" -ForegroundColor Cyan
        Write-Host "  Usage: .\dev.ps1 <command>" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  Development:" -ForegroundColor Yellow
        Write-Host "    dev              Start all services (Docker)"
        Write-Host "    dev-backend      Run backend only (Spring Boot)"
        Write-Host "    dev-frontend     Run frontend only (Vite)"
        Write-Host "    dev-pdf          Run PDF extractor only (FastAPI)"
        Write-Host ""
        Write-Host "  Production:" -ForegroundColor Yellow
        Write-Host "    prod             Start all services (Docker, production)"
        Write-Host ""
        Write-Host "  Quality:" -ForegroundColor Yellow
        Write-Host "    test             Run test suites"
        Write-Host "    lint             Run linters"
        Write-Host ""
        Write-Host "  Docker:" -ForegroundColor Yellow
        Write-Host "    build            Build all Docker images"
        Write-Host "    clean            Stop + remove containers & volumes"
        Write-Host "    logs             Tail all container logs"
        Write-Host "    status           Show running containers"
        Write-Host ""
        Write-Host "  Deploy:" -ForegroundColor Yellow
        Write-Host "    deploy-frontend  Deploy to Vercel"
        Write-Host "    deploy-backend   Trigger Render deploy"
        Write-Host "    deploy-pdf       Deploy to Railway"
        Write-Host ""
    }
}
