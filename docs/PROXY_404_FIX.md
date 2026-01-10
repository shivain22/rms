# Fix: 404 Error Instead of 401 When Proxying to Remote Backend

## Problem Analysis

### Symptoms

- **Direct access** (`https://rmsgateway.atparui.com`): `/api/account` returns `200 OK` (authenticated) or `401 Unauthorized` (not authenticated)
- **Local frontend** (`http://localhost:9000`): `/api/account` returns `404 Not Found`
- **Proxy logs** show: `[Webpack Proxy] Response: 404 for /api/account`

### Root Cause

The webpack proxy had `changeOrigin: false`, which preserved the original `Host` header as `localhost:9000`. When the request reached the production backend at `https://rmsgateway.atparui.com`:

1. **Nginx/Reverse Proxy** in front of the backend validates the `Host` header
2. The `Host: localhost:9000` header doesn't match the expected `Host: rmsgateway.atparui.com`
3. The request is rejected at the reverse proxy level with **404 Not Found** before it even reaches the Spring Boot application
4. This happens **before** authentication checks, so you get 404 instead of 401

### Why 404 Instead of 401?

- **404 (Not Found)**: The reverse proxy/nginx doesn't recognize `localhost:9000` as a valid host for the server, so it rejects the request before it reaches the Spring Boot application
- **401 (Unauthorized)**: Would only occur if the request reached the Spring Boot application and authentication failed

The request never reaches the Spring Boot application, so authentication is never checked.

## Solution

### Change Made

Updated `webpack/webpack.dev.js`:

```javascript
changeOrigin: true, // CRITICAL: Change Host header to match target, otherwise backend returns 404
```

### How It Works Now

1. **Browser** sends request to `http://localhost:9000/api/account` with `Host: localhost:9000`
2. **BrowserSync** forwards to webpack dev server on `localhost:9060`
3. **Webpack Proxy** intercepts `/api/account`:
   - **Changes** `Host` header to `rmsgateway.atparui.com` (required for backend to accept request)
   - **Preserves** original host in `X-Forwarded-Host: localhost:9000` (for OAuth2 redirect URI generation)
   - **Sets** `X-Forwarded-Proto: https` (backend expects HTTPS)
   - **Forwards** request to `https://rmsgateway.atparui.com/api/account`
4. **Backend** receives request with:
   - `Host: rmsgateway.atparui.com` ✅ (matches expected host)
   - `X-Forwarded-Host: localhost:9000` ✅ (preserved for OAuth2 redirects)
   - Request is accepted and processed
5. **Backend** returns:
   - `200 OK` if authenticated
   - `401 Unauthorized` if not authenticated
   - **Not** `404 Not Found` anymore

### Headers Comparison

#### Before Fix (404 Error)

```
Request Headers:
  Host: localhost:9000  ❌ (doesn't match backend expected host)
  Origin: http://localhost:9000
  Referer: http://localhost:9000/
```

#### After Fix (200/401 Response)

```
Request Headers:
  Host: rmsgateway.atparui.com  ✅ (matches backend expected host)
  X-Forwarded-Host: localhost:9000  ✅ (preserved for OAuth2 redirects)
  X-Forwarded-Proto: https  ✅ (backend expects HTTPS)
  Origin: http://localhost:9000  ✅ (for CORS)
  Referer: http://localhost:9000/  ✅ (for CORS)
```

## Testing

After applying the fix:

1. **Restart webpack dev server:**

   ```bash
   # Stop current server (Ctrl+C)
   npm start
   ```

2. **Test API request:**

   - Open `http://localhost:9000`
   - Open DevTools → Network tab
   - Check `/api/account` request:
     - Should return `200 OK` (if authenticated) or `401 Unauthorized` (if not authenticated)
     - Should **NOT** return `404 Not Found`

3. **Verify proxy logs:**
   ```
   [Webpack Proxy] Proxying request: GET /api/account to https://rmsgateway.atparui.com
   [Webpack Proxy] Original Host: localhost:9000 | Origin: http://localhost:9000 | Referer: http://localhost:9000/
   [Webpack Proxy] Set X-Forwarded-Host to original client host: localhost:9000
   [Webpack Proxy] Response: 200 for /api/account  (or 401 if not authenticated)
   ```

## Key Takeaways

1. **`changeOrigin: true`** is **required** when proxying to a remote backend that validates the Host header
2. **`X-Forwarded-Host`** must be set to preserve the original client host for OAuth2 redirect URI generation
3. **404 errors** from a proxy often indicate Host header mismatch, not missing routes
4. **401 errors** indicate the request reached the application but authentication failed (this is expected behavior)

## Related Configuration

- **Backend CORS**: Must allow `http://localhost:9000` (already configured)
- **Keycloak**: Must allow `http://localhost:9000` in Valid Redirect URIs and Web Origins
- **Webpack Proxy**: Now correctly sets Host header while preserving original host in X-Forwarded-Host
