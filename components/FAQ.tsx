import React from 'react';
import { FAQItem } from '../types';
import { ChevronDown } from 'lucide-react';

const faqs: FAQItem[] = [
  {
    question: "Does NoteDrop work without Obsidian?",
    answer: "Yes! NoteDrop works with any folder structure. Just choose 'Simple Folder' mode and point it to where you want your notes saved as markdown files."
  },
  {
    question: "Is there an iOS version?",
    answer: "Not yet. NoteDrop is currently Android-only. iOS version is being considered based on demand."
  },
  {
    question: "Does it work with Obsidian Sync?",
    answer: "Yes! NoteDrop writes to your local vault. Obsidian Sync handles cloud synchronization as normal."
  },
  {
    question: "Where is my data stored?",
    answer: "Only on your device. Notes are captured to local storage and synced to your vault folder. Nothing is sent to servers."
  },
  {
    question: "How fast is 'instant' capture?",
    answer: "From widget tap to saved note: under 2 seconds typically. Depends on device speed and vault location."
  }
];

const FAQ: React.FC = () => {
  return (
    <section id="faq" className="py-24 bg-md-surface-2">
      <div className="max-w-3xl mx-auto px-6">
        <h2 className="text-3xl font-bold text-center text-white mb-12">Frequently Asked Questions</h2>
        
        <div className="space-y-4">
          {faqs.map((faq, i) => (
            <details key={i} className="group bg-md-surface rounded-2xl p-6 cursor-pointer border border-white/5 hover:border-white/10 transition-all">
              <summary className="flex justify-between items-center font-bold text-lg text-white list-none">
                {faq.question}
                <ChevronDown className="w-5 h-5 text-gray-500 transition-transform group-open:rotate-180" />
              </summary>
              <p className="mt-4 text-gray-400 leading-relaxed pr-8">
                {faq.answer}
              </p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
};

export default FAQ;