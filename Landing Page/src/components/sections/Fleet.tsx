/**
 * Fleet.tsx — Fleet specifications section
 *
 * Content is LEFT-aligned. A GlassCard contains the headline, a brief
 * paragraph about the Airbus fleet, and a 2×2 grid of spec cards with
 * subtle inner borders. Background stays transparent for the 3D scene.
 *
 * Spec data:
 *  • Wingspan  — 35.8 m
 *  • Range     — 6,150 km
 *  • Capacity  — 180 Seats
 *  • Speed     — Mach 0.78
 */

import GlassCard from "../ui/GlassCard";
import AnimatedText from "../ui/AnimatedText";

interface SpecCardProps {
  icon: React.ReactNode;
  value: string;
  label: string;
}

/** Individual spec metric card */
const SpecCard = ({ icon, value, label }: SpecCardProps) => (
  <div className="flex flex-col items-center justify-center p-5 rounded-xl border border-charcoal/5 bg-white/30">
    <span className="text-2xl mb-2">{icon}</span>
    <p className="text-3xl font-bold text-brand-blue font-display">{value}</p>
    <p className="text-sm text-warm-gray mt-1 font-body">{label}</p>
  </div>
);

const Fleet = () => {
  const specs: SpecCardProps[] = [
    {
      icon: (
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="text-brand-blue">
          <path d="M2 12L22 2L18 12H2Z" />
          <path d="M2 12L22 22L18 12" />
        </svg>
      ),
      value: "35.8m",
      label: "Wingspan",
    },
    {
      icon: (
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="text-brand-blue">
          <circle cx="12" cy="12" r="10" />
          <path d="M2 12h20" />
          <path d="M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z" />
        </svg>
      ),
      value: "6,150 km",
      label: "Range",
    },
    {
      icon: (
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="text-brand-blue">
          <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 00-3-3.87" />
          <path d="M16 3.13a4 4 0 010 7.75" />
        </svg>
      ),
      value: "180",
      label: "Seats",
    },
    {
      icon: (
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="text-brand-blue">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
        </svg>
      ),
      value: "Mach 0.78",
      label: "Cruise Speed",
    },
  ];

  return (
    <section className="section flex items-center justify-start px-6 sm:px-12 lg:px-24">
      <GlassCard className="max-w-xl w-full space-y-6">
        {/* Label */}
        <span className="text-brand-blue text-sm font-semibold uppercase tracking-widest font-body">
          Our Fleet
        </span>

        {/* Headline */}
        <h2 className="text-4xl font-display font-bold text-charcoal tracking-tight leading-tight">
          <AnimatedText>Engineering Excellence</AnimatedText>
        </h2>

        {/* Subtext */}
        <p className="text-warm-gray text-base leading-relaxed font-body">
          Our Airbus A320neo family aircraft set the benchmark for fuel
          efficiency, passenger comfort, and operational reliability. Every
          detail — from the winglets to the cabin air filtration — is
          engineered with purpose.
        </p>

        {/* Spec grid — 2×2 */}
        <div className="grid grid-cols-2 gap-4 pt-2">
          {specs.map((spec) => (
            <SpecCard key={spec.label} {...spec} />
          ))}
        </div>
      </GlassCard>
    </section>
  );
};

export default Fleet;
