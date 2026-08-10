/**
 * ExperienceCabin.tsx — Comfort & cabin experience section
 *
 * Content is RIGHT-aligned to keep the 3D plane visible on the left.
 * Features are displayed as icon + title + description rows inside a
 * GlassCard. Warmer accent tones (brand-gold) are used for the icons
 * to differentiate this section's mood from the cooler fleet section.
 */

import GlassCard from "../ui/GlassCard";
import AnimatedText from "../ui/AnimatedText";

interface FeatureRowProps {
  icon: string;
  title: string;
  description: string;
}

/** A single cabin feature with emoji icon, bold title, and description */
const FeatureRow = ({ icon, title, description }: FeatureRowProps) => (
  <div className="flex gap-4 items-start">
    {/* Icon badge */}
    <span className="flex-shrink-0 w-12 h-12 flex items-center justify-center rounded-xl bg-brand-gold/10 text-2xl">
      {icon}
    </span>

    {/* Copy */}
    <div>
      <h3 className="text-lg font-semibold text-charcoal font-display">
        {title}
      </h3>
      <p className="text-warm-gray text-sm leading-relaxed mt-1 font-body">
        {description}
      </p>
    </div>
  </div>
);

const ExperienceCabin = () => {
  const features: FeatureRowProps[] = [
    {
      icon: "🪑",
      title: "Premium Seating",
      description:
        "Ergonomically contoured seats with up to 38 inches of legroom, memory-foam cushions, and adjustable lumbar support — so you arrive as rested as you left.",
    },
    {
      icon: "🍽️",
      title: "Gourmet Dining",
      description:
        "Seasonal menus crafted by Michelin-trained chefs, paired with a curated wine list. Dietary preferences are always honoured — just let us know when you book.",
    },
    {
      icon: "🎬",
      title: "In-Flight Entertainment",
      description:
        "A 13-inch HD seatback display loaded with 500+ films, live sports, podcasts, and noise-cancelling headphones included in every seat class.",
    },
  ];

  return (
    <section className="section flex items-center justify-end px-6 sm:px-12 lg:px-24">
      <GlassCard className="max-w-lg w-full space-y-6">
        {/* Label */}
        <span className="text-brand-blue text-sm font-semibold uppercase tracking-widest font-body">
          The Experience
        </span>

        {/* Headline */}
        <h2 className="text-4xl font-display font-bold text-charcoal tracking-tight leading-tight">
          <AnimatedText>Comfort at 35,000 Feet</AnimatedText>
        </h2>

        {/* Intro line */}
        <p className="text-warm-gray text-base leading-relaxed font-body">
          Every cabin touchpoint is designed to transform hours in the air
          into an experience you genuinely look forward to.
        </p>

        {/* Feature rows */}
        <div className="space-y-6 pt-2">
          {features.map((feature) => (
            <FeatureRow key={feature.title} {...feature} />
          ))}
        </div>
      </GlassCard>
    </section>
  );
};

export default ExperienceCabin;
