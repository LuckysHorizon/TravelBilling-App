# TravelBill Pro — Architecture Design Document
> **Version:** 1.0.0 | **Stack:** Java 21 + Spring Boot 3.x + React 18 | **Database:** PostgreSQL 15

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Full System Diagram (ASCII)](#2-full-system-diagram-ascii)
3. [Tier 1 — Client Layer](#3-tier-1--client-layer)
4. [Tier 2 — Security Gateway](#4-tier-2--security-gateway)
5. [Tier 3 — REST API Controllers](#5-tier-3--rest-api-controllers)
6. [Tier 4 — Service Layer](#6-tier-4--service-layer)
7. [Tier 5 — External Integrations](#7-tier-5--external-integrations)
8. [Tier 6 — Data Layer](#8-tier-6--data-layer)
9. [Tier 7 — Infrastructure](#9-tier-7--infrastructure)
10. [Ticket Processing Flow (End-to-End)](#10-ticket-processing-flow-end-to-end)
11. [Invoice Generation Flow](#11-invoice-generation-flow)
12. [Authentication Flow](#12-authentication-flow)
13. [Database Schema (ERD)](#13-database-schema-erd)
14. [API Design Standards](#14-api-design-standards)
15. [Security Architecture](#15-security-architecture)
16. [Deployment Architecture](#16-deployment-architecture)
17. [Technology Stack Summary](#17-technology-stack-summary)
18. [Scalability Strategy](#18-scalability-strategy)

---

## 1. Architecture Overview

TravelBill Pro follows a **layered monolith with async job processing** pattern. A single Spring Boot application handles all API requests, with heavy async tasks (OCR, AI extraction, invoice generation) offloaded to Spring Batch jobs.

### Design Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Architecture style | Layered monolith | Simpler ops for a small team; can be split into services later |
| Frontend | React SPA | Decoupled UI; better review screen UX |
| Database | PostgreSQL | ACID for financials; JSONB for audit logs |
| Async processing | Spring Batch | File processing without blocking API threads |
| File storage | AWS S3 / MinIO | Durable, cheap, pre-signed URL support |
| AI extraction | Google Gemini Pro | Best multilingual ticket parsing; Indian language support |
| Cache | Redis | Session management + rate limiting + dashboard cache |

---

## 2. Full System Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BROWSER / CLIENT                             │
│                   React 18 + TypeScript + Vite                       │
│          Redux Toolkit │ React Query │ Axios │ Recharts              │
└────────────────────────────┬────────────────────────────────────────┘
                             │  HTTPS (TLS 1.3)
                             │  JWT in HttpOnly Cookie
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SECURITY GATEWAY                                │
│   Spring Security Filter Chain → JWT Validator → RBAC Check         │
│   Rate Limiter (Bucket4j / Redis) │ CORS │ CSRF │ Security Headers  │
│   Apache Tika File Scan (on upload) │ Input Validation               │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       REST API CONTROLLERS                           │
│  /auth  /companies  /tickets  /invoices  /reports  /users  /config  │
│               Spring Boot 3.x │ Java 21 │ OpenAPI 3.0               │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        SERVICE LAYER                                 │
│  TicketParserService │ AIExtractionService │ BillingService          │
│  InvoiceService │ EmailService │ AuditService │ UserService          │
│                    @Transactional boundaries                         │
└──────┬──────────────────┬──────────────────┬───────────────────────-┘
       │                  │                  │
       ▼                  ▼                  ▼
┌────────────┐  ┌──────────────────┐  ┌────────────────────────────┐
│ EXTERNAL   │  │  FILE PROCESSING │  │  DOCUMENT GENERATION       │
│ APIs       │  │  (Spring Batch)  │  │                            │
│            │  │                  │  │  Apache POI → Excel XLSX   │
│ Gemini Pro │  │  PDFBox 3.x      │  │  iText 7    → Invoice PDF  │
│ SendGrid   │  │  Tesseract OCR 5 │  │  SMTP/SendGrid → Email     │
└────────────┘  └──────────────────┘  └────────────────────────────┘
       │                  │                  │
       └──────────────────┴──────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                  │
│                                                                      │
│  ┌──────────────────┐  ┌────────────────┐  ┌─────────────────────┐ │
│  │  PostgreSQL 15   │  │   Redis 7      │  │   AWS S3 / MinIO    │ │
│  │  (Primary DB)    │  │   (Cache +     │  │   (File Storage)    │ │
│  │                  │  │    Sessions)   │  │                     │ │
│  │  9 core tables   │  │  JWT tokens    │  │  Ticket uploads     │ │
│  │  ACID txns       │  │  Rate limits   │  │  Invoice Excel/PDF  │ │
│  │  JSONB audit     │  │  Dashboard     │  │  Pre-signed URLs    │ │
│  │  Flyway migr.    │  │  cache (5min)  │  │  AES-256 SSE        │ │
│  │  HikariCP pool   │  │                │  │  Cross-region bkp   │ │
│  └──────────────────┘  └────────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       INFRASTRUCTURE                                 │
│  Docker / Kubernetes │ GitHub Actions CI/CD │ Prometheus + Grafana  │
│  AWS Secrets Manager │ pg_dump S3 Backup │ Flyway Migrations        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Tier 1 — Client Layer

### Stack

```
React 18           → UI framework
TypeScript (strict)→ Type safety across all components
Vite               → Build tool (fast HMR in dev, optimised prod build)
Ant Design         → Component library base (customised to TravelBill theme)
Redux Toolkit      → Global state (auth, notifications, UI state)
React Query        → Server state: caching, background refresh, optimistic updates
Axios              → HTTP client with interceptors for JWT refresh
Recharts           → Dashboard charts (bar, line, pie)
react-pdf-viewer   → PDF ticket preview in Review screen
```

### Key Architecture Decisions (Frontend)

**JWT Refresh Flow:**
```
Axios request interceptor:
  1. Attach access token from memory (NOT localStorage)
  2. On 401 response → call /api/auth/refresh with HttpOnly cookie
  3. If refresh succeeds → retry original request
  4. If refresh fails → clear auth state → redirect /login
```

**React Query Cache Strategy:**
```
Dashboard metrics:    staleTime: 5min,  refetchInterval: 5min
Ticket list:          staleTime: 30sec, refetchOnWindowFocus: true
Companies list:       staleTime: 10min, refetchOnMount: true
Invoice (generated):  staleTime: Infinity (immutable after creation)
```

**File Upload (multipart):**
```javascript
// Chunk large files, show per-file progress
const uploadTicket = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('companyId', selectedCompanyId);

  return axios.post('/api/tickets/upload', formData, {
    onUploadProgress: (e) => setProgress(Math.round(e.loaded * 100 / e.total))
  });
};
```

---

## 4. Tier 2 — Security Gateway

All requests pass through this layer before reaching any business logic.

### Filter Chain Order

```
1. SecurityContextPersistenceFilter   → Load SecurityContext
2. JwtAuthenticationFilter (custom)   → Validate JWT, set Authentication
3. RateLimitFilter (custom)           → Bucket4j per-IP and per-user
4. CorsFilter                         → Validate Origin header
5. CsrfFilter                         → SameSite cookie enforcement
6. AuthorizationFilter                → RBAC check (@PreAuthorize)
```

### JWT Validation Logic

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        // 1. Extract JWT from HttpOnly cookie (never Authorization header)
        String token = extractFromCookie(request, "access_token");

        // 2. Validate signature, expiry, issuer
        if (token != null && jwtService.isValid(token)) {
            // 3. Check token JTI not in Redis revocation set
            if (!tokenRevocationService.isRevoked(jwtService.getJti(token))) {
                // 4. Set SecurityContext
                Authentication auth = jwtService.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
```

### Rate Limiting Configuration

```java
// Per user: 200 requests per minute
// Per IP: 500 requests per minute
// Upload endpoint: 10 uploads per minute per user
// Login endpoint: 10 attempts per 15 minutes per IP
```

### Security Headers Applied

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'; script-src 'self'
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

---

## 5. Tier 3 — REST API Controllers

### Controller Responsibilities

Each controller is **thin** — validates request, delegates to service, returns response. No business logic in controllers.

### Endpoint Reference

```
AUTH
  POST   /api/auth/login              Public       Login, returns JWT pair in cookies
  POST   /api/auth/refresh            Cookie auth  Refresh access token
  POST   /api/auth/logout             Any auth     Revoke refresh token in Redis
  GET    /api/auth/me                 Any auth     Current user profile

COMPANIES
  GET    /api/companies               All roles    List (paginated, searchable)
  POST   /api/companies               STAFF+       Create company
  GET    /api/companies/:id           All roles    Company detail + stats
  PUT    /api/companies/:id           STAFF+       Update (audited)
  DELETE /api/companies/:id           ADMIN        Soft delete

TICKETS
  GET    /api/tickets                 All roles    List (filterable, paginated)
  POST   /api/tickets/upload          STAFF+       Upload files (multipart, batch)
  GET    /api/tickets/upload/:batchId STAFF+       Upload batch status (polling)
  POST   /api/tickets/manual          STAFF+       Manual ticket entry
  GET    /api/tickets/:id             All roles    Ticket detail
  PUT    /api/tickets/:id/confirm     STAFF+       Confirm AI-extracted data
  PUT    /api/tickets/:id             STAFF+       Update (UNBILLED only)
  DELETE /api/tickets/:id             STAFF+       Soft delete (UNBILLED only)
  GET    /api/tickets/export          All roles    Export filtered CSV

INVOICES
  GET    /api/invoices                All roles    List invoices (paginated)
  POST   /api/invoices/generate       STAFF+       Generate invoice (company + month)
  GET    /api/invoices/:id            All roles    Invoice detail
  GET    /api/invoices/:id/excel      All roles    Download Excel (pre-signed S3 URL)
  GET    /api/invoices/:id/pdf        All roles    Download PDF (pre-signed S3 URL)
  POST   /api/invoices/:id/send-email STAFF+       Send invoice by email
  POST   /api/invoices/:id/credit-note ADMIN       Create credit note

REPORTS
  GET    /api/reports/dashboard       All roles    KPI metrics for dashboard
  GET    /api/reports/revenue         All roles    Revenue by period + company
  GET    /api/reports/company/:id     All roles    Per-company spend report
  GET    /api/reports/export          All roles    Export full report to Excel

USERS (Admin only)
  GET    /api/users                   ADMIN        List all users
  POST   /api/users                   ADMIN        Create user
  PUT    /api/users/:id               ADMIN        Update user / change role
  DELETE /api/users/:id               ADMIN        Deactivate user

CONFIG (Admin only)
  GET    /api/config/gst              ADMIN        Current GST rates
  PUT    /api/config/gst              ADMIN        Update GST rates (effective date)
  GET    /api/config/email-templates  ADMIN        List email templates
  PUT    /api/config/email-templates/:key ADMIN    Update template
```

### Standard Response Envelope

```json
{
  "status": "SUCCESS",
  "message": "Invoice generated successfully",
  "data": { ... },
  "timestamp": "2026-03-14T10:30:00Z",
  "requestId": "req_abc123"
}
```

### Error Response Format

```json
{
  "status": "ERROR",
  "code": "DUPLICATE_PNR",
  "message": "PNR 6X9QR2 already exists in the system",
  "field": "pnrNumber",
  "existingRecordId": 4821,
  "timestamp": "2026-03-14T10:30:00Z"
}
```

### Standard Error Codes

| HTTP | Code | Meaning |
|------|------|---------|
| 400 | `VALIDATION_ERROR` | Request validation failed (field-level errors included) |
| 400 | `DUPLICATE_PNR` | PNR already exists in system |
| 401 | `TOKEN_EXPIRED` | JWT access token has expired |
| 401 | `INVALID_TOKEN` | JWT signature invalid |
| 403 | `INSUFFICIENT_ROLE` | User role does not permit this action |
| 404 | `RESOURCE_NOT_FOUND` | Entity not found by ID |
| 409 | `TICKET_ALREADY_BILLED` | Cannot modify a billed ticket |
| 409 | `INVOICE_ALREADY_EXISTS` | Invoice already generated for this company + month |
| 422 | `LOW_AI_CONFIDENCE` | AI extraction confidence below threshold — review required |
| 429 | `RATE_LIMIT_EXCEEDED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Unexpected server error (logged, never exposes stack trace) |

---

## 6. Tier 4 — Service Layer

### Service Responsibilities

```
TicketParserService    → OCR pipeline: file → raw text
AIExtractionService    → Raw text → structured TicketDTO (via Gemini Pro)
BillingService         → Calculate GST, service charges, totals
InvoiceService         → Generate Excel + PDF, store in S3
EmailService           → Template rendering, SMTP/SendGrid dispatch
AuditService           → Log all entity changes to audit_logs table
UserService            → User CRUD, password hashing, account lockout
CompanyService         → Company CRUD, validation, stats aggregation
TicketService          → Ticket CRUD, status transitions, duplicate checks
ReportService          → Aggregate queries for dashboard + reports
```

### TicketParserService — OCR Pipeline

```java
@Service
public class TicketParserService {

    public RawTicketText parseFile(MultipartFile file, String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> parsePdf(file);
            case "image/png", "image/jpeg", "image/tiff" -> parseImage(file);
            default -> throw new UnsupportedFileTypeException(mimeType);
        };
    }

    private RawTicketText parsePdf(MultipartFile file) {
        // PDFBox: extract text layer first
        // If text layer empty (scanned PDF), fall through to Tesseract
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            String text = new PDFTextStripper().getText(doc);
            if (text.trim().length() > 50) {
                return new RawTicketText(text, ExtractionMethod.PDFBOX);
            }
        }
        // Scanned PDF → convert to image → Tesseract
        return parseScannedPdf(file);
    }

    private RawTicketText parseImage(MultipartFile file) {
        // Pre-process: auto-rotate, deskew, contrast enhancement
        BufferedImage img = preprocessImage(ImageIO.read(file.getInputStream()));
        Tesseract tess = new Tesseract();
        tess.setDatapath("/usr/share/tessdata");
        tess.setLanguage("eng+hin"); // English + Hindi support
        tess.setPageSegMode(1);
        String text = tess.doOCR(img);
        return new RawTicketText(text, ExtractionMethod.TESSERACT);
    }
}
```

### AIExtractionService — Gemini Pro Integration

```java
@Service
public class AIExtractionService {

    private static final String EXTRACTION_PROMPT = """
        You are a travel ticket data extraction assistant for an Indian travel agency.
        Extract the following fields from the OCR text and return ONLY valid JSON.
        For each field, return a confidence score 0-100.

        Required fields:
        - pnr_number (6 alphanumeric characters)
        - passenger_name (full name as on ticket)
        - travel_date (YYYY-MM-DD format)
        - origin_city (city or airport code)
        - destination_city (city or airport code)
        - base_fare (numeric, INR)
        - taxes (numeric, INR, 0 if not found)
        - total_amount (numeric, INR)
        - ticket_type ("FLIGHT" or "BUS")
        - operator_name (airline or bus company name)
        - seat_number (if present, else null)

        Response format ONLY:
        {
          "fields": {
            "pnr_number": { "value": "6X9QR2", "confidence": 97 },
            ...
          },
          "overall_confidence": 91
        }

        NEVER hallucinate values. Return null + confidence 0 for missing fields.

        OCR Text:
        """;

    public ExtractionResult extract(String rawText) {
        try {
            String response = callGeminiApi(EXTRACTION_PROMPT + rawText);
            return parseExtractionResponse(response);
        } catch (GeminiApiException e) {
            // Fallback to regex extraction
            log.warn("Gemini API failed, falling back to regex: {}", e.getMessage());
            return regexFallbackExtraction(rawText);
        }
    }

    private ExtractionResult regexFallbackExtraction(String text) {
        // Regex patterns for common ticket formats
        // All confidence scores capped at 60 (flagged for manual review)
        Map<String, FieldExtraction> fields = new HashMap<>();
        fields.put("pnr_number", extractWithRegex(text,
            "PNR[:\\s]*([A-Z0-9]{5,8})", 60));
        fields.put("total_amount", extractWithRegex(text,
            "(?:Total|Amount|Fare)[:\\s]*[₹Rs\\.\\s]*(\\d[\\d,]*\\.?\\d*)", 55));
        // ... other patterns
        return new ExtractionResult(fields, 50, ExtractionMethod.REGEX_FALLBACK);
    }
}
```

### BillingService — GST Calculation

```java
@Service
public class BillingService {

    public InvoiceTotals calculateTotals(List<Ticket> tickets, Company company) {
        // Fetch current GST rates (effective for billing period)
        GstConfig gst = gstConfigRepo.findEffectiveRate(LocalDate.now());

        BigDecimal subtotal = tickets.stream()
            .map(Ticket::getBaseFare)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceCharge = subtotal
            .multiply(company.getServiceChargePct())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal cgst = serviceCharge
            .multiply(gst.getCgstRate())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal sgst = serviceCharge
            .multiply(gst.getSgstRate())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal grandTotal = subtotal
            .add(serviceCharge)
            .add(cgst)
            .add(sgst);

        return new InvoiceTotals(subtotal, serviceCharge, cgst, sgst, grandTotal);
    }
}
```

---

## 7. Tier 5 — External Integrations

### Google Gemini Pro API

```yaml
endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
auth: API key (AWS Secrets Manager)
timeout: 30 seconds
retry: 3 attempts with exponential backoff (1s, 2s, 4s)
fallback: Regex-based extraction (confidence capped at 60%)
rate_limit: 60 requests/minute (Gemini free tier: upgrade for production)
```

### AWS S3 / MinIO

```yaml
bucket_structure:
  tickets/
    {companyId}/
      {year}/{month}/
        {uuid}_{originalFilename}  # Ticket originals

  invoices/
    {companyId}/
      {year}/
        {invoiceNumber}.xlsx
        {invoiceNumber}.pdf

access_control:
  upload:       Server-side only (never direct browser upload)
  download:     Pre-signed URL, 15-minute expiry
  encryption:   SSE-S3 (AES-256)
  versioning:   Enabled (protects against accidental overwrites)
```

### SendGrid Email

```yaml
from: billing@youragency.com
templates:
  invoice_sent:
    subject: "Invoice {invoiceNumber} — {companyName} — {billingMonth}"
    attachment: invoice PDF
    cc: admin@youragency.com

  payment_reminder:
    subject: "Payment Due — Invoice {invoiceNumber}"
    trigger: invoice overdue by 7 days

delivery_tracking:
  webhook: /api/webhooks/sendgrid (delivered, bounced, opened)
  retry: 3 attempts on bounce
```

### Apache PDFBox + Tesseract OCR

```yaml
pdfbox:
  version: 3.0.1
  use_case: Digital PDF text extraction
  fallback: If text layer < 50 chars, treat as scanned

tesseract:
  version: 5.x
  languages: [eng, hin]
  models: tessdata-best (higher accuracy, slower)
  preprocessing:
    - Auto-rotate (deskew)
    - Contrast enhancement
    - Noise reduction
    - DPI upscale to 300 if < 200
  page_segmentation: PSM_AUTO_OSD (1)
```

---

## 8. Tier 6 — Data Layer

### Database: PostgreSQL 15

#### Connection Pool (HikariCP)

```yaml
maximum-pool-size: 20
minimum-idle: 5
connection-timeout: 30000ms
idle-timeout: 600000ms
max-lifetime: 1800000ms
leak-detection-threshold: 60000ms
```

#### All Tables

| Table | Purpose | Rows (Est. 1yr) |
|-------|---------|-----------------|
| `users` | System users | < 50 |
| `companies` | Corporate clients | 50–500 |
| `tickets` | Individual travel tickets | 10k–100k |
| `invoices` | Generated monthly invoices | 500–5000 |
| `credit_notes` | Invoice corrections | < 200 |
| `audit_logs` | All entity change history | 100k–1M |
| `gst_config` | GST rate history | < 20 |
| `email_templates` | Invoice + reminder templates | < 10 |
| `system_config` | Key-value system settings | < 50 |

#### Key Constraints & Indexes

```sql
-- Critical financial constraints
ALTER TABLE tickets ADD CONSTRAINT tickets_pnr_unique UNIQUE (pnr_number);
ALTER TABLE invoices ADD CONSTRAINT invoices_number_unique UNIQUE (invoice_number);
ALTER TABLE companies ADD CONSTRAINT companies_gst_unique UNIQUE (gst_number);

-- Performance indexes
CREATE INDEX idx_tickets_company_status ON tickets(company_id, status);
CREATE INDEX idx_tickets_travel_date ON tickets(travel_date);
CREATE INDEX idx_tickets_invoice_id ON tickets(invoice_id);
CREATE INDEX idx_invoices_company_month ON invoices(company_id, billing_month);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, created_at);

-- Partial index for active companies (most queries filter is_active=true)
CREATE INDEX idx_companies_active ON companies(name) WHERE is_active = true;
```

#### Invoice Number Sequence (Race-Condition Safe)

```sql
-- Dedicated sequence table with SELECT FOR UPDATE
CREATE TABLE invoice_sequences (
    financial_year  VARCHAR(7) PRIMARY KEY,  -- e.g. '2026-27'
    next_number     INTEGER DEFAULT 1
);

-- Java: within @Transactional
String nextNumber = jdbcTemplate.queryForObject(
    "SELECT next_number FROM invoice_sequences " +
    "WHERE financial_year = ? FOR UPDATE",
    String.class, financialYear);

jdbcTemplate.update(
    "UPDATE invoice_sequences SET next_number = next_number + 1 " +
    "WHERE financial_year = ?", financialYear);

return String.format("TBP/%s/%05d", financialYear, nextNumber);
```

### Cache: Redis 7

```yaml
data_structures:
  jwt_revocation:
    key:   "revoked:{jti}"
    value: "1"
    ttl:   7 days (matches refresh token max lifetime)
    use:   Logout invalidates all sessions immediately

  rate_limit:
    key:   "rl:user:{userId}"  OR  "rl:ip:{ipAddress}"
    type:  Bucket4j distributed bucket
    use:   API rate limiting across multiple server instances

  dashboard_cache:
    key:   "dashboard:{userId}"
    value: JSON blob of KPI metrics
    ttl:   5 minutes
    use:   Avoid expensive aggregation queries on every dashboard load

  session_meta:
    key:   "session:{userId}"
    value: Last login IP, device info
    ttl:   30 days
```

### File Storage: AWS S3 / MinIO

```java
// Upload ticket file (always server-side, never direct browser upload)
public String uploadTicketFile(MultipartFile file, Long companyId) {
    String key = String.format("tickets/%d/%s/%s_%s",
        companyId,
        YearMonth.now().toString(),
        UUID.randomUUID(),
        sanitizeFilename(file.getOriginalFilename())
    );

    s3Client.putObject(PutObjectRequest.builder()
        .bucket(ticketsBucket)
        .key(key)
        .serverSideEncryption(ServerSideEncryption.AES256)
        .build(),
        RequestBody.fromInputStream(file.getInputStream(), file.getSize())
    );

    return key; // Stored in tickets.file_path
}

// Generate pre-signed URL for download (15 min expiry)
public URL generatePresignedUrl(String s3Key) {
    return s3Presigner.presignGetObject(r -> r
        .signatureDuration(Duration.ofMinutes(15))
        .getObjectRequest(g -> g.bucket(ticketsBucket).key(s3Key))
    ).url();
}
```

---

## 9. Tier 7 — Infrastructure

### Docker Compose (Development)

```yaml
version: '3.8'

services:
  backend:
    build: ./backend
    ports: ["8080:8080"]
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/travelbill
      - SPRING_REDIS_HOST=redis
      - AWS_S3_ENDPOINT=http://minio:9000  # MinIO for local S3
      - GEMINI_API_KEY=${GEMINI_API_KEY}
    depends_on: [db, redis, minio]
    volumes:
      - ./backend/src:/app/src  # Hot reload

  frontend:
    build: ./frontend
    ports: ["3000:3000"]
    environment:
      - VITE_API_BASE_URL=http://localhost:8080
    volumes:
      - ./frontend/src:/app/src  # HMR

  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: travelbill
      POSTGRES_USER: travelbill
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports: ["5432:5432"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --appendonly yes

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    ports: ["9000:9000", "9001:9001"]
    environment:
      MINIO_ROOT_USER: ${MINIO_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD}

volumes:
  postgres_data:
```

### Kubernetes Production (Helm chart structure)

```
k8s/
├── namespace.yaml
├── backend/
│   ├── deployment.yaml      # 2 replicas min, HPA up to 8
│   ├── service.yaml
│   ├── configmap.yaml       # Non-secret env vars
│   └── hpa.yaml             # Scale on CPU > 70%
├── frontend/
│   ├── deployment.yaml      # 2 replicas (nginx serving built assets)
│   └── service.yaml
├── ingress.yaml             # NGINX ingress + TLS cert (cert-manager)
├── postgres/
│   └── statefulset.yaml     # Or use AWS RDS (recommended for prod)
└── redis/
    └── statefulset.yaml     # Or use ElastiCache
```

### CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run backend tests
        run: ./mvnw test
      - name: Run frontend tests
        run: cd frontend && npm test
      - name: SonarQube scan
        run: ./mvnw sonar:sonar

  build:
    needs: test
    steps:
      - name: Build Docker images
        run: docker build -t travelbill-backend:$SHA .
      - name: Push to ECR
        run: docker push $ECR_REGISTRY/travelbill-backend:$SHA

  deploy:
    needs: build
    steps:
      - name: Deploy to Kubernetes
        run: kubectl set image deployment/backend backend=$IMAGE:$SHA
      - name: Wait for rollout
        run: kubectl rollout status deployment/backend
      - name: Run smoke tests
        run: ./scripts/smoke-test.sh $PRODUCTION_URL
```

### Observability Stack

```yaml
metrics:
  collector: Micrometer → Prometheus
  dashboards: Grafana
  key_metrics:
    - api_request_duration_p95
    - active_db_connections
    - ai_extraction_success_rate
    - invoice_generation_duration
    - redis_hit_rate

logging:
  framework: SLF4J + Logback
  format: JSON structured logs
  aggregation: Promtail → Loki → Grafana
  sensitive_fields_masked: [password, token, api_key]

alerting:
  channels: [PagerDuty, Slack #alerts]
  rules:
    - API p95 > 500ms for 5min
    - DB connection pool > 90% for 2min
    - AI extraction failure rate > 20% for 10min
    - Any 5xx error rate > 1% for 5min
    - Disk usage > 80%
```

---

## 10. Ticket Processing Flow (End-to-End)

```
                    STAFF                    SYSTEM
                      │
     Upload files ────┤
                      │──► POST /api/tickets/upload ──────────────────┐
                      │                                                │
                      │         Save files to S3 (UUID keys)          │
                      │         Create ticket records (status=PROCESSING)
                      │         Return batchId immediately             │
                      │◄── 200 OK { batchId: "batch_abc" } ──────────┘
                      │
     Poll status ─────┤──► GET /api/tickets/upload/batch_abc
                      │
                      │    [Spring Batch Job — Async]
                      │
                      │    Step 1: For each file in batch:
                      │      ├─ PDF? → PDFBox text extraction
                      │      └─ Image? → Tesseract OCR
                      │
                      │    Step 2: Send raw text to Gemini Pro
                      │      ├─ Returns structured JSON with confidence
                      │      └─ Fallback: regex if API fails
                      │
                      │    Step 3: Compute overall confidence
                      │      ├─ All fields ≥ 85%? → status=PENDING_REVIEW
                      │      └─ Any field < 60%? → status=PENDING_REVIEW + flag
                      │
                      │◄── Batch complete notification ──────────────┘
                      │
  Open review ────────┤──► GET /api/tickets/review?batchId=batch_abc
                      │◄── List of extracted tickets with confidence scores
                      │
  Confirm ticket ─────┤
                      │    Step 4: Check PNR duplicate
                      │      ├─ SELECT * FROM tickets WHERE pnr_number = ?
                      │      └─ If found → 409 DUPLICATE_PNR error
                      │
                      │    Step 5: Calculate financials
                      │      ├─ service_charge = base_fare × company.service_pct
                      │      ├─ cgst = service_charge × gst_config.cgst_rate
                      │      ├─ sgst = service_charge × gst_config.sgst_rate
                      │      └─ total = base_fare + service_charge + cgst + sgst
                      │
                      │    Step 6: Save ticket (ACID transaction)
                      │      ├─ INSERT INTO tickets (all fields, status=APPROVED)
                      │      └─ INSERT INTO audit_logs (action=TICKET_CREATED)
                      │
                      │◄── 201 Created { ticketId, status: "APPROVED" }
                      │
```

---

## 11. Invoice Generation Flow

```
     STAFF                    BILLING SERVICE
       │
       │──► POST /api/invoices/generate
       │    { companyId: 42, billingMonth: "2026-03" }
       │
       │    1. Validate: No existing invoice for this company+month
       │       └─ If exists → 409 INVOICE_ALREADY_EXISTS
       │
       │    2. Fetch all APPROVED tickets for company + month
       │       └─ SELECT * FROM tickets
       │            WHERE company_id = 42
       │              AND status = 'APPROVED'
       │              AND travel_date BETWEEN '2026-03-01' AND '2026-03-31'
       │
       │    3. Lock: SELECT FOR UPDATE on invoice_sequences
       │       └─ Get next invoice number: TBP/2026-27/00142
       │
       │    4. Calculate totals (BillingService)
       │       ├─ subtotal = SUM(base_fare)
       │       ├─ service_charge = subtotal × company.service_charge_pct%
       │       ├─ cgst = service_charge × 9%
       │       ├─ sgst = service_charge × 9%
       │       └─ grand_total = subtotal + service_charge + cgst + sgst
       │
       │    5. Generate Excel (Apache POI)
       │       ├─ Sheet 1: Ticket details table
       │       ├─ Sheet 2: GST summary
       │       └─ Upload to S3: invoices/42/2026/TBP-2026-27-00142.xlsx
       │
       │    6. Generate PDF (iText)
       │       ├─ Company letterhead
       │       ├─ Ticket table (paginated for large invoices)
       │       ├─ GST breakup summary
       │       └─ Upload to S3: invoices/42/2026/TBP-2026-27-00142.pdf
       │
       │    7. Save invoice record + mark tickets BILLED (one transaction)
       │       ├─ INSERT INTO invoices (all fields, status=GENERATED)
       │       └─ UPDATE tickets SET status='BILLED', invoice_id=? WHERE id IN (...)
       │
       │    8. Audit log (InvoiceGenerated event)
       │
       │◄── 201 Created { invoiceId, invoiceNumber, grandTotal, excelUrl, pdfUrl }
       │
       │──► POST /api/invoices/142/send-email (optional next step)
       │    ├─ Render HTML email template
       │    ├─ Attach PDF invoice
       │    ├─ Send via SendGrid
       │    ├─ UPDATE invoices SET status='SENT', email_sent_at=NOW()
       │    └─ Audit log (InvoiceSent event)
```

---

## 12. Authentication Flow

```
LOGIN
  1. POST /api/auth/login { username, password }
  2. Load user from DB by username
  3. Check is_active=true, locked_until < NOW()
  4. Verify bcrypt password hash
  5. On failure:
     - Increment failed_attempts
     - If failed_attempts >= 5: SET locked_until = NOW() + 30min
     - Return 401 INVALID_CREDENTIALS
  6. On success:
     - Reset failed_attempts = 0
     - Generate access token (JWT, 8h, signed RS256)
     - Generate refresh token (JWT, 7d, JTI stored in Redis)
     - Set HttpOnly Secure SameSite=Strict cookies
     - Audit log: LOGIN_SUCCESS (user, IP, timestamp)

TOKEN REFRESH
  1. POST /api/auth/refresh (refresh token in HttpOnly cookie)
  2. Validate refresh token signature + expiry
  3. Check JTI exists in Redis (not revoked)
  4. Delete old JTI from Redis (rotation — one-time use)
  5. Generate new access token + new refresh token
  6. Set new cookies

LOGOUT
  1. POST /api/auth/logout
  2. Extract refresh token JTI from cookie
  3. Add JTI to Redis revocation set (TTL = remaining token lifetime)
  4. Clear both cookies (Set-Cookie with Max-Age=0)
  5. Audit log: LOGOUT (user, IP, timestamp)
```

---

## 13. Database Schema (ERD)

```
users
  id              BIGSERIAL PK
  username        VARCHAR(100) UNIQUE NOT NULL
  email           VARCHAR(255) UNIQUE NOT NULL
  password_hash   VARCHAR(255) NOT NULL         -- bcrypt
  role            VARCHAR(20) NOT NULL           -- ADMIN/BILLING_STAFF/VIEWER
  failed_attempts INTEGER DEFAULT 0
  locked_until    TIMESTAMP
  is_active       BOOLEAN DEFAULT TRUE
  created_at      TIMESTAMP NOT NULL
  updated_at      TIMESTAMP NOT NULL

companies
  id                  BIGSERIAL PK
  name                VARCHAR(255) NOT NULL
  gst_number          VARCHAR(15) UNIQUE NOT NULL
  billing_email       VARCHAR(255) NOT NULL
  address             TEXT
  service_charge_pct  DECIMAL(5,2) NOT NULL      -- Per-company %
  billing_cycle       VARCHAR(10) NOT NULL        -- MONTHLY/WEEKLY
  credit_limit        DECIMAL(12,2)               -- Optional
  is_active           BOOLEAN DEFAULT TRUE
  created_at          TIMESTAMP NOT NULL
  updated_at          TIMESTAMP NOT NULL
  created_by          BIGINT FK → users.id

tickets
  id              BIGSERIAL PK
  company_id      BIGINT NOT NULL FK → companies.id
  pnr_number      VARCHAR(20) UNIQUE NOT NULL    -- Critical constraint
  ticket_type     VARCHAR(10) NOT NULL           -- FLIGHT/BUS
  passenger_name  VARCHAR(255) NOT NULL
  travel_date     DATE NOT NULL
  origin          VARCHAR(100)
  destination     VARCHAR(100)
  operator_name   VARCHAR(150)
  base_fare       DECIMAL(10,2) NOT NULL
  service_charge  DECIMAL(10,2) NOT NULL
  cgst            DECIMAL(10,2) NOT NULL
  sgst            DECIMAL(10,2) NOT NULL
  total_amount    DECIMAL(10,2) NOT NULL         -- Stored, not computed
  status          VARCHAR(20) NOT NULL           -- PENDING_REVIEW/APPROVED/BILLED/PAID
  file_path       TEXT                           -- S3 object key
  ai_confidence   DECIMAL(5,2)                   -- Overall AI confidence 0-100
  invoice_id      BIGINT FK → invoices.id        -- NULL until billed
  created_at      TIMESTAMP NOT NULL
  updated_at      TIMESTAMP NOT NULL
  created_by      BIGINT FK → users.id

invoices
  id              BIGSERIAL PK
  invoice_number  VARCHAR(30) UNIQUE NOT NULL    -- TBP/2026-27/00142
  company_id      BIGINT NOT NULL FK → companies.id
  billing_month   VARCHAR(7) NOT NULL            -- 2026-03
  subtotal        DECIMAL(12,2) NOT NULL
  service_charge  DECIMAL(12,2) NOT NULL
  cgst_total      DECIMAL(12,2) NOT NULL
  sgst_total      DECIMAL(12,2) NOT NULL
  grand_total     DECIMAL(12,2) NOT NULL         -- Immutable after generation
  status          VARCHAR(20) NOT NULL           -- GENERATED/SENT/PAID/OVERDUE
  excel_s3_key    TEXT
  pdf_s3_key      TEXT
  email_sent_at   TIMESTAMP
  paid_at         TIMESTAMP
  created_at      TIMESTAMP NOT NULL
  created_by      BIGINT FK → users.id

audit_logs
  id              BIGSERIAL PK
  entity_type     VARCHAR(50) NOT NULL           -- TICKET/INVOICE/COMPANY/USER
  entity_id       BIGINT NOT NULL
  action          VARCHAR(50) NOT NULL           -- CREATED/UPDATED/DELETED/etc
  old_value       JSONB                          -- Previous state
  new_value       JSONB                          -- New state
  user_id         BIGINT FK → users.id
  ip_address      VARCHAR(45)
  created_at      TIMESTAMP NOT NULL

gst_config
  id              BIGSERIAL PK
  cgst_rate       DECIMAL(5,2) NOT NULL          -- Default 9.00
  sgst_rate       DECIMAL(5,2) NOT NULL          -- Default 9.00
  effective_from  DATE NOT NULL
  created_by      BIGINT FK → users.id
  created_at      TIMESTAMP NOT NULL

credit_notes
  id              BIGSERIAL PK
  credit_note_number VARCHAR(30) UNIQUE NOT NULL
  invoice_id      BIGINT NOT NULL FK → invoices.id
  reason          TEXT NOT NULL
  amount          DECIMAL(12,2) NOT NULL
  type            VARCHAR(10) NOT NULL           -- CREDIT/DEBIT
  created_at      TIMESTAMP NOT NULL
  created_by      BIGINT FK → users.id
```

---

## 14. API Design Standards

### Pagination

All list endpoints support:
```
GET /api/tickets?page=0&size=25&sort=travelDate,desc

Response:
{
  "data": [...],
  "pagination": {
    "page": 0,
    "size": 25,
    "totalElements": 847,
    "totalPages": 34,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### Filtering

```
GET /api/tickets?companyId=42&status=APPROVED&type=FLIGHT&dateFrom=2026-03-01&dateTo=2026-03-31
```

### Sorting

```
GET /api/tickets?sort=travelDate,desc&sort=totalAmount,asc
```

### Field Selection (sparse fieldsets)

```
GET /api/tickets?fields=id,pnrNumber,passengerName,totalAmount,status
```

---

## 15. Security Architecture

### Defense in Depth

```
Layer 1: Network     → TLS 1.3 only; no plain HTTP; HSTS enforced
Layer 2: Auth        → JWT RS256; HttpOnly cookies; refresh rotation
Layer 3: AuthZ       → RBAC on every endpoint; server-side ownership checks
Layer 4: Input       → Bean Validation; JPA parameterized queries; Tika scan
Layer 5: Output      → No stack traces in errors; sensitive fields masked in logs
Layer 6: Data        → AES-256 at rest; DECIMAL types for money (no float)
Layer 7: Audit       → Every financial change logged with old/new values
```

### OWASP Top 10 Controls

| Risk | Control |
|------|---------|
| A01 Broken Access Control | RBAC via @PreAuthorize; server-side ownership check on every entity GET/PUT/DELETE |
| A02 Cryptographic Failures | TLS 1.3; bcrypt cost 12; AES-256 at rest; no MD5/SHA-1 |
| A03 Injection | JPA parameterized queries only; never string concat in SQL; input validation |
| A04 Insecure Design | Threat model documented; immutable invoices; financial integrity enforced by DB constraints |
| A05 Security Misconfiguration | Spring Security hardened defaults; all security headers applied |
| A06 Vulnerable Components | OWASP Dependency Check in CI/CD; monthly dependency updates |
| A07 Auth Failures | Account lockout; JWT rotation; revocation; no JWT in localStorage |
| A08 Software Integrity | Docker image digest pinning; signed commits; no untrusted dependencies |
| A09 Logging Failures | Structured audit logs; append-only; 7-year retention enforced |
| A10 SSRF | No user-supplied URLs fetched; no outbound calls except whitelisted APIs |

---

## 16. Deployment Architecture

```
                          Internet
                             │
                    ┌────────▼────────┐
                    │   CloudFront    │  (CDN for React SPA)
                    │   + WAF         │  (rate limit, bot protection)
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  NGINX Ingress  │  (Kubernetes)
                    │  + TLS termination
                    └────┬────────┬──┘
                         │        │
               ┌─────────▼─┐  ┌───▼──────────┐
               │ Frontend  │  │  Backend API  │
               │ (nginx)   │  │  (Spring Boot)│
               │ 2 pods    │  │  2–8 pods HPA │
               └───────────┘  └───────┬───────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                   │
          ┌─────────▼──────┐  ┌────────▼───┐  ┌──────────▼───┐
          │  AWS RDS        │  │ ElastiCache│  │   AWS S3     │
          │  PostgreSQL 15  │  │  Redis 7   │  │  (files)     │
          │  Multi-AZ       │  │  Cluster   │  │  Cross-region│
          │  Daily backup   │  │            │  │  replication │
          └─────────────────┘  └────────────┘  └──────────────┘
```

### Environment Strategy

| Environment | Purpose | Data |
|-------------|---------|------|
| `local` | Developer machines | Docker Compose, seed data |
| `dev` | Feature branch testing | K8s, anonymised prod data |
| `staging` | Pre-release QA | K8s, production-like data |
| `production` | Live system | K8s Multi-AZ, real data |

---

## 17. Technology Stack Summary

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| Language (BE) | Java | 21 LTS | Backend runtime |
| Framework (BE) | Spring Boot | 3.x | REST API framework |
| Security | Spring Security | 6.x | Auth, RBAC, CSRF |
| Persistence | Spring Data JPA | 3.x | ORM layer |
| Batch | Spring Batch | 5.x | Async file processing |
| DB Migrations | Flyway | 9.x | Schema version control |
| Connection Pool | HikariCP | bundled | DB connection management |
| Language (FE) | TypeScript | 5.x | Type-safe frontend |
| Framework (FE) | React | 18.x | UI framework |
| Build tool | Vite | 5.x | Frontend build |
| UI Components | Ant Design | 5.x | Component library base |
| State | Redux Toolkit | 2.x | Global client state |
| Data fetching | React Query | 5.x | Server state, caching |
| Charts | Recharts | 2.x | Dashboard visualisations |
| Database | PostgreSQL | 15 | Primary data store |
| Cache / Session | Redis | 7.x | Token store, rate limits |
| File Storage | AWS S3 / MinIO | — | Ticket files, invoices |
| OCR | Tesseract | 5.x | Image ticket OCR |
| PDF parsing | Apache PDFBox | 3.0.1 | Digital PDF text extraction |
| Excel | Apache POI | 5.2.5 | Invoice XLSX generation |
| PDF generation | iText | 7.x | Invoice PDF generation |
| AI Extraction | Google Gemini Pro | v1beta | Structured ticket data |
| Email | SendGrid / SMTP | — | Invoice delivery |
| File scan | Apache Tika | 2.x | MIME type + malware check |
| Rate limiting | Bucket4j | 8.x | API rate limiting |
| JWT | JJWT | 0.12.x | Token generation/validation |
| Container | Docker | 24.x | Containerisation |
| Orchestration | Kubernetes | 1.29 | Production deployment |
| CI/CD | GitHub Actions | — | Build, test, deploy |
| Monitoring | Prometheus + Grafana | — | Metrics + dashboards |
| Logging | Loki + Grafana | — | Log aggregation |
| Secrets | AWS Secrets Manager | — | Credential management |

---

## 18. Scalability Strategy

### Current Architecture Limits

| Component | Current Config | Safe Limit |
|-----------|---------------|------------|
| Spring Boot pods | 2 | 8 (HPA) |
| PostgreSQL | Single instance | RDS Multi-AZ read replica |
| Redis | Single instance | ElastiCache cluster |
| S3 | Single bucket | No practical limit |

### Scaling Path

**Phase 1 (0–2,000 tickets/month):** Single Spring Boot pod, single PostgreSQL, local MinIO.

**Phase 2 (2,000–20,000 tickets/month):** 2–4 pods behind load balancer. RDS PostgreSQL with daily snapshots. ElastiCache Redis. AWS S3 with CloudFront.

**Phase 3 (20,000+ tickets/month):** PostgreSQL read replica for reports. Separate Spring Batch worker pods. S3 cross-region replication. Consider extracting AI extraction into separate service.

### Stateless Design (enables horizontal scaling)

- No local session state — all sessions in Redis
- No local file storage — all files in S3
- No in-memory caches beyond Redis — Spring Cache backed by Redis
- JWT tokens contain all auth information needed — no DB lookup on every request
- Idempotent upload endpoint — safe to retry on network failure

---

*Document maintained alongside source code. Update when any architectural decision changes.*