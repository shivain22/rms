import './footer.scss';

import React from 'react';
import { Translate } from 'react-jhipster';

const Footer = () => (
  <div className="footer page-content">
    <div className="grid grid-cols-12">
      <div className="col-span-12">
        <p>
          <Translate contentKey="footer">Your footer</Translate>
        </p>
      </div>
    </div>
  </div>
);

export default Footer;
