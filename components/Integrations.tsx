import React from 'react';

const Integrations: React.FC = () => {
  return (
    <section id="integrations" className="py-24 border-y border-white/5 bg-md-surface text-center">
      <div className="max-w-4xl mx-auto px-6">
        <h2 className="text-3xl font-bold text-white mb-10">
           Primary Integration: <span className="text-purple-400">Obsidian</span>
        </h2>
        
        <div className="flex flex-wrap justify-center items-center gap-4 mb-8">
            <div className="px-8 py-4 rounded-2xl bg-purple-900/20 border border-purple-500/30 text-purple-200 text-xl font-bold flex items-center gap-3 shadow-[0_0_30px_rgba(168,85,247,0.15)]">
               <div className="w-3 h-3 rounded-full bg-purple-500 animate-pulse"></div>
               Obsidian Vault
            </div>
        </div>
        
        <p className="text-gray-500 mb-8">Also compatible with standard Markdown workflows</p>

        <div className="flex flex-wrap justify-center gap-3 text-sm font-medium text-gray-600">
            <span className="px-4 py-2 rounded-full border border-gray-800 bg-gray-900/50">Logseq (Coming Soon)</span>
            <span className="px-4 py-2 rounded-full border border-gray-800 bg-gray-900/50">Notion (Coming Soon)</span>
            <span className="px-4 py-2 rounded-full border border-gray-800 bg-gray-900/50">Capacities (Coming Soon)</span>
        </div>
      </div>
    </section>
  );
};

export default Integrations;