# Logo and Favicon Replacement Guide

## Overview

This guide explains how to replace the logo and favicon with your custom aPAR branding.

## Logo Replacement

### Current Implementation

The logo is now implemented as a CSS-based design in:

- `src/main/webapp/app/shared/layout/header/header-components.tsx` (Brand component)
- `src/main/webapp/app/shared/layout/sidebar/sidebar.tsx` (Sidebar logo)

### Option 1: Use Image File (Recommended)

1. Place your logo image file in `src/main/webapp/content/images/`

   - Recommended formats: PNG, SVG
   - Recommended size: 32x32px to 64x64px for icon, or larger for full logo

2. Update `header-components.tsx`:

```tsx
export const BrandIcon = props => (
  <div {...props} className="flex items-center justify-center w-8 h-8">
    <img src="content/images/apar-logo.png" alt="aPAR Logo" className="w-full h-full object-contain" />
  </div>
);

export const Brand = () => (
  <Link to="/" className="flex items-center space-x-2 brand-logo">
    <BrandIcon />
    <div className="flex flex-col">
      <span className="text-lg font-bold text-foreground">PAR</span>
      <span className="text-xs text-muted-foreground">intelligence</span>
    </div>
  </Link>
);
```

3. Update `sidebar.tsx` (around line 35):

```tsx
<Link to="/" className="flex items-center space-x-2">
  <img src="content/images/apar-logo.png" alt="aPAR Logo" className="w-8 h-8 object-contain" />
  <div className="flex flex-col">
    <span className="text-lg font-bold text-foreground">PAR</span>
    <span className="text-xs text-muted-foreground">intelligence</span>
  </div>
</Link>
```

### Option 2: Keep CSS-based Logo

The current implementation uses a gradient background with the letter "a". You can customize the colors in the `bg-gradient-to-br from-orange-500 to-green-500` classes to match your brand colors.

## Favicon Replacement

### Steps:

1. Create or obtain your favicon file:

   - Format: `.ico` (recommended) or `.png`
   - Sizes: 16x16, 32x32, 48x48 pixels
   - You can use online tools like [favicon.io](https://favicon.io) or [realfavicongenerator.net](https://realfavicongenerator.net)

2. Replace the existing favicon:

   - Location: `src/main/webapp/favicon.ico`
   - Simply overwrite the existing file with your new favicon

3. (Optional) Update HTML reference:

   - File: `src/main/webapp/index.html`
   - Line 11: `<link rel="icon" href="favicon.ico" />`
   - This should already be correct, but verify the path matches your file location

4. For better browser support, you can add multiple favicon sizes:
   ```html
   <link rel="icon" type="image/png" sizes="32x32" href="favicon-32x32.png" />
   <link rel="icon" type="image/png" sizes="16x16" href="favicon-16x16.png" />
   <link rel="apple-touch-icon" sizes="180x180" href="apple-touch-icon.png" />
   ```

### After Replacement:

1. Clear browser cache or do a hard refresh (Ctrl+F5 / Cmd+Shift+R)
2. Restart the development server if needed
3. The new favicon should appear in browser tabs

## Notes

- The logo appears in both the sidebar (left) and header (top on mobile)
- The favicon appears in browser tabs and bookmarks
- Make sure image files are optimized for web (compressed) to improve load times
- SVG format is recommended for logos as it scales perfectly at any size
