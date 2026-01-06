# Troubleshooting 404 Error for /oauth2/authorization/oidc

## Issue

Getting 404 error when accessing `localhost:9000/oauth2/authorization/oidc`

## Possible Causes

### 1. BrowserSync Not Forwarding Request

BrowserSync (port 9000) might be trying to serve the route as a static file instead of forwarding to webpack dev server (port 9060).

**Solution**: Try accessing the webpack dev server directly:

- Instead of: `http://localhost:9000/oauth2/authorization/oidc`
- Use: `http://localhost:9060/oauth2/authorization/oidc`

### 2. Proxy Not Matching Route

The webpack proxy might not be matching `/oauth2/authorization/oidc` correctly.

**Check**: Look for this log message when you make the request:

```
[Webpack Proxy] Matching path for proxy: /oauth2/authorization/oidc
[Webpack Proxy] Proxying request: GET /oauth2/authorization/oidc to https://rmsgateway.atparui.com
```

If you don't see these logs, the proxy isn't matching.

### 3. BrowserSync Configuration

BrowserSync might need additional configuration to forward all requests.

## Quick Fixes

### Option 1: Access Webpack Dev Server Directly

1. Open `http://localhost:9060` instead of `http://localhost:9000`
2. This bypasses BrowserSync and goes directly to webpack dev server
3. The proxy should work correctly

### Option 2: Update Frontend Code

Modify the login redirect to use the full production backend URL when in development:

```typescript
// In login-redirect.tsx or wherever the OAuth2 redirect is triggered
const oauth2Url =
  process.env.NODE_ENV === 'development' ? 'https://rmsgateway.atparui.com/oauth2/authorization/oidc' : '/oauth2/authorization/oidc';
window.location.href = oauth2Url;
```

### Option 3: Check BrowserSync Logs

Look for BrowserSync logs to see if it's forwarding the request or trying to serve it as a static file.

## Verification Steps

1. **Check webpack dev server logs** for:

   - `[Webpack Proxy] Matching path for proxy: /oauth2/authorization/oidc`
   - `[Webpack Proxy] Proxying request: GET /oauth2/authorization/oidc`

2. **Check browser Network tab**:

   - Request to `localhost:9000/oauth2/authorization/oidc` should show status 302 (redirect) or 200
   - If 404, the request isn't reaching the proxy

3. **Test direct access**:
   - Try `http://localhost:9060/oauth2/authorization/oidc` directly
   - If this works, the issue is with BrowserSync forwarding

## Expected Behavior

When you click login:

1. Browser navigates to `localhost:9000/oauth2/authorization/oidc`
2. BrowserSync forwards to `localhost:9060/oauth2/authorization/oidc`
3. Webpack dev server proxy matches the route
4. Proxy forwards to `https://rmsgateway.atparui.com/oauth2/authorization/oidc`
5. Backend redirects to Keycloak
6. After auth, redirects back to `localhost:9000`
