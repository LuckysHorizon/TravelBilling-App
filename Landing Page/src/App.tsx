/**
 * App — Root component with Lenis + GSAP ScrollTrigger integration
 */
import { useEffect } from 'react'
import Lenis from 'lenis'
import { gsap, ScrollTrigger } from './lib/gsapConfig'

import Navbar from './components/ui/Navbar'
import Hero from './components/sections/Hero'
import LogoCloud from './components/sections/LogoCloud'
import Problem from './components/sections/Problem'
import Features from './components/sections/Features'
import AISpotlight from './components/sections/AISpotlight'
import HowItWorks from './components/sections/HowItWorks'
import Stats from './components/sections/Stats'
import Testimonials from './components/sections/Testimonials'
import Pricing from './components/sections/Pricing'
import FAQ from './components/sections/FAQ'
import CTA from './components/sections/CTA'
import Footer from './components/sections/Footer'

export default function App() {
  useEffect(() => {
    const lenis = new Lenis({
      duration: 1.2,
      easing: (t: number) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
    })

    // Sync Lenis → ScrollTrigger
    lenis.on('scroll', ScrollTrigger.update)

    // Drive Lenis from GSAP ticker (single clock)
    const tick = (time: number) => lenis.raf(time * 1000)
    gsap.ticker.add(tick)
    gsap.ticker.lagSmoothing(0)

    return () => {
      gsap.ticker.remove(tick)
      lenis.destroy()
    }
  }, [])

  return (
    <>
      <Navbar />
      <main>
        <Hero />
        <LogoCloud />
        <Problem />
        <Features />
        <AISpotlight />
        <HowItWorks />
        <Stats />
        <Testimonials />
        <Pricing />
        <FAQ />
        <CTA />
      </main>
      <Footer />
    </>
  )
}
