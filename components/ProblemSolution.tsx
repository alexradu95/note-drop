import React from 'react';
import { AlertCircle, CheckCircle2 } from 'lucide-react';

const ProblemSolution: React.FC = () => {
  return (
    <section className="py-20 bg-md-bg relative">
      <div className="max-w-6xl mx-auto px-6">
        <div className="grid md:grid-cols-2 gap-8 md:gap-16">
          
          {/* The Problem */}
          <div className="bg-md-surface-2/50 p-10 rounded-[2.5rem] border border-red-500/10 hover:border-red-500/20 transition-all">
            <div className="flex items-center gap-3 mb-6">
              <AlertCircle className="w-8 h-8 text-red-400" />
              <h2 className="text-2xl font-bold text-white">The Problem</h2>
            </div>
            <h3 className="text-3xl font-bold text-red-200 mb-4">Great ideas vanish in seconds.</h3>
            <p className="text-gray-400 text-lg leading-relaxed">
              By the time you unlock your phone, open Obsidian, navigate to the right place, and start typing... you've forgotten what you wanted to capture.
              <br/><br/>
              Traditional note apps are too slow. Cloud services create sync friction. And Obsidian mobile, while powerful, isn't optimized for quick capture.
            </p>
          </div>

          {/* The Solution */}
          <div className="bg-md-primary-container/20 p-10 rounded-[2.5rem] border border-md-primary/20 hover:border-md-primary/40 transition-all">
            <div className="flex items-center gap-3 mb-6">
              <CheckCircle2 className="w-8 h-8 text-md-primary" />
              <h2 className="text-2xl font-bold text-white">The Solution</h2>
            </div>
            <h3 className="text-3xl font-bold text-md-primary mb-4">NoteDrop makes capture instant.</h3>
            <p className="text-gray-300 text-lg leading-relaxed">
              Tap a widget. Your thought is captured. It's synced to your vault. Done.
              <br/><br/>
              No app opening. No navigation. No friction. Just pure, instant capture the way it should be.
            </p>
          </div>

        </div>
      </div>
    </section>
  );
};

export default ProblemSolution;