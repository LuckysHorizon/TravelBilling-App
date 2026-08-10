/**
 * Icons — Custom SVG icon set for TravelBilling Pro
 * Replaces all emojis with clean, consistent stroke icons.
 * All icons are 24x24 viewBox, 1.5px stroke, currentColor.
 */

interface IconProps {
  className?: string
  size?: number
}

const base = (size: number, className: string) => ({
  width: size,
  height: size,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  className,
  'aria-hidden': true,
})

export function SpreadsheetIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M3 9h18" /><path d="M3 15h18" /><path d="M9 3v18" />
    </svg>
  )
}

export function PenEditIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M17 3a2.83 2.83 0 114 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
    </svg>
  )
}

export function PhoneIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="5" y="2" width="14" height="20" rx="2" />
      <path d="M12 18h.01" />
    </svg>
  )
}

export function ClipboardIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2" />
      <rect x="8" y="2" width="8" height="4" rx="1" />
    </svg>
  )
}

export function RefreshIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M23 4v6h-6" /><path d="M1 20v-6h6" />
      <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" />
    </svg>
  )
}

export function ClockIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
    </svg>
  )
}

export function BrainIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M9.5 2A5.5 5.5 0 005 7.5c0 .97.25 1.88.7 2.67A5.48 5.48 0 004 14.5 5.5 5.5 0 009.5 20h.5" />
      <path d="M14.5 2A5.5 5.5 0 0120 7.5c0 .97-.25 1.88-.7 2.67A5.48 5.48 0 0121 14.5a5.5 5.5 0 01-5.5 5.5H15" />
      <path d="M12 2v20" />
    </svg>
  )
}

export function ZapIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
    </svg>
  )
}

export function CloudIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M18 10h-1.26A8 8 0 109 20h9a5 5 0 000-10z" />
    </svg>
  )
}

export function TrendUpIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M23 6l-9.5 9.5-5-5L1 18" /><path d="M17 6h6v6" />
    </svg>
  )
}

export function BuildingIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="4" y="2" width="16" height="20" rx="2" />
      <path d="M9 22V12h6v10" /><path d="M8 6h.01" /><path d="M16 6h.01" />
      <path d="M8 10h.01" /><path d="M16 10h.01" />
    </svg>
  )
}

export function ShieldIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  )
}

export function FileTextIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
      <path d="M14 2v6h6" /><path d="M16 13H8" /><path d="M16 17H8" /><path d="M10 9H8" />
    </svg>
  )
}

export function UsersIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 00-3-3.87" /><path d="M16 3.13a4 4 0 010 7.75" />
    </svg>
  )
}

export function BarChartIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M18 20V10" /><path d="M12 20V4" /><path d="M6 20v-6" />
    </svg>
  )
}

export function SettingsIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z" />
    </svg>
  )
}

export function CreditCardIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="1" y="4" width="22" height="16" rx="2" />
      <path d="M1 10h22" />
    </svg>
  )
}

export function BoxIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
      <path d="M3.27 6.96L12 12.01l8.73-5.05" /><path d="M12 22.08V12" />
    </svg>
  )
}

export function LockIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="3" y="11" width="18" height="11" rx="2" />
      <path d="M7 11V7a5 5 0 0110 0v4" />
    </svg>
  )
}

export function UploadIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
      <polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
    </svg>
  )
}

export function DashboardIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="3" y="3" width="7" height="9" rx="1" />
      <rect x="14" y="3" width="7" height="5" rx="1" />
      <rect x="14" y="12" width="7" height="9" rx="1" />
      <rect x="3" y="16" width="7" height="5" rx="1" />
    </svg>
  )
}

export function CheckCircleIcon({ className = '', size = 24 }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
      <polyline points="22 4 12 14.01 9 11.01" />
    </svg>
  )
}
