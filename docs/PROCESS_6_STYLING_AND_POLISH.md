# PROCESS 6: Styling and Polish

**Status**: 🔄 **Can run after most components are migrated**  
**Estimated Time**: 2-3 hours  
**Dependencies**: Processes 2-5 (most components migrated)  
**Can Run in Parallel**: ✅ Yes - Can work alongside final testing

---

## Overview

This process involves final styling adjustments, theme customization, and ensuring visual consistency across all migrated components. This can be done incrementally as components are completed.

---

## Prerequisites

- ✅ Process 1 completed (Setup)
- ✅ Most components migrated (Processes 2-5)
- Tailwind CSS configured
- shadcn components installed

---

## Step-by-Step Instructions

### Step 1: Review Global Styles

#### 1.1 Update app.scss

**File**: `src/main/webapp/app/app.scss`

Review and update custom styles:

**Keep/Update:**

- `.app-container` styles (convert to Tailwind if possible)
- `.view-container` styles
- Custom utility classes that are still needed

**Remove:**

- Bootstrap-specific classes
- References to Bootstrap variables
- Bootstrap mixins

**Convert to Tailwind:**
Many custom classes can be replaced with Tailwind utilities:

```scss
// Before
.title {
  font-size: 1.25em;
  margin: 1px 10px 1px 10px;
}

// After - Use Tailwind classes directly in components
// className="text-xl mx-2.5 my-0.5"
```

#### 1.2 Remove Bootstrap Variables File

**File**: `src/main/webapp/app/_bootstrap-variables.scss`

Either:

- Delete the file (if not needed)
- Or repurpose for Tailwind theme customization

---

### Step 2: Theme Customization

#### 2.1 Review Current Theme

Check what colors and styling the current Bootswatch "Brite" theme uses.

#### 2.2 Update tailwind.config.js

**File**: `tailwind.config.js`

Customize theme to match existing design (if needed):

```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        // Add custom colors to match existing theme
        primary: {
          DEFAULT: '#533f03', // From app.scss
          // ... other shades
        },
      },
      // Add custom spacing, fonts, etc.
    },
  },
};
```

#### 2.3 Update components.json

**File**: `components.json`

If you want to customize shadcn component styling:

```json
{
  "style": "default",
  "rsc": false,
  "tsx": true,
  "tailwind": {
    "config": "tailwind.config.js",
    "css": "src/main/webapp/app/app.scss",
    "baseColor": "slate",
    "cssVariables": true
  }
}
```

---

### Step 3: Component-Specific Styling Review

#### 3.1 Review Each Migrated Component

Go through each migrated file and check:

- ✅ Hover states work correctly
- ✅ Focus states are visible
- ✅ Transitions are smooth
- ✅ Spacing is consistent
- ✅ Colors match design
- ✅ Responsive breakpoints work

#### 3.2 Common Styling Issues to Fix

**Issue: Button spacing**

```tsx
// Add proper spacing
<Button className="mr-2">
```

**Issue: Table row hover**

```tsx
<TableRow className="hover:bg-muted/50">
```

**Issue: Badge colors**

```tsx
// Use appropriate variants or custom classes
<Badge variant="destructive" className="bg-red-500">
```

---

### Step 4: Responsive Design Verification

#### 4.1 Test All Breakpoints

Test each page at:

- Mobile (320px - 640px)
- Tablet (641px - 1024px)
- Desktop (1025px+)

#### 4.2 Fix Responsive Issues

Common fixes:

**Tables:**

```tsx
<div className="overflow-x-auto">
  <Table>{/* ... */}</Table>
</div>
```

**Grid Layouts:**

```tsx
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
```

**Navigation:**

- Ensure mobile menu works
- Desktop navigation displays correctly
- Hamburger menu appears at correct breakpoint

---

### Step 5: Remove Bootstrap Dependencies

#### 5.1 Final Check

Ensure all Bootstrap/reactstrap components are removed:

```bash
# Search for any remaining reactstrap imports
grep -r "from 'reactstrap'" src/
grep -r "from \"reactstrap\"" src/
```

#### 5.2 Remove from package.json

**File**: `package.json`

Remove:

```json
{
  "dependencies": {
    "bootstrap": "5.3.6", // REMOVE
    "reactstrap": "9.2.3", // REMOVE
    "bootswatch": "5.3.5" // REMOVE
  }
}
```

