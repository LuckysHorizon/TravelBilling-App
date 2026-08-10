/**
 * DashboardMockup — Clean interactive dashboard with custom SVG icons,
 * GSAP-powered scroll animations, and shadcn-ui Tabs primitives.
 */
import { useEffect, useRef, useState } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'
import {
  DashboardIcon,
  FileTextIcon,
  UsersIcon,
  BrainIcon,
  BarChartIcon,
  SettingsIcon
} from '../ui/Icons'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'

const DATASETS = {
  Week: [30, 45, 60, 40, 50, 75, 55, 80, 70, 85, 90, 65],
  Month: [40, 55, 35, 72, 58, 85, 45, 92, 68, 78, 95, 82],
  Year: [50, 65, 45, 80, 60, 90, 70, 95, 75, 85, 98, 88],
}

export default function DashboardMockup() {
  const [activeSidebar, setActiveSidebar] = useState('Dashboard')
  const [activeTab, setActiveTab] = useState<'Week' | 'Month' | 'Year'>('Month')
  const containerRef = useRef<HTMLDivElement>(null)
  const barRefs = useRef<(HTMLDivElement | null)[]>([])

  const sidebarItems = [
    { label: 'Dashboard', Icon: DashboardIcon },
    { label: 'Invoices', Icon: FileTextIcon },
    { label: 'Customers', Icon: UsersIcon },
    { label: 'AI Extract', Icon: BrainIcon },
    { label: 'Reports', Icon: BarChartIcon },
    { label: 'Settings', Icon: SettingsIcon },
  ]

  // Initial scroll entrance animation (Staggered grow from 0%)
  useEffect(() => {
    const el = containerRef.current
    if (!el) return

    const dataset = DATASETS[activeTab]

    // Set initial height to 0%
    barRefs.current.forEach((bar) => {
      if (bar) gsap.set(bar, { height: '0%' })
    })

    const ctx = gsap.context(() => {
      ScrollTrigger.create({
        trigger: el,
        start: 'top 85%',
        once: true,
        onEnter: () => {
          barRefs.current.forEach((bar, index) => {
            if (bar) {
              gsap.to(bar, {
                height: `${dataset[index]}%`,
                duration: 0.8,
                delay: index * 0.04,
                ease: 'power2.out',
              })
            }
          })
        }
      })
    }, el)

    return () => ctx.revert()
  }, [])

  // Smooth height-morph on dataset toggling
  useEffect(() => {
    const dataset = DATASETS[activeTab]
    dataset.forEach((value, index) => {
      const bar = barRefs.current[index]
      if (bar) {
        gsap.to(bar, {
          height: `${value}%`,
          duration: 0.5,
          ease: 'power3.out',
          overwrite: 'auto'
        })
      }
    })
  }, [activeTab])

  return (
    <div ref={containerRef} className="bg-white rounded-xl overflow-hidden border border-border-light shadow-sm">
      {/* Window chrome */}
      <div className="flex items-center gap-2 px-4 py-2.5 border-b border-border-light bg-surface-tertiary">
        {/* macOS traffic lights with actual hover behaviour */}
        <div className="flex gap-1.5 group/chrome">
          {/* Red / Close */}
          <div className="w-3 h-3 rounded-full bg-[#FF5F57] shadow-[inset_0_0.5px_1px_rgba(0,0,0,0.2)] flex items-center justify-center relative cursor-pointer" aria-label="Close window">
            <svg className="opacity-0 group-hover/chrome:opacity-60 transition-opacity duration-100 w-1.5 h-1.5 text-black/80" viewBox="0 0 6 6" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round">
              <path d="M1.5 1.5L4.5 4.5M4.5 1.5L1.5 4.5" />
            </svg>
          </div>
          {/* Yellow / Minimize */}
          <div className="w-3 h-3 rounded-full bg-[#FEBC2E] shadow-[inset_0_0.5px_1px_rgba(0,0,0,0.2)] flex items-center justify-center relative cursor-pointer" aria-label="Minimize window">
            <svg className="opacity-0 group-hover/chrome:opacity-60 transition-opacity duration-100 w-1.5 h-1.5 text-black/80" viewBox="0 0 6 6" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round">
              <line x1="1" y1="3" x2="5" y2="3" />
            </svg>
          </div>
          {/* Green / Maximize */}
          <div className="w-3 h-3 rounded-full bg-[#28C840] shadow-[inset_0_0.5px_1px_rgba(0,0,0,0.2)] flex items-center justify-center relative cursor-pointer" aria-label="Maximize window">
            <svg className="opacity-0 group-hover/chrome:opacity-60 transition-opacity duration-100 w-1.5 h-1.5 text-black/80" viewBox="0 0 6 6" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
              <path d="M1 5L5 1M5 1H3M5 1V3M1 5H3M1 5V3" />
            </svg>
          </div>
        </div>

        <div className="flex-1 flex justify-center">
          <div className="px-4 py-0.5 rounded-md bg-surface-secondary text-[10px] text-text-tertiary font-mono select-none">
            app.travelbilling.pro/dashboard
          </div>
        </div>
        <div className="w-12" />
      </div>

      <div className="flex min-h-[380px]">
        {/* Sidebar Navigation */}
        <div className="hidden sm:block w-48 border-r border-border-light bg-surface-tertiary/50 p-3">
          <Tabs value={activeSidebar} onValueChange={setActiveSidebar} orientation="vertical" className="w-full">
            <TabsList className="flex flex-col bg-transparent w-full p-0 gap-0.5" aria-label="Dashboard Sidebar">
              {sidebarItems.map(({ label, Icon }) => (
                <TabsTrigger
                  key={label}
                  value={label}
                  className={`w-full flex items-center gap-2.5 px-2.5 py-2 rounded-lg text-xs font-medium transition-all duration-150 cursor-pointer active:scale-[0.98] outline-none focus-visible:ring-2 focus-visible:ring-black focus-visible:ring-offset-1 border-none justify-start ${
                    activeSidebar === label 
                      ? 'data-active:bg-black data-active:text-white bg-black text-white' 
                      : 'bg-transparent text-text-secondary hover:bg-gray-100 active:bg-gray-200 data-active:bg-transparent'
                  }`}
                >
                  <Icon size={14} className="transition-colors duration-150 shrink-0" aria-hidden="true" />
                  {label}
                </TabsTrigger>
              ))}
            </TabsList>
          </Tabs>
        </div>

        {/* Main Panel */}
        <div className="flex-1 p-4 space-y-4">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            {[
              { label: 'Total Revenue', value: '₹24,82,400', change: '+12.5%' },
              { label: 'Outstanding', value: '₹3,45,200', change: '-8.2%' },
              { label: 'Invoices', value: '1,247', change: '+24' },
              { label: 'Customers', value: '342', change: '+18' },
            ].map((stat) => (
              <div key={stat.label} className="bg-surface-tertiary rounded-xl p-3 border border-border-light">
                <p className="text-[10px] text-text-tertiary font-medium uppercase tracking-wider">{stat.label}</p>
                <p className="text-lg font-bold text-text-primary mt-0.5">{stat.value}</p>
                <p className="text-[10px] font-medium mt-0.5 text-text-secondary">{stat.change}</p>
              </div>
            ))}
          </div>

          {/* Revenue Overview Section */}
          <div className="bg-surface-tertiary rounded-xl p-4 border border-border-light">
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs font-semibold text-text-primary">Revenue Overview</p>
              
              {/* Tabs for dataset toggle */}
              <Tabs value={activeTab} onValueChange={(val) => setActiveTab(val as 'Week' | 'Month' | 'Year')}>
                <TabsList className="bg-surface-secondary border border-border-light rounded-lg p-0.5 flex gap-1 h-7" aria-label="Revenue period selection">
                  {(['Week', 'Month', 'Year'] as const).map((t) => (
                    <TabsTrigger
                      key={t}
                      value={t}
                      className="text-[9px] px-2.5 py-1 rounded-md font-medium data-active:bg-black data-active:text-white text-text-tertiary bg-transparent hover:text-text-primary transition-all duration-150 border-none h-full cursor-pointer"
                    >
                      {t}
                    </TabsTrigger>
                  ))}
                </TabsList>
              </Tabs>
            </div>
            
            {/* Chart Area */}
            <div className="flex items-end gap-1.5 h-28 border-b border-gray-200/50 pb-1">
              {DATASETS[activeTab].map((h, i) => {
                const isCurrentPeriod = i === 10 // November as current period
                return (
                  <div key={i} className="flex-1 flex flex-col items-center gap-1 h-full justify-end">
                    <div
                      ref={(el) => { barRefs.current[i] = el }}
                      className={`w-full rounded-t-sm transition-colors duration-150 ${
                        isCurrentPeriod 
                          ? 'bg-gradient-to-t from-black to-black/60 shadow-[0_1px_4px_rgba(0,0,0,0.15)]' 
                          : 'bg-black/10 hover:bg-black/25'
                      }`}
                      style={{ height: '0%' }} // Animated via GSAP
                    />
                    <span className="text-[7px] text-text-tertiary font-medium mt-1 select-none">
                      {['J','F','M','A','M','J','J','A','S','O','N','D'][i]}
                    </span>
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
