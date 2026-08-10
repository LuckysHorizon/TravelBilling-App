/**
 * BookingCTA.tsx — Final call-to-action section
 *
 * Centered layout with a large headline, supporting copy, a primary
 * "Book Your Flight" button (with framer-motion hover scale), and a
 * secondary "View All Routes →" text link. Background is transparent
 * to let the 3D canvas dominate.
 */

import { motion } from "framer-motion";
import AnimatedText from "../ui/AnimatedText";

const BookingCTA = () => {
  return (
    <section className="section flex flex-col justify-center items-center text-center px-6 sm:px-12 lg:px-24">
      <div className="max-w-2xl space-y-8">
        {/* Headline */}
        <h2 className="text-6xl md:text-7xl font-display font-bold text-charcoal tracking-tighter leading-[0.95]">
          <AnimatedText>Your Journey Begins</AnimatedText>
        </h2>

        {/* Subtext */}
        <p className="text-xl text-warm-gray leading-relaxed font-body">
          Book your next flight with RamnetSolutions and experience the
          difference. Seamless booking, world-class service, unforgettable
          destinations.
        </p>

        {/* CTA button with hover scale */}
        <div className="flex flex-col items-center gap-5 pt-4">
          <motion.a
            href="#"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.98 }}
            transition={{ type: "spring", stiffness: 400, damping: 20 }}
            className="inline-block bg-brand-blue text-white px-10 py-4 rounded-full text-lg font-semibold shadow-lg shadow-brand-blue/25 hover:bg-brand-blue/90 transition-colors font-body"
          >
            Book Your Flight
          </motion.a>

          {/* Secondary link */}
          <a
            href="#"
            className="text-brand-blue text-sm font-semibold hover:underline inline-flex items-center gap-1 font-body"
          >
            View All Routes
            <span aria-hidden="true">→</span>
          </a>
        </div>
      </div>
    </section>
  );
};

export default BookingCTA;
