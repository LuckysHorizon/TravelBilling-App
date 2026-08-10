/**
 * Footer — Clean black/white footer
 */
export default function Footer() {
  const LINKS: Record<string, string[]> = {
    Product: ['Features', 'Pricing', 'AI Engine', 'Dashboard', 'Reports', 'Integrations'],
    Company: ['About', 'Careers', 'Press', 'Partners', 'Blog'],
    Support: ['Help Center', 'Documentation', 'API Reference', 'Status', 'Contact'],
    Legal: ['Privacy Policy', 'Terms of Service', 'Cookie Policy', 'GDPR'],
  }

  return (
    <footer className="bg-surface-tertiary border-t border-border-light">
      <div className="section-container px-6">
        <div className="py-12 grid grid-cols-2 md:grid-cols-5 gap-8">
          <div className="col-span-2 md:col-span-1">
            <div className="flex items-center gap-2 mb-3">
              <div className="w-6 h-6 rounded-md bg-black flex items-center justify-center">
                <span className="text-white text-[10px] font-bold">T</span>
              </div>
              <span className="font-semibold text-sm text-text-primary">TravelBilling Pro</span>
            </div>
            <p className="text-xs text-text-tertiary leading-relaxed max-w-[200px]">
              AI-powered billing and invoice management for modern travel agencies.
            </p>
          </div>

          {Object.entries(LINKS).map(([title, links]) => (
            <div key={title}>
              <h4 className="text-xs font-semibold text-text-primary mb-3">{title}</h4>
              <ul className="space-y-2">
                {links.map((link) => (
                  <li key={link}>
                    <a href="#" className="text-xs text-text-tertiary hover:text-text-primary transition-colors duration-200">
                      {link}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="py-5 border-t border-border-light flex flex-col md:flex-row items-center justify-between gap-3">
          <p className="text-[11px] text-text-tertiary">
            © {new Date().getFullYear()} TravelBilling Pro. All rights reserved.
          </p>
          <div className="flex items-center gap-5">
            {['Twitter', 'LinkedIn', 'GitHub'].map((s) => (
              <a key={s} href="#" className="text-[11px] text-text-tertiary hover:text-text-primary transition-colors">{s}</a>
            ))}
          </div>
        </div>
      </div>
    </footer>
  )
}
