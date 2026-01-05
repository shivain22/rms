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
    <div className="flex flex-col items-center justify-center min-h-screen px-4 py-12">
      {/* Logo Section - Centered */}
      <div className="flex-1 flex items-center justify-center mb-auto">
        <img src="content/images/atpar_logo.jpg" alt="aPAR Logo" className="max-w-md w-full h-auto" />
      </div>

      {/* Sign In Button - Bottom Center */}
      <div className="w-full flex justify-center mt-auto pb-12">
        <Button
          size="lg"
          className="min-w-[200px]"
          onClick={() =>
            navigate(getLoginUrl(), {
              state: { from: pageLocation },
            })
          }
        >
          <Translate contentKey="global.messages.info.authenticated.link">Sign In</Translate>
        </Button>
      </div>
    </div>
  );
};

export default Home;
