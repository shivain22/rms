# Debugging JWT 401 Issue

## Quick Debug Steps

### 1. Check Frontend Token Extraction

After logging in, open browser console and run:

```javascript
window.checkToken();
```

This will show:

- ✓ If token is in sessionStorage
- ✓ Token details (subject, issuer, expiration)
- ✓ If token is expired

### 2. Check Frontend Logs

After login, look for these console logs:

```
[Axios Interceptor] ✓ Token extracted from URL hash
[Axios Interceptor] Token stored in sessionStorage
[Axios Interceptor] ✓ Bearer token added to request: /api/account
```

**If you see:**

- `⚠ No token found in sessionStorage` → Token not extracted from URL
- `⚠ No token found in URL hash` → Backend not sending token in redirect

### 3. Check Network Tab

1. Open DevTools → Network tab
2. Find `/api/account` request
3. Check **Request Headers**:
   - Should have: `Authorization: Bearer <token>`
   - If missing → Token not being sent

### 4. Check Backend Logs

Look for these logs in backend console:

**After OAuth2 login:**

```
=== OAuth2 Authentication Success ===
ID Token (first 50 chars): eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Redirecting to frontend URL: http://localhost:9000/
ID token included in redirect (frontend should extract and store it)
```

**When API request arrives:**

```
=== JWT Decoder Called (Bearer Token from Request) ===
Decoding JWT token (first 50 chars): eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
✓ JWT decoded successfully for subject: <user-id>
=== JWT Authentication Converter Called (Bearer Token) ===
```

**If you see:**

- `✗ JWT decode error` → Token validation failed (wrong signature, expired, etc.)
- No JWT decoder logs → Bearer token not received (check frontend)

**In AccountResource:**

```
=== Account Endpoint Called ===
✓ Authenticated via JWT Bearer token (oauth2ResourceServer)
```

## Common Issues

### Issue 1: Token Not in URL Hash

**Symptoms:**

- URL after login: `http://localhost:9000/` (no hash)
- Console: `⚠ No token in URL hash`

**Cause:**

- Backend not including token in redirect URL

**Solution:**

- Check backend logs for "ID token included in redirect"
- Verify `oauth2AuthenticationSuccessHandler` is extracting token

### Issue 2: Token Not Extracted

**Symptoms:**

- URL has `#access_token=...` but `checkToken()` shows no token

**Cause:**

- Token extraction code not running
- Token format issue

**Solution:**

- Check browser console for extraction logs
- Verify `axios-interceptor.ts` is loaded

### Issue 3: Token Not Sent

**Symptoms:**

- Token in sessionStorage but Network tab shows no `Authorization` header

**Cause:**

- Axios interceptor not adding header

**Solution:**

- Check console for "Bearer token added to request" log
- Verify `onRequestSuccess` is being called

### Issue 4: Token Invalid/Expired

**Symptoms:**

- Backend logs: `✗ JWT decode error`
- Error message about signature or expiration

**Cause:**

- Token expired
- Token signature invalid
- Wrong issuer/audience

**Solution:**

- Run `window.checkToken()` to check expiration
- Verify token issuer matches backend configuration
- Check token audience matches `jhipster.security.oauth2.audience`

### Issue 5: Backend Not Processing Bearer Token

**Symptoms:**

- No JWT decoder logs in backend
- Backend returns 401 immediately

**Cause:**

- `oauth2ResourceServer` not configured correctly
- Bearer token not being checked

**Solution:**

- Verify `oauth2ResourceServer` is configured in `SecurityConfiguration`
- Check filter order (should process before session auth)

## Testing Checklist

- [ ] Login completes successfully
- [ ] URL has `#access_token=...` after redirect
- [ ] `window.checkToken()` shows token details
- [ ] Console shows "Token extracted from URL hash"
- [ ] Console shows "Bearer token added to request"
- [ ] Network tab shows `Authorization: Bearer <token>` header
- [ ] Backend logs show "JWT Decoder Called"
- [ ] Backend logs show "JWT decoded successfully"
- [ ] Backend logs show "Authenticated via JWT Bearer token"
- [ ] `/api/account` returns 200 OK

## Next Steps

1. **Run the debug steps above**
2. **Check which step fails** (frontend extraction, sending, or backend validation)
3. **Share the logs** from both frontend console and backend console
4. **Check token details** using `window.checkToken()`

The enhanced logging will help identify exactly where the authentication flow is breaking.
