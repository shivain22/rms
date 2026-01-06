# Tailwind CSS v4 + shadcn/ui Setup Guide

## Issue Fixed

The error `Cannot apply unknown utility class 'bg-background'` occurred because **Tailwind CSS v4** doesn't support using `@apply` with custom color utilities that reference CSS variables.

## Correct Approach for Tailwind CSS v4

### ❌ **WRONG** (Doesn't work in Tailwind v4):

```scss
@layer base {
  body {
    @apply bg-background text-foreground; // ❌ This fails in v4
  }
}
```

### ✅ **CORRECT** (Works in Tailwind v4):

```scss
@layer base {
  body {
    background-color: hsl(var(--background));
    color: hsl(var(--foreground));
    margin: 0;
  }
}
```

## Key Differences: Tailwind v3 vs v4

### Tailwind CSS v3

- Used `@tailwind base;`, `@tailwind components;`, `@tailwind utilities;`
- Could use `@apply` with custom utilities
- Required separate config file

### Tailwind CSS v4

- Uses `@import 'tailwindcss';` (single import)
- Uses `@layer theme, base, components, utilities;` for organization
- **Cannot use `@apply` with CSS variable-based custom colors**
- Must use CSS variables directly with `hsl(var(--variable))`

## Proper Setup for Tailwind v4 + shadcn/ui

### 1. **CSS Variables Setup** (`app.scss`)

```scss
@import 'tailwindcss';

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --primary: 45 92% 16%;
    /* ... other variables */
  }

  .dark {
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    /* ... dark mode variables */
  }
}

@layer base {
  body {
    /* ✅ Use CSS variables directly, NOT @apply */
    background-color: hsl(var(--background));
    color: hsl(var(--foreground));
    margin: 0;
  }
}
```

### 2. **Tailwind Config** (`tailwind.config.js`)

```javascript
module.exports = {
  darkMode: ['class'],
  content: ['./src/main/webapp/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        // ... other colors
      },
    },
  },
};
```

### 3. **PostCSS Config** (`postcss.config.js`)

```javascript
module.exports = {
  plugins: {
    '@tailwindcss/postcss': {}, // ✅ Tailwind v4 plugin
    autoprefixer: {},
  },
};
```

## Migration Checklist

### ✅ Completed

- [x] Removed `@apply bg-background text-foreground` (not supported in v4)
- [x] Using CSS variables directly with `hsl(var(--variable))`
- [x] Proper `@layer` organization
- [x] Tailwind v4 PostCSS plugin configured
- [x] Colors defined in both CSS variables and Tailwind config

### ⚠️ Important Notes

1. **Never use `@apply` with custom color utilities in Tailwind v4**

   - Use CSS variables directly: `background-color: hsl(var(--background));`
   - Or use utility classes in JSX: `className="bg-background"`

2. **CSS Variables Format**

   - Store values without `hsl()`: `--background: 0 0% 100%;`
   - Use with `hsl()` when applying: `hsl(var(--background))`

3. **Component Usage**
   - In JSX: `<div className="bg-background text-foreground">` ✅ Works
   - In CSS: `background-color: hsl(var(--background));` ✅ Works
   - In CSS: `@apply bg-background;` ❌ Doesn't work in v4

## Removing Reactstrap/JHipster Styles

### Step 1: Remove Dependencies (Optional - keep for now)

```bash
# Don't remove yet if still using some components
# npm uninstall reactstrap bootstrap
```

### Step 2: Scope Legacy Styles

- Rename generic classes: `.card` → `.legacy-card`
- Rename generic classes: `.btn` → `.legacy-btn`
- Scope to specific containers to avoid conflicts

### Step 3: Use Tailwind Utilities

- Replace Bootstrap classes with Tailwind utilities
- Use shadcn/ui components instead of Reactstrap

## Testing

After applying these fixes:

1. ✅ Build should compile without errors
2. ✅ `bg-background` and `text-foreground` utilities work in JSX
3. ✅ CSS variables work correctly
4. ✅ No conflicts with legacy styles

## References

- [Tailwind CSS v4 Documentation](https://tailwindcss.com/docs)
- [shadcn/ui Documentation](https://ui.shadcn.com)
- [Tailwind v4 Migration Guide](https://tailwindcss.com/docs/upgrade-guide)