#### 5.3 Clean Install

```bash
npm install
```

#### 5.4 Remove Bootstrap from app.scss (if still there)

Ensure no Bootstrap imports remain in `app.scss`.

---

### Step 6: Optimize Tailwind CSS

#### 6.1 Verify Purge Configuration

**File**: `tailwind.config.js`

Ensure `content` paths are correct:

```javascript
content: [
  './src/main/webapp/**/*.{js,jsx,ts,tsx}',
  './src/main/webapp/index.html',
],
```

#### 6.2 Build and Check Bundle Size

```bash
npm run webapp:build:prod
```

Check that:

- Bundle size is reasonable
- Tailwind CSS is purged correctly
- No unused styles are included

---

### Step 7: Custom Utility Classes

#### 7.1 Create Custom Utilities (if needed)

**File**: `tailwind.config.js`

Add custom utilities:

```javascript
module.exports = {
  theme: {
    extend: {
      // ... existing extensions
    },
  },
  plugins: [
    function ({ addUtilities }) {
      addUtilities({
        '.break': {
          'white-space': 'normal',
          'word-break': 'break-all',
        },
        '.break-word': {
          'white-space': 'normal',
          'word-break': 'keep-all',
        },
      });
    },
  ],
};
```

---

### Step 8: Final Visual Polish

#### 8.1 Consistency Check

- ✅ All buttons use consistent variants
- ✅ All badges use consistent variants
- ✅ All tables have consistent styling
- ✅ All alerts use consistent styling
- ✅ Spacing is consistent throughout
- ✅ Colors are consistent

#### 8.2 Accessibility Check

- ✅ Focus states are visible
- ✅ Color contrast meets WCAG standards
- ✅ Interactive elements are keyboard accessible
- ✅ ARIA labels are present where needed

---

## Deliverables Checklist

- [ ] Global styles reviewed and updated
- [ ] Bootstrap variables file removed/repurposed
- [ ] Theme customized (if needed)
- [ ] All components styled consistently
- [ ] Responsive design verified
- [ ] Bootstrap dependencies removed
- [ ] Tailwind CSS optimized
- [ ] Custom utilities added (if needed)
- [ ] Visual polish complete
- [ ] Accessibility verified

---

## Styling Reference Guide

### Color Mapping

| Bootstrap/reactstrap | shadcn Variant          | Tailwind Class  |
| -------------------- | ----------------------- | --------------- |
| `color="primary"`    | `variant="default"`     | `bg-primary`    |
| `color="secondary"`  | `variant="secondary"`   | `bg-secondary`  |
| `color="success"`    | `variant="default"`     | `bg-green-500`  |
| `color="danger"`     | `variant="destructive"` | `bg-red-500`    |
| `color="warning"`    | `variant="secondary"`   | `bg-yellow-500` |
| `color="info"`       | `variant="outline"`     | `bg-blue-500`   |

### Spacing

| Bootstrap | Tailwind |
| --------- | -------- |
| `p-1`     | `p-1`    |
| `p-2`     | `p-2`    |
| `m-3`     | `m-3`    |
| `mb-4`    | `mb-4`   |

### Layout

| Bootstrap                 | Tailwind          |
| ------------------------- | ----------------- |
| `d-flex`                  | `flex`            |
| `justify-content-between` | `justify-between` |
| `align-items-center`      | `items-center`    |
| `text-center`             | `text-center`     |

---

## Troubleshooting

### Issue: Styles not applying

**Solution**:

1. Check Tailwind is processing correctly
2. Verify classes are not being purged
3. Check for CSS specificity issues

### Issue: Colors don't match

**Solution**:

1. Update `tailwind.config.js` with custom colors
2. Use CSS variables for theme colors
3. Override component styles if needed

### Issue: Bundle size too large

**Solution**:

1. Verify Tailwind purge is working
2. Remove unused custom styles
3. Check for duplicate CSS

---

## Notes

- This process can be done incrementally
- Work alongside other processes as components are completed
- Document any custom styling decisions
- Keep styling consistent with shadcn design system

---

**Process Owner**: [Agent Name]  
**Start Date**: [Date]  
**Completion Date**: [Date]  
**Status**: ⏳ In Progress / ✅ Complete
