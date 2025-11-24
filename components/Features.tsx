import React from 'react';
import { Smartphone, Database, Mic, Lock, Zap, Sliders } from 'lucide-react';
import { Feature } from '../types';

const features: Feature[] = [
  {
    title: "Lightning-Fast Capture",
    description: "From thought to note in under 2 seconds. Our home screen widgets eliminate every barrier between you and your notes.",
    icon: Zap
  },
  {
    title: "Obsidian Vault Integration",
    description: "Direct sync to your vault. No cloud middleman. Works with Daily Notes, templates, and your custom folder structure.",
    icon: Database
  },
  {
    title: "Multiple Capture Modes",
    description: "Capture however you think. Type quick notes, record voice memos, or snap photos with instant captions.",
    icon: Mic
  },
  {
    title: "Flexible & Customizable",
    description: "Your workflow, your rules. Configurable timestamps, save locations (Inbox vs Daily), and file naming strategies.",
    icon: Sliders
  },
  {
    title: "Local-First & Private",
    description: "Your notes never touch our servers. NoteDrop operates entirely on your device using Android's secure file access.",
    icon: Lock
  },
  {
    title: "Universal Android",
    description: "Available on Phone, Tablet, Wear OS, and Android TV. Capture from anywhere in your ecosystem.",
    icon: Smartphone
  }
];

const Features: React.FC = () => {
  return (
    <section id="features" className="py-32 bg-md-bg relative">
      <div className="max-w-7xl mx-auto px-6">
        <div className="mb-20 text-center">
            <span className="text-md-primary font-bold tracking-wider uppercase text-sm">Key Features</span>
            <h2 className="text-4xl md:text-6xl font-bold text-white mt-4 mb-6">Built for Frictionless<br/>Thoughts.</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <div 
              key={index} 
              className="group p-8 rounded-[2rem] bg-md-surface-2 hover:bg-md-surface-3 border border-white/5 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_40px_rgba(0,0,0,0.4)] flex flex-col items-start relative overflow-hidden"
            >
              <div className="w-14 h-14 bg-md-surface-3 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 group-hover:bg-md-primary-container transition-all duration-300">
                <feature.icon className="w-7 h-7 text-gray-400 group-hover:text-md-primary transition-colors" />
              </div>
              
              <h3 className="text-2xl font-bold text-white mb-3">{feature.title}</h3>
              <p className="text-gray-400 text-lg leading-relaxed font-light">
                {feature.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Features;