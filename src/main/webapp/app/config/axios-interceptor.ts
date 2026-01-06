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
        // Store token in sessionStorage
        sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
        // Remove token from URL hash for security
        window.history.replaceState(null, '', window.location.pathname + window.location.search);
      }
    }

    // Get token from storage and add to Authorization header
    const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
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
