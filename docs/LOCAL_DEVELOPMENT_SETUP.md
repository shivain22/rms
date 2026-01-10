# Local Frontend Development Setup Guide

This guide explains how to develop the frontend locally (on `localhost:9000`) while the backend runs in production profile.

## Architecture

The OAuth2 flow works as follows:

1. **Frontend** (`localhost:9000`) → User clicks login
2. **Backend** (`localhost:8080` or `localhost:9293`) → Redirects to Keycloak with redirect URI pointing to backend
3. **Keycloak** → Authenticates user and redirects back to backend callback
4. **Backend** → Processes OAuth2 callback and redirects to frontend (`localhost:9000`)

## Key Points

### 1. OAuth2 Redirect URI (Keycloak Configuration)

The redirect URI **must point to the backend**, not the frontend:

- ✅ Correct: `http://localhost:8080/login/oauth2/code/oidc`
- ✅ Correct: `http://localhost:9293/login/oauth2/code/oidc`
- ❌ Wrong: `http://localhost:9000/login/oauth2/code/oidc`

### 2. Keycloak Client Configuration

In Keycloak, you need to configure the OAuth2 client with the following redirect URIs:

**For Local Development:**

```
http://localhost:8080/login/oauth2/code/oidc
http://localhost:9293/login/oauth2/code/oidc
http://127.0.0.1:8080/login/oauth2/code/oidc
http://127.0.0.1:9293/login/oauth2/code/oidc
```

**For Production:**

```
https://rmsgateway.atparui.com/login/oauth2/code/oidc
```

**Steps to Configure in Keycloak:**

1. Log in to Keycloak Admin Console
2. Navigate to your realm (e.g., `atparui`)
3. Go to **Clients** → Select your client (e.g., `rms-gateway`)
4. In the **Valid Redirect URIs** field, add:
   ```
   http://localhost:8080/login/oauth2/code/oidc
   http://localhost:9293/login/oauth2/code/oidc
   http://127.0.0.1:8080/login/oauth2/code/oidc
   http://127.0.0.1:9293/login/oauth2/code/oidc
   ```
5. In **Web Origins**, add:
   ```
   http://localhost:9000
   http://localhost:9060
   ```
   (This allows CORS requests from the frontend dev server)
6. Save the configuration

### 3. Backend Port Detection

The backend automatically detects which port it's running on:

- **Port 8080**: Typical Spring Boot container port
- **Port 9293**: Typical host-mapped port for JHipster gateway
- **Port 8082**: Alternative backend port
- **Other ports**: Will be detected automatically

The redirect URI sent to Keycloak will use the backend's actual port.

### 4. Frontend Redirect After Authentication

After successful authentication, the backend automatically redirects to:

- `http://localhost:9000/` (if Origin/Referer header indicates localhost:9000)
- Or falls back to detecting localhost backend and defaulting to localhost:9000

### 5. CORS Configuration

CORS is automatically configured to allow `localhost:9000` when the backend detects it's running on localhost, even in production profile.

## Testing the Setup

1. **Start Backend** (in prod profile):

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
   ```

   Or if using Docker:

   ```bash
   docker-compose up
   ```

2. **Start Frontend Dev Server**:

   ```bash
   npm start
   ```

   This should start on `http://localhost:9000`

3. **Verify Keycloak Configuration**:

   - Check that redirect URIs include your backend port
   - Check that Web Origins include `http://localhost:9000`

4. **Test Login Flow**:
   - Navigate to `http://localhost:9000`
   - Click login
   - Should redirect to Keycloak
   - After authentication, should redirect back to `http://localhost:9000`

## Troubleshooting

### Issue: "Invalid redirect URI" error from Keycloak

**Solution**: Make sure the redirect URI in Keycloak matches exactly:

- Check what port your backend is running on
- Add that exact URI to Keycloak's Valid Redirect URIs
- Example: If backend is on port 8080, add `http://localhost:8080/login/oauth2/code/oidc`

### Issue: Redirects to production domain instead of localhost:9000

**Solution**:

- Check that Origin header is being sent from frontend
- Check backend logs for "Redirecting to frontend URL" message
- Verify backend is running on localhost (not production server)

### Issue: CORS errors when calling APIs

**Solution**:

- Backend automatically adds localhost:9000 to CORS when running on localhost
- If still having issues, check WebConfigurer logs for CORS configuration
- Verify Keycloak Web Origins includes `http://localhost:9000`

## Environment Variables

You can override CORS configuration using environment variables:

```bash
export CORS_ALLOWED_ORIGINS="http://localhost:9000,https://rmsgateway.atparui.com"
```

This is useful if you need to add additional origins beyond the automatic localhost detection.
