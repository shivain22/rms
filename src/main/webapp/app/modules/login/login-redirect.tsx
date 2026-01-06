import { useEffect } from 'react';
import { REDIRECT_URL } from 'app/shared/util/url-utils';
import { useLocation } from 'react-router';

export const LoginRedirect = () => {
  const pageLocation = useLocation();

  useEffect(() => {
    localStorage.setItem(REDIRECT_URL, pageLocation.state.from.pathname);

    // In development (localhost:9000), use full production backend URL for OAuth2
    // This bypasses the proxy issue where BrowserSync might return 404
    const isDevelopment = window.location.hostname === 'localhost' && (window.location.port === '9000' || window.location.port === '9060');
    const oauth2Url = isDevelopment ? 'https://rmsgateway.atparui.com/oauth2/authorization/oidc' : '/oauth2/authorization/oidc';

    window.location.href = oauth2Url;
  });

  return null;
};

export default LoginRedirect;
