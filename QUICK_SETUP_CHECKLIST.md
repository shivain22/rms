# Quick Setup Checklist - Local Frontend with Remote Backend

## What You Need to Do

### ✅ Frontend (Local - Already Done)

- [x] Webpack proxy configured to forward to `https://rmsgateway.atparui.com`
- [x] SERVER_API_URL set to empty (uses relative URLs via proxy)
- [x] BrowserSync configured on port 9000
- [x] Webpack dev server configured on port 9060

### ⚠️ Backend (Production Server - ACTION REQUIRED)

You need to configure the production backend to allow CORS from `http://localhost:9000`.

**Option 1: Environment Variable (Recommended - No Redeploy Needed)**

Set this environment variable on your production server:

```bash
CORS_ALLOWED_ORIGINS=https://rmsgateway.atparui.com,http://localhost:9000,http://localhost:9060
```

**How to set:**

- If using Docker: Add to `docker-compose.yml` or `.env` file
- If using systemd: Add to service file or `/etc/environment`
- If using Kubernetes: Add to deployment YAML or ConfigMap
- If using a cloud platform: Set via platform's environment variable configuration

**Option 2: Update Configuration File (Requires Redeploy)**

If you can rebuild and redeploy, the `application-prod.yml` file has been updated to include localhost origins. After redeploying, the backend will accept requests from localhost.

### ⚠️ Keycloak (Production Server - ACTION REQUIRED)

You need to configure Keycloak to allow `http://localhost:9000` as a valid origin and redirect URI.

**Steps:**

1. **Access Keycloak Admin Console:**

   - Go to `https://rmsauth.atparui.com/admin`
   - Log in with admin credentials
   - Select the `gateway` realm

2. **Configure Client:**

   - Go to **Clients** → Select `gateway-web`
   - In **Settings** tab, update:

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

   - Click **Save**

3. **Verify:**
   - Test login from `http://localhost:9000`
   - Check browser console for CORS errors

## Testing the Setup

1. **Start Frontend:**

   ```bash
   npm start
   ```

2. **Access Application:**

   - Open `http://localhost:9000` in browser

3. **Check Network Tab:**

   - Open DevTools → Network
   - Look for API requests (should show `/api/**` paths)
   - Check that requests are successful (not CORS errors)

4. **Test Authentication:**
   - Click "Sign In"
   - Should redirect to Keycloak
   - After login, should redirect back to `http://localhost:9000`
   - Should be authenticated

## Common Issues

### CORS Error

**Symptom:** `Access to XMLHttpRequest ... has been blocked by CORS policy`

**Solution:**

- Verify `CORS_ALLOWED_ORIGINS` environment variable includes `http://localhost:9000`
- Or verify `application-prod.yml` was updated and backend was redeployed

### OAuth2 Redirect Error

**Symptom:** `Invalid redirect URI` or authentication fails after Keycloak login

**Solution:**

- Verify Keycloak client has `http://localhost:9000/*` in Valid Redirect URIs
- Verify Keycloak client has `http://localhost:9000` in Web Origins

### 404 on API Requests

**Symptom:** API requests return 404

**Solution:**

- Check webpack dev server console for proxy logs
- Verify backend URL is correct: `https://rmsgateway.atparui.com`
- Verify backend is running and accessible

## Files Modified

1. `src/main/resources/config/application-prod.yml` - Added localhost to CORS allowed origins
2. `webpack/webpack.dev.js` - Fixed X-Forwarded-Host header for OAuth2 redirects
3. `LOCAL_FRONTEND_REMOTE_BACKEND_SETUP.md` - Comprehensive setup guide

## Next Steps

1. ✅ Configure backend CORS (environment variable or redeploy)
2. ✅ Configure Keycloak client settings
3. ✅ Start frontend: `npm start`
4. ✅ Test the setup

## Need Help?

See `LOCAL_FRONTEND_REMOTE_BACKEND_SETUP.md` for detailed troubleshooting and explanation.
