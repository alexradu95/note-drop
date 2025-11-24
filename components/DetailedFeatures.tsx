import React from 'react';
import { Layout, FileJson, Hash, Link } from 'lucide-react';

const DetailedFeatures: React.FC = () => {
  return (
    <section className="py-24 bg-md-bg">
      <div className="max-w-7xl mx-auto px-6">
        
        {/* Widget Collection */}
        <div className="mb-24">
          <h2 className="text-3xl font-bold text-white mb-10 text-center">Widget Collection</h2>
          <div className="grid md:grid-cols-4 gap-4">
             {[
               { title: "Quick Capture", desc: "All 3 modes in one widget.", color: "bg-gray-800" },
               { title: "Text Widget", desc: "Minimalist text input.", color: "bg-md-primary/20 text-md-primary" },
               { title: "Voice Widget", desc: "Instant recording.", color: "bg-purple-900/30 text-purple-300" },
               { title: "Camera Widget", desc: "Photo + caption.", color: "bg-yellow-900/30 text-yellow-300" },
             ].map((widget, i) => (
               <div key={i} className={`p-6 rounded-[2rem] border border-white/5 ${widget.color} flex flex-col justify-center items-center text-center aspect-square md:aspect-auto md:h-40`}>
                  <Layout className="w-8 h-8 mb-3 opacity-80" />
                  <h3 className="font-bold mb-1">{widget.title}</h3>
                  <p className="text-xs opacity-70">{widget.desc}</p>
               </div>
             ))}
          </div>
        </div>

        {/* Obsidian Deep Dive */}
        <div className="bg-md-surface rounded-[2.5rem] p-8 md:p-12 border border-white/5">
           <div className="text-center mb-12">
             <h2 className="text-3xl font-bold text-white">Obsidian-Specific Integration</h2>
             <p className="text-gray-400 mt-2">We read your config files to ensure perfect harmony.</p>
           </div>
           
           <div className="grid md:grid-cols-2 gap-12">
              <div className="space-y-6">
                 <div className="flex gap-4">
                    <div className="w-10 h-10 rounded-full bg-md-surface-3 flex items-center justify-center shrink-0">
                       <FileJson className="w-5 h-5 text-gray-300" />
                    </div>
                    <div>
                       <h4 className="text-white font-bold text-lg">Config Parsing</h4>
                       <p className="text-gray-400 text-sm leading-relaxed">
                         Reads <code className="bg-black/30 px-1 rounded">.obsidian/daily-notes.json</code> to respect your specific date formats (YYYY-MM-DD), folder locations, and templates.
                       </p>
                    </div>
                 </div>
                 <div className="flex gap-4">
                    <div className="w-10 h-10 rounded-full bg-md-surface-3 flex items-center justify-center shrink-0">
                       <Hash className="w-5 h-5 text-gray-300" />
                    </div>
                    <div>
                       <h4 className="text-white font-bold text-lg">Smart Header Insertion</h4>
                       <p className="text-gray-400 text-sm leading-relaxed">
                         Appends notes under your custom header (e.g. <code className="bg-black/30 px-1 rounded">## Captured</code>). Creates the header if it doesn't exist.
                       </p>
                    </div>
                 </div>
              </div>

              <div className="space-y-6">
                 <div className="flex gap-4">
                    <div className="w-10 h-10 rounded-full bg-md-surface-3 flex items-center justify-center shrink-0">
                       <Link className="w-5 h-5 text-gray-300" />
                    </div>
                    <div>
                       <h4 className="text-white font-bold text-lg">Markdown Preservation</h4>
                       <p className="text-gray-400 text-sm leading-relaxed">
                         Maintains frontmatter, wiki-links <code className="bg-black/30 px-1 rounded">[[like this]]</code>, and tags. No messed up formatting.
                       </p>
                    </div>
                 </div>
                 <div className="flex gap-4">
                    <div className="w-10 h-10 rounded-full bg-md-surface-3 flex items-center justify-center shrink-0">
                       <Layout className="w-5 h-5 text-gray-300" />
                    </div>
                    <div>
                       <h4 className="text-white font-bold text-lg">Templates</h4>
                       <p className="text-gray-400 text-sm leading-relaxed">
                         Applies your chosen templates when creating new daily notes automatically.
                       </p>
                    </div>
                 </div>
              </div>
           </div>
        </div>

      </div>
    </section>
  );
};

export default DetailedFeatures;