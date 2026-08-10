/**
 * Testimonials — Marquee of customer review cards
 * Infinite horizontal scroll, pauses on hover.
 */
import { Marquee } from '@/components/ui/marquee'
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import GlassCard from '../ui/GlassCard'

const TESTIMONIALS = [
  {
    quote: 'TravelBilling Pro cut our invoice processing time from 2 hours to 15 minutes. The AI extraction is genuinely magical — it reads our airline tickets perfectly.',
    name: 'Priya Sharma',
    role: 'Operations Manager',
    company: 'Skyline Travel Agency',
    avatar: 'PS',
  },
  {
    quote: 'Managing 3 branches used to be chaos. Now everything is centralized, each branch has its own data, and I can see the full picture from one dashboard.',
    name: 'Arjun Mehta',
    role: 'Founder & CEO',
    company: 'Wanderlust Tours',
    avatar: 'AM',
  },
  {
    quote: "The GST calculations alone saved us from so many errors. Add the PDF extraction and real-time reports — it's transformed how we run our finances.",
    name: 'Fatima Khan',
    role: 'Finance Lead',
    company: 'Global Wings Travel',
    avatar: 'FK',
  },
  {
    quote: 'We went from 3 accounting software to just one. The multi-tenant setup lets each branch operate independently while I see the big picture.',
    name: 'Vikram Desai',
    role: 'Branch Director',
    company: 'Horizon Travels',
    avatar: 'VD',
  },
  {
    quote: 'The onboarding was effortless — we were generating invoices within 10 minutes. The clean interface makes training new staff a breeze.',
    name: 'Sneha Patel',
    role: 'Admin Lead',
    company: 'Jetstream Airways',
    avatar: 'SP',
  },
  {
    quote: 'Real-time revenue dashboards changed how we make business decisions. We can spot trends and act on them the same day.',
    name: 'Rahul Nair',
    role: 'CFO',
    company: 'Atlas Tour Group',
    avatar: 'RN',
  },
]

/* Split into two rows for visual interest */
const row1 = TESTIMONIALS.slice(0, 3)
const row2 = TESTIMONIALS.slice(3)

function ReviewCard({ t }: { t: typeof TESTIMONIALS[number] }) {
  return (
    <div className="w-[340px] shrink-0 mx-3">
      <GlassCard className="p-6 h-full flex flex-col" hover>
        {/* Stars */}
        <div className="flex gap-0.5 mb-3">
          {[...Array(5)].map((_, j) => (
            <svg key={j} className="w-3.5 h-3.5 text-text-primary" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
            </svg>
          ))}
        </div>

        <p className="text-[13px] text-text-secondary leading-relaxed flex-1">
          &ldquo;{t.quote}&rdquo;
        </p>

        <div className="flex items-center gap-3 mt-4 pt-4 border-t border-border-light">
          <div className="w-8 h-8 rounded-full bg-black flex items-center justify-center text-[10px] font-bold text-white shrink-0">
            {t.avatar}
          </div>
          <div>
            <p className="text-[13px] font-semibold text-text-primary">{t.name}</p>
            <p className="text-[10px] text-text-tertiary">{t.role}, {t.company}</p>
          </div>
        </div>
      </GlassCard>
    </div>
  )
}

export default function Testimonials() {
  return (
    <section className="section-padding bg-white overflow-hidden">
      <div className="section-container">
        <AnimatedSection className="text-center mb-12">
          <AnimatedText as="h2" className="headline-lg font-display text-text-primary">
            Loved by travel professionals.
          </AnimatedText>
        </AnimatedSection>
      </div>

      {/* Row 1 — scrolls left */}
      <Marquee pauseOnHover speed={35}>
        {row1.map((t) => (
          <ReviewCard key={t.name} t={t} />
        ))}
      </Marquee>

      {/* Row 2 — scrolls right */}
      <Marquee pauseOnHover speed={35} direction="right" className="mt-4">
        {row2.map((t) => (
          <ReviewCard key={t.name} t={t} />
        ))}
      </Marquee>
    </section>
  )
}
