import './home.scss';

import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Translate } from 'react-jhipster';
import { Alert, AlertDescription } from '@/components/ui/alert';

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

  return (
    <div className="grid grid-cols-12 gap-4">
      <div className="col-span-12 md:col-span-3 pad">
        <span className="hipster rounded" />
      </div>
      <div className="col-span-12 md:col-span-9">
        <h1 className="display-4">
          <Translate contentKey="home.title">Welcome, Java Hipster!</Translate>
        </h1>
        <p className="lead">
          <Translate contentKey="home.subtitle">This is your homepage</Translate>
        </p>
        {account?.login ? (
          <div>
            <Alert className="border-green-500 bg-green-50 text-green-900 dark:bg-green-900/20 dark:text-green-400">
              <AlertDescription>
                <Translate contentKey="home.logged.message" interpolate={{ username: account.login }}>
                  You are logged in as user {account.login}.
                </Translate>
              </AlertDescription>
            </Alert>
          </div>
        ) : (
          <div>
            <Alert className="border-yellow-500 bg-yellow-50 text-yellow-900 dark:bg-yellow-900/20 dark:text-yellow-400">
              <AlertDescription>
                <Translate contentKey="global.messages.info.authenticated.prefix">If you want to </Translate>

                <a
                  className="alert-link underline cursor-pointer"
                  onClick={() =>
                    navigate(getLoginUrl(), {
                      state: { from: pageLocation },
                    })
                  }
                >
                  <Translate contentKey="global.messages.info.authenticated.link">sign in</Translate>
                </a>
                <Translate contentKey="global.messages.info.authenticated.suffix">
                  , you can try the default accounts:
                  <br />- Administrator (login=&quot;admin&quot; and password=&quot;admin&quot;)
                  <br />- User (login=&quot;user&quot; and password=&quot;user&quot;).
                </Translate>
              </AlertDescription>
            </Alert>
          </div>
        )}
        <p>
          <Translate contentKey="home.question">If you have any question on JHipster:</Translate>
        </p>

        <ul>
          <li>
            <a href="https://www.jhipster.tech/" target="_blank" rel="noopener noreferrer">
              <Translate contentKey="home.link.homepage">JHipster homepage</Translate>
            </a>
          </li>
          <li>
            <a href="https://stackoverflow.com/tags/jhipster/info" target="_blank" rel="noopener noreferrer">
              <Translate contentKey="home.link.stackoverflow">JHipster on Stack Overflow</Translate>
            </a>
          </li>
          <li>
            <a href="https://github.com/jhipster/generator-jhipster/issues?state=open" target="_blank" rel="noopener noreferrer">
              <Translate contentKey="home.link.bugtracker">JHipster bug tracker</Translate>
            </a>
          </li>
          <li>
            <a href="https://gitter.im/jhipster/generator-jhipster" target="_blank" rel="noopener noreferrer">
              <Translate contentKey="home.link.chat">JHipster public chat room</Translate>
            </a>
          </li>
          <li>
            <a href="https://twitter.com/jhipster" target="_blank" rel="noopener noreferrer">
              <Translate contentKey="home.link.follow">follow @jhipster on Twitter</Translate>
            </a>
          </li>
        </ul>

        <p>
          <Translate contentKey="home.like">If you like JHipster, do not forget to give us a star on</Translate>{' '}
          <a href="https://github.com/jhipster/generator-jhipster" target="_blank" rel="noopener noreferrer">
            GitHub
          </a>
          !
        </p>
        <div className="mt-4">
          <Alert className="border-blue-500 bg-blue-50 text-blue-900 dark:bg-blue-900/20 dark:text-blue-400 small">
            <AlertDescription>
              <strong>Application Version:</strong> {VERSION}
              {typeof APP_COMMIT_HASH !== 'undefined' && APP_COMMIT_HASH !== 'unknown' && (
                <>
                  <br />
                  <strong>Build Info:</strong> Commit {APP_COMMIT_HASH} (Count: {APP_COMMIT_COUNT || '0'}, Branch: {APP_BRANCH || 'unknown'}
                  )
                  {typeof APP_BUILD_TIMESTAMP !== 'undefined' && APP_BUILD_TIMESTAMP !== 'unknown' && (
                    <>
                      <br />
                      <strong>Build Time:</strong> {APP_BUILD_TIMESTAMP}
                    </>
                  )}
                </>
              )}
            </AlertDescription>
          </Alert>
        </div>
      </div>
    </div>
  );
};

export default Home;
