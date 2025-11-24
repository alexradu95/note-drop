import React from 'react';
import { Droplets, Twitter, Github, Linkedin, Mail } from 'lucide-react';

const Footer: React.FC = () => {
  return (
    <footer className="bg-md-bg border-t border-white/5 pt-20 pb-10">
      <div className="max-w-7xl mx-auto px-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-16">
          <div className="col-span-1 md:col-span-1">
            <div className="flex items-center space-x-2 mb-6">
              <div className="w-10 h-10 bg-md-secondary-container rounded-xl flex items-center justify-center">
                <Droplets className="text-md-primary w-6 h-6 fill-current" />
              </div>
              <span className="text-2xl font-bold tracking-tight text-white">NoteDrop</span>
            </div>
            <p className="text-gray-500 text-sm leading-relaxed mb-6">
              The fastest way to capture notes into Obsidian. <br/>
              Local. Private. Frictionless.
            </p>
            <div className="flex space-x-4">
              <a href="#" className="text-gray-500 hover:text-white transition-colors"><Twitter className="w-5 h-5" /></a>
              <a href="#" className="text-gray-500 hover:text-white transition-colors"><Github className="w-5 h-5" /></a>
              <a href="mailto:support@notedrop.app" className="text-gray-500 hover:text-white transition-colors"><Mail className="w-5 h-5" /></a>
            </div>
          </div>

          {[
              { title: "Product", items: ["Download", "Changelog", "Roadmap", "Pricing"] },
              { title: "Resources", items: ["Obsidian Setup Guide", "Documentation", "Community Forum", "Help Center"] },
              { title: "Legal", items: ["Privacy Policy", "Terms of Service"] }
          ].map((col, idx) => (
            <div key={idx}>
                <h4 className="text-white font-bold mb-6 text-lg">{col.title}</h4>
                <ul className="space-y-4 text-sm text-gray-400">
                {col.items.map(item => (
                    <li key={item}><a href="#" className="hover:text-md-primary transition-colors hover:underline decoration-md-primary underline-offset-4">{item}</a></li>
                ))}
                </ul>
            </div>
          ))}
        </div>

        <div className="border-t border-white/5 pt-8 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-gray-600 text-sm">
            © {new Date().getFullYear()} NoteDrop. Not affiliated with Obsidian.
          </p>
          <div className="flex items-center gap-2">
             <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
             <span className="text-gray-500 text-sm">Systems Operational</span>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;