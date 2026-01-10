# Local Frontend Development with Remote Backend Setup Guide

## Overview

This guide explains how to run the JHipster gateway frontend locally on `http://localhost:9000` while connecting to:

- **Backend Gateway**: `https://rmsgateway.atparui.com`
- **Auth Server (Keycloak)**: `https://auth.atparui.com`

## Architecture

```
Browser (localhost:9000)
    ↓
BrowserSync (localhost:9000)
    ↓
Webpack Dev Server (localhost:9060)
    ↓
Webpack Proxy
    ↓
Production Backend (https://rmsgateway.atparui.com)
    ↓
Keycloak (https://auth.atparui.com)
```

## Prerequisites

1. Node.js and npm installed
2. Frontend dependencies installed (`npm install`)
3. Production backend running and accessible
4. Keycloak server running and accessible

## Configuration Steps

### 1. Backend CORS Configuration

The production backend needs to allow requests from `http://localhost:9000`.

**Option A: Environment Variable (Recommended for Production)**

Set the `CORS_ALLOWED_ORIGINS` environment variable on the production server:

```bash
CORS_ALLOWED_ORIGINS=https://rmsgateway.atparui.com,http://localhost:9000
```

**Option B: Update application-prod.yml**

If you have access to modify the production configuration, update `src/main/resources/config/application-prod.yml`:

```yaml
jhipster:
  cors:
    allowed-origins: 'https://rmsgateway.atparui.com,http://localhost:9000'
    allowed-methods: '*'
    allowed-headers: '*'
    exposed-headers: 'Authorization,Link,X-Total-Count,X-${jhipster.clientApp.name}-alert,X-${jhipster.clientApp.name}-error,X-${jhipster.clientApp.name}-params'
    allow-credentials: true
    max-age: 1800
```

**Important**: After updating, you need to rebuild and redeploy the backend, or set the environment variable on the running server.

### 2. Keycloak Configuration

Keycloak needs to be configured to allow `http://localhost:9000` as a valid origin and redirect URI.

#### Step 1: Access Keycloak Admin Console

