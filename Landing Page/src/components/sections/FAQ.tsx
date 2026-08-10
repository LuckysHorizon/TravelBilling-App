/**
 * FAQ — Accordion section for frequently asked questions
 */
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion'

const FAQS = [
  {
    question: 'How accurate is the AI PDF extraction?',
    answer: 'Our AI engine is specifically trained on travel industry documents including airline tickets, hotel invoices, and visa receipts. It achieves over 98% accuracy. For any exceptions, the system flags the field for your manual review before finalizing the invoice.',
  },
  {
    question: 'Can I manage multiple branches or sub-agencies?',
    answer: 'Yes! TravelBilling Pro is built as a multi-tenant SaaS. You can create multiple organizations or branches under a single parent account. Each branch gets its own isolated data, users, and branding, while you can view aggregated reports at the top level.',
  },
  {
    question: 'Does it automatically calculate GST and taxes?',
    answer: 'Absolutely. We have built-in tax engines that handle complex GST calculations for different services (flights, hotels, service charges) based on your region and the customer\'s state, preventing costly compliance errors.',
  },
  {
    question: 'Is my data secure?',
    answer: 'Security is our top priority. We use enterprise-grade encryption for data at rest and in transit. Our role-based access control (RBAC) ensures your team members only see what they need to, and tenant isolation guarantees your data never mixes with others.',
  },
  {
    question: 'Can I export my data to other accounting software?',
    answer: 'Yes, you can easily export your invoices, customer data, and reports in Excel, CSV, and PDF formats. We also plan to release direct API integrations with Tally and QuickBooks soon.',
  },
]

export default function FAQ() {
  return (
    <section className="section-padding bg-surface" id="faq">
      <div className="section-container max-w-3xl">
        <AnimatedSection className="text-center mb-12">
          <AnimatedText as="h2" className="headline-md font-display text-text-primary">
            Frequently Asked Questions
          </AnimatedText>
        </AnimatedSection>

        <AnimatedSection delay={0.1}>
          <Accordion className="w-full">
            {FAQS.map((faq, i) => (
              <AccordionItem key={i} value={`item-${i}`} className="border-b border-border-light py-2">
                <AccordionTrigger className="text-left text-[15px] font-semibold text-text-primary hover:text-black hover:no-underline">
                  {faq.question}
                </AccordionTrigger>
                <AccordionContent className="text-[14px] text-text-secondary leading-relaxed pt-1 pb-4">
                  {faq.answer}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </AnimatedSection>
      </div>
    </section>
  )
}
