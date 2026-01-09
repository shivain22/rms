import React, { useEffect } from 'react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { logout } from 'app/shared/reducers/authentication';
import { clearAuthToken } from 'app/config/axios-interceptor';

export const Logout = () => {
  const authentication = useAppSelector(state => state.authentication);
  const dispatch = useAppDispatch();

  useEffect(() => {
    // Clear token immediately when logout component mounts
    clearAuthToken();
    dispatch(logout());
  }, [dispatch]);

  useEffect(() => {
    if (authentication.logoutUrl) {
      // Clear token again before redirecting to Keycloak logout
      clearAuthToken();
      window.location.href = authentication.logoutUrl;
    } else if (authentication.sessionHasBeenFetched && !authentication.isAuthenticated) {
      // Clear token before redirecting to home
      clearAuthToken();
      window.location.href = '/';
    }
  }, [authentication.logoutUrl, authentication.isAuthenticated, authentication.sessionHasBeenFetched]);

  return (
    <div className="p-5">
      <h4>Logging out...</h4>
    </div>
  );
};

export default Logout;
