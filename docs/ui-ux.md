# TravelBill Pro — UI/UX Design Document
> **Version:** 1.0.0 | **Theme:** Premium White | **Framework:** React 18 + Ant Design | **Standard:** WCAG 2.1 AA

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Design Tokens — Colors](#2-design-tokens--colors)
3. [Design Tokens — Spacing & Radius](#3-design-tokens--spacing--radius)
4. [Typography System](#4-typography-system)
5. [Component Library — Buttons](#5-component-library--buttons)
6. [Component Library — Badges & Status](#6-component-library--badges--status)
7. [Component Library — Form Fields](#7-component-library--form-fields)
8. [Component Library — Metric Cards](#8-component-library--metric-cards)
9. [Screen Specifications](#9-screen-specifications)
   - [9.1 Dashboard](#91-dashboard)
   - [9.2 Ticket Upload](#92-ticket-upload)
   - [9.3 Review & Confirm](#93-review--confirm)
   - [9.4 Ticket List](#94-ticket-list)
   - [9.5 Invoice Preview](#95-invoice-preview)
   - [9.6 Company Detail](#96-company-detail)
   - [9.7 Reports](#97-reports)
10. [Layout Grid](#10-layout-grid)
11. [Motion & Animation](#11-motion--animation)
12. [States & Feedback Patterns](#12-states--feedback-patterns)
13. [Accessibility Standards](#13-accessibility-standards)
14. [Responsive Breakpoints](#14-responsive-breakpoints)

---

## 1. Design Philosophy

TravelBill Pro follows a **Refined White** aesthetic — the visual language of premium financial software. Every decision prioritises legibility of numbers, clarity of financial data, and zero ambiguity in status communication.

### Core Principles

| Principle | Description |
|-----------|-------------|
| **Clarity First** | Financial data must be unambiguous. Amounts, PNRs, and statuses are always rendered in high-contrast, monospaced or serif typefaces |
| **Efficiency** | Primary actions (Upload, Generate Invoice, Confirm) reachable in ≤ 2 clicks from any screen |
| **Error Prevention** | Confirmation dialogs for all irreversible actions. Inline validation fires on blur, not on submit |
| **Trust Through Polish** | Premium white backgrounds, fine 1px borders, and generous whitespace signal reliability — critical for a financial tool |
| **Purposeful Motion** | Animations communicate system state (loading, saving, success). Never decorative |

### What This System Is NOT

- Not a dark-mode-first design (light/professional is intentional)
- Not dense/data-grid heavy (readability over information density)
- Not icon-only navigation (labels always accompany icons)

---

## 2. Design Tokens — Colors

All colors are defined as CSS custom properties and consumed via Tailwind config or Ant Design theme tokens.

```css
:root {
  /* ── Background ── */
  --color-bg-page:        #FAFAF9;   /* Page background — warm off-white */
  --color-bg-surface:     #F5F4F1;   /* Card backgrounds, table alternates */
  --color-bg-white:       #FFFFFF;   /* Pure white — modals, inputs */

  /* ── Text ── */
  --color-ink-primary:    #111110;   /* Headings, primary content */
  --color-ink-secondary:  #3D3C38;   /* Body text */
  --color-ink-muted:      #6B6860;   /* Labels, metadata */
  --color-ink-placeholder:#9E9B94;   /* Placeholder, disabled */

  /* ── Borders ── */
  --color-border-default: #E8E6E0;   /* Standard borders */
  --color-border-strong:  #D4D0C8;   /* Hover borders, focused inputs */

  /* ── Brand Accent ── */
  --color-gold:           #B8860B;   /* Primary brand accent */
  --color-gold-light:     #F5DFA0;   /* Gold backgrounds */
  --color-gold-medium:    #D4A843;   /* Gold mid-tone */
  --color-gold-xl:        #FBF3DC;   /* Gold wash backgrounds */

  /* ── Semantic — Info / Flight ── */
  --color-blue:           #1A4FA0;
  --color-blue-light:     #EBF0FA;

  /* ── Semantic — Success / Approved ── */
  --color-teal:           #0B6E5A;
  --color-teal-light:     #E5F4EF;

  /* ── Semantic — Error / Overdue ── */
  --color-red:            #9B1C1C;
  --color-red-light:      #FEF2F2;

  /* ── Semantic — Warning / Pending ── */
  --color-amber:          #92400E;
  --color-amber-light:    #FFFBEB;
}
```

### Color Usage Rules

```
Page background   →  --color-bg-page       (#FAFAF9)
Cards & panels    →  --color-bg-white      (#FFFFFF) with 1px --color-border-default
Table alt rows    →  --color-bg-surface    (#F5F4F1)
Primary buttons   →  --color-ink-primary   (#111110) bg, white text
Brand CTA buttons →  --color-gold          (#B8860B) bg, white text
Sidebar / Nav     →  --color-ink-primary   (#111110) bg
```

### Semantic Color Matrix

| State | Background | Text | Border |
|-------|-----------|------|--------|
| Approved / Success | `#E5F4EF` | `#0B6E5A` | `#9ADEC9` |
| Pending / Warning | `#FFFBEB` | `#92400E` | `#FCD34D` |
| Billed / Info | `#EBF0FA` | `#1A4FA0` | `#B8CCF0` |
| Overdue / Error | `#FEF2F2` | `#9B1C1C` | `#FCA5A5` |
| Paid | `#F0FDF4` | `#166534` | `#A7F3D0` |
| Draft / Neutral | `#F5F4F1` | `#6B6860` | `#E8E6E0` |
| Flight badge | `#EBF0FA` | `#1A4FA0` | — |
| Bus badge | `#E5F4EF` | `#0B6E5A` | — |

---

## 3. Design Tokens — Spacing & Radius

### Border Radius Scale

```css
--radius-xs:   4px;    /* Input fields */
--radius-sm:   6px;    /* Small tags, inline elements */
--radius-md:   8px;    /* Buttons, small cards */
--radius-lg:   12px;   /* Main cards, panels */
--radius-xl:   16px;   /* Modals, bottom sheets */
--radius-pill: 9999px; /* Badges, status chips */
```

### Spacing Scale (8px baseline grid)

```
4px   → Icon internal padding
8px   → Inline element gaps, compact list items
12px  → Component internal padding (small)
16px  → Component internal padding (standard)
20px  → Card padding (compact)
24px  → Card padding (standard), section gaps
32px  → Between sections within a page
40px  → Major section separation
48px  → Page-level horizontal padding
64px  → Large visual breathing room
```

### Border Weights

```
1px   → Standard card & container borders
0.5px → Dividers within components, table row separators
2px   → Focus ring offset, "featured" card accent
```

---

## 4. Typography System

### Typeface Roles

| Font | Role | Weights Used |
|------|------|-------------|
| **Playfair Display** | Display headings, financial figures, invoice amounts | 400 (regular), 600 (semibold), 400 italic |
| **Plus Jakarta Sans** | All UI text — labels, body, buttons, navigation | 300, 400, 500, 600 |
| **JetBrains Mono** | PNR numbers, invoice IDs, amounts in tables, code | 400, 500 |

### Type Scale

```css
/* Display — Playfair Display */
.text-display    { font-family: 'Playfair Display'; font-size: 36px; line-height: 1.1; }
/* Used for: Dashboard totals (₹18.4L), invoice grand total */

.text-heading-1  { font-family: 'Playfair Display'; font-size: 28px; line-height: 1.2; }
/* Used for: Page titles, section headings */

.text-heading-2  { font-family: 'Playfair Display'; font-size: 22px; line-height: 1.25; }
/* Used for: Card headings, modal titles */

/* UI — Plus Jakarta Sans */
.text-ui-heading { font-family: 'Plus Jakarta Sans'; font-size: 15px; font-weight: 600; line-height: 1.4; }
/* Used for: Table column names, panel titles */

.text-body       { font-family: 'Plus Jakarta Sans'; font-size: 13px; font-weight: 400; line-height: 1.6; }
/* Used for: Primary body text, descriptions */

.text-small      { font-family: 'Plus Jakarta Sans'; font-size: 12px; font-weight: 400; line-height: 1.5; }
/* Used for: Table data, secondary labels */

.text-tiny       { font-family: 'Plus Jakarta Sans'; font-size: 11px; font-weight: 400; line-height: 1.5; }
/* Used for: Captions, hints, metadata */

.text-label      { font-family: 'Plus Jakarta Sans'; font-size: 10px; font-weight: 600;
                   text-transform: uppercase; letter-spacing: 0.1em; }
/* Used for: Section eyebrows, column headers */

/* Mono — JetBrains Mono */
.text-pnr        { font-family: 'JetBrains Mono'; font-size: 13px; font-weight: 500; color: #1A4FA0; }
/* Used for: PNR numbers in all contexts */

.text-amount     { font-family: 'JetBrains Mono'; font-size: 13px; font-weight: 500; }
/* Used for: Fare amounts in tables */

.text-invoice-id { font-family: 'JetBrains Mono'; font-size: 11px; font-weight: 400; color: #6B6860; }
/* Used for: Invoice numbers, GST numbers */
```

### Typography Rules

- **Financial amounts** always use `Playfair Display` for display size, `JetBrains Mono` in tables
- **PNR numbers** always use `JetBrains Mono` + `--color-blue` regardless of context
- **Never use Inter, Roboto, or system fonts** — the typeface choices are load-bearing for brand feel
- **Line length** for body text: max 72 characters (640px at 13px)
- **Numbers in tables**: right-aligned, monospaced, fixed decimal places

---

## 5. Component Library — Buttons

### Variants

```tsx
// Primary — dark ink background
<Button variant="primary">Generate Invoice</Button>
// Background: #111110 | Text: #FFFFFF | Border: #111110
// Hover: background #333333

// Secondary — white with border
<Button variant="secondary">Edit Ticket</Button>
// Background: #FFFFFF | Text: #111110 | Border: #D4D0C8
// Hover: background #F5F4F1

// Brand / CTA — gold
<Button variant="gold">Confirm & Save</Button>
// Background: #B8860B | Text: #FFFFFF | Border: #B8860B
// Hover: background #996F09

// Ghost — no background
<Button variant="ghost">Cancel</Button>
// Background: transparent | Text: #6B6860 | Border: transparent
// Hover: background #F5F4F1, text #111110

// Danger — red outline
<Button variant="danger">Delete</Button>
// Background: #FFFFFF | Text: #9B1C1C | Border: #FCA5A5
// Hover: background #FEF2F2
```

### Sizes

```tsx
<Button size="lg">Generate Invoice</Button>   // padding: 11px 22px, font: 14px
<Button size="md">Edit Ticket</Button>         // padding: 9px 18px,  font: 13px  ← default
<Button size="sm">Upload</Button>              // padding: 6px 12px,  font: 12px
<Button size="icon">...</Button>               // 36px × 36px, icon only
```

### States

```
Default   → as specified above
Hover     → background shift (150ms ease transition)
Focus     → 2px solid #B8860B outline, 2px offset
Active    → transform: scale(0.98)
Disabled  → opacity: 0.4, cursor: not-allowed, no hover effect
Loading   → left-aligned spinner, text remains, width locked
```

### Button Placement Rules

- **Primary action** (e.g., "Generate Invoice") always rightmost in a button group
- **Destructive action** (e.g., "Delete") never adjacent to primary without a visual gap
- **Max 3 buttons** in one action group; use dropdown overflow for more
- **Full-width buttons** only inside modals and mobile views

---

## 6. Component Library — Badges & Status

### Status Badges (Ticket lifecycle)

```tsx
// Dot + label pill format
<Badge status="approved">Approved</Badge>       // bg: #E5F4EF, text: #0B6E5A, dot: #0B6E5A
<Badge status="pending">Pending review</Badge>  // bg: #FFFBEB, text: #92400E, dot: #D97706
<Badge status="billed">Billed</Badge>           // bg: #EBF0FA, text: #1A4FA0, dot: #1A4FA0
<Badge status="paid">Paid</Badge>               // bg: #F0FDF4, text: #166534, dot: #16A34A
<Badge status="overdue">Overdue</Badge>         // bg: #FEF2F2, text: #9B1C1C, dot: #DC2626
<Badge status="draft">Draft</Badge>             // bg: #F5F4F1, text: #6B6860, dot: #9E9B94
```

### Ticket Type Chips (compact, no dot)

```tsx
<TicketTypeBadge type="FLIGHT" />   // bg: #EBF0FA, text: #1A4FA0, font: 10px 600 uppercase, radius: 4px
<TicketTypeBadge type="BUS" />      // bg: #E5F4EF, text: #0B6E5A, font: 10px 600 uppercase, radius: 4px
```

### AI Confidence Bar

```tsx
<ConfidenceBar value={96} />
// Green fill (#16A34A) for ≥ 85%
// Amber fill (#D97706) for 60–84%
// Red fill   (#DC2626) for < 60%
// Bar height: 3px, border-radius: 2px
// Accompanied by label: "96% AI confidence"
// Fields < 85%: field border changes to #FCD34D, background #FFFDF0
```

### Badge Rules

- Status badges always show a colored dot — never rely on color alone (accessibility)
- Badge width is content-driven, never fixed
- In tables, badges are center-aligned in their column
- Status transitions must be instant (no animation) — financial state is fact, not progress

---

## 7. Component Library — Form Fields

### Standard Field Anatomy

```
[Label — 12px/500, #3D3C38]
[Input — 36px height, 1px #D4D0C8 border, 9px 13px padding]
[Hint or Confidence bar — 11px, #9E9B94]
```

### States

```css
/* Default */
border: 1px solid #D4D0C8;
background: #FFFFFF;
border-radius: 4px;
font-size: 13px;

/* Focus */
border-color: #111110;
box-shadow: 0 0 0 3px rgba(17,17,16,0.06);

/* Error */
border-color: #EF4444;
/* Accompanied by red error message below */

/* AI Low Confidence (< 85%) */
border-color: #FCD34D;
background: #FFFDF0;

/* Disabled */
background: #F5F4F1;
color: #9E9B94;
cursor: not-allowed;
```

### Special Field: PNR Input

```tsx
<PNRField
  value="6X9QR2"
  style={{
    fontFamily: 'JetBrains Mono',
    fontSize: 14,
    fontWeight: 500,
    color: '#1A4FA0',
    textTransform: 'uppercase',
    letterSpacing: '0.08em'
  }}
/>
// Validates on blur: 6 alphanumeric characters
// Shows duplicate warning inline if PNR exists in DB
```

### Special Field: Amount Input

```tsx
<AmountField
  prefix="₹"
  value="12450.00"
  style={{ fontFamily: 'JetBrains Mono' }}
/>
// Formats with Indian number system on blur (12,450.00)
// Right-aligned text
// Validates: positive number, max 2 decimal places
```

### Validation Rules

| Field | Rule | Error Message |
|-------|------|--------------|
| PNR Number | 6 alphanumeric, uppercase | "PNR must be 6 characters (letters and numbers)" |
| GST Number | 15 chars, valid GST format regex | "Invalid GST format — must be 15 characters" |
| Email | RFC 5322 format | "Please enter a valid email address" |
| Amount | Positive decimal, max 2 places | "Enter a valid amount (e.g. 12450.00)" |
| Travel Date | Not in future (for existing tickets) | "Travel date cannot be in the future" |
| Company | Required selection | "Please select a company" |

---

## 8. Component Library — Metric Cards

Used in dashboard summary rows. Always in groups of 3–4.

```tsx
<MetricCard
  label="Total Revenue (Mar)"
  value="₹18.4L"
  delta="+12.4% vs Feb"
  deltaType="positive"
/>
```

### Spec

```css
.metric-card {
  background: #FFFFFF;
  border: 1px solid #E8E6E0;
  border-radius: 12px;
  padding: 16px 18px;
}

.metric-label {
  font-size: 11px;
  color: #9E9B94;
  letter-spacing: 0.02em;
  margin-bottom: 6px;
}

.metric-value {
  font-family: 'Playfair Display';
  font-size: 24px;
  color: #111110;
  letter-spacing: -0.02em;
}

.metric-delta {
  font-size: 11px;
  margin-top: 4px;
}

.delta-positive { color: #16A34A; }
.delta-negative { color: #DC2626; }
.delta-warning  { color: #D97706; }
```

### Delta Icon Rules

- `↑` for positive growth
- `↓` for negative (use red sparingly — not every dip is alarming)
- `⚠` for "action required" states (e.g., pending review count)
- Never use red for negative business metrics unless it's an error state

---

## 9. Screen Specifications

### 9.1 Dashboard

**Route:** `/dashboard` | **Access:** All roles

#### Layout

```
┌─────────────────────────────────────────────────────────┐
│ TOPBAR: Logo + "March 2026" + Search + User Menu        │
├────────────────────────────────────────────────────────┤
│ SIDEBAR (240px) │ CONTENT AREA (fluid)                  │
│                 │                                        │
│ ● Dashboard     │ [Metric] [Metric] [Metric] [Metric]   │
│   Companies     │                                        │
│   Tickets       │ Revenue chart (bar, 6-month)          │
│   Invoices      │                                        │
│   Reports       │ Recent Tickets table (5 rows)         │
│                 │                                        │
│   ─────────     │ Pending Review alert banner           │
│   Settings      │                                        │
└─────────────────────────────────────────────────────────┘
```

#### Key Requirements

- Metric cards: Revenue (MTD), Tickets Processed, Invoices Sent, Pending Review
- Revenue chart: last 6 months bar chart, current month in `--color-gold`, others in `#D4D0C8`
- Recent tickets table: PNR, Company, Route, Amount, Status — 5 rows, "View All" link
- Pending Review banner: amber background, count, "Review Now" CTA — only shown if count > 0
- Dashboard loads in **< 2 seconds** — skeleton screens during load
- Auto-refresh every 5 minutes (React Query `refetchInterval`)

---

### 9.2 Ticket Upload

**Route:** `/tickets/upload` | **Access:** BILLING_STAFF, ADMIN

#### Layout

```
┌──────────────────────────────────────────────┐
│ Page Title: "Upload Tickets"                 │
│ Subtitle: "Drag files or click to browse"    │
├──────────────────────────────────────────────┤
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │   ↑  Drop tickets here                │  │
│  │                                        │  │
│  │   PDF · PNG · JPG · TIFF              │  │
│  │   Max 10MB per file · Up to 50 files  │  │
│  │                                        │  │
│  │        [Browse Files]                  │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  [PDF] indigo_6X9QR2.pdf    234KB  [✓ Done] │
│        ████████████████████ 100%             │
│                                              │
│  [IMG] scan_0314.jpg       1.2MB  [OCR…]   │
│        █████████████░░░░░░░ 72%              │
│                                              │
│  [PDF] FAILED_ticket.pdf    890KB  [✗ Error]│
│        Error: Password-protected PDF         │
│                                              │
│        [Process 2 Files →]                  │
└──────────────────────────────────────────────┘
```

#### Key Requirements

- Drag-and-drop zone with visual hover state (border becomes solid gold)
- Per-file progress bars with percentage
- Per-file status: Queued / OCR Processing / AI Extracting / Done / Error
- Error details shown inline per file (not in a modal)
- "Process Files" button disabled until at least 1 file is successfully extracted
- Company selector at top — pre-select before upload to batch-assign tickets
- Batch upload runs async — user can navigate away; notification on completion

---

### 9.3 Review & Confirm

**Route:** `/tickets/review/:uploadBatchId` | **Access:** BILLING_STAFF, ADMIN

**This is the most critical screen in the system.** Errors here cause financial loss.

#### Layout

```
┌──────────────────────────────────────────────────────────┐
│ "Review Extracted Data" | 3 of 12 tickets | [Skip] [→]   │
├─────────────────────────┬────────────────────────────────┤
│                         │                                │
│   ORIGINAL TICKET       │   EXTRACTED FIELDS             │
│   (PDF viewer /         │                                │
│    image display)       │   PNR          [6X9QR2   ] 96%│
│                         │   Passenger    [R. Sharma] 91%│
│   Zoomable              │   Company      [TCS ▼    ]    │
│   Pan-able              │   Travel Date  [14 Mar   ] 98%│
│   Page controls         │   Origin       [BOM       ] 94%│
│   for multi-page PDFs   │   Destination  [DEL       ] 94%│
│                         │   Total Fare   [12,450.00] 62%⚠│
│                         │   Ticket Type  [FLIGHT ▼  ]    │
│                         │   Operator     [IndiGo    ] 88%│
│                         │                                │
│                         │   ⚠ 1 field needs verification │
│                         │                                │
│                         │   [← Back] [Skip]  [Confirm →] │
└─────────────────────────┴────────────────────────────────┘
```

#### Confidence Threshold Behaviour

| Confidence | Visual Treatment | User Action |
|------------|-----------------|-------------|
| ≥ 85% | Normal field, green bar | No action needed |
| 60–84% | Amber border + amber bar | Should verify |
| < 60% | **Amber border + amber bar + ⚠ label** | **Must verify** — Confirm button blocked |

#### Key Requirements

- Ticket image must be visible at all times — never hidden behind the form
- All fields are always editable, regardless of confidence score
- Duplicate PNR check fires on PNR field blur — shows inline warning immediately
- "Confirm" button text changes to "Confirm (1 issue)" if low-confidence fields exist
- Progress indicator: "3 of 12 tickets" with linear progress bar
- Keyboard navigation: Tab through fields, Enter to confirm, Escape to skip
- "Skip" moves to next ticket without saving; skipped tickets return to PENDING_REVIEW

---

### 9.4 Ticket List

**Route:** `/tickets` | **Access:** All roles

#### Table Columns

| Column | Width | Notes |
|--------|-------|-------|
| PNR | 100px | `JetBrains Mono`, blue, clickable |
| Passenger | 180px | Truncate at 24 chars |
| Company | 160px | Link to company detail |
| Route | 120px | "BOM → DEL" format |
| Travel Date | 100px | DD MMM YYYY |
| Type | 80px | FLIGHT / BUS chip |
| Amount | 110px | Right-aligned, `JetBrains Mono` |
| Status | 120px | Badge with dot |
| Actions | 80px | ··· overflow menu |

#### Filter Bar

```
[Search PNR or Passenger...]  [Company ▼]  [Date Range]  [Type ▼]  [Status ▼]  [Export CSV]
```

#### Key Requirements

- Client-side sort on all columns (server-side for large datasets)
- Bulk actions: select rows → "Mark as Approved" / "Assign to Company"
- Row hover: subtle `#F5F4F1` background
- Clicking PNR opens ticket detail slide-over panel (not navigation)
- Export CSV respects active filters — exports only visible results
- Pagination: 25/50/100 rows per page selector
- Empty state: illustrated empty state with "Upload your first ticket" CTA

---

### 9.5 Invoice Preview

**Route:** `/invoices/preview/:id` | **Access:** All roles (view), BILLING_STAFF (generate)

#### Layout

```
┌──────────────────────────────────────────────┐
│ [← Back]  Invoice TBP/2026-27/00142  [Actions ▼] │
├──────────────────────────────────────────────┤
│                                              │
│  ████████████████████████████████████████   │
│  █ INVOICE HEADER (dark band)             █  │
│  █  Agency Name        Tax Invoice        █  │
│  █  GSTIN: ...         TBP/2026-27/00142  █  │
│  ████████████████████████████████████████   │
│                                              │
│  Bill To: TCS Ltd              Mar 2026      │
│  GSTIN: 27AADCT2911H1Z0        142 tickets  │
│                                              │
│  ┌──────────────────────────────────────┐   │
│  │ PNR  │ Passenger │ Route │ Amt       │   │
│  │──────────────────────────────────────│   │
│  │ 6X.. │ R. Sharma │ BOM→  │ ₹12,450  │   │
│  │ 9P.. │ A. Nair   │ BLR→  │ ₹8,200   │   │
│  │ ... 139 more rows ...                │   │
│  └──────────────────────────────────────┘   │
│                                              │
│  ████████████████████████████████████████   │
│  █ Subtotal: ₹4,02,400  GST: ₹22,435    █  │
│  █ Grand Total: ₹4,24,835               █  │
│  ████████████████████████████████████████   │
│                                              │
│  [Download Excel] [Download PDF] [Send Email]│
└──────────────────────────────────────────────┘
```

#### Key Requirements

- Preview exactly matches the generated Excel/PDF output
- Invoice is **read-only** once generated — no edit mode
- "Send Email" opens a confirmation modal showing recipient address
- Download links are pre-signed S3 URLs (15-minute expiry)
- "Actions" menu: Resend Email / Download / View Audit Trail / Create Credit Note

---

### 9.6 Company Detail

**Route:** `/companies/:id` | **Access:** All roles

#### Layout (tab-based)

```
┌────────────────────────────────────────────────┐
│ [TC] Tata Consultancy Services    ● Active      │
│      GSTIN: 27AADCT2911H1Z0  |  Monthly billing │
├────────────────────────────────────────────────┤
│ [₹38.4L YTD]  [142 tickets]  [₹4.2L outstanding]│
├────────────────────────────────────────────────┤
│ [All Tickets] [Invoices] [Settings] [Audit Log] │
├────────────────────────────────────────────────┤
│                                                 │
│ INVOICES TAB:                                   │
│ TBP/2026-27/00142  March 2026  ₹4,24,835  SENT │
│ TBP/2026-27/00118  Feb 2026    ₹3,81,200  PAID  │
│ TBP/2026-27/00094  Jan 2026    ₹4,10,500  PAID  │
│                                                 │
└────────────────────────────────────────────────┘
```

#### Key Requirements

- Outstanding balance shown in red if > 0
- Credit limit indicator (bar showing % utilised) if credit limit is set
- Settings tab: Edit company details with inline save — no separate edit page
- Audit Log tab: All changes to this company record, sorted newest first
- "Generate Invoice" quick-action button in top-right (pre-selects this company)

---

### 9.7 Reports

**Route:** `/reports` | **Access:** All roles

#### Panels

1. **Revenue Overview** — 6-month bar chart, current month highlighted in gold
2. **Top Companies by Spend** — Horizontal bar chart, top 10
3. **Ticket Volume by Type** — Pie/donut: FLIGHT vs BUS split
4. **Monthly Comparison Table** — Tabular YOY comparison, exportable

#### Key Requirements

- Date range picker (default: current financial year)
- All charts use `Recharts` library
- "Export Report" button generates Excel with all data
- Charts respect filter selections — company filter, date range, ticket type

---

## 10. Layout Grid

### Application Shell

```
Viewport: 1440px (primary design target)
Minimum supported: 1024px

┌──────────────────────────────────────────────┐
│              TOPBAR (56px height)            │
├──────────────┬───────────────────────────────┤
│              │                               │
│   SIDEBAR    │        CONTENT AREA           │
│   240px      │        fluid                  │
│   (fixed)    │        48px horizontal pad    │
│              │        32px vertical pad      │
│              │                               │
│              │                               │
└──────────────┴───────────────────────────────┘
```

### Sidebar Specification

```css
.sidebar {
  width: 240px;
  background: #111110;
  position: fixed;
  top: 0; left: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Collapsed state at 1280px breakpoint */
@media (max-width: 1280px) {
  .sidebar { width: 64px; } /* Icon only */
  .nav-label { display: none; }
}
```

### Sidebar Navigation Items

```
Logo / Brand mark (28px height, gold accent)
───────────────────
● Dashboard
  Companies
  Tickets
  Invoices
  Reports
───────────────────
  Settings
  [User Avatar] Name  (bottom)
```

### Content Area Content Widths

| Content Type | Max Width |
|-------------|-----------|
| Full-width tables | 100% (fluid) |
| Invoice preview | 900px centered |
| Forms (create/edit) | 720px centered |
| Modals | 480px (standard), 640px (wide) |
| Dashboard | 100% with 48px padding |

---

## 11. Motion & Animation

### Approved Timing Values

```css
--duration-instant:  100ms;  /* Tooltips, highlights */
--duration-fast:     150ms;  /* Hover states, badges */
--duration-normal:   250ms;  /* Panel open/close, modals */
--duration-slow:     400ms;  /* Page transitions, success states */
--duration-skeleton: 1400ms; /* Shimmer loading animation */
```

### Easing Functions

```css
--ease-standard: cubic-bezier(0.4, 0, 0.2, 1);   /* General transitions */
--ease-decelerate: cubic-bezier(0, 0, 0.2, 1);   /* Elements entering screen */
--ease-accelerate: cubic-bezier(0.4, 0, 1, 1);   /* Elements leaving screen */
--ease-sharp: cubic-bezier(0.4, 0, 0.6, 1);      /* Quick toggles */
```

### Animation Catalogue

| Name | Duration | Easing | When Used |
|------|----------|--------|-----------|
| Skeleton shimmer | 1400ms loop | ease-in-out | Any data loading > 300ms |
| Toast slide-in | 500ms | ease-decelerate | Success, error, warning notifications |
| Toast slide-out | 300ms | ease-accelerate | Auto-dismiss after 4s |
| Modal appear | 200ms | ease-decelerate | `opacity: 0→1` + `translateY(8px→0)` |
| Spinner rotate | 800ms loop | linear | API calls, file processing |
| Success checkmark | 400ms | ease-out | Ticket confirmed, invoice generated |
| Status pulse dot | 1800ms loop | ease-in-out | Active processing indicator |
| Button active press | 100ms | ease-sharp | `scale(0.98)` |

### Rules

- **All animations respect `prefers-reduced-motion: reduce`** — reduce to instant or fade-only
- **No decorative animations** — every animation communicates state
- **Page transitions**: fade-only (`opacity: 0→1`, 200ms) — no slide/swipe between routes
- **Skeleton screens** instead of spinners for any load > 300ms (tables, dashboard, charts)

---

## 12. States & Feedback Patterns

### Toast Notifications

Positioned: **top-right**, 16px from viewport edge.
Stack up to 4, oldest auto-dismissed after 4 seconds.

```tsx
// Success
<Toast type="success" title="Invoice generated" body="TBP/2026-27/00142 sent to TCS" />
// Border: #A7F3D0 | Icon bg: #D1FAE5 | Icon: ✓ green

// Warning
<Toast type="warning" title="Low AI confidence" body="3 fields need manual review" />
// Border: #FCD34D | Icon bg: #FFFBEB | Icon: ! amber

// Error
<Toast type="error" title="Duplicate PNR detected" body="6X9QR2 already exists" />
// Border: #FCA5A5 | Icon bg: #FEF2F2 | Icon: ✕ red

// Info
<Toast type="info" title="Upload complete" body="12 of 14 tickets extracted successfully" />
// Border: #B8CCF0 | Icon bg: #EBF0FA | Icon: ℹ blue
```

### Confirmation Dialogs

Required for all destructive and irreversible actions.

```tsx
<ConfirmDialog
  title="Delete ticket?"
  body={`Remove ticket ${pnr} (${passenger} · ₹${amount}). This cannot be undone.`}
  confirmLabel="Yes, delete ticket"
  confirmVariant="danger"
  cancelLabel="Cancel"
/>
```

**Rule:** The confirmation button must always restate what will happen — never just "Yes" or "OK".

### Empty States

Every list/table must have a designed empty state:

```
[Illustrated icon — minimal line art]
No tickets found
[Primary CTA: "Upload your first ticket"]
[Secondary CTA: "Or add manually"]
```

Empty state rules:
- Never show an empty table with column headers and no rows
- Always provide a direct action to fix the empty state
- Use muted illustration — not heavy graphics

### Loading States

```
Table loading   → Skeleton rows (3 rows with shimmer)
Dashboard       → Skeleton metric cards + skeleton chart
File upload     → Progress bar per file
AI extraction   → "Extracting data…" spinner per file item
Invoice gen     → Full-page loading overlay: "Generating invoice…"
```

---

## 13. Accessibility Standards

### WCAG 2.1 AA Compliance

| Requirement | Implementation |
|-------------|---------------|
| Color contrast (text) | All text meets 4.5:1 minimum ratio |
| Color contrast (UI) | All interactive elements meet 3:1 ratio |
| Focus indicators | 2px solid `#B8860B` outline, 2px offset, on all interactive elements |
| Keyboard navigation | Full Tab/Shift+Tab support. Logical DOM order. No focus traps |
| Screen reader support | All images have `alt` text. Icon-only buttons have `aria-label` |
| Status badges | Never rely on color alone — always include text label |
| Form fields | All inputs have associated `<label>` (not placeholder-only) |
| Error messages | `role="alert"` or `aria-live="polite"` for dynamic errors |
| Financial amounts | `aria-label="18 lakh 40 thousand rupees"` for screen readers |

### Keyboard Shortcuts (Global)

| Key | Action |
|-----|--------|
| `G + D` | Go to Dashboard |
| `G + T` | Go to Tickets |
| `G + I` | Go to Invoices |
| `G + C` | Go to Companies |
| `U` | Open Upload modal |
| `?` | Open keyboard shortcuts help |
| `Escape` | Close modal / slide-over |

---

## 14. Responsive Breakpoints

| Breakpoint | Width | Behaviour |
|------------|-------|-----------|
| Desktop XL | ≥ 1440px | Full layout as designed |
| Desktop | 1280–1439px | Sidebar collapses to 64px icons-only |
| Laptop | 1024–1279px | Content area condensed; some columns hidden in tables |
| Tablet | 768–1023px | Single column; sidebar becomes bottom nav; read-only mode |
| Mobile | < 768px | Not supported for data entry; read-only dashboard only |

### What Changes at Each Breakpoint

**At 1280px (Laptop):**
- Sidebar collapses to icon-only (64px)
- Table: hide "Operator" and "Travel Date" columns (accessible via row expand)
- Metric cards: 2-column grid instead of 4-column

**At 1024px (Small Laptop):**
- Forms go single-column
- Review screen: image stacks above form fields
- Charts reduce to simplified versions

**At 768px (Tablet):**
- Full bottom navigation bar replaces sidebar
- All tables paginate at 10 rows
- No file upload (read-only access for auditors on tablet)

---

*Document maintained by the TravelBill Pro design team. Update this file when any token, component, or screen spec changes.*