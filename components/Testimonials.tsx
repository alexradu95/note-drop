import React from 'react';
import { Testimonial } from '../types';
import { Quote } from 'lucide-react';

const testimonials: Testimonial[] = [
  {
    quote: "This is the missing piece for Obsidian on Android. I've tried every quick capture solution, and this is the first that actually works seamlessly with my vault structure.",
    author: "Sarah K.",
    role: "Zettelkasten Practitioner"
  },
  {
    quote: "I've been building my second brain for 3 years. NoteDrop finally makes mobile capture as friction-free as it should be. The daily notes integration is perfect.",
    author: "Michael T.",
    role: "Knowledge Worker"
  },
  {
    quote: "As a GTD practitioner, quick capture is non-negotiable. NoteDrop's widgets are faster than anything I've used. My inbox processing in Obsidian has never been smoother.",
    author: "David R.",
    role: "Productivity Coach"
  }
];

const Testimonials: React.FC = () => {
  return (
    <section className="py-24 bg-md-bg border-t border-white/5">
      <div className="max-w-7xl mx-auto px-6">
        <h2 className="text-3xl font-bold text-center text-white mb-16">Trusted by the PKM Community</h2>
        
        <div className="grid md:grid-cols-3 gap-8">
          {testimonials.map((t, i) => (
            <div key={i} className="bg-md-surface p-8 rounded-[2rem] relative">
              <Quote className="w-8 h-8 text-md-primary/30 mb-4 absolute top-8 right-8" />
              <p className="text-gray-300 leading-relaxed mb-6 italic">"{t.quote}"</p>
              <div>
                <p className="text-white font-bold">{t.author}</p>
                <p className="text-sm text-md-primary">{t.role}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Testimonials;