import React from 'react';
import { Translate } from 'react-jhipster';

import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

export const Brand = () => (
  <Link to="/" className="flex items-center space-x-3 brand-logo">
    <img src="content/images/atpar_logo.jpg" alt="At PAR UI Technologies Logo" className="h-8 w-auto object-contain" />
    <div className="flex flex-col">
      <span className="text-sm font-semibold text-foreground leading-tight">At PAR UI Technologies</span>
      <span className="text-xs text-muted-foreground leading-tight">Intention and Intelligence</span>
    </div>
  </Link>
);

export const Home = () => (
  <Link to="/" className="flex items-center space-x-2 px-3 py-2 rounded-md hover:bg-accent transition-colors">
    <FontAwesomeIcon icon="home" />
    <span>
      <Translate contentKey="global.menu.home">Home</Translate>
    </span>
  </Link>
);
