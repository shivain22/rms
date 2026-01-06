# JWT-Based Authentication Setup

## Overview

The application has been configured to use **JWT Bearer token authentication** instead of session cookies. This solves the session cookie issues when developing the frontend locally while connecting to a remote backend.

## How It Works

### Authentication Flow

1. **User clicks "Sign In"** → Frontend redirects to `/oauth2/authorization/oidc`
2. **Backend redirects to Keycloak** → User authenticates
3. **Keycloak redirects back** → `/login/oauth2/code/oidc?code=...`
4. **Backend processes OAuth2 callback** → Extracts ID token from `OidcUser`
5. **Backend redirects to frontend** → Includes token in URL hash: `http://localhost:9000/#access_token=<token>`
6. **Frontend extracts token** → Stores in `sessionStorage`
7. **Frontend sends token** → As `Authorization: Bearer <token>` header in all API requests
8. **Backend validates token** → Using `oauth2ResourceServer` with JWT decoder

### Key Changes

#### Backend (`SecurityConfiguration.java`)

1. **OAuth2 Success Handler** - Modified to extract ID token and include it in redirect URL:

   ```java
   // Extract ID token from OidcUser
   String token = oidcUser.getIdToken().getTokenValue();
   // Include in redirect URL as hash fragment
   redirectUrl = frontendUrl + "#access_token=" + token;
   ```

2. **Session Management** - Sessions are no longer required (stateless authentication)

3. **JWT Resource Server** - Already configured to validate Bearer tokens

#### Frontend (`axios-interceptor.ts`)

1. **Token Extraction** - Extracts token from URL hash after OAuth2 redirect:

   ```typescript
   const hash = window.location.hash;
   if (hash && hash.includes('access_token=')) {
     const token = hash.split('access_token=')[1]?.split('&')[0];
     sessionStorage.setItem('jhipster-authenticationToken', token);
   }
   ```

2. **Token Injection** - Adds Bearer token to all requests:

   ```typescript
   const token = sessionStorage.getItem('jhipster-authenticationToken');
   if (token) {
     config.headers.Authorization = `Bearer ${token}`;
   }
   ```

3. **Token Cleanup** - Removes token from URL hash and clears on 401

## Benefits

1. **No Cookie Issues** - No need to worry about cookie domain, path, Secure flag, or SameSite
2. **Stateless** - No server-side session storage required
3. **Proxy-Friendly** - Works seamlessly with webpack proxy
4. **Scalable** - Stateless authentication scales better
5. **SPA-Friendly** - Designed for single-page applications

## Testing

1. **Start the application:**

   ```bash
   npm start
   ```

2. **Test authentication:**

   - Click "Sign In"
   - Complete OAuth2 login at Keycloak
   - Check browser console for token extraction
   - Check Network tab - requests should have `Authorization: Bearer <token>` header
   - `/api/account` should return `200 OK` with user data

3. **Verify token storage:**

   - Open DevTools → Application → Session Storage
   - Look for `jhipster-authenticationToken` key
   - Token should be present after login

4. **Check backend logs:**
   ```
   === OAuth2 Authentication Success ===
   ID Token (first 50 chars): eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
   Redirecting to frontend URL: http://localhost:9000/
   ID token included in redirect (frontend should extract and store it)
   ```

## Troubleshooting

### Issue: Token Not Extracted

**Symptoms:**

- URL has `#access_token=...` but token not stored

**Solution:**

- Check browser console for errors
- Verify `axios-interceptor.ts` is loaded
- Check that token extraction code runs on page load

### Issue: Token Not Sent

**Symptoms:**

- Token stored but requests don't have `Authorization` header

**Solution:**

- Check `axios-interceptor.ts` `onRequestSuccess` function
- Verify token is retrieved from `sessionStorage`
- Check Network tab to see if header is added

### Issue: 401 Unauthorized

**Symptoms:**

- Token sent but backend returns 401

**Solution:**

- Verify token is valid (not expired)
- Check backend logs for JWT validation errors
- Verify `oauth2ResourceServer` is configured correctly
- Check that JWT decoder can validate the token

### Issue: Token Expired

**Symptoms:**

- Works initially but stops working after some time

**Solution:**

- ID tokens typically expire after 1 hour
- User needs to re-authenticate
- Consider implementing token refresh (future enhancement)

## Security Considerations

1. **Token Storage** - Using `sessionStorage` (cleared on tab close) instead of `localStorage`
2. **URL Hash** - Token in hash fragment is not sent to server (more secure than query parameter)
3. **HTTPS** - In production, always use HTTPS to protect tokens in transit
4. **Token Expiration** - Tokens expire automatically (configured in Keycloak)

## Migration Notes

- **Old behavior**: Session cookies (`JSESSIONID`)
- **New behavior**: JWT Bearer tokens
- **Compatibility**: Both can coexist, but JWT is preferred for SPAs
- **Backend**: No changes needed to API endpoints (they work with both)

## Related Files

- `src/main/java/com/atparui/rms/config/SecurityConfiguration.java` - Backend OAuth2 configuration
- `src/main/webapp/app/config/axios-interceptor.ts` - Frontend token handling
- `src/main/webapp/app/modules/login/login-redirect.tsx` - Login redirect component
