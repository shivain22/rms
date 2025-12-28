# Keycloak Role Mapping Configuration

## Problem

The `/account` endpoint returns empty authorities `[]` because Keycloak roles are not being properly mapped to JWT claims that JHipster can understand.

## Solution

### 1. Configure Client Mappers in Keycloak

1. **Login to Keycloak Admin Console**

   - Go to: https://rmsauth.atparui.com/admin/
   - Login with admin credentials

2. **Navigate to your client (web_app)**

   - Go to: Clients → web_app → Client scopes → web_app-dedicated → Mappers

3. **Create Realm Roles Mapper**

   - Click "Add mapper" → "By configuration" → "User Realm Role"
   - Configuration:
     - Name: `realm-roles`
     - Mapper Type: `User Realm Role`
     - Multivalued: `ON`
     - Token Claim Name: `roles`
     - Claim JSON Type: `String`
     - Add to ID token: `ON`
     - Add to access token: `ON`
     - Add to userinfo: `ON`

4. **Create Client Roles Mapper**
   - Click "Add mapper" → "By configuration" → "User Client Role"
   - Configuration:
     - Name: `client-roles`
     - Mapper Type: `User Client Role`
     - Multivalued: `ON`
     - Token Claim Name: `roles`
     - Claim JSON Type: `String`
     - Client ID: `web_app`
     - Add to ID token: `ON`
     - Add to access token: `ON`
     - Add to userinfo: `ON`

### 2. Alternative: Create Groups Mapper (if using groups)

If you prefer using groups instead of roles:

1. **Create Groups Mapper**
   - Click "Add mapper" → "By configuration" → "Group Membership"
   - Configuration:
     - Name: `groups`
     - Mapper Type: `Group Membership`
     - Token Claim Name: `groups`
     - Full group path: `OFF`
     - Add to ID token: `ON`
     - Add to access token: `ON`
     - Add to userinfo: `ON`

### 3. Verify Role Names

Ensure your roles in Keycloak are named with `ROLE_` prefix:

- `ROLE_ADMIN`
- `ROLE_USER`

### 4. Test the Configuration

1. **Get a new token** (logout and login again)
2. **Decode the JWT token** to verify it contains the roles claim
3. **Check the database** - `jhi_user_authority` table should now have entries

### 5. Debug JWT Token

You can decode your JWT token at https://jwt.io to verify it contains:

```json
{
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
  // ... other claims
}
```

## Expected Database Changes

After successful configuration, you should see entries in:

- `jhi_user` table (user record)
- `jhi_authority` table (authority records)
- `jhi_user_authority` table (user-authority relationships)
