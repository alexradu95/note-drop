import React, { useState } from 'react';
import { Loader2, Mic, Camera, FileText, ArrowDown, Save } from 'lucide-react';

const DemoSection: React.FC = () => {
  const [input, setInput] = useState("- [ ] Review quarter results #work");
  const [simulatedTime, setSimulatedTime] = useState(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
  const [output, setOutput] = useState("");
  const [loading, setLoading] = useState(false);

  const handleCapture = () => {
    setLoading(true);
    setSimulatedTime(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
    
    setTimeout(() => {
        setOutput(`## Captured: ${simulatedTime}\n${input}\n\n`);
        setLoading(false);
    }, 800);
  };

  return (
    <section id="how-it-works" className="py-32 bg-md-surface-2 relative overflow-hidden">
      <div className="max-w-6xl mx-auto px-6">
        <div className="grid lg:grid-cols-2 gap-16 items-center">
          
          {/* Text Content */}
          <div className="order-2 lg:order-1">
            <span className="text-md-primary font-bold tracking-wider uppercase text-sm">How It Works</span>
            <h2 className="text-4xl md:text-5xl font-bold text-white mb-6 mt-2">
              Three Simple Steps.
            </h2>
            <p className="text-gray-400 text-xl mb-10 leading-relaxed font-light">
              See how fast it is. Type a note in the widget, tap save, and it's instantly appended to your Daily Note or Inbox file.
            </p>
            
            <div className="space-y-6">
                <div className="flex gap-4 group">
                    <div className="w-10 h-10 rounded-full bg-md-primary/20 flex items-center justify-center text-md-primary font-bold shrink-0 group-hover:bg-md-primary group-hover:text-md-on-primary transition-colors">1</div>
                    <div>
                        <h4 className="text-white font-bold text-lg">Tap the Widget</h4>
                        <p className="text-gray-400">Add the NoteDrop widget to your home screen.</p>
                    </div>
                </div>
                <div className="flex gap-4 group">
                    <div className="w-10 h-10 rounded-full bg-md-primary/20 flex items-center justify-center text-md-primary font-bold shrink-0 group-hover:bg-md-primary group-hover:text-md-on-primary transition-colors">2</div>
                    <div>
                        <h4 className="text-white font-bold text-lg">Capture Instantly</h4>
                        <p className="text-gray-400">Type, speak, or shoot. No app opening, no delays.</p>
                    </div>
                </div>
                <div className="flex gap-4 group">
                    <div className="w-10 h-10 rounded-full bg-md-primary/20 flex items-center justify-center text-md-primary font-bold shrink-0 group-hover:bg-md-primary group-hover:text-md-on-primary transition-colors">3</div>
                    <div>
                        <h4 className="text-white font-bold text-lg">Auto-Sync to Vault</h4>
                        <p className="text-gray-400">Notes appear in your daily note or inbox automatically.</p>
                    </div>
                </div>
            </div>
          </div>

          {/* Interactive UI */}
          <div className="order-1 lg:order-2 flex flex-col items-center gap-8">
            
            {/* Widget Simulator */}
            <div className="w-full max-w-sm bg-md-surface p-4 rounded-[1.5rem] border border-white/10 shadow-2xl relative">
                <div className="absolute -top-3 left-6 text-xs text-gray-500 uppercase tracking-widest font-bold bg-md-surface-2 px-2">Android Widget</div>
                <div className="flex gap-2 mb-3">
                    <button className="flex-1 p-2 bg-md-primary text-md-on-primary rounded-xl flex items-center justify-center gap-2 font-bold text-sm">
                        <FileText className="w-4 h-4" /> Text
                    </button>
                    <button className="flex-1 p-2 bg-md-surface-3 text-gray-300 rounded-xl flex items-center justify-center hover:bg-white/10">
                        <Mic className="w-4 h-4" />
                    </button>
                    <button className="flex-1 p-2 bg-md-surface-3 text-gray-300 rounded-xl flex items-center justify-center hover:bg-white/10">
                        <Camera className="w-4 h-4" />
                    </button>
                </div>
                <textarea 
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    className="w-full bg-md-surface-3 rounded-xl p-3 text-white text-sm outline-none border border-transparent focus:border-md-primary/50 resize-none h-24 font-mono"
                    placeholder="Type quick note..."
                />
                <button 
                    onClick={handleCapture}
                    disabled={loading}
                    className="mt-3 w-full py-2 bg-md-secondary-container text-md-secondary font-bold rounded-xl flex items-center justify-center gap-2 hover:bg-opacity-80 transition-all"
                >
                    {loading ? <Loader2 className="w-4 h-4 animate-spin"/> : <Save className="w-4 h-4"/>}
                    Drop to Vault
                </button>
            </div>

            <ArrowDown className="text-gray-600 animate-bounce" />

            {/* Obsidian File Simulator */}
            <div className="w-full max-w-sm bg-[#1e1e1e] p-6 rounded-[1rem] border border-gray-800 shadow-2xl font-mono text-sm relative overflow-hidden min-h-[160px]">
                <div className="absolute top-0 left-0 w-full h-8 bg-[#252525] flex items-center px-4 text-xs text-gray-500 border-b border-black">
                    daily-notes/{new Date().toISOString().split('T')[0]}.md
                </div>
                <div className="pt-6 text-gray-400">
                    <p># Daily Notes</p>
                    <br/>
                    <p>## Morning Routine</p>
                    <p>- [x] Coffee</p>
                    <p>- [x] Meditate</p>
                    <br/>
                    {output ? (
                        <div className="animate-fade-in-up">
                            <p className="text-purple-400">{output.split('\n')[0]}</p>
                            <p className="text-white">{output.split('\n')[1]}</p>
                        </div>
                    ) : (
                        <span className="text-gray-700 italic">... waiting for capture ...</span>
                    )}
                </div>
            </div>

          </div>

        </div>
      </div>
    </section>
  );
};

export default DemoSection;