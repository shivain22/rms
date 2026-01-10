# Tailwind CSS v3 Migration Fix

## Problem Analysis

**Root Cause:** Tailwind CSS v4 has breaking changes and limited shadcn/ui support:

- `@apply` doesn't work with custom utilities
- shadcn/ui was built for Tailwind v3
- v4 is still in early stages with compatibility issues

## Solution: Downgrade to Tailwind CSS v3

Tailwind CSS v3 is the **recommended and stable version** for shadcn/ui projects.

## Migration Steps

### 1. Update package.json

Replace Tailwind v4 packages with v3:

- Remove: `@tailwindcss/postcss: ^4.1.18`
- Remove: `tailwindcss: ^4.1.18`
- Add: `tailwindcss: ^3.4.0`
- Add: `postcss: ^8.4.0`
- Add: `autoprefixer: ^10.4.0`

### 2. Update app.scss

Change from v4 syntax to v3 syntax:

- Replace `@import 'tailwindcss';` with:
  ```css
  @tailwind base;
  @tailwind components;
  @tailwind utilities;
  ```

### 3. Update postcss.config.js

Change from v4 plugin to v3:

```javascript
module.exports = {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

### 4. Keep tailwind.config.js

The existing config works with v3.

## Why This Fix Works

1. **Full shadcn/ui Compatibility**: v3 is what shadcn/ui was designed for
2. **@apply Support**: Works with custom utilities
3. **Stable & Proven**: Widely used, well-documented
4. **No Breaking Changes**: Predictable behavior
