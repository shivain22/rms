/**
 * Debug utility to check JWT token in browser console
 * Usage: In browser console, type: window.checkToken()
 */

export const checkToken = () => {
  const TOKEN_STORAGE_KEY = 'jhipster-authenticationToken';
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);

  // eslint-disable-next-line no-console
  console.log('=== JWT Token Debug ===');
  // eslint-disable-next-line no-console
  console.log('Token in sessionStorage:', token ? '✓ Found' : '✗ Not found');

  if (token) {
    // eslint-disable-next-line no-console
    console.log('Token length:', token.length);
    // eslint-disable-next-line no-console
    console.log('Token (first 100 chars):', token.substring(0, 100) + '...');

    // Try to decode JWT (just for viewing, not validation)
    try {
      const parts = token.split('.');
      if (parts.length === 3) {
        // eslint-disable-next-line no-console
        console.log('✓ Valid JWT format (3 parts)');
        const header = JSON.parse(atob(parts[0]));
        const payload = JSON.parse(atob(parts[1]));
        // eslint-disable-next-line no-console
        console.log('JWT Header:', header);
        // eslint-disable-next-line no-console
        console.log('JWT Payload:', payload);
        // eslint-disable-next-line no-console
        console.log('Subject:', payload.sub);
        // eslint-disable-next-line no-console
        console.log('Issuer:', payload.iss);
        // eslint-disable-next-line no-console
        console.log('Audience:', payload.aud);
        // eslint-disable-next-line no-console
        console.log('Expires at:', payload.exp ? new Date(payload.exp * 1000).toISOString() : 'N/A');
        // eslint-disable-next-line no-console
        console.log('Issued at:', payload.iat ? new Date(payload.iat * 1000).toISOString() : 'N/A');
        // eslint-disable-next-line no-console
        console.log('Is expired?', payload.exp ? Date.now() / 1000 > payload.exp : 'Unknown');
      } else {
        console.warn('✗ Invalid JWT format - expected 3 parts, got', parts.length);
      }
    } catch (e) {
      console.error('✗ Error decoding JWT:', e);
    }
  } else {
    // eslint-disable-next-line no-console
    console.log('Checking URL hash for token...');
    const hash = window.location.hash;
    if (hash && hash.includes('access_token=')) {
      const urlToken = hash.split('access_token=')[1]?.split('&')[0];
      // eslint-disable-next-line no-console
      console.log('✓ Token found in URL hash');
      // eslint-disable-next-line no-console
      console.log('Token (first 100 chars):', urlToken?.substring(0, 100) + '...');
    } else {
      // eslint-disable-next-line no-console
      console.log('✗ No token in URL hash');
    }
  }

  // eslint-disable-next-line no-console
  console.log('=== End Token Debug ===');
  return token;
};

// Make it available globally for easy console access
if (typeof window !== 'undefined') {
  (window as any).checkToken = checkToken;
}
