import React, { useState, useEffect } from 'react';
import { Droplets } from 'lucide-react';

const Navbar: React.FC = () => {
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="fixed top-0 left-0 right-0 z-50 flex justify-center pt-6 px-4">
      <nav 
        className={`
          transition-all duration-500 ease-in-out
          ${isScrolled 
            ? 'w-[95%] max-w-5xl bg-md-surface-3/80 backdrop-blur-xl shadow-xl rounded-full px-6 py-3 border border-white/5' 
            : 'w-full max-w-7xl bg-transparent px-6 py-4'
          }
          flex justify-between items-center
        `}
      >
        <div className="flex items-center space-x-3 cursor-pointer group" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth'})}>
          <div className="w-10 h-10 bg-md-primary-container rounded-xl flex items-center justify-center transition-transform group-hover:scale-110 group-active:scale-95 duration-300">
            <Droplets className="text-md-primary w-6 h-6 fill-current" />
          </div>
          <span className={`text-xl font-bold tracking-tight transition-colors ${isScrolled ? 'text-white' : 'text-white'}`}>
            NoteDrop
          </span>
        </div>

        <div className="hidden md:flex items-center space-x-2">
          {['Features', 'How it Works', 'Comparison', 'FAQ'].map((item) => (
            <a 
              key={item}
              href={`#${item.toLowerCase().replace(/ /g, '-')}`} 
              className="px-5 py-2 rounded-full text-sm font-medium text-gray-300 hover:text-md-primary hover:bg-white/5 transition-all duration-300 material-ripple"
            >
              {item}
            </a>
          ))}
        </div>

        <button className="hidden md:block bg-md-primary text-md-on-primary font-bold py-3 px-6 rounded-full transition-all transform hover:shadow-[0_0_20px_rgba(140,214,154,0.3)] hover:scale-105 active:scale-95 active:bg-opacity-90">
          Get App
        </button>
      </nav>
    </div>
  );
};

export default Navbar;