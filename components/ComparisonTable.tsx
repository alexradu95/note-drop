import React from 'react';
import { Check, X, AlertTriangle } from 'lucide-react';

const ComparisonTable: React.FC = () => {
  return (
    <section id="comparison" className="py-20 bg-md-surface-2/50">
      <div className="max-w-6xl mx-auto px-6">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-5xl font-bold text-white mb-6">Why NoteDrop?</h2>
          <p className="text-gray-400 text-xl max-w-2xl mx-auto">
            Stop waiting for apps to load. Compare the speed and privacy of NoteDrop against the rest.
          </p>
        </div>

        <div className="overflow-x-auto rounded-[2rem] border border-white/5 shadow-2xl">
          <table className="w-full text-left border-collapse min-w-[600px]">
            <thead>
              <tr className="bg-md-surface-3 text-white">
                <th className="p-6 font-bold text-lg">Feature</th>
                <th className="p-6 font-bold text-lg text-md-primary bg-md-primary/10">NoteDrop</th>
                <th className="p-6 font-bold text-lg text-gray-400">Standard Note Apps</th>
                <th className="p-6 font-bold text-lg text-purple-300">Obsidian Mobile</th>
              </tr>
            </thead>
            <tbody className="bg-md-surface">
              {[
                { label: "Capture Speed", noteDrop: "< 2 sec (widget)", other: "5-10 sec (launch)", native: "3-5 sec (launch)" },
                { label: "Home Screen Widgets", noteDrop: "Multiple types", other: "Basic/None", native: "Limited" },
                { label: "Direct Vault Sync", noteDrop: "Instant Local", other: "Cloud/Export req.", native: "Manual Save" },
                { label: "Offline Support", noteDrop: "Complete", other: "Often Limited", native: "Complete" },
                { label: "Privacy (No Cloud)", noteDrop: "100% Local", other: "Usually Cloud", native: "Local-First" },
                { label: "Daily Notes Append", noteDrop: "Automatic", other: "Manual", native: "Manual Nav" },
              ].map((row, i) => (
                <tr key={i} className="border-t border-white/5 hover:bg-white/5 transition-colors">
                  <td className="p-6 text-gray-300 font-medium">{row.label}</td>
                  <td className="p-6 text-white font-bold bg-md-primary/5 border-l border-r border-md-primary/10 flex items-center gap-2">
                    <Check className="w-4 h-4 text-md-primary" /> {row.noteDrop}
                  </td>
                  <td className="p-6 text-gray-500">
                     <div className="flex items-center gap-2">
                        {row.other.includes("Limited") || row.other.includes("Cloud") ? <AlertTriangle className="w-4 h-4 text-yellow-500"/> : null}
                        {row.other}
                     </div>
                  </td>
                  <td className="p-6 text-gray-400">
                    <div className="flex items-center gap-2">
                       {row.native.includes("Manual") || row.native.includes("Limited") ? <AlertTriangle className="w-4 h-4 text-yellow-500"/> : <Check className="w-4 h-4 text-gray-500" />}
                       {row.native}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
};

export default ComparisonTable;