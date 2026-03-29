param(
    [Parameter(Mandatory = $true)]
    [string]$Owner,

    [Parameter(Mandatory = $true)]
    [string]$Repo,

    [string]$Branch = "main"
)

$requiredChecks = @(
    "Frontend - Build & Deploy to Vercel / Build Check",
    "Frontend - Build & Deploy to Vercel / Preview Deploy (PR)",
    "Backend - Build, Test & Deploy to Render / Maven Test",
    "PDF Extractor - Build, Test & Deploy to Railway / Lint and test",
    "PR Checks - Security and Dependency Gate / Security Scan",
    "PR Checks - Security and Dependency Gate / Detect Leaked Secrets",
    "PR Checks - Security and Dependency Gate / Dependency Review"
)

$contextsJson = ($requiredChecks | ForEach-Object { "\"$_\"" }) -join ","
$payload = @"
{
  "required_status_checks": {
    "strict": true,
    "contexts": [ $contextsJson ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1,
    "require_last_push_approval": true
  },
  "restrictions": null,
  "required_conversation_resolution": true,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_linear_history": true,
  "lock_branch": false,
  "allow_fork_syncing": true
}
"@

Write-Host "Applying branch protection for $Owner/$Repo branch '$Branch'..."

$payload | gh api --method PUT `
  -H "Accept: application/vnd.github+json" `
  "/repos/$Owner/$Repo/branches/$Branch/protection" `
  --input -

if ($LASTEXITCODE -eq 0) {
    Write-Host "Branch protection applied successfully."
    Write-Host "If any status check name differs in Actions UI, update requiredChecks in this script and run again."
} else {
    Write-Host "Failed to apply branch protection. Ensure gh is installed and authenticated with repo admin permission."
    exit 1
}
