# CORS Configuration Guide

## Understanding the CORS Error

The CORS (Cross-Origin Resource Sharing) error you're seeing occurs when:

- **Origin**: `https://rmsgateway.atparui.com` (your frontend)
- **Target**: `https://rmsauth.atparui.com` (Keycloak authentication server)
- **Error**: "No 'Access-Control-Allow-Origin' header is present"

This happens because the browser is trying to access Keycloak directly, and Keycloak doesn't have CORS configured to allow requests from your gateway domain.

## Solution: Configure CORS in Keycloak

The CORS error needs to be fixed in **Keycloak**, not in the RMS application. Here's how:

### Option 1: Configure CORS in Keycloak Realm Settings (Recommended)

1. **Log into Keycloak Admin Console**

   - Navigate to `https://rmsauth.atparui.com/admin`
   - Select your realm (likely `gateway`)

2. **Configure Web Origins**

   - Go to **Realm Settings** → **Security Defenses** tab
   - Or go to **Clients** → Select your client → **Settings** tab
   - Find the **Web Origins** field
   - Add: `https://rmsgateway.atparui.com`
   - Or use `*` for development (not recommended for production)

3. **Configure Valid Redirect URIs**
   - In **Clients** → Select your client → **Settings** tab
   - Add to **Valid Redirect URIs**: `https://rmsgateway.atparui.com/*`
   - Add to **Web Origins**: `https://rmsgateway.atparui.com`

### Option 2: Configure via Keycloak Admin API

If you have access to the Keycloak Admin API, you can configure CORS programmatically:

```bash
# Update client settings to allow CORS from your gateway
curl -X PUT "https://rmsauth.atparui.com/admin/realms/gateway/clients/{client-id}" \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "webOrigins": ["https://rmsgateway.atparui.com"],
    "redirectUris": ["https://rmsgateway.atparui.com/*"]
  }'
```

### Option 3: Configure Keycloak via Docker/Configuration Files

If Keycloak is running in Docker, you can configure CORS in the Keycloak configuration:

```yaml
# In your Keycloak docker-compose.yml or configuration
environment:
  - KEYCLOAK_FRONTEND_URL=https://rmsauth.atparui.com
  # Or configure via realm JSON import
```

## RMS Application CORS Configuration

The RMS application already has CORS configured in `application-prod.yml`:

```yaml
jhipster:
  cors:
    allowed-origins: 'https://rmsgateway.atparui.com'
    allowed-methods: '*'
    allowed-headers: '*'
    allow-credentials: true
```

This configuration handles CORS for:

- `/api/**` endpoints
- `/management/**` endpoints
- `/oauth2/**` endpoints
- `/login/**` endpoints

**Note**: This only affects requests to the RMS backend, not direct requests to Keycloak.

## Why This Error Occurs

1. **Browser Security**: Browsers enforce the Same-Origin Policy, which blocks cross-origin requests unless explicitly allowed via CORS headers.

2. **OAuth2 Flow**: During the OAuth2 authentication flow:

   - User clicks "Sign In" on `rmsgateway.atparui.com`
   - Browser redirects to `rmsauth.atparui.com` (Keycloak)
   - Keycloak needs to send CORS headers allowing `rmsgateway.atparui.com` as the origin

3. **Manifest Request**: The error mentions `manifest.webapp` - this is a PWA manifest file that the browser requests, and Keycloak is redirecting it, causing the CORS check.

## Testing the Fix

After configuring CORS in Keycloak:

1. **Clear browser cache** or use an incognito window
2. **Open browser DevTools** → Network tab
3. **Try to sign in** and check:
   - No CORS errors in the console
   - Successful redirect to Keycloak
   - Successful redirect back to the gateway

## Additional Notes

- The 401 Unauthorized error for `/api/account` is expected when not authenticated - this is normal behavior
- The CORS error is the main issue preventing proper authentication flow
- Once CORS is configured in Keycloak, the authentication flow should work correctly

## Troubleshooting

If CORS errors persist after configuration:

1. **Verify Keycloak client settings**:

   - Web Origins includes your gateway URL
   - Valid Redirect URIs includes your gateway URL patterns

2. **Check Keycloak logs** for CORS-related errors

3. **Verify the realm name** matches what's configured in your application

4. **Test with browser DevTools**:
   - Check the Network tab for the actual request/response headers
   - Verify `Access-Control-Allow-Origin` header is present in Keycloak responses
