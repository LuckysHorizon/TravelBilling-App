/**
 * About.tsx — Brand story section
 *
 * Content is RIGHT-aligned so the rotating 3D plane on the left
 * of the canvas remains visible. All copy lives inside a GlassCard
 * to provide readable contrast while keeping the 3D scene visible.
 *
 * Includes:
 *  • "OUR STORY" section label
 *  • Headline via AnimatedText
 *  • 2-3 paragraphs of realistic agency copy
 *  • Mini stat row: Routes · Passengers · On-Time rate
 */

import GlassCard from "../ui/GlassCard";
import AnimatedText from "../ui/AnimatedText";

/** Small stat display used in the bottom row */
const Stat = ({ value, label }: { value: string; label: string }) => (
  <div className="text-center">
    <p className="text-2xl font-bold text-brand-blue font-display">{value}</p>
    <p className="text-xs text-warm-gray mt-1 font-body">{label}</p>
  </div>
);

const About = () => {
  return (
    <section className="section flex items-center justify-end px-6 sm:px-12 lg:px-24">
      <GlassCard className="max-w-lg w-full space-y-6">
        {/* Label */}
        <span className="text-brand-blue text-sm font-semibold uppercase tracking-widest font-body">
          Our Story
        </span>

        {/* Headline */}
        <h2 className="text-4xl font-display font-bold text-charcoal tracking-tight leading-tight">
          <AnimatedText>Redefining Air Travel Since 2018</AnimatedText>
        </h2>

        {/* Body copy */}
        <div className="space-y-4 text-warm-gray text-base leading-relaxed font-body">
          <p>
            RamnetSolutions was founded with a singular vision: to bridge the
            gap between commercial efficiency and private-jet luxury. We
            believe every journey should feel intentional — from the moment you
            book to the second you touch down.
          </p>
          <p>
            Our fleet of next-generation Airbus aircraft operates across 50+
            curated routes, connecting the world's most sought-after cities
            with service that's consistently ranked among the industry's best.
          </p>
          <p>
            Behind the scenes, a dedicated operations team and AI-driven
            logistics platform ensure that punctuality, safety, and passenger
            comfort are never compromised.
          </p>
        </div>

        {/* Stats row */}
        <div className="flex items-center justify-between pt-4 border-t border-charcoal/10">
          <Stat value="50+" label="Routes" />
          <Stat value="2M+" label="Passengers" />
          <Stat value="99.2%" label="On-Time" />
        </div>
      </GlassCard>
    </section>
  );
};

export default About;
