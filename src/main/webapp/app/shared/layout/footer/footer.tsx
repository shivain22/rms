import './footer.scss';

import React from 'react';
import { useAppSelector } from 'app/config/store';

const Footer = () => {
  const isInProduction = useAppSelector(state => state.applicationProfile.isInProduction);
  const currentYear = new Date().getFullYear();

  return (
    <footer className="footer flex-shrink-0 border-t border-border/20 bg-background">
      <div className="container mx-auto px-6 py-4">
        <div className="flex flex-col md:flex-row justify-between items-center gap-4">
          <div className="text-sm text-muted-foreground">© {currentYear} PAR Intelligence. All rights reserved.</div>
          <div className="flex items-center gap-4 text-sm text-muted-foreground">
            {!isInProduction && (
              <span className="px-2 py-1 bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-200 rounded text-xs font-medium">
                Development Mode
              </span>
            )}
            <span className="text-xs">Version 1.0.0</span>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
