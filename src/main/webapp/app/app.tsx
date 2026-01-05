import 'react-toastify/dist/ReactToastify.css';
import './app.scss';
import 'app/config/dayjs';

import React, { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getSession } from 'app/shared/reducers/authentication';
import { getProfile } from 'app/shared/reducers/application-profile';
import Header from 'app/shared/layout/header/header';
import Footer from 'app/shared/layout/footer/footer';
import { Sidebar } from 'app/shared/layout/sidebar';
import { hasAnyAuthority } from 'app/shared/auth/private-route';
import ErrorBoundary from 'app/shared/error/error-boundary';
import { AUTHORITIES } from 'app/config/constants';
import AppRoutes from 'app/routes';

const baseElement = document.querySelector('base');
const baseHref = baseElement ? baseElement.getAttribute('href')?.replace(/\/$/, '') || '' : '';

export const App = () => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getSession());
    dispatch(getProfile());
  }, []);

  const currentLocale = useAppSelector(state => state.locale.currentLocale);
  const isAuthenticated = useAppSelector(state => state.authentication.isAuthenticated);
  const account = useAppSelector(state => state.authentication.account);
  const sessionHasBeenFetched = useAppSelector(state => state.authentication.sessionHasBeenFetched);
  const isAdmin = useAppSelector(state => hasAnyAuthority(state.authentication.account?.authorities, [AUTHORITIES.ADMIN]));
  const ribbonEnv = useAppSelector(state => state.applicationProfile.ribbonEnv);
  const isInProduction = useAppSelector(state => state.applicationProfile.isInProduction);
  const isOpenAPIEnabled = useAppSelector(state => state.applicationProfile.isOpenAPIEnabled);

  return (
    <BrowserRouter basename={baseHref}>
      <ToastContainer position="top-left" className="toastify-container" toastClassName="toastify-toast" />
      {!sessionHasBeenFetched ? (
        // Loading state while session is being fetched
        <div className="min-h-screen bg-background flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">Loading...</p>
          </div>
        </div>
      ) : isAuthenticated ? (
        // Dashboard layout when authenticated
        <div className="flex h-screen bg-background">
          <ErrorBoundary>
            <Sidebar isAuthenticated={isAuthenticated} isAdmin={isAdmin} isOpenAPIEnabled={isOpenAPIEnabled} />
          </ErrorBoundary>
          <div className="flex flex-col flex-1 overflow-hidden">
            <ErrorBoundary>
              <Header
                isAuthenticated={isAuthenticated}
                isAdmin={isAdmin}
                currentLocale={currentLocale}
                ribbonEnv={ribbonEnv}
                isInProduction={isInProduction}
                isOpenAPIEnabled={isOpenAPIEnabled}
                account={account}
              />
            </ErrorBoundary>
            <main className="flex-1 overflow-y-auto p-6 bg-muted/40">
              <ErrorBoundary>
                <AppRoutes />
              </ErrorBoundary>
            </main>
            <Footer />
          </div>
        </div>
      ) : (
        // Landing page layout when not authenticated
        <div className="min-h-screen bg-background">
          <ErrorBoundary>
            <AppRoutes />
          </ErrorBoundary>
        </div>
      )}
    </BrowserRouter>
  );
};

export default App;
