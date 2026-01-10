# BrowserSync Proxy Issue - 404 on API Requests

## Problem

When accessing the app through BrowserSync on `localhost:9000`, API requests (like `/api/account`) are returning 404 errors. The requests are not being forwarded to the webpack dev server on port 9060.

## Root Cause

BrowserSync on port 9000 is not correctly forwarding API requests to the webpack dev server on port 9060, which has the proxy configuration to forward requests to the production backend.

## Solution Options

### Option 1: Access Webpack Dev Server Directly (Recommended)

Instead of using BrowserSync on port 9000, access the webpack dev server directly on port 9060:

1. Open `http://localhost:9060` in your browser
2. This bypasses BrowserSync and goes directly to the webpack dev server
3. The webpack dev server has the correct proxy configuration to forward API requests to the backend

**Advantages:**

- No proxy chain (BrowserSync → Webpack Dev Server → Backend)
- Direct access to the webpack dev server with proper proxy configuration
- Cookies and sessions work correctly
- Simpler debugging

### Option 2: Fix BrowserSync Configuration

If you need to use BrowserSync on port 9000, ensure it's correctly forwarding all requests:

1. Check BrowserSync logs to see if requests are being forwarded
2. Verify that BrowserSync is proxying to `localhost:9060`
3. Ensure BrowserSync middleware isn't intercepting API requests

## Current Configuration

- **BrowserSync**: `localhost:9000` → Proxies to webpack dev server
- **Webpack Dev Server**: `localhost:9060` → Proxies API requests to `https://rmsgateway.atparui.com`
- **Backend**: `https://rmsgateway.atparui.com` → Handles authentication and API requests

## Testing

After making changes:

1. Clear browser cookies and cache
2. Restart the dev server: `npm start`
3. Access the app at `http://localhost:9060` (not 9000)
4. Test login flow
5. Verify `/api/account` returns 200 (not 404)

## Notes

- The webpack dev server proxy is correctly configured with cookie forwarding
- Axios is configured with `withCredentials: true` to send cookies
- The backend CORS configuration allows `localhost:9000` and `localhost:9060`
