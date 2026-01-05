import './home.scss';

import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Translate } from 'react-jhipster';
import { Button } from '@/components/ui/button';

import { REDIRECT_URL, getLoginUrl } from 'app/shared/util/url-utils';
import { useAppSelector } from 'app/config/store';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);
  const pageLocation = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const redirectURL = localStorage.getItem(REDIRECT_URL);
    if (redirectURL) {
      localStorage.removeItem(REDIRECT_URL);
      location.href = `${location.origin}${redirectURL}`;
    }
  });

  // If user is already logged in, redirect to dashboard or show different content
  if (account?.login) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <img src="content/images/atpar_logo.jpg" alt="aPAR Logo" className="max-w-md mb-8" />
        <p className="text-muted-foreground mb-4">
          <Translate contentKey="home.logged.message" interpolate={{ username: account.login }}>
            You are logged in as user {account.login}.
          </Translate>
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen px-4">
      {/* Logo and Sign In Section - Centered with spacing */}
      <div className="flex flex-col items-center justify-center gap-8">
        {/* Logo */}
        <div className="flex items-center justify-center">
          <img src="content/images/atpar_logo.jpg" alt="aPAR Logo" className="max-w-md w-full h-auto" />
        </div>

        {/* Enter Button - Closer to logo, styled like login screen */}
        <Button
          size="lg"
          className="min-w-[200px] bg-black text-white hover:bg-black/90 shadow-sm hover:shadow-md transition-shadow"
          onClick={() => navigate('/sign-in', { state: { from: pageLocation } })}
        >
          Enter
        </Button>
      </div>
    </div>
  );
};

export default Home;
