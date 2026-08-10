/**
 * Destinations.tsx — Destination showcase section
 *
 * Full-width section with a centered headline and a horizontally-
 * scrollable row of 5 destination cards. Each card is a GlassCard
 * featuring a coloured gradient header, city & country, price,
 * and an "Explore →" link.
 *
 * On desktop the cards fan out in a row; on mobile they scroll
 * horizontally inside an overflow container.
 */

import GlassCard from "../ui/GlassCard";
import AnimatedText from "../ui/AnimatedText";

interface DestinationProps {
  city: string;
  country: string;
  price: string;
  gradient: string;
}

/** Individual destination card */
const DestinationCard = ({ city, country, price, gradient }: DestinationProps) => (
  <GlassCard className="min-w-[280px] flex-shrink-0 overflow-hidden p-0">
    {/* Gradient header band */}
    <div className={`h-36 w-full ${gradient} flex items-end p-5`}>
      <div>
        <h3 className="text-2xl font-display font-bold text-white drop-shadow-md">
          {city}
        </h3>
        <p className="text-white/80 text-sm font-body">{country}</p>
      </div>
    </div>

    {/* Card body */}
    <div className="p-5 space-y-3">
      <p className="text-warm-gray text-sm font-body">Starting from</p>
      <p className="text-2xl font-bold text-charcoal font-display">{price}</p>
      <a
        href="#"
        className="inline-flex items-center gap-1 text-brand-blue text-sm font-semibold hover:underline font-body"
      >
        Explore
        <span aria-hidden="true">→</span>
      </a>
    </div>
  </GlassCard>
);

const Destinations = () => {
  const destinations: DestinationProps[] = [
    {
      city: "Dubai",
      country: "United Arab Emirates",
      price: "$1,240",
      gradient: "bg-gradient-to-br from-brand-gold/80 to-amber-600/70",
    },
    {
      city: "Singapore",
      country: "Republic of Singapore",
      price: "$1,580",
      gradient: "bg-gradient-to-br from-emerald-500/70 to-teal-600/60",
    },
    {
      city: "London",
      country: "United Kingdom",
      price: "$980",
      gradient: "bg-gradient-to-br from-sky-500/70 to-indigo-600/60",
    },
    {
      city: "Tokyo",
      country: "Japan",
      price: "$1,420",
      gradient: "bg-gradient-to-br from-rose-400/70 to-pink-600/60",
    },
    {
      city: "New York",
      country: "United States",
      price: "$860",
      gradient: "bg-gradient-to-br from-violet-500/70 to-purple-700/60",
    },
  ];

  return (
    <section className="section flex flex-col justify-center px-6 sm:px-12 lg:px-24">
      {/* ── Centered header ── */}
      <div className="text-center mb-12 space-y-4">
        <span className="text-brand-blue text-sm font-semibold uppercase tracking-widest font-body">
          Destinations
        </span>

        <h2 className="text-5xl font-display font-bold text-charcoal tracking-tight">
          <AnimatedText>Where Will You Go?</AnimatedText>
        </h2>
      </div>

      {/* ── Scrollable card row ── */}
      <div className="flex gap-6 overflow-x-auto pb-4 snap-x snap-mandatory scrollbar-hide">
        {destinations.map((dest) => (
          <div key={dest.city} className="snap-start">
            <DestinationCard {...dest} />
          </div>
        ))}
      </div>
    </section>
  );
};

export default Destinations;
