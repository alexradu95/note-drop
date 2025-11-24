import React from 'react';
import { ArrowRight, Zap, Smartphone, Mic, Camera, FileText, Database, Play } from 'lucide-react';

const Hero: React.FC = () => {
  return (
    <section className="relative min-h-screen pt-32 pb-20 overflow-hidden flex flex-col justify-center">
      {/* Material You Animated Background Blobs */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden -z-10 opacity-30">
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-md-primary-container rounded-full mix-blend-multiply filter blur-[80px] animate-blob"></div>
        <div className="absolute top-20 right-1/4 w-80 h-80 bg-purple-900 rounded-full mix-blend-multiply filter blur-[80px] animate-blob [animation-delay:2s]"></div>
        <div className="absolute -bottom-32 left-1/3 w-96 h-96 bg-md-secondary-container rounded-full mix-blend-multiply filter blur-[80px] animate-blob [animation-delay:4s]"></div>
      </div>

      <div className="max-w-7xl mx-auto px-6 text-center relative z-10">
        
        {/* Chip */}
        <div className="inline-flex items-center pl-2 pr-4 py-1.5 rounded-full border border-md-outline/30 bg-md-surface-2 text-md-primary text-sm font-medium mb-10 animate-fade-in-up hover:bg-md-surface-3 transition-colors cursor-pointer shadow-lg">
          <span className="flex items-center justify-center h-6 w-6 rounded-full bg-md-primary text-md-on-primary mr-3 text-xs font-bold">
            <Zap className="w-3 h-3" />
          </span>
          Capture in &lt; 2 seconds
        </div>

        {/* Display Large Typography */}
        <h1 className="text-5xl md:text-7xl lg:text-8xl font-black tracking-tighter text-white mb-8 leading-[1.1] animate-fade-in-up [animation-delay:100ms]">
          Capture Notes in Seconds. <br />
          Sync to <span className="text-purple-400 relative inline-block">Obsidian.</span>
        </h1>

        <p className="text-xl md:text-2xl text-gray-300 max-w-3xl mx-auto mb-10 animate-fade-in-up [animation-delay:200ms] font-light">
          Lightning-fast note capture for Android. 
          <br className="hidden md:block"/>
          <span className="text-white font-medium">Home screen widgets that add notes directly to your vault.</span>
        </p>

        {/* Buttons */}
        <div className="flex flex-col sm:flex-row justify-center items-center gap-6 animate-fade-in-up [animation-delay:300ms]">
          <button className="group w-full sm:w-auto h-16 px-10 rounded-full bg-md-primary text-md-on-primary text-lg font-bold shadow-lg shadow-md-primary/20 hover:shadow-xl hover:shadow-md-primary/30 transition-all transform hover:-translate-y-1 active:scale-95 flex items-center justify-center gap-3">
            <Smartphone className="w-5 h-5" />
            Download on Google Play
            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </button>
          <button className="w-full sm:w-auto h-16 px-10 rounded-full bg-md-surface-3 border border-md-outline/20 text-md-secondary text-lg font-semibold hover:bg-md-surface-2 transition-all transform hover:-translate-y-1 active:scale-95 flex items-center justify-center gap-3">
            <Play className="w-5 h-5 fill-current" />
            Watch Demo
          </button>
        </div>

        {/* Visual: Simulated Home Screen Widgets */}
        <div className="mt-24 relative max-w-5xl mx-auto h-64 hidden md:block">
            {/* Widget 1: Quick Actions */}
            <div className="absolute left-[15%] top-0 animate-float [animation-delay:0ms] rotate-[-3deg] hover:rotate-0 transition-transform duration-500 z-10">
                <div className="w-72 p-5 bg-[#2B2D31] rounded-[2rem] border border-white/5 shadow-2xl flex flex-col gap-4">
                    <div className="flex justify-between items-center text-gray-400 text-xs uppercase tracking-widest font-bold">
                        <span>Quick Capture</span>
                        <div className="w-2 h-2 rounded-full bg-green-500"></div>
                    </div>
                    <div className="flex gap-3">
                        <div className="flex-1 aspect-square rounded-2xl bg-[#3E4045] hover:bg-[#4E5055] transition-colors flex flex-col items-center justify-center gap-2 cursor-pointer group">
                             <Mic className="w-6 h-6 text-purple-400 group-hover:scale-110 transition-transform" />
                             <span className="text-xs text-gray-300">Voice</span>
                        </div>
                        <div className="flex-1 aspect-square rounded-2xl bg-[#3E4045] hover:bg-[#4E5055] transition-colors flex flex-col items-center justify-center gap-2 cursor-pointer group">
                             <Camera className="w-6 h-6 text-yellow-400 group-hover:scale-110 transition-transform" />
                             <span className="text-xs text-gray-300">Cam</span>
                        </div>
                        <div className="flex-1 aspect-square rounded-2xl bg-md-primary hover:bg-md-primary-container transition-colors flex flex-col items-center justify-center gap-2 cursor-pointer group">
                             <FileText className="w-6 h-6 text-md-on-primary group-hover:scale-110 transition-transform" />
                             <span className="text-xs text-md-on-primary font-bold">Text</span>
                        </div>
                    </div>
                </div>
            </div>
            
            {/* Widget 2: Recent Note (Obsidian Style) */}
            <div className="absolute left-1/2 -translate-x-1/2 top-10 z-0 animate-float [animation-delay:1000ms] hover:scale-105 transition-transform duration-500">
                <div className="w-96 p-6 bg-[#1A1A1A] rounded-[2rem] border border-purple-500/20 shadow-[0_20px_50px_rgba(0,0,0,0.5)] flex flex-col gap-3">
                    <div className="flex items-center gap-3 border-b border-white/10 pb-3">
                      <Database className="w-5 h-5 text-purple-400" />
                      <span className="text-purple-100 font-mono text-sm">Vault / Daily Notes</span>
                    </div>
                    <div className="space-y-2 font-mono text-xs text-gray-400">
                       <p><span className="text-purple-400">#</span> 2025-01-24</p>
                       <p className="pl-4 border-l-2 border-md-primary/30 text-gray-300">
                         - [ ] Capture new app idea <span className="text-md-primary">#ideas</span>
                       </p>
                       <p className="pl-4 border-l-2 border-yellow-500/30 text-gray-300">
                         - Meeting notes: Quick sync ...
                       </p>
                    </div>
                </div>
            </div>

            {/* Widget 3: Voice Note */}
            <div className="absolute right-[15%] top-8 animate-float [animation-delay:2000ms] rotate-[3deg] hover:rotate-0 transition-transform duration-500 z-10">
                <div className="w-64 p-4 bg-md-secondary-container rounded-[2rem] border border-white/5 shadow-2xl flex items-center gap-4">
                    <div className="w-12 h-12 rounded-full bg-md-on-secondary-container/20 flex items-center justify-center text-md-on-secondary-container animate-pulse-slow">
                      <Mic className="w-6 h-6" />
                    </div>
                    <div className="flex flex-col gap-1">
                      <span className="text-md-on-secondary-container text-sm font-bold">Idea from walk...</span>
                      <div className="h-1.5 w-24 bg-md-on-secondary-container/20 rounded-full overflow-hidden">
                          <div className="h-full bg-md-on-secondary-container w-2/3 animate-pulse"></div>
                      </div>
                    </div>
                </div>
            </div>
        </div>
      </div>
    </section>
  );
};

export default Hero;