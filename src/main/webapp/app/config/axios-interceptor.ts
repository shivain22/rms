import axios, { type AxiosError } from 'axios';

const TIMEOUT = 1 * 60 * 1000;
axios.defaults.timeout = TIMEOUT;
axios.defaults.baseURL = SERVER_API_URL;
// JWT-based authentication - no need for cookies
// axios.defaults.withCredentials = true; // Disabled for JWT Bearer token authentication

// Storage key for JWT token
const TOKEN_STORAGE_KEY = 'jhipster-authenticationToken';

const setupAxiosInterceptors = onUnauthenticated => {
  const onRequestSuccess = config => {
    // Extract token from URL hash if present (after OAuth2 redirect)
    const hash = window.location.hash;
    if (hash && hash.includes('access_token=')) {
      const token = hash.split('access_token=')[1]?.split('&')[0];
      if (token) {
        // eslint-disable-next-line no-console
        console.log('[Axios Interceptor] ✓ Token extracted from URL hash');
        // eslint-disable-next-line no-console
        console.log('[Axios Interceptor] Token (first 50 chars):', token.substring(0, 50) + '...');
        // Store token in sessionStorage
        sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
        // eslint-disable-next-line no-console
        console.log('[Axios Interceptor] Token stored in sessionStorage');
        // Remove token from URL hash for security
        window.history.replaceState(null, '', window.location.pathname + window.location.search);
        // eslint-disable-next-line no-console
        console.log('[Axios Interceptor] Token removed from URL hash');
      } else {
        console.warn('[Axios Interceptor] ⚠ Token found in hash but could not extract');
      }
    }

    // Get token from storage and add to Authorization header
    const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
      // eslint-disable-next-line no-console
      console.log('[Axios Interceptor] ✓ Bearer token added to request:', config.url);
      // eslint-disable-next-line no-console
      console.log('[Axios Interceptor] Token (first 50 chars):', token.substring(0, 50) + '...');
    } else {
      console.warn('[Axios Interceptor] ⚠ No token found in sessionStorage for request:', config.url);
    }

    return config;
  };
  const onResponseSuccess = response => response;
  const onResponseError = (err: AxiosError) => {
    const status = err.status || (err.response ? err.response.status : 0);
    if (status === 401) {
      // Clear token on 401
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
      onUnauthenticated();
    }
    return Promise.reject(err);
  };
  axios.interceptors.request.use(onRequestSuccess);
  axios.interceptors.response.use(onResponseSuccess, onResponseError);
};

export default setupAxiosInterceptors;
