# TravelBill Pro — Start Commands (PowerShell)

## 1. Backend (Spring Boot — Port 8080)

```powershell
cd d:\BillingAutomation-Antigravity\backend
mvn spring-boot:run
```

## 2. Frontend (Vite — Port 5173)

```powershell
cd d:\BillingAutomation-Antigravity\frontend
npm run dev
```

## 3. PDF Extractor (FastAPI — Port 8000)

```powershell
cd d:\BillingAutomation-Antigravity\pdf-extractor
venv\Scripts\activate
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

---

## Quick Start (All 3 in separate terminals)

```powershell
# Terminal 1 — Backend
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd d:\BillingAutomation-Antigravity\backend; mvn spring-boot:run"

# Terminal 2 — Frontend
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd d:\BillingAutomation-Antigravity\frontend; npm run dev"

# Terminal 3 — PDF Extractor
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd d:\BillingAutomation-Antigravity\pdf-extractor; venv\Scripts\activate; uvicorn main:app --host 0.0.0.0 --port 8000 --reload"
```