1. Navigate to `https://auth.atparui.com/admin`
2. Log in with admin credentials
3. Select the `gateway` realm (or your application's realm)

#### Step 2: Configure Client Settings

1. Go to **Clients** → Select `gateway-web` (or your client ID)
2. In the **Settings** tab:

   **Valid Redirect URIs:**

   ```
   https://rmsgateway.atparui.com/*
   http://localhost:9000/*
   http://localhost:9060/*
   ```

   **Web Origins:**

   ```
   https://rmsgateway.atparui.com
   http://localhost:9000
   http://localhost:9060
   ```

3. Click **Save**

#### Step 3: Configure Realm Settings (Optional but Recommended)

1. Go to **Realm Settings** → **Security Defenses** tab
2. Ensure CORS is properly configured for the realm

### 3. Frontend Configuration

The frontend is already configured correctly:

- **Webpack Proxy**: Already configured in `webpack/webpack.dev.js` to proxy API requests to `https://rmsgateway.atparui.com`
- **SERVER_API_URL**: Set to empty string (`''`), which means it uses relative URLs (correct when using proxy)
- **Port Configuration**:
  - BrowserSync runs on port 9000
  - Webpack Dev Server runs on port 9060

**No changes needed** to the frontend configuration.

### 4. Running the Frontend

1. **Start the development server:**

   ```bash
   npm start
   ```

2. **Access the application:**

   - BrowserSync URL: `http://localhost:9000`
   - Webpack Dev Server (direct): `http://localhost:9060`

3. **Verify the setup:**
   - Open browser DevTools → Network tab
   - Check that API requests are being proxied (you should see requests to `/api/**` going through)
   - Check console for any CORS errors

## How It Works

### Request Flow

1. **Browser Request**: User accesses `http://localhost:9000`
2. **BrowserSync**: Proxies to webpack dev server on `localhost:9060`
3. **Webpack Dev Server**:
   - Serves static assets directly
   - Proxies API requests (`/api/**`, `/management/**`, `/oauth2/**`, etc.) to `https://rmsgateway.atparui.com`
4. **Backend**: Processes request and returns response
5. **Response**: Flows back through the proxy chain to the browser

### OAuth2 Authentication Flow

1. **User clicks "Sign In"** on `localhost:9000`
2. **Frontend redirects** to `/oauth2/authorization/oidc`
3. **Webpack proxy** forwards to `https://rmsgateway.atparui.com/oauth2/authorization/oidc`
4. **Backend** redirects to Keycloak with redirect URI set to `http://localhost:9000/login/oauth2/code/oidc`
5. **Keycloak** authenticates user
6. **Keycloak redirects** back to `http://localhost:9000/login/oauth2/code/oidc`
7. **Webpack proxy** forwards to backend
8. **Backend** processes OAuth2 callback and sets session cookie
9. **User is authenticated**

**Critical**: The backend's `DynamicServerOAuth2AuthorizationRequestResolver` automatically detects the forwarded headers and sets the correct redirect URI based on the `X-Forwarded-Host` header.

## Troubleshooting

### Issue 1: CORS Errors

**Symptoms:**

```
Access to XMLHttpRequest at 'https://rmsgateway.atparui.com/api/...' from origin 'http://localhost:9000' has been blocked by CORS policy
```

**Solution:**

1. Verify backend CORS configuration includes `http://localhost:9000`
2. Check environment variable `CORS_ALLOWED_ORIGINS` on production server
3. Restart backend if configuration was changed

### Issue 2: OAuth2 Redirect Errors

**Symptoms:**

- Redirect URI mismatch errors
- Authentication fails after Keycloak login

**Solution:**

1. Verify Keycloak client has `http://localhost:9000/*` in Valid Redirect URIs
2. Verify Keycloak client has `http://localhost:9000` in Web Origins
3. Check browser console for specific error messages

### Issue 3: Proxy Not Working

**Symptoms:**

- 404 errors on API requests
- Requests not reaching backend

**Solution:**

1. Check webpack dev server logs for proxy messages:
   ```
   [Webpack Proxy] Matching path for proxy: /api/...
   [Webpack Proxy] Proxying request: GET /api/... to https://rmsgateway.atparui.com
   ```
2. Verify webpack proxy configuration in `webpack/webpack.dev.js`
3. Check that the backend URL is correct and accessible

### Issue 4: Cookies Not Working

**Symptoms:**

- Session not persisting
- User gets logged out on refresh

**Solution:**

1. Check that `allow-credentials: true` is set in CORS configuration
2. Verify cookies are being set (check DevTools → Application → Cookies)
3. Check that cookie domain rewriting is working in webpack proxy (see `onProxyRes` in `webpack.dev.js`)

### Issue 5: SSL Certificate Errors

**Symptoms:**

- SSL/TLS errors when proxying to HTTPS backend

**Solution:**
The webpack proxy is already configured with `secure: false` to bypass SSL certificate validation for development. If you still see errors:

1. Check that `secure: false` is set in proxy configuration
2. Verify the backend SSL certificate is valid

## Verification Checklist

- [ ] Backend CORS allows `http://localhost:9000`
- [ ] Keycloak client has `http://localhost:9000/*` in Valid Redirect URIs
- [ ] Keycloak client has `http://localhost:9000` in Web Origins
- [ ] Webpack proxy is configured correctly
- [ ] Frontend starts without errors
- [ ] API requests are proxied (check Network tab)
- [ ] OAuth2 login flow works
- [ ] Session cookies are set and persisted

## Environment Variables Reference

### Backend (Production Server)

```bash
# CORS Configuration
CORS_ALLOWED_ORIGINS=https://rmsgateway.atparui.com,http://localhost:9000
CORS_ALLOWED_METHODS=*
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=1800
```

### Frontend (Local Development)

No environment variables needed - configuration is in `webpack/webpack.dev.js`.

## Additional Notes

1. **Development vs Production**: This setup is for local frontend development only. In production, the frontend is served by the backend.

2. **Security**: The `secure: false` option in webpack proxy bypasses SSL certificate validation. This is acceptable for development but should never be used in production.

3. **Performance**: Proxying through webpack dev server adds a small latency. This is normal and acceptable for development.

4. **Hot Reload**: BrowserSync provides hot reload functionality. Changes to frontend code will automatically refresh the browser.

## Related Documentation

- [JHipster Separating Front-end and API Server](https://www.jhipster.tech/separating-front-end-and-api/)
- [Webpack Dev Server Proxy Configuration](https://webpack.js.org/configuration/dev-server/#devserverproxy)
- [Keycloak CORS Configuration](https://www.keycloak.org/docs/latest/server_admin/#_cors)
