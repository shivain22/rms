# PROCESS 1: Setup and Infrastructure

**Status**: ⚠️ **CRITICAL - MUST BE COMPLETED FIRST**  
**Estimated Time**: 2-3 hours  
**Dependencies**: None  
**Can Run in Parallel**: ❌ No - Only one agent should execute this process

---

## Overview

This process sets up the foundation for the migration by installing Tailwind CSS, configuring shadcn/ui, and removing Bootstrap dependencies. This must be completed before any other processes can begin.

---

## Prerequisites

- Node.js and npm installed
- Access to the project repository
- Ability to modify `package.json`, webpack config, and SCSS files

---

## Step-by-Step Instructions

### Step 1: Install Tailwind CSS and Dependencies

```bash
# Navigate to project root
cd c:\Users\shiva\eclipse-workspace\rms

# Install Tailwind CSS and required dependencies
npm install -D tailwindcss postcss autoprefixer

# Initialize Tailwind configuration
npx tailwindcss init -p
```

**Expected Output:**

- `tailwind.config.js` file created
- `postcss.config.js` file created (if it didn't exist)

---

### Step 2: Configure Tailwind CSS

#### 2.1 Update `tailwind.config.js`

Open `tailwind.config.js` and configure it as follows:

```javascript
/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['class'],
  content: ['./src/main/webapp/**/*.{js,jsx,ts,tsx}', './src/main/webapp/index.html'],
  theme: {
    extend: {
      // Add custom theme extensions here if needed
      // Colors, spacing, etc. can be customized
    },
  },
  plugins: [],
};
```

#### 2.2 Verify `postcss.config.js`

Ensure `postcss.config.js` contains:

```javascript
module.exports = {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

---

### Step 3: Initialize shadcn/ui

```bash
# Initialize shadcn/ui (this will create components.json)
npx shadcn@latest init
```

**During initialization, answer prompts:**

- **Style**: Default (or choose based on preference)
- **Base color**: Slate (or match existing theme)
- **CSS variables**: Yes
- **Where is your global CSS file?**: `src/main/webapp/app/app.scss`
- **Would you like to use CSS variables for colors?**: Yes
- **Are you using a custom tailwind prefix?**: No (unless you have one)
- **Where is your tailwind.config.js located?**: `./tailwind.config.js`
- **Configure the import alias for components?**: `@/components` (or your preference)
- **Configure the import alias for utils?**: `@/lib/utils` (or your preference)

**Expected Output:**

- `components.json` file created in project root
- `src/lib/utils.ts` file created (or path you specified)

---

### Step 4: Update `app.scss` to Use Tailwind

#### 4.1 Read Current `app.scss`

Open `src/main/webapp/app/app.scss` and note the current Bootstrap imports.

#### 4.2 Replace Bootstrap Imports with Tailwind

Replace the Bootstrap imports at the top of `app.scss`:

**REMOVE:**

```scss
// Override Bootstrap variables
@import 'bootstrap-variables';
@import 'bootswatch/dist/brite/variables';
// Import Bootstrap source files from node_modules
@import 'bootstrap/scss/bootstrap';
@import 'bootswatch/dist/brite/bootswatch';
```

**ADD:**

```scss
@tailwind base;
@tailwind components;
@tailwind utilities;
```

#### 4.3 Keep Custom Styles

Keep all the custom styles below (`.app-container`, `.fullscreen`, etc.) as they may still be needed or can be converted to Tailwind classes later.

---

### Step 5: Update Webpack Configuration (if needed)

#### 5.1 Check Webpack Config

Verify that your webpack configuration includes `postcss-loader` for processing CSS. Check `webpack/webpack.dev.js` and `webpack/webpack.prod.js`.

#### 5.2 Add PostCSS Loader (if missing)

If `postcss-loader` is not configured, add it to your CSS processing chain:

```javascript
{
  test: /\.scss$/,
  use: [
    'style-loader',
    'css-loader',
    'postcss-loader',  // Add this
    'sass-loader'
  ]
}
```

---

### Step 6: Test Build Process

```bash
# Test that the build works with Tailwind
npm run webapp:build:dev
```

**Expected Result:**

- Build completes without errors
- Tailwind CSS is processed correctly
- No Bootstrap-related errors (may have warnings about missing Bootstrap classes, which is expected)

---

### Step 7: Remove Bootstrap Dependencies (DO NOT DELETE YET)

⚠️ **IMPORTANT**: Do not remove Bootstrap from `package.json` yet. We'll keep it temporarily to avoid breaking the application during migration.

**Instead, create a note in package.json:**

```json
{
  "dependencies": {
    "bootstrap": "5.3.6", // TODO: Remove after migration complete
    "reactstrap": "9.2.3", // TODO: Remove after migration complete
    "bootswatch": "5.3.5" // TODO: Remove after migration complete
  }
}
```

---

### Step 8: Create Layout Utilities File

Create a new file for layout utilities that will replace Row/Col:

**File**: `src/main/webapp/app/shared/layout/layout-utils.tsx`

```tsx
import React from 'react';
import { cn } from '@/lib/utils'; // Adjust import path based on your shadcn config

/**
 * Row component to replace reactstrap Row
 * Uses Tailwind's flexbox utilities
 */
export const Row: React.FC<{
  children: React.ReactNode;
  className?: string;
}> = ({ children, className }) => {
  return <div className={cn('flex flex-wrap', className)}>{children}</div>;
};

/**
 * Col component to replace reactstrap Col
 * Uses Tailwind's grid/flex utilities
 *
 * @param md - Medium breakpoint column span (1-12)
 * @param sm - Small breakpoint column span (1-12)
 * @param lg - Large breakpoint column span (1-12)
 */
export const Col: React.FC<{
  children: React.ReactNode;
  className?: string;
  md?: number | string;
  sm?: number | string;
  lg?: number | string;
  xs?: number | string;
}> = ({ children, className, md, sm, lg, xs }) => {
  const colClasses = [];

  // Default: full width on mobile
  if (xs) {
    colClasses.push(`col-span-${xs}`);
  } else {
    colClasses.push('col-span-12');
  }

  // Small breakpoint
  if (sm) {
    colClasses.push(`sm:col-span-${sm}`);
  }

  // Medium breakpoint
  if (md) {
    colClasses.push(`md:col-span-${md}`);
  }

  // Large breakpoint
  if (lg) {
    colClasses.push(`lg:col-span-${lg}`);
  }

  return <div className={cn('grid grid-cols-12 gap-4', colClasses.join(' '), className)}>{children}</div>;
};

// Alternative: Simple Col using flex
export const ColFlex: React.FC<{
  children: React.ReactNode;
  className?: string;
  md?: number | string;
  sm?: number | string;
}> = ({ children, className, md, sm }) => {
  const widthClasses = [];

  if (md) {
    widthClasses.push(`md:w-${md}/12`);
  }
  if (sm) {
    widthClasses.push(`sm:w-${sm}/12`);
  }

  return <div className={cn('w-full', widthClasses.join(' '), className)}>{children}</div>;
};
```

**Note**: Adjust the import path for `cn` based on where your `utils.ts` file is located (check `components.json`).

---

### Step 9: Verify Setup

#### 9.1 Check Files Created/Modified

Verify these files exist or were modified:

- ✅ `tailwind.config.js` (created)
- ✅ `postcss.config.js` (created or updated)
- ✅ `components.json` (created)
- ✅ `src/lib/utils.ts` (created, path may vary)
- ✅ `src/main/webapp/app/app.scss` (updated)
- ✅ `src/main/webapp/app/shared/layout/layout-utils.tsx` (created)

#### 9.2 Test Application

```bash
# Start development server
npm run start
```

**Expected Result:**

- Application starts without errors
- No console errors related to missing Bootstrap
- Application may look broken (expected - Bootstrap styles removed)
- Tailwind is working (you can test by adding a Tailwind class to verify)

---

## Deliverables Checklist

- [ ] Tailwind CSS installed and configured
- [ ] PostCSS configured
- [ ] shadcn/ui initialized
- [ ] `app.scss` updated with Tailwind directives
- [ ] Webpack configured for PostCSS (if needed)
- [ ] Layout utilities file created
- [ ] Build process works
- [ ] Development server starts successfully

---

## Handoff Information

Once this process is complete, notify other agents that they can begin:

- **Process 2**: Can begin (Core Component Migration)
- **Process 3**: Can begin after Process 2
- **Process 4**: Can begin after Process 2
- **Process 5**: Can begin after Process 2
- **Process 6**: Can begin after most components are migrated
- **Process 7**: Can begin incrementally as components are completed

---

## Troubleshooting

### Issue: Build fails with "Cannot find module 'tailwindcss'"

**Solution**: Run `npm install` again to ensure dependencies are installed.

### Issue: Tailwind classes not working

**Solution**:

1. Verify `content` paths in `tailwind.config.js` include your files
2. Check that `@tailwind` directives are in `app.scss`
3. Restart the dev server

### Issue: PostCSS errors

**Solution**: Verify `postcss.config.js` exists and webpack is configured to use `postcss-loader`.

### Issue: shadcn init fails

**Solution**: Ensure you're in the project root directory and have write permissions.

---

## Notes

- Bootstrap is kept in dependencies temporarily to avoid breaking the app
- The app will look broken until components are migrated - this is expected
- Layout utilities are a temporary solution - some pages may need custom Tailwind classes
- All agents should coordinate to avoid merge conflicts in shared files

---

**Process Owner**: [Agent Name]  
**Start Date**: [Date]  
**Completion Date**: [Date]  
**Status**: ⏳ In Progress / ✅ Complete
