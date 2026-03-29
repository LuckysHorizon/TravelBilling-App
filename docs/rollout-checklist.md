# Production Rollout Checklist

## 1. Prerequisites

- Ensure `gh` CLI is installed and authenticated with admin access to the repository.
- Ensure all required GitHub Actions secrets are configured.
- Ensure Render, Railway, and Vercel projects are already created.

## 2. Enable Branch Protection (One Command)

Run from repo root:

```powershell
./scripts/enable-branch-protection.ps1 -Owner LuckysHorizon -Repo TravelBilling-App -Branch main
```

If any check context mismatch occurs, open the Actions UI, copy exact check names, update `requiredChecks` in the script, and rerun.

## 3. Verify Required Checks Trigger

- Create a test PR touching only `frontend/` and confirm frontend + PR checks run.
- Create a test PR touching only `backend/` and confirm backend + PR checks run.
- Create a test PR touching only `pdf-extractor/` and confirm pdf-extractor + PR checks run.

## 4. Verify Deploy Paths on Main

- Merge frontend PR -> Vercel production deploy succeeds.
- Merge backend PR -> GHCR image build/push and Render deploy succeeds.
- Merge pdf-extractor PR -> GHCR image build/push and Railway deploy succeeds.

## 5. Health Verification

Run:

```bash
curl -f https://travelbilling-api.onrender.com/health
curl -f https://pdf-service.up.railway.app/health
```

Open frontend production URL and validate login/upload/extraction flow.

## 6. Failure Policy

- Any failed required check blocks merge to `main`.
- High/Critical Trivy findings now fail PR checks.
- Mypy failures in pdf-extractor now fail CI.

## 7. Rollback Notes

- Frontend rollback: redeploy previous Vercel deployment.
- Backend rollback: redeploy previous Render image/version.
- PDF extractor rollback: redeploy previous Railway deployment.
