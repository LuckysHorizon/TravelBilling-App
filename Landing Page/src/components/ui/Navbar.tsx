/**
 * Navbar — Floating glassmorphism pill
 *
 * Rounded pill container floating with margin from top/sides.
 * Semi-transparent white with blur, rainbow blob behind.
 * Condenses (less blur/opacity) on scroll.
 */
import { useState, useEffect, useRef } from 'react'
import { Button } from './button'
import { useSmoothScroll } from '../../hooks/useSmoothScroll'

/** TravelBilling application URL — driven by VITE_TRAVELBILLING_URL */
const TRAVELBILLING_URL = import.meta.env.VITE_TRAVELBILLING_URL

if (!TRAVELBILLING_URL) {
  console.warn('[Landing Page] VITE_TRAVELBILLING_URL is not set. Sign In / Get Started buttons will not redirect.')
}

const NAV_LINKS = [
  { label: 'Features', href: '#features' },
  { label: 'AI Engine', href: '#ai-spotlight' },
  { label: 'How It Works', href: '#how-it-works' },
  { label: 'Pricing', href: '#pricing' },
]

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const [activeLink, setActiveLink] = useState('Features')
  const [mobileOpen, setMobileOpen] = useState(false)
  const pillRef = useRef<HTMLElement>(null)
  const { handleAnchorClick } = useSmoothScroll({ offsetY: -80 })

  useEffect(() => {
    const onScroll = () => {
      setScrolled(window.scrollY > 60)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // Track active section based on scroll position
  useEffect(() => {
    const sections = NAV_LINKS.map((l) => l.href.replace('#', ''))
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const match = NAV_LINKS.find(
              (l) => l.href === `#${entry.target.id}`
            )
            if (match) setActiveLink(match.label)
          }
        })
      },
      { rootMargin: '-40% 0px -55% 0px' }
    )

    sections.forEach((id) => {
      const el = document.getElementById(id)
      if (el) observer.observe(el)
    })

    return () => observer.disconnect()
  }, [])

  return (
    <div className="fixed top-0 left-0 right-0 z-50 flex justify-center px-4 pt-4">
      {/* Rainbow / pastel gradient glow blob behind the pill */}
      <div className="navbar-glow" />

      {/* Pill container */}
      <nav
        ref={pillRef}
        className="relative flex items-center justify-between w-full max-w-3xl transition-all duration-500"
        style={{
          borderRadius: '999px',
          background: scrolled
            ? 'rgba(255,255,255,0.55)'
            : 'rgba(255,255,255,0.68)',
          backdropFilter: scrolled
            ? 'blur(14px) saturate(150%)'
            : 'blur(20px) saturate(180%)',
          WebkitBackdropFilter: scrolled
            ? 'blur(14px) saturate(150%)'
            : 'blur(20px) saturate(180%)',
          border: '1px solid rgba(255,255,255,0.4)',
          boxShadow: '0 4px 24px rgba(0,0,0,0.06), 0 1px 4px rgba(0,0,0,0.02)',
          padding: '0 6px',
          height: scrolled ? '44px' : '48px',
        }}
      >
        {/* ── Logo + Wordmark ─────────────────────────── */}
        <a href="#" onClick={(e) => { e.preventDefault(); window.scrollTo({ top: 0, behavior: 'smooth' }) }} className="flex items-center gap-2 pl-2 shrink-0">
          <div className="w-7 h-7 shrink-0 overflow-hidden flex items-center justify-center">
            <img src="/logos/icon-full.svg" alt="TravelBilling logo" className="w-full h-full object-contain" />
          </div>
          <span className="text-[14px] font-bold uppercase text-text-primary tracking-[0.1em] hidden sm:inline">
            TRAVELBILLING
          </span>
        </a>

        {/* ── Centered Nav Links ──────────────────────── */}
        <div className="hidden md:flex items-center gap-1 absolute left-1/2 -translate-x-1/2">
          {NAV_LINKS.map((link) => {
            const isActive = activeLink === link.label
            return (
              <a
                key={link.label}
                href={link.href}
                onClick={(e) => {
                  setActiveLink(link.label)
                  handleAnchorClick(e)
                }}
                className="relative px-3 py-1.5 text-[13px] font-medium transition-colors duration-200"
                style={{
                  color: isActive ? '#1d1d1f' : '#6b7280',
                }}
                onMouseEnter={(e) => {
                  if (!isActive) e.currentTarget.style.color = '#1d1d1f'
                }}
                onMouseLeave={(e) => {
                  if (!isActive) e.currentTarget.style.color = '#6b7280'
                }}
              >
                {link.label}
                {/* Active indicator */}
                {isActive && (
                  <span
                    className="absolute bottom-0 left-1/2 -translate-x-1/2 h-[2px] bg-text-primary rounded-full transition-all duration-300"
                    style={{ width: '16px' }}
                  />
                )}
              </a>
            )
          })}
        </div>

        {/* ── Auth Actions ────────────────────────────── */}
        <div className="hidden md:flex items-center gap-2 pr-1">
          <a
            href={TRAVELBILLING_URL || '#'}
            className="text-[13px] font-medium text-text-secondary hover:text-text-primary transition-colors px-3 py-1.5"
          >
            Sign In
          </a>
          <Button render={<a href={TRAVELBILLING_URL || '#'} className="text-[13px] font-semibold" />} className="rounded-full">
            Get Started
          </Button>
        </div>

        {/* ── Mobile Toggle ───────────────────────────── */}
        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="md:hidden relative w-8 h-8 flex flex-col justify-center items-center gap-[5px] mr-1"
          aria-label="Menu"
        >
          <span
            className="block w-4 h-[1.5px] bg-text-primary rounded transition-all duration-300"
            style={{
              transform: mobileOpen ? 'rotate(45deg) translateY(3.25px)' : 'none',
            }}
          />
          <span
            className="block w-4 h-[1.5px] bg-text-primary rounded transition-all duration-300"
            style={{
              transform: mobileOpen ? 'rotate(-45deg) translateY(-3.25px)' : 'none',
            }}
          />
        </button>
      </nav>

      {/* ── Mobile Dropdown ─────────────────────────── */}
      {mobileOpen && (
        <div
          className="md:hidden fixed top-[72px] left-4 right-4 z-50 p-4 space-y-1"
          style={{
            borderRadius: '20px',
            background: 'rgba(255,255,255,0.85)',
            backdropFilter: 'blur(30px) saturate(180%)',
            border: '1px solid rgba(255,255,255,0.4)',
            boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
          }}
        >
          {NAV_LINKS.map((link) => (
            <a
              key={link.label}
              href={link.href}
              onClick={(e) => {
                setMobileOpen(false)
                handleAnchorClick(e)
              }}
              className="block text-[15px] font-medium text-text-secondary hover:text-text-primary py-2 px-3 rounded-xl hover:bg-black/[0.03] transition-all"
            >
              {link.label}
            </a>
          ))}
          <div className="pt-2 border-t border-border-light mt-2 flex gap-2">
            <a href={TRAVELBILLING_URL || '#'} onClick={() => setMobileOpen(false)} className="flex-1 text-center text-[14px] font-medium text-text-secondary py-2.5 rounded-xl hover:bg-black/[0.03]">
              Sign In
            </a>
            <Button render={<a href={TRAVELBILLING_URL || '#'} onClick={() => setMobileOpen(false)} className="text-[14px] font-semibold" />} className="flex-1 rounded-full">
              Get Started
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
