# Local Frontend Development Guide

This guide explains how to develop the frontend locally (using `npm start`) while connecting to a production backend.

## Architecture

- **Frontend**: Runs locally on `http://localhost:9000` (BrowserSync) via `npm start`
- **Backend**: Runs in production at `https://rmsgateway.atparui.com`
- **Keycloak**: Production instance at `https://auth.atparui.com`

## Setup

### 1. Frontend Configuration

The webpack proxy is already configured in `webpack/webpack.dev.js` to proxy API requests to the production backend:

```javascript
proxy: [
  {
    context: ['/api', '/services', '/management', '/v3/api-docs', '/h2-console', '/auth', '/oauth2', '/login'],
    target: 'https://rmsgateway.atparui.com',
    secure: true,
    changeOrigin: true,
    logLevel: 'debug',
  },
];
```

**No changes needed** - the proxy is already pointing to the production backend.

### 2. Backend Configuration

The backend automatically:

- **CORS**: Allows `localhost:9000` and `localhost:9060` in CORS configuration
- **OAuth2 Redirect URI**: Points to production backend (`https://rmsgateway.atparui.com/login/oauth2/code/oidc`)
- **Post-Authentication Redirect**: Redirects to `localhost:9000` when frontend is detected as local

### 3. Keycloak Configuration

Make sure your Keycloak client has these redirect URIs configured:

**Valid Redirect URIs:**

```
https://rmsgateway.atparui.com/login/oauth2/code/oidc
```

**Web Origins** (for CORS):

```
http://localhost:9000
http://localhost:9060
https://rmsgateway.atparui.com
```

## Running the Frontend Locally

1. **Install dependencies** (if not already done):

   ```bash
   npm install
   ```

2. **Start the frontend dev server**:

   ```bash
   npm start
   ```

3. **Access the application**:
   - BrowserSync UI: `http://localhost:9000`
   - Webpack Dev Server: `http://localhost:9060`

## How It Works

### Request Flow

1. **Frontend Request** (`localhost:9000`) → User clicks login
2. **Webpack Proxy** → Proxies `/oauth2/authorization/oidc` to `https://rmsgateway.atparui.com`
3. **Backend** → Detects Origin header `http://localhost:9000` and:
   - Stores `http://localhost:9000` in session
   - Builds redirect URI: `https://rmsgateway.atparui.com/login/oauth2/code/oidc`
   - Redirects to Keycloak
4. **Keycloak** → Authenticates and redirects back to `https://rmsgateway.atparui.com/login/oauth2/code/oidc`
5. **Backend** → Processes authentication and:
   - Retrieves stored `http://localhost:9000` from session
   - Redirects user to `http://localhost:9000/`

### API Requests

All API requests from `localhost:9000` are automatically proxied to `https://rmsgateway.atparui.com`:

- `/api/**` → `https://rmsgateway.atparui.com/api/**`
- `/services/**` → `https://rmsgateway.atparui.com/services/**`
- `/management/**` → `https://rmsgateway.atparui.com/management/**`
- `/oauth2/**` → `https://rmsgateway.atparui.com/oauth2/**`
- `/login/**` → `https://rmsgateway.atparui.com/login/**`

## Troubleshooting

### Issue: CORS errors when calling APIs

**Solution**:

- Backend automatically adds `localhost:9000` to CORS allowed origins
- Check backend logs for "Auto-added localhost:9000 to CORS allowed origins"
- Verify Keycloak Web Origins includes `http://localhost:9000`

### Issue: Redirects to production domain instead of localhost:9000

**Solution**:

- Check backend logs for "Frontend running locally (localhost:9000) detected"
- Verify Origin header is being sent (check browser DevTools → Network tab)
- Check session storage: "Stored frontend URL in session: http://localhost:9000"

### Issue: OAuth2 redirect URI error from Keycloak

**Solution**:

- Verify Keycloak has `https://rmsgateway.atparui.com/login/oauth2/code/oidc` in Valid Redirect URIs
- Check backend logs for "Building OAuth2 authorization request with redirect URI"

### Issue: Proxy not working

**Solution**:

- Verify webpack dev server is running on port 9060
- Check that BrowserSync is running on port 9000
- Check browser console for proxy errors
- Verify `target: 'https://rmsgateway.atparui.com'` in `webpack.dev.js`

## Environment Variables

You can override CORS configuration using environment variables on the backend:

```bash
export CORS_ALLOWED_ORIGINS="http://localhost:9000,https://rmsgateway.atparui.com"
```

## Testing

1. **Start frontend**: `npm start`
2. **Open browser**: `http://localhost:9000`
3. **Click login**: Should redirect to Keycloak
4. **After authentication**: Should redirect back to `http://localhost:9000`
5. **Check API calls**: Should work via proxy to production backend

## Notes

- The backend is **always in production mode** - no need to change profiles
- The frontend runs locally for hot reload and fast development
- All API calls are proxied to production backend
- OAuth2 flow works seamlessly with production backend and Keycloak
- Session-based frontend URL storage ensures correct redirect after authentication
