# Session Cookie Not Working After OAuth2 Authentication

## Problem

After successful OAuth2 authentication:

- Authentication completes successfully
- User is redirected back to frontend
- `/api/account` endpoint still returns `401 Unauthorized`
- `/management/info` works (returns `200 OK`)

## Root Cause Analysis

Spring WebFlux with OAuth2 uses **session-based authentication** (not JWT tokens). The session cookie (typically `JSESSIONID` or similar) must be:

1. Set by the backend after OAuth2 callback
2. Preserved by the browser
3. Sent with subsequent API requests

### Why `/management/info` Works But `/api/account` Doesn't

- `/management/info` might be publicly accessible or have different security rules
- `/api/account` requires full authentication with a valid session cookie

## Solution Applied

### 1. Cookie Rewriting in Webpack Proxy

Updated `webpack/webpack.dev.js` to:

- Remove `Domain` restriction from cookies
- Remove `Secure` flag (HTTPS-only) so cookies work on HTTP localhost
- Replace `SameSite=None` with `SameSite=Lax` (since we removed Secure)

### 2. Enhanced Logging

Added detailed logging to track:

- Which cookies are being set by the backend
- Which cookies are being sent with requests
- Whether session cookies are present

## Verification Steps

### Step 1: Check Proxy Logs

After restarting the dev server and attempting login, check the logs for:

```
[Webpack Proxy] Backend set cookies (raw): [...]
[Webpack Proxy] ✓ Session cookie detected in Set-Cookie headers
[Webpack Proxy] Cookie rewritten: JSESSIONID=... -> JSESSIONID=...
```

**If you see:**

```
[Webpack Proxy] ⚠ No session cookie detected
```

This means the backend is not setting a session cookie after OAuth2 callback.

### Step 2: Check Browser Cookies

1. Open DevTools → Application → Cookies → `http://localhost:9000`
2. Look for:
   - `JSESSIONID` or `SESSION` cookie
   - `XSRF-TOKEN` cookie (should be present)

**If session cookie is missing:**

- The backend might not be setting it
- The cookie might be blocked by browser security settings
- The OAuth2 callback might not be completing properly

### Step 3: Check OAuth2 Callback Flow

1. Open DevTools → Network tab
2. Click "Sign In"
3. Complete authentication at Keycloak
4. Watch for redirect to `/login/oauth2/code/oidc`
5. Check the response headers for `Set-Cookie` headers

**Expected flow:**

```
GET /oauth2/authorization/oidc
  → 302 Redirect to Keycloak
  → User authenticates
  → 302 Redirect to /login/oauth2/code/oidc?code=...
  → 302 Redirect to / (with Set-Cookie: JSESSIONID=...)
  → GET /api/account (with Cookie: JSESSIONID=...)
  → 200 OK
```

## Common Issues

### Issue 1: Session Cookie Not Set

**Symptoms:**

- OAuth2 callback completes
- No `JSESSIONID` cookie in browser
- Logs show: `⚠ No session cookie detected`

**Possible Causes:**

1. Backend session configuration issue
2. Cookie domain/path mismatch
3. Backend security configuration blocking cookie

**Solution:**

- Check backend logs for session creation
- Verify `server.servlet.session.cookie` configuration
- Check if backend is using reactive session management correctly

### Issue 2: Cookie Set But Not Sent

**Symptoms:**

- `JSESSIONID` cookie exists in browser
- Logs show: `⚠ No session cookie (JSESSIONID/SESSION) found in request`

**Possible Causes:**

1. Cookie domain/path doesn't match request
2. Cookie has `Secure` flag but request is HTTP
3. Cookie has `SameSite=Strict` blocking cross-site requests
4. `withCredentials` not set (already fixed - should be `true`)

**Solution:**

- Verify cookie rewriting is working (check logs)
- Check browser console for cookie warnings
- Verify `axios.defaults.withCredentials = true` is set

### Issue 3: Cookie Sent But Backend Rejects

**Symptoms:**

- Cookie is sent with request (visible in logs)
- Backend still returns 401

**Possible Causes:**

1. Session expired or invalid
2. Backend session store issue
3. CORS configuration blocking credentials

**Solution:**

- Verify CORS allows credentials: `allow-credentials: true`
- Check backend session store (in-memory vs persistent)
- Check backend logs for session validation errors

## Debugging Commands

### Check Cookies in Browser Console

```javascript
// List all cookies
document.cookie;

// Check for session cookie
document.cookie.includes('JSESSIONID') || document.cookie.includes('SESSION');
```

### Check Network Request Headers

In DevTools → Network → Select `/api/account` request → Headers tab:

- Check `Request Headers` → `Cookie:` should include `JSESSIONID=...`
- Check `Response Headers` → `Set-Cookie:` (if present, session was reset)

## Next Steps

1. **Restart webpack dev server** to apply cookie rewriting changes
2. **Clear browser cookies** for `localhost:9000`
3. **Test OAuth2 login flow** and watch proxy logs
4. **Check browser cookies** after authentication
5. **Verify session cookie** is sent with `/api/account` request

## Expected Log Output After Fix

```
[Webpack Proxy] Backend set cookies (raw): ['JSESSIONID=ABC123...; Path=/; HttpOnly; SameSite=Lax', 'XSRF-TOKEN=...']
[Webpack Proxy] ✓ Session cookie detected in Set-Cookie headers
[Webpack Proxy] Cookie rewritten: JSESSIONID=ABC123... -> JSESSIONID=ABC123...; Path=/; HttpOnly; SameSite=Lax
[Webpack Proxy] All cookies rewritten for localhost: JSESSIONID=ABC123..., XSRF-TOKEN=...

[Webpack Proxy] Forwarding cookies: JSESSIONID=ABC123...; XSRF-TOKEN=...
[Webpack Proxy] ✓ Session cookie found in request
[Webpack Proxy] Response: 200 for /api/account
```

## Related Files

- `webpack/webpack.dev.js` - Cookie rewriting logic
- `src/main/webapp/app/config/axios-interceptor.ts` - Axios withCredentials configuration
- `src/main/resources/config/application-prod.yml` - Backend CORS configuration
