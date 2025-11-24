import React from 'react';
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import ProblemSolution from './components/ProblemSolution';
import Features from './components/Features';
import DemoSection from './components/DemoSection'; // Renamed to How It Works in UI
import DetailedFeatures from './components/DetailedFeatures';
import ComparisonTable from './components/ComparisonTable';
import Testimonials from './components/Testimonials';
import FAQ from './components/FAQ';
import Footer from './components/Footer';

const App: React.FC = () => {
  return (
    <div className="min-h-screen bg-md-bg text-white font-sans selection:bg-md-primary-container selection:text-md-on-primary-container">
      <Navbar />
      <main>
        <Hero />
        <ProblemSolution />
        <Features />
        <DemoSection />
        <DetailedFeatures />
        <ComparisonTable />
        <Testimonials />
        <FAQ />
        
        {/* Call to Action Section */}
        <section className="py-32 px-6 bg-md-bg">
          <div className="max-w-5xl mx-auto bg-md-primary-container rounded-[3rem] p-12 md:p-24 text-center relative overflow-hidden shadow-2xl">
            {/* Abstract shapes */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-md-primary opacity-10 rounded-full blur-[80px]"></div>
            <div className="absolute bottom-0 left-0 w-64 h-64 bg-md-secondary opacity-10 rounded-full blur-[80px]"></div>
            
            <h2 className="text-4xl md:text-6xl font-black text-white mb-8 relative z-10">
              Ready to Capture Faster?
            </h2>
            <p className="text-xl text-md-on-primary-container/80 mb-12 max-w-2xl mx-auto relative z-10 font-medium">
              Stop losing thoughts. Join thousands of PKM enthusiasts capturing instantly with NoteDrop.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center relative z-10">
                <button className="bg-md-primary text-md-on-primary text-lg font-bold py-4 px-10 rounded-full shadow-lg hover:shadow-xl hover:bg-white hover:text-md-primary-container transition-all transform hover:scale-105 active:scale-95">
                Download on Google Play
                </button>
                <button className="bg-transparent border border-md-on-primary-container/30 text-md-on-primary-container text-lg font-bold py-4 px-10 rounded-full hover:bg-md-on-primary-container/10 transition-all">
                Download APK
                </button>
            </div>
            <p className="mt-6 text-sm text-md-on-primary-container/60 relative z-10">
                Free forever for core features. No account required.
            </p>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  );
};

export default App;