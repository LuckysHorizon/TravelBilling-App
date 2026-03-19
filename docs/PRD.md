# TravelBill Pro — Product Requirements Document (PRD)
Application Name Will Be something like RamnetSolutions Billing Generator.

> **Product:** TravelBill Pro — Travel Agency Billing Automation System
> **Version:** 1.0.0
> **Status:** In Review
> **Last Updated:** March 2026
> **Owner:** Product Team

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals & Success Metrics](#3-goals--success-metrics)
4. [User Personas](#4-user-personas)
5. [User Stories](#5-user-stories)
6. [Feature Requirements](#6-feature-requirements)
   - [F-01 Authentication & Access Control](#f-01-authentication--access-control)
   - [F-02 Company Management](#f-02-company-management)
   - [F-03 Ticket Upload & AI Extraction](#f-03-ticket-upload--ai-extraction)
   - [F-04 Ticket Management](#f-04-ticket-management)
   - [F-05 Invoice Generation](#f-05-invoice-generation)
   - [F-06 Email Dispatch](#f-06-email-dispatch)
   - [F-07 Dashboard & Reports](#f-07-dashboard--reports)
   - [F-08 Audit Log](#f-08-audit-log)
   - [F-09 System Configuration](#f-09-system-configuration)
7. [Non-Goals (Explicitly Out of Scope)](#7-non-goals-explicitly-out-of-scope)
8. [Assumptions & Dependencies](#8-assumptions--dependencies)
9. [Constraints](#9-constraints)
10. [Acceptance Criteria](#10-acceptance-criteria)
11. [Release Milestones](#11-release-milestones)
12. [Open Questions](#12-open-questions)
13. [Appendix — Glossary](#13-appendix--glossary)

---

## 1. Executive Summary

TravelBill Pro is an internal billing automation platform built for a travel agency that books flight and bus tickets on behalf of corporate clients. Today, the agency generates monthly invoices manually using Excel — a process that takes 2–3 days per month, introduces arithmetic errors, and leaves no audit trail.

TravelBill Pro eliminates this entirely. Staff upload ticket files (PDF or image), the system extracts all billing data using OCR and AI, staff review and confirm, and the system generates a GST-compliant invoice in one click. The invoice is emailed automatically to the corporate client.

The result: billing that previously took days takes minutes, with zero manual arithmetic and a complete financial audit trail.

---

## 2. Problem Statement

### Current State (Before TravelBill Pro)

```
1. Tickets arrive via email, WhatsApp, or physical printout
2. Billing staff manually type ticket details into Excel row by row
3. Formulas calculate fares — but are error-prone and untested
4. A manager reviews the sheet and approves (or finds errors)
5. Invoice PDF is manually formatted in Word or Excel
6. Emailed to the client from a personal inbox
7. No record of what was sent, when, or who approved it
```

### Pain Points

| Pain Point | Impact | Frequency |
|------------|--------|-----------|
| Manual data entry from ticket PDFs | 2–3 days/month of staff time | Every billing cycle |
| Arithmetic errors in fare totals | Financial loss / client disputes | ~2–3 errors per month |
| Duplicate tickets billed twice | Direct financial loss | Occasional but costly |
| Wrong GST calculation | Compliance risk | When GST rates change |
| No audit trail | Cannot answer "who changed this?" | Whenever disputes arise |
| No visibility into outstanding balances | Cash flow problems | Ongoing |
| Invoice format inconsistency | Unprofessional client experience | Every billing cycle |
| Manual email from personal inbox | No record, no tracking | Every billing cycle |

### Root Cause

The agency has no purpose-built software for billing. Everything runs on Excel, email, and institutional memory. As the client base grows, this approach does not scale and the financial risk increases proportionally.

---

## 3. Goals & Success Metrics

### Primary Goals

| Goal | Metric | Target |
|------|--------|--------|
| Eliminate manual data entry | Time spent on billing per month | < 30 minutes (from 2–3 days) |
| Eliminate billing errors | Arithmetic errors per month | 0 |
| Prevent duplicate billing | Duplicate PNR incidents | 0 |
| GST compliance | GST calculation accuracy | 100% |
| Complete audit trail | % of changes with audit record | 100% |
| Faster invoice delivery | Time from month-end to invoice sent | < 1 hour |

### Secondary Goals

| Goal | Metric | Target |
|------|--------|--------|
| Real-time financial visibility | Dashboard load time | < 2 seconds |
| Staff adoption | % of billing done via system | 100% within 30 days of launch |
| Client satisfaction | Invoice disputes after launch | < 1 per quarter |
| AI extraction accuracy | Fields correctly extracted without manual edit | ≥ 85% |

### Anti-Goals (What We Are Not Optimising For)

- Mobile usage (this is a desktop-first internal tool)
- Speed of development at the cost of financial accuracy
- Maximum feature set in v1 — correctness over completeness

---

## 4. User Personas

### Persona 1 — Priya, Billing Staff

```
Age:        28
Role:       Billing Executive
Tech level: Comfortable with Excel and email; no coding knowledge
Goal:       Get invoices out fast and correctly, without stress
Pain today: Spends the last 3 days of every month manually entering data
            and double-checking totals. Terrified of making mistakes.
Needs from TravelBill Pro:
  - Upload tickets and have data appear automatically
  - Know instantly if something looks wrong
  - Generate and send invoice in a few clicks
  - Not worry about GST calculations
```

### Persona 2 — Rahul, Agency Owner / Manager

```
Age:        44
Role:       Owner and approver
Tech level: Uses WhatsApp, Excel; minimal software experience
Goal:       Know revenue is correct, invoices go out on time, no disputes
Pain today: Has to manually audit Excel sheets for errors each month.
            No visibility into which clients have paid and which haven't.
Needs from TravelBill Pro:
  - Dashboard showing monthly revenue and outstanding balances
  - Confidence that numbers are correct without reviewing every row
  - Know when invoices were sent and whether they were opened
  - Audit trail if a client disputes a charge
```

### Persona 3 — Arjun, System Administrator

```
Age:        32
Role:       IT Admin (part-time responsibility)
Tech level: Technical; can read logs, manage servers, deploy software
Goal:       System runs reliably, is secure, and is easy to maintain
Pain today: No current system to maintain — concern about building one
Needs from TravelBill Pro:
  - Role-based access with proper user management
  - Audit logs for security and compliance
  - Easy backup/restore
  - GST rate can be updated without a code change
```

### Persona 4 — Kavya, External Auditor (Viewer)

```
Age:        38
Role:       Chartered Accountant reviewing agency accounts
Tech level: Moderate; comfortable with accounting software
Goal:       Verify invoices, GST compliance, and billing accuracy
Pain today: Has to ask for Excel sheets and PDF invoices separately
Needs from TravelBill Pro:
  - Read-only access to all tickets, invoices, and reports
  - GST breakdown visible on every invoice
  - Export data to Excel for offline analysis
```

---

## 5. User Stories

### Epic 1: Authentication

| ID | Story | Priority |
|----|-------|----------|
| US-01 | As a staff member, I want to log in with my username and password so that I can access the system securely | P0 |
| US-02 | As any user, I want my session to stay active for 8 hours so that I don't get logged out mid-work | P0 |
| US-03 | As any user, I want to be locked out after 5 failed login attempts so that the system is protected from brute force | P0 |
| US-04 | As an admin, I want to create, edit, and deactivate user accounts so that I control who has access | P0 |
| US-05 | As any user, I want to be able to log out and have my session immediately invalidated | P1 |

### Epic 2: Company Management

| ID | Story | Priority |
|----|-------|----------|
| US-06 | As a billing staff member, I want to add a new corporate client with their GST details and service charge so that I can assign tickets to them | P0 |
| US-07 | As any user, I want to search for companies by name or GST number so that I can find them quickly | P0 |
| US-08 | As a billing staff member, I want to edit a company's details with the change recorded in history | P1 |
| US-09 | As an admin, I want to deactivate a company without deleting their historical data | P1 |
| US-10 | As any user, I want to see a company's total billed amount, outstanding balance, and ticket history on one screen | P1 |

### Epic 3: Ticket Upload & AI Extraction

| ID | Story | Priority |
|----|-------|----------|
| US-11 | As a billing staff member, I want to upload multiple ticket PDFs at once so that I can process a whole day's bookings in one go | P0 |
| US-12 | As a billing staff member, I want the system to automatically extract the PNR, passenger name, route, date, and fare from uploaded tickets so that I don't have to type them manually | P0 |
| US-13 | As a billing staff member, I want to see the original ticket image alongside the extracted data so that I can verify accuracy without switching apps | P0 |
| US-14 | As a billing staff member, I want fields with low AI confidence highlighted so that I know exactly what to double-check | P0 |
| US-15 | As a billing staff member, I want to edit any extracted field before confirming so that I can fix AI errors | P0 |
| US-16 | As a billing staff member, I want the system to alert me immediately if a PNR already exists so that I never bill the same ticket twice | P0 |
| US-17 | As a billing staff member, I want to upload image photos of physical tickets (not just PDFs) so that I can process all ticket formats | P1 |
| US-18 | As a billing staff member, I want to see upload progress per file so that I know the system is working | P1 |

### Epic 4: Ticket Management

| ID | Story | Priority |
|----|-------|----------|
| US-19 | As a billing staff member, I want to view all tickets filtered by company, date, type, and status so that I can find what I need quickly | P0 |
| US-20 | As a billing staff member, I want to manually enter a ticket without uploading a file so that I can handle cases where a physical ticket has no digital version | P1 |
| US-21 | As a billing staff member, I want to edit a ticket before it is billed, with the original values saved | P1 |
| US-22 | As an admin, I want to unlock and edit a billed ticket with a mandatory reason recorded so that corrections can be made with accountability | P1 |
| US-23 | As a billing staff member, I want to soft-delete an unbilled ticket (not a billed one) so that I can remove mistakes | P1 |
| US-24 | As any user, I want to export the filtered ticket list to CSV so that I can analyse it in Excel | P2 |

### Epic 5: Invoice Generation

| ID | Story | Priority |
|----|-------|----------|
| US-25 | As a billing staff member, I want to generate a monthly invoice for a company in one click, covering all approved tickets for that period | P0 |
| US-26 | As any user, I want to preview the invoice before generating it so that I can verify it looks correct | P0 |
| US-27 | As any user, I want to download the invoice as both Excel and PDF so that I can send the format the client prefers | P0 |
| US-28 | As a billing staff member, I want the invoice to include the correct CGST and SGST breakdown so that it is GST compliant | P0 |
| US-29 | As any user, I want invoice numbers to be sequential and never repeat so that they can be used for accounting | P0 |
| US-30 | As an admin, I want to raise a credit note against an existing invoice so that billing corrections are properly documented | P2 |

### Epic 6: Email Dispatch

| ID | Story | Priority |
|----|-------|----------|
| US-31 | As a billing staff member, I want to send an invoice to the client's billing email with one click, with the PDF attached | P0 |
| US-32 | As any user, I want to see when an invoice was sent and to whom so that I have a delivery record | P1 |
| US-33 | As a billing staff member, I want to resend an invoice if it was not received by the client | P1 |
| US-34 | As an admin, I want to customise the invoice email subject and body template | P2 |

### Epic 7: Dashboard & Reports

| ID | Story | Priority |
|----|-------|----------|
| US-35 | As a manager, I want a dashboard showing this month's revenue, ticket count, invoices sent, and pending reviews so that I have a daily overview | P0 |
| US-36 | As a manager, I want a 6-month revenue trend chart so that I can see growth over time | P1 |
| US-37 | As a manager, I want to see outstanding balances per company so that I know who has not paid | P1 |
| US-38 | As any user, I want a per-company spending report with drill-down into individual months | P2 |
| US-39 | As any user, I want to export any report to Excel for offline use | P2 |

### Epic 8: Audit & Compliance

| ID | Story | Priority |
|----|-------|----------|
| US-40 | As an admin, I want every change to a ticket or invoice to be logged with who made the change and when | P0 |
| US-41 | As an auditor, I want read-only access to all tickets, invoices, and audit logs without being able to change anything | P0 |
| US-42 | As an admin, I want the audit log to be non-editable — even by admins — so that it is tamper-evident | P0 |
| US-43 | As an admin, I want to view the full history of any entity (ticket, invoice, company) on its detail page | P1 |

---

## 6. Feature Requirements

### F-01: Authentication & Access Control

#### Description
Secure login system with role-based access control. All sessions are stateless JWT tokens stored in HttpOnly cookies.

#### Roles

| Role | Capabilities |
|------|-------------|
| `ADMIN` | Full access. Manages users, system config, GST rates, email templates. Can unlock billed tickets. Can view all audit logs. |
| `BILLING_STAFF` | Upload tickets, manage companies, confirm tickets, generate invoices, send emails. Cannot manage users or system settings. |
| `VIEWER` | Read-only access to all records, invoices, reports. Cannot create, edit, or delete anything. |

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-01-01 | Login with username + password. Returns JWT access token (8h) + refresh token (7d) in HttpOnly cookies | P0 |
| F-01-02 | JWT refresh: new access token issued silently using refresh cookie. No re-login required within 7 days | P0 |
| F-01-03 | Account lockout: 5 consecutive failed logins → account locked for 30 minutes. Stored in database (survives server restart) | P0 |
| F-01-04 | Logout: refresh token revoked in Redis immediately. Both cookies cleared | P0 |
| F-01-05 | All API endpoints require authentication. All endpoints enforce role-based access at method level | P0 |
| F-01-06 | Admin: Create, edit, deactivate users. Deactivated users cannot log in but their records are preserved | P0 |
| F-01-07 | Password requirements: minimum 10 characters, must include uppercase, lowercase, digit, and special character | P1 |
| F-01-08 | All login attempts (success and failure) logged with user, IP, timestamp, and outcome | P1 |

---

### F-02: Company Management

#### Description
Full CRUD management of corporate client companies. Each company has its own service charge rate, billing cycle, and GST details.

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-02-01 | Create company: name, GST number (validated format), billing email, address, service charge %, billing cycle (monthly/weekly), credit limit (optional) | P0 |
| F-02-02 | GST number must be unique across all companies. Attempt to save duplicate GST returns a clear error | P0 |
| F-02-03 | List companies: paginated, searchable by name and GST number, filterable by active/inactive | P0 |
| F-02-04 | Company detail page: overview stats (total billed YTD, tickets this month, outstanding balance) + ticket history tab + invoice history tab | P0 |
| F-02-05 | Edit company: all fields editable. Before/after values logged in audit trail. Changes only affect future invoices — past invoices are immutable | P1 |
| F-02-06 | Deactivate company (soft delete): all historical data preserved. Company no longer appears in active dropdowns | P1 |
| F-02-07 | Hard delete: restricted to ADMIN. Requires confirmation dialog. Blocked if company has any tickets or invoices | P2 |
| F-02-08 | Outstanding balance alert: if a company's unpaid invoices exceed their credit limit, show a warning banner on their detail page | P2 |

---

### F-03: Ticket Upload & AI Extraction

#### Description
The core differentiating feature. Staff upload ticket files; the system extracts all billing data automatically using OCR and AI, then presents it for human review before saving.

#### File Support

| Format | Processing Method |
|--------|-----------------|
| PDF (digital/text-based) | Apache PDFBox — extracts text layer directly |
| PDF (scanned/image-based) | PDFBox detects empty text layer → Tesseract OCR |
| PNG, JPG, JPEG | Tesseract OCR with image pre-processing |
| TIFF | Tesseract OCR |

**Limits:** Max 10MB per file. Up to 50 files per batch upload.

#### AI Extracted Fields

| Field | Source | Fallback if Not Found |
|-------|--------|-----------------------|
| PNR number | OCR + AI | `null` (manual entry required) |
| Passenger name | OCR + AI | `null` |
| Travel date | OCR + AI | `null` |
| Origin city/airport | OCR + AI | `null` |
| Destination city/airport | OCR + AI | `null` |
| Base fare (₹) | OCR + AI | `null` |
| Taxes (₹) | OCR + AI | `0` |
| Total amount (₹) | OCR + AI | `null` |
| Ticket type | OCR + AI | FLIGHT (default) |
| Operator name | OCR + AI | `null` |
| Seat number | OCR + AI | `null` (optional) |

#### Confidence Threshold Rules

| Confidence | Field Treatment | User Action |
|------------|----------------|-------------|
| ≥ 85% | Normal field — green confidence bar | Optional to review |
| 60–84% | Amber border + amber bar + "please verify" label | Should verify |
| < 60% | Amber border + amber bar + ⚠ warning | **Must verify** — Confirm button blocked until field is manually edited |

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-03-01 | Drag-and-drop upload zone accepting PDF, PNG, JPG, JPEG, TIFF. Max 10MB per file, 50 files per batch | P0 |
| F-03-02 | Upload processing is asynchronous. UI shows per-file progress (Queued → OCR → Extracting → Done / Error). User can navigate away | P0 |
| F-03-03 | File type verification via MIME type (Apache Tika) — not just file extension. Password-protected PDFs return a clear error | P0 |
| F-03-04 | OCR pipeline: PDFBox for digital PDFs; Tesseract (eng+hin) for images and scanned PDFs. Auto-rotate, deskew, contrast pre-processing applied to images | P0 |
| F-03-05 | Raw OCR text sent to Gemini Pro with structured extraction prompt. Response parsed to typed fields with per-field confidence scores | P0 |
| F-03-06 | Review & Confirm screen: original ticket (image or PDF viewer) displayed on left. Extracted fields (all editable) displayed on right. Confidence bar per field | P0 |
| F-03-07 | Confirm button is disabled if any field has confidence < 60% AND has not been manually edited | P0 |
| F-03-08 | Duplicate PNR check on PNR field blur. If PNR already exists in DB, show inline warning with link to the existing ticket | P0 |
| F-03-09 | Company selector at top of upload screen. All files in a batch are assigned to the selected company | P0 |
| F-03-10 | Original uploaded file stored permanently in S3, linked to the ticket record | P0 |
| F-03-11 | If Gemini API is unavailable, fall back to regex extraction. All fields in fallback mode capped at 60% confidence and flagged for review | P1 |
| F-03-12 | Batch status polling: GET endpoint returns extraction status per file. Frontend polls every 3 seconds until batch is complete | P1 |

---

### F-04: Ticket Management

#### Description
Full lifecycle management of individual tickets from creation through billing and payment.

#### Ticket Status Lifecycle

```
PENDING_REVIEW → APPROVED → BILLED → PAID
                    ↓
                (DELETED — soft, APPROVED only)
```

| Status | Meaning | Who Can Change It |
|--------|---------|------------------|
| `PENDING_REVIEW` | Uploaded, awaiting human confirmation | BILLING_STAFF → APPROVED via Confirm action |
| `APPROVED` | Confirmed, ready for billing | BILLING_STAFF → BILLED via Invoice generation |
| `BILLED` | Included in a generated invoice | ADMIN only (with justification) → back to APPROVED |
| `PAID` | Payment received and recorded | BILLING_STAFF → PAID via manual status update |

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-04-01 | Ticket list: paginated table with sort and filter by company, status, ticket type, travel date range, and text search on PNR or passenger name | P0 |
| F-04-02 | Each status transition is logged in audit_logs with the user, timestamp, and reason (reason mandatory for BILLED → APPROVED reversal) | P0 |
| F-04-03 | Edit ticket (APPROVED status): all fields editable. Old values saved in audit log before save | P1 |
| F-04-04 | Edit billed ticket: blocked for BILLING_STAFF. ADMIN can unlock with a mandatory justification field. Justification stored in audit log | P1 |
| F-04-05 | Soft delete: available for PENDING_REVIEW and APPROVED tickets only. Deleted tickets hidden from all views but preserved in DB | P1 |
| F-04-06 | Manual ticket entry: form with all required fields as alternative to file upload | P1 |
| F-04-07 | Ticket detail slide-over panel: clicking a PNR opens a panel without leaving the list view. Shows all fields, status history, and link to original ticket file | P1 |
| F-04-08 | Export filtered ticket list to CSV. Export respects all active filters | P2 |
| F-04-09 | Bulk approve: select multiple PENDING_REVIEW tickets → bulk approve action | P2 |
| F-04-10 | Bulk company assign: select multiple tickets → assign to a different company (APPROVED status only) | P2 |

---

### F-05: Invoice Generation

#### Description
One-click generation of a GST-compliant invoice covering all APPROVED tickets for a given company and billing period. Invoices are immutable once generated.

#### Invoice Contents

| Section | Contents |
|---------|----------|
| Header | Agency name, GSTIN, address, logo |
| Bill To | Company name, GSTIN, address |
| Invoice Details | Invoice number, billing period, generated date, due date |
| Ticket Table | PNR, passenger, route, travel date, type (FLIGHT/BUS), base fare |
| Charges Summary | Subtotal, service charge, CGST (9%), SGST (9%) |
| Grand Total | Final payable amount in large text |
| Footer | Payment terms, bank details, authorised signatory |

#### GST Calculation

```
Service Charge  = Base Fare Total × company.service_charge_pct / 100
CGST            = Service Charge × cgst_rate / 100    (default 9%)
SGST            = Service Charge × sgst_rate / 100    (default 9%)
Grand Total     = Base Fare Total + Service Charge + CGST + SGST
```

All rates are pulled from `gst_config` table using the rate effective on the invoice date.

#### Invoice Number Format

```
TBP / YYYY-YY / NNNNN
TBP/2026-27/00001   ← First invoice of FY 2026-27
TBP/2026-27/00142   ← 142nd invoice
TBP/2027-28/00001   ← Resets on April 1 each year
```

Generated atomically using `SELECT FOR UPDATE` — guaranteed unique, no gaps, no race conditions.

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-05-01 | Generate invoice for a selected company + billing month. Only APPROVED tickets in that period are included | P0 |
| F-05-02 | Invoice preview screen shows the complete invoice (matching final output) before generation is confirmed | P0 |
| F-05-03 | Invoice generation creates: (a) styled Excel XLSX via Apache POI, (b) formatted PDF via iText. Both stored in S3 | P0 |
| F-05-04 | GST calculated using rates from gst_config table effective on invoice date. Service charge rate from company record | P0 |
| F-05-05 | Invoice numbers are sequential per financial year and unique across the system. Generated within a DB transaction (SELECT FOR UPDATE) | P0 |
| F-05-06 | Once generated, invoice is immutable. All fields (totals, ticket list, GST) are stored — never recalculated on read | P0 |
| F-05-07 | All tickets included in the invoice have their status changed to BILLED in the same DB transaction as invoice creation | P0 |
| F-05-08 | If an invoice already exists for the same company + billing month, return error 409 with link to existing invoice | P0 |
| F-05-09 | Download invoice as Excel or PDF via pre-signed S3 URL (15-minute expiry) | P0 |
| F-05-10 | Credit note: ADMIN can create a credit/debit note linked to an existing invoice. Credit note has its own sequential number | P2 |

---

### F-06: Email Dispatch

#### Description
Automated delivery of invoices to corporate client billing contacts, with delivery tracking and retry capability.

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-06-01 | Send invoice email: PDF attached, to company's billing_email, CC to agency admin email | P0 |
| F-06-02 | Email confirmation modal: shows recipient address and CC before sending. Requires explicit confirm click | P0 |
| F-06-03 | On send: invoice status updated to SENT. email_sent_at timestamp recorded | P0 |
| F-06-04 | Email delivery tracking via SendGrid webhooks. Statuses (delivered, bounced, opened) stored and visible on invoice detail | P1 |
| F-06-05 | Resend: available from invoice detail screen. Does not change invoice number or content | P1 |
| F-06-06 | Email retry: if delivery fails, retry 3 times with exponential backoff (1min, 5min, 30min). Failure after all retries triggers admin notification | P1 |
| F-06-07 | Email templates: ADMIN can edit subject and body template via system config screen. Template supports variables: `{invoiceNumber}`, `{companyName}`, `{billingMonth}`, `{grandTotal}`, `{dueDate}` | P2 |
| F-06-08 | Payment reminder email: configurable to trigger automatically N days after invoice is SENT if still not PAID | P3 |

---

### F-07: Dashboard & Reports

#### Description
Real-time financial overview for the agency owner and management. Read-only reporting accessible to all roles.

#### Dashboard Metrics

| Metric | Description | Refresh |
|--------|-------------|---------|
| Revenue (MTD) | Sum of all invoiced amounts in current month | 5 min |
| Tickets Processed | Count of APPROVED + BILLED + PAID tickets this month | 5 min |
| Invoices Sent | Count of invoices with status SENT or PAID this month | 5 min |
| Pending Review | Count of tickets in PENDING_REVIEW status | 5 min |
| Outstanding Balance | Sum of all SENT invoices not yet PAID | 5 min |

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-07-01 | Dashboard: 5 KPI metric cards + 6-month revenue bar chart + recent tickets table (5 rows) + pending review alert banner | P0 |
| F-07-02 | Dashboard loads in < 2 seconds. Skeleton screens shown during load | P0 |
| F-07-03 | Pending Review banner: shown only if count > 0. Disappears when all tickets are reviewed | P1 |
| F-07-04 | Revenue chart: last 6 months. Current month highlighted in gold. Hoverable bars showing exact value | P1 |
| F-07-05 | Outstanding balance: per-company table with overdue flag if invoice > 30 days unpaid | P1 |
| F-07-06 | Reports page: date range picker (default: current FY). Filter by company, ticket type | P2 |
| F-07-07 | Per-company report: all invoices, total spend, payment history for a selected company + date range | P2 |
| F-07-08 | Export any report view to Excel | P2 |
| F-07-09 | Ticket volume by type (FLIGHT vs BUS) — pie/donut chart | P3 |

---

### F-08: Audit Log

#### Description
Immutable, tamper-evident record of every significant action in the system. Required for financial compliance and dispute resolution.

#### Events Logged

| Category | Events |
|----------|--------|
| Authentication | Login success, login failure, logout, account lockout |
| Tickets | Created, updated (old → new values), status changed, deleted, unlocked |
| Invoices | Generated, downloaded, emailed, status changed, credit note created |
| Companies | Created, updated, deactivated |
| Users | Created, role changed, deactivated, password reset |
| System Config | GST rate changed, email template edited |

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-08-01 | Every financial record change (ticket edit, invoice action) automatically creates an audit_logs row with entity type, entity ID, action, old value (JSONB), new value (JSONB), user ID, IP address, timestamp | P0 |
| F-08-02 | Audit logs are append-only. No API endpoint exists to edit or delete audit records — not even for ADMIN | P0 |
| F-08-03 | Audit log viewer: ADMIN can view all logs, filterable by entity type, user, date range, and action | P1 |
| F-08-04 | Entity audit history: on any ticket, invoice, or company detail page, an "Audit Log" tab shows the full history of changes to that specific record | P1 |
| F-08-05 | Financial audit log retention: minimum 7 years. Authentication log retention: minimum 5 years | P0 |
| F-08-06 | VIEWER role can access audit logs (read-only) for compliance review purposes | P1 |

---

### F-09: System Configuration

#### Description
Admin-only settings that control system-wide behaviour without requiring code changes or redeployment.

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F-09-01 | GST rate management: ADMIN can add a new GST rate record with an effective_from date. Invoice generation automatically uses the rate effective on the invoice date | P0 |
| F-09-02 | Agency settings: agency name, GSTIN, address, logo (used in invoice header). Editable by ADMIN | P0 |
| F-09-03 | Email templates: ADMIN can edit subject and body for invoice_sent and payment_reminder templates | P2 |
| F-09-04 | Invoice prefix: ADMIN can change the invoice number prefix (default: TBP). Change only affects future invoices | P2 |
| F-09-05 | SMTP settings: configure outbound email server (host, port, username, password). Stored encrypted | P1 |

---

## 7. Non-Goals (Explicitly Out of Scope)

The following are explicitly **not** being built in v1.0. They may be considered for future versions.

| Feature | Reason Not In Scope |
|---------|---------------------|
| Online ticket booking / GDS integration | Out of agency's business model for this tool |
| Payment gateway / online collections | Handled via bank transfer outside the system |
| Customer-facing portal for corporate clients | v1 is internal-only |
| Mobile native app (iOS / Android) | Desktop-first; staff use desktops |
| Multi-currency support | Agency operates only in INR |
| Automated reconciliation with bank statements | Too complex for v1 |
| Airline / bus operator API integration | Tickets come via email/manual means |
| WhatsApp / SMS notifications to clients | Out of scope for v1 |
| Multi-branch / multi-agency support | Single agency use case |
| Inventory or seat management | Not a booking system |

---

## 8. Assumptions & Dependencies

### Assumptions

| # | Assumption |
|---|------------|
| A-01 | All ticket files are in one of the supported formats: PDF, PNG, JPG, JPEG, TIFF |
| A-02 | Ticket PDFs are single-ticket documents. Multi-ticket booking PDFs may require manual splitting |
| A-03 | All monetary values are in Indian Rupees (INR). No foreign currency |
| A-04 | GST applies as CGST + SGST (intra-state). IGST (inter-state) not required in v1 |
| A-05 | The agency has a stable internet connection for Gemini Pro API calls |
| A-06 | Corporate clients are billed monthly. Weekly billing is supported but monthly is the primary use case |
| A-07 | The agency has an AWS account (or will use MinIO locally) for S3-compatible file storage |
| A-08 | Staff use the system on desktop browsers (Chrome 120+, Firefox 120+, Edge 120+) |
| A-09 | OCR accuracy on well-printed tickets will be ≥ 90%. Handwritten or very low-quality scans may require manual entry |
| A-10 | Gemini Pro API will be available ≥ 99% of the time. Fallback regex extraction covers outages |

### External Dependencies

| Dependency | Purpose | Risk if Unavailable |
|------------|---------|---------------------|
| Google Gemini Pro API | AI ticket data extraction | Fallback to regex (lower accuracy). Manual review covers the gap |
| AWS S3 / MinIO | File storage for tickets and invoices | Cannot save uploads or serve invoice downloads |
| SendGrid / SMTP server | Invoice email delivery | Invoices can be downloaded and emailed manually |
| PostgreSQL 15 | Primary data store | System cannot function |
| Redis 7 | Session management + rate limiting | System cannot function |

---

## 9. Constraints

### Technical Constraints

| Constraint | Detail |
|------------|--------|
| Language | Java 21 + Spring Boot 3.x for backend. React 18 + TypeScript for frontend |
| Database | PostgreSQL 15. No NoSQL stores for financial data |
| All money as DECIMAL | Never `float` or `double` for financial amounts — `DECIMAL(10,2)` in DB, `BigDecimal` in Java |
| No client-side financial logic | All fare and GST calculations happen server-side only |
| Immutable invoices | Once an invoice is generated, its amounts and ticket list cannot be changed. Corrections via credit note only |
| Audit log is append-only | No delete or update allowed on audit_logs table at any level |

### Regulatory Constraints

| Constraint | Detail |
|------------|--------|
| GST compliance | All invoices must include GSTIN of both agency and client, CGST/SGST breakdown, HSN/SAC code |
| Financial record retention | 7 years minimum for all invoice and ticket records (Income Tax Act requirement) |
| Data privacy | No client PAN or Aadhaar stored. Employee passwords hashed with bcrypt (never plaintext or reversible encryption) |

### Business Constraints

| Constraint | Detail |
|------------|--------|
| Budget | Internal tool — no paid SaaS features; open-source dependencies preferred |
| Timeline | Must be live before start of FY 2026-27 (April 1, 2026) |
| Training | System must be learnable by non-technical billing staff in < 1 day |
| Downtime | Maximum acceptable downtime: 4 hours/month |

---

## 10. Acceptance Criteria

The product is considered ready for production release when ALL of the following are met:

### Financial Accuracy (P0 — must pass 100%)

- [ ] Generate an invoice for a test company with 50 tickets. Grand total must match manual calculation to the last paisa
- [ ] Change a company's service charge rate. Verify new invoices use the new rate and old invoices are unchanged
- [ ] Upload a ticket with a PNR that already exists. Verify the system blocks the save and shows a clear error
- [ ] Upload the same ticket file twice. Verify duplicate PNR is detected and blocked

### AI Extraction (P0 — must pass for UAT sign-off)

- [ ] Upload 50 real ticket PDFs (mix of airlines and bus operators). AI extraction accuracy ≥ 85% of fields without manual correction
- [ ] Upload a low-quality scanned ticket image. System must flag low-confidence fields — must not silently accept wrong data
- [ ] Simulate Gemini API failure. System must fall back to regex and flag all fields for review

### Security (P0 — must pass all)

- [ ] Attempt 5 failed logins. Account must lock. Verify it cannot be logged into for 30 minutes
- [ ] Log in as BILLING_STAFF. Attempt to access `/api/users` (admin-only). Must receive 403
- [ ] Log out. Attempt to use the old refresh token. Must receive 401
- [ ] Log in as VIEWER. Attempt to create a company via API. Must receive 403
- [ ] Attempt SQL injection in the PNR search field. Must be blocked — no query error, no data leak

### Invoice Integrity (P0 — must pass all)

- [ ] Generate an invoice. Attempt to edit its grand total via API. Must be rejected
- [ ] Generate an invoice for March 2026. Attempt to generate another invoice for the same company + March 2026. Must receive 409 error
- [ ] Generate two invoices simultaneously (race condition test). Both must get unique sequential invoice numbers

### Audit Trail (P0 — must pass all)

- [ ] Edit a ticket. Verify audit_logs contains the old and new values, user ID, and timestamp
- [ ] Attempt to delete an audit log entry via API (even as ADMIN). Must be blocked
- [ ] Generate an invoice, send it, mark it paid. Verify all three state transitions appear in audit log

### Performance (P1 — must pass for launch)

- [ ] Dashboard loads in < 2 seconds with 500 tickets and 50 companies in the database
- [ ] Generate an invoice for 200 tickets in < 10 seconds
- [ ] Upload and process 10 ticket PDFs simultaneously — all complete within 5 minutes

### UAT Sign-Off (required)

- [ ] Billing staff (Priya persona) can complete the full workflow — upload → review → confirm → generate invoice → send email — without any guidance after initial training
- [ ] Agency owner (Rahul persona) can read the dashboard and understand the financial status without explanation
- [ ] All critical P0 features demonstrated working in staging environment

---

## 11. Release Milestones

### v1.0 — Initial Release (Target: April 1, 2026)

**Included:**
- All F-01 (Auth) requirements
- All F-02 (Company Management) requirements
- All F-03 (Ticket Upload & AI) P0 requirements
- All F-04 (Ticket Management) P0 requirements
- All F-05 (Invoice Generation) P0 requirements
- F-06-01 to F-06-03 (basic email send)
- F-07-01 to F-07-03 (basic dashboard)
- All F-08 (Audit Log) P0 requirements
- F-09-01, F-09-02 (GST config + agency settings)

**Not included in v1.0:**
- Credit notes (F-05-10)
- Email templates editor (F-06-07)
- Payment reminder emails (F-06-08)
- Full reports page (F-07-06 onwards)
- Bulk ticket actions (F-04-09, F-04-10)

### v1.1 — Post-Launch Stabilisation (Target: May 2026)

- Bug fixes from v1.0 UAT feedback
- F-03-11 (AI fallback) hardening
- F-06-04 to F-06-06 (email delivery tracking and retry)
- F-07-05 (outstanding balance per company)
- F-04-07 (ticket detail slide-over)

### v1.2 — Reports & Polish (Target: July 2026)

- Full reports page (F-07-06 to F-07-09)
- Credit notes (F-05-10)
- Email templates editor (F-06-07)
- Bulk ticket actions (F-04-09, F-04-10)
- Export improvements (CSV, Excel)

### v2.0 — Scale & Automation (Target: Q4 2026)

- Payment reminder automation
- Multi-branch support (if needed)
- Deeper analytics
- API access for future integrations

---

## 12. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| OQ-01 | Should the system support IGST (inter-state) billing in addition to CGST+SGST? This affects invoice template and calculation logic | Finance team | **Open** |
| OQ-02 | What is the preferred GST invoice format — do clients require a specific layout or HSN/SAC code format? | Client-facing team | **Open** |
| OQ-03 | Should staff be able to assign different service charge rates per ticket type (FLIGHT vs BUS) or is the company-level rate sufficient? | Agency Owner | **Open** |
| OQ-04 | Does the agency want to store client payment method / bank details in the system, or is payment tracking simply a manual status update? | Agency Owner | **Open** |
| OQ-05 | For bulk uploads: should all files in a batch be assigned to one company, or should the system try to auto-detect the company from the ticket content? | Billing Staff | **Open** |
| OQ-06 | What happens to tickets uploaded for a month that has already been invoiced? Should they be rejected, or held for the next billing cycle? | Agency Owner | **Open** |
| OQ-07 | Is MinIO (self-hosted S3) acceptable for production, or is AWS S3 required? This affects initial infrastructure cost | IT Admin | **Open** |
| OQ-08 | Should the system send a Slack or WhatsApp notification (in addition to email) when an invoice is generated? | Agency Owner | **Open** |
| OQ-09 | What is the agreed-upon payment terms period to put on invoices (e.g., Net 15, Net 30)? | Agency Owner | **Open** |
| OQ-10 | Who should receive email notifications when a billing staff member makes a high-risk action (e.g., unlocking a billed ticket)? | Agency Owner | **Open** |

---

## 13. Appendix — Glossary

| Term | Definition |
|------|------------|
| **PNR** | Passenger Name Record. The unique alphanumeric code (typically 6 characters) that identifies a booking with an airline or bus operator. Example: `6X9QR2` |
| **OCR** | Optical Character Recognition. Technology that converts images of printed text into machine-readable text. Used to extract data from scanned ticket images |
| **AI Extraction** | The process of sending raw OCR text to Google Gemini Pro and receiving structured JSON data (passenger name, fare, route, etc.) in return |
| **Confidence Score** | A 0–100 score assigned by the AI to each extracted field, indicating how certain the AI is about the extracted value. Fields below 85% are flagged for human review |
| **GST** | Goods and Services Tax. The indirect tax applied to the agency's service charges |
| **CGST** | Central Goods and Services Tax. The central government's portion of GST (typically 9% of the taxable amount) |
| **SGST** | State Goods and Services Tax. The state government's portion of GST (typically 9% of the taxable amount) |
| **IGST** | Integrated GST. Applied on inter-state transactions instead of CGST+SGST. Not in scope for v1.0 |
| **Service Charge** | The agency's fee, expressed as a percentage of the base fare. Configured per company. Example: 5% of base fare |
| **Base Fare** | The ticket price before any taxes or service charges |
| **Grand Total** | The final amount payable by the corporate client: Base Fare + Service Charge + CGST + SGST |
| **Invoice Number** | A sequential, unique identifier for each invoice. Format: `TBP/YYYY-YY/NNNNN`. Resets each financial year |
| **Financial Year** | April 1 to March 31 (Indian financial year). Invoice sequences reset on April 1 |
| **Credit Note** | A document issued to correct a billing error on an already-generated invoice. Cannot modify the original invoice — the credit note is a separate linked document |
| **Billing Cycle** | How often a company is invoiced: `MONTHLY` or `WEEKLY` |
| **Soft Delete** | Marking a record as inactive/deleted without physically removing it from the database. Historical data is preserved for audit purposes |
| **Audit Log** | An append-only record of every significant action in the system, including who did it, when, and what changed |
| **RBAC** | Role-Based Access Control. A security model where permissions are assigned to roles (ADMIN, BILLING_STAFF, VIEWER) rather than individual users |
| **JWT** | JSON Web Token. A signed token used for stateless authentication. Stored in HttpOnly cookies |
| **HttpOnly Cookie** | A browser cookie that cannot be accessed by JavaScript. Used to store JWT tokens securely, preventing XSS attacks |
| **Pre-signed URL** | A temporary URL for a private S3 file that grants time-limited access (15 minutes). Used for secure invoice downloads without exposing the S3 bucket |
| **MinIO** | An open-source, self-hosted alternative to AWS S3, compatible with the S3 API. Used for development and on-premises deployments |
| **HikariCP** | A high-performance JDBC connection pool library for Java. Manages the pool of database connections |
| **Flyway** | A database migration tool. Manages PostgreSQL schema changes as versioned SQL files |
| **Spring Batch** | A Spring framework module for processing large volumes of records asynchronously. Used for the OCR + AI extraction pipeline |
| **Bucket4j** | A Java rate-limiting library that integrates with Redis to enforce per-user and per-IP request limits across multiple server instances |

---

*This document is the single source of truth for TravelBill Pro v1.0 product requirements. All feature development, testing, and acceptance decisions reference this document.*