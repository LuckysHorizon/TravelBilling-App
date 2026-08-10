/**
 * Features — Feature grid with promoted card, GSAP hover micro-interactions,
 * keyboard accessibility, and shadcn-ui Card components.
 */
import { useRef } from 'react'
import { Card, CardHeader, CardTitle, CardDescription } from '../ui/card'
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import { FileTextIcon, UsersIcon, CreditCardIcon, BoxIcon, BarChartIcon, LockIcon } from '../ui/Icons'
import { gsap } from '../../lib/gsapConfig'

const FEATURES = [
  {
    Icon: FileTextIcon,
    title: 'Invoice Management',
    desc: 'Create, edit, download, and track invoices — flights, hotels, visa, taxes — all computed automatically.',
  },
  {
    Icon: UsersIcon,
    title: 'Customer Management',
    desc: 'Centralized customer records with contact, GST details, history tracking, and smart search.',
  },
  {
    Icon: CreditCardIcon,
    title: 'Billing Engine',
    desc: 'Automatic GST calculation, discount handling, tax breakdowns, and payment status management.',
    isPromoted: true, // Promoted hero card
  },
  {
    Icon: BoxIcon,
    title: 'Multi-Tenant SaaS',
    desc: 'Each organization gets isolated data, users, and settings. Scale from one branch to enterprise.',
  },
  {
    Icon: BarChartIcon,
    title: 'Analytics Dashboard',
    desc: 'Revenue trends, outstanding payments, monthly reports, customer growth — all in real-time.',
  },
  {
    Icon: LockIcon,
    title: 'Enterprise Security',
    desc: 'JWT authentication, role-based access control, encrypted data, and tenant isolation.',
  },
]

interface FeatureCardProps {
  title: string
  desc: string
  Icon: React.ComponentType<any>
  isPromoted?: boolean
  index: number
}

function FeatureCard({ title, desc, Icon, isPromoted, index }: FeatureCardProps) {
  const cardRef = useRef<HTMLDivElement>(null)
  const iconRef = useRef<HTMLDivElement>(null)

  const handleActive = () => {
    if (cardRef.current) {
      gsap.to(cardRef.current, {
        y: -5,
        boxShadow: '0 12px 30px -10px rgba(0, 0, 0, 0.08)',
        borderColor: isPromoted ? 'rgba(0, 0, 0, 0.18)' : 'rgba(0, 0, 0, 0.12)',
        duration: 0.3,
        ease: 'power2.out',
        overwrite: 'auto',
      })
    }
    if (iconRef.current) {
      gsap.to(iconRef.current, {
        scale: 1.12,
        rotate: isPromoted ? -3 : 3,
        duration: 0.4,
        ease: 'back.out(1.5)',
        overwrite: 'auto',
      })
    }
  }

  const handleInactive = () => {
    if (cardRef.current) {
      gsap.to(cardRef.current, {
        y: 0,
        boxShadow: isPromoted ? '0 8px 24px -8px rgba(0,0,0,0.04)' : 'none',
        borderColor: isPromoted ? 'rgba(0, 0, 0, 0.1)' : 'rgba(0, 0, 0, 0.06)',
        duration: 0.3,
        ease: 'power2.out',
        overwrite: 'auto',
      })
    }
    if (iconRef.current) {
      gsap.to(iconRef.current, {
        scale: 1,
        rotate: 0,
        duration: 0.4,
        ease: 'power2.out',
        overwrite: 'auto',
      })
    }
  }

  return (
    <div
      ref={cardRef}
      onMouseEnter={handleActive}
      onMouseLeave={handleInactive}
      onFocus={handleActive}
      onBlur={handleInactive}
      tabIndex={0}
      className={`h-full rounded-xl transition-all duration-300 outline-none focus-visible:ring-2 focus-visible:ring-black focus-visible:ring-offset-2 select-none cursor-default ${
        isPromoted
          ? 'bg-gradient-to-br from-white to-surface-secondary border border-black/10 shadow-[0_8px_24px_-8px_rgba(0,0,0,0.04)] ring-1 ring-black/[0.02]'
          : 'bg-white border border-border-light'
      }`}
    >
      <Card className="border-none ring-0 bg-transparent p-7 h-full flex flex-col justify-start">
        {/* Animated icon chip */}
        <div
          ref={iconRef}
          className={`w-11 h-11 rounded-xl flex items-center justify-center mb-5 shrink-0 ${
            isPromoted 
              ? 'bg-black text-white shadow-[0_4px_12px_rgba(0,0,0,0.15)]' 
              : 'bg-surface-secondary text-text-primary'
          }`}
        >
          <Icon size={20} aria-hidden="true" />
        </div>
        <CardHeader className="p-0">
          <CardTitle className="text-[15px] font-semibold text-text-primary mb-2">
            {title}
          </CardTitle>
          <CardDescription className="text-[13px] text-text-secondary leading-relaxed">
            {desc}
          </CardDescription>
        </CardHeader>
      </Card>
    </div>
  )
}

export default function Features() {
  return (
    <section className="py-16 md:py-20 bg-surface-secondary" id="features">
      <div className="section-container">
        {/* Eyebrow Restraint: omitted eyebrow here to avoid templated repetition */}
        <AnimatedSection className="text-center mb-12">
          <AnimatedText as="h2" className="headline-lg font-display text-text-primary">
            Everything you need to run your agency.
          </AnimatedText>
          <p className="body-large mt-4 max-w-xl mx-auto">
            From invoicing to AI document processing — one platform to manage your entire billing workflow.
          </p>
        </AnimatedSection>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5 items-stretch">
          {FEATURES.map((f, i) => (
            <AnimatedSection key={f.title} delay={i * 0.06} scale={0.96} className="h-full">
              <FeatureCard
                title={f.title}
                desc={f.desc}
                Icon={f.Icon}
                isPromoted={f.isPromoted}
                index={i}
              />
            </AnimatedSection>
          ))}
        </div>
      </div>
    </section>
  )
}
