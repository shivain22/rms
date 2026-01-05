import React from 'react';
import { Translate } from 'react-jhipster';

import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { cn } from '@/lib/utils';

export const BrandIcon = props => (
  <div {...props} className="brand-icon">
    <img src="content/images/logo-jhipster.png" alt="Logo" />
  </div>
);

export const Brand = () => (
  <Link to="/" className="flex items-center space-x-2 brand-logo">
    <BrandIcon />
    <span className="brand-title">
      <Translate contentKey="global.title">Rms</Translate>
    </span>
    <span className="navbar-version">{VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`}</span>
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
