/**
 * Pricing — Three-tier cards, pure black/white
 */
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import GlassCard from '../ui/GlassCard'
import { CheckCircleIcon } from '../ui/Icons'
import { Button } from '../ui/button'

interface Plan {
  name: string; price: string; period: string; description: string
  features: string[]; cta: string; popular?: boolean
}

const PLANS: Plan[] = [
  {
    name: 'Starter', price: '₹999', period: '/month',
    description: 'For small agencies getting started with digital billing.',
    features: ['Up to 100 invoices/month', '1 organization', '3 team members', 'Customer management', 'Basic reports', 'Email support'],
    cta: 'Start Free Trial',
  },
  {
    name: 'Professional', price: '₹2,499', period: '/month',
    description: 'For growing agencies that need AI and automation.',
    features: ['Unlimited invoices', '3 organizations', '10 team members', 'AI PDF extraction', 'Advanced analytics', 'Priority support', 'Export (PDF, Excel, CSV)', 'Redis caching'],
    cta: 'Start Free Trial', popular: true,
  },
  {
    name: 'Enterprise', price: 'Custom', period: '',
    description: 'For large travel companies with custom needs.',
    features: ['Everything in Professional', 'Unlimited organizations', 'Unlimited team members', 'Custom integrations', 'Dedicated account manager', 'SLA guarantee', 'On-premise option', 'SSO & SAML'],
    cta: 'Contact Sales',
  },
]

export default function Pricing() {
  return (
    <section className="section-padding bg-surface-secondary" id="pricing">
      <div className="section-container">
        <AnimatedSection className="text-center mb-16">
          <AnimatedText as="h2" className="headline-lg font-display text-text-primary">
            Simple, transparent pricing.
          </AnimatedText>
          <p className="body-large mt-4 max-w-lg mx-auto">
            Start free. Upgrade when you're ready. No surprises.
          </p>
        </AnimatedSection>

        <div className="grid md:grid-cols-3 gap-6 max-w-4xl mx-auto items-stretch">
          {PLANS.map((plan, i) => (
            <AnimatedSection key={plan.name} delay={i * 0.1} scale={0.95}>
              <GlassCard
                className={`p-7 h-full flex flex-col relative ${plan.popular ? 'ring-2 ring-black/10' : ''}`}
                variant={plan.popular ? 'strong' : 'default'}
                hover
              >
                {plan.popular && (
                  <span className="absolute -top-3 left-1/2 -translate-x-1/2 text-[10px] font-bold uppercase tracking-widest text-white bg-black px-3 py-1 rounded-full">
                    Most Popular
                  </span>
                )}

                <div>
                  <h3 className="text-lg font-semibold text-text-primary">{plan.name}</h3>
                  <div className="flex items-baseline gap-1 mt-3">
                    <span className="text-4xl font-bold text-text-primary">{plan.price}</span>
                    <span className="text-sm text-text-tertiary">{plan.period}</span>
                  </div>
                  <p className="text-sm text-text-secondary mt-2">{plan.description}</p>
                </div>

                <ul className="space-y-2.5 mt-6 flex-1">
                  {plan.features.map((f) => (
                    <li key={f} className="flex items-start gap-2.5 text-[13px] text-text-secondary">
                      <CheckCircleIcon size={16} className="text-text-primary shrink-0 mt-0.5" />
                      {f}
                    </li>
                  ))}
                </ul>

                <Button render={<a href="#cta" />} className={`mt-6 w-full rounded-full ${
                  plan.popular
                    ? 'bg-black text-white hover:bg-primary-hover'
                    : 'bg-surface-secondary text-text-primary hover:bg-white border border-border-medium'
                }`}>
                  {plan.cta}
                </Button>
              </GlassCard>
            </AnimatedSection>
          ))}
        </div>
      </div>
    </section>
  )
}
