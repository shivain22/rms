import React from 'react';
import { Translate } from 'react-jhipster';

import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { cn } from '@/lib/utils';

export const BrandIcon = props => (
  <div {...props} className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-orange-500 to-green-500 rounded-lg">
    <span className="text-white font-bold text-lg">a</span>
  </div>
);

export const Brand = () => (
  <Link to="/" className="flex items-center space-x-2 brand-logo">
    <BrandIcon />
    <div className="flex flex-col">
      <span className="text-lg font-bold text-foreground">PAR</span>
      <span className="text-xs text-muted-foreground">intelligence</span>
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
