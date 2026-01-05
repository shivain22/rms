# PROCESS 2: Core Component Migration

**Status**: 🔄 **Can run after Process 1 completes**  
**Estimated Time**: 4-5 hours  
**Dependencies**: Process 1 (Setup and Infrastructure)  
**Can Run in Parallel**: ⚠️ Partially - Sub-tasks can be parallelized

---

## Overview

This process installs the base shadcn/ui components and migrates the simplest files that use only basic components (Alert, Card). This establishes the foundation for all other migrations.

---

## Prerequisites

- ✅ Process 1 must be completed
- shadcn/ui initialized
- Tailwind CSS configured and working

---

## Step-by-Step Instructions

### Step 1: Install shadcn Base Components

Run these commands to install the core shadcn components:

```bash
# Navigate to project root
cd c:\Users\shiva\eclipse-workspace\rms

# Install base components
npx shadcn@latest add button
npx shadcn@latest add badge
npx shadcn@latest add card
npx shadcn@latest add input
npx shadcn@latest add alert
npx shadcn@latest add table
```

**Expected Output:**

- Components installed in `src/components/ui/` (or path specified in `components.json`)
- Each component includes:
  - Component file (e.g., `button.tsx`)
  - TypeScript types
  - Tailwind styling

**Verify Installation:**
Check that these files exist:

- `src/components/ui/button.tsx`
- `src/components/ui/badge.tsx`
- `src/components/ui/card.tsx`
- `src/components/ui/input.tsx`
- `src/components/ui/alert.tsx`
- `src/components/ui/table.tsx`

---

### Step 2: Create Component Mapping Helper

Create a utility file to help with component mapping:

**File**: `src/main/webapp/app/shared/utils/component-mapping.ts`

```typescript
/**
 * Helper functions for mapping reactstrap props to shadcn variants
 */

export const mapBadgeColor = (color: string): string => {
  const colorMap: Record<string, string> = {
    success: 'default',
    danger: 'destructive',
    warning: 'secondary',
    info: 'outline',
    primary: 'default',
    secondary: 'secondary',
  };
  return colorMap[color] || 'default';
};

export const mapButtonColor = (color: string): string => {
  const colorMap: Record<string, string> = {
    primary: 'default',
    secondary: 'secondary',
    success: 'default',
    danger: 'destructive',
    warning: 'secondary',
    info: 'outline',
    link: 'link',
  };
  return colorMap[color] || 'default';
};
```

---

### Step 3: Migrate Error Loading Component

**File**: `src/main/webapp/app/shared/error/error-loading.tsx`

#### 3.1 Read Current File

Current content uses:

```tsx
import { Alert } from 'reactstrap';
```

#### 3.2 Replace with shadcn

```tsx
import React from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';

const ErrorLoading = () => {
  return (
    <div>
      <Alert variant="destructive">
        <AlertDescription>Error loading component</AlertDescription>
      </Alert>
    </div>
  );
};

export default ErrorLoading;
```

**Note**: Adjust import path based on your `components.json` configuration.

---

### Step 4: Migrate Page Not Found Component

**File**: `src/main/webapp/app/shared/error/page-not-found.tsx`

#### 4.1 Read Current File

Check the current implementation.

#### 4.2 Replace with shadcn

```tsx
import React from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Translate } from 'react-jhipster';

const PageNotFound = () => {
  return (
    <div>
      <Alert>
        <AlertDescription>
          <Translate contentKey="error.notfound">The page does not exist.</Translate>
        </AlertDescription>
      </Alert>
    </div>
  );
};

export default PageNotFound;
```

**Note**: Preserve any existing Translate components and styling.

---

### Step 5: Migrate App Component (Card)

**File**: `src/main/webapp/app/app.tsx`

#### 5.1 Read Current File

Current content uses:

```tsx
import { Card } from 'reactstrap';
```

#### 5.2 Replace with shadcn

```tsx
// ... existing imports ...
import { Card, CardContent } from '@/components/ui/card';

// ... existing code ...

// Replace:
<Card className="jh-card">
  <ErrorBoundary>
    <AppRoutes />
  </ErrorBoundary>
</Card>

// With:
<Card className="jh-card">
  <CardContent>
    <ErrorBoundary>
      <AppRoutes />
    </ErrorBoundary>
  </CardContent>
</Card>
```

**Important**:

- Keep the `className="jh-card"` if it has custom styles
- Wrap content in `CardContent` component
- Preserve all existing functionality

---

### Step 6: Test Migrated Components

#### 6.1 Start Development Server

```bash
npm run start
```

#### 6.2 Verify Each Component

- ✅ Error Loading page displays correctly
- ✅ 404 page displays correctly
- ✅ Main app card displays correctly
- ✅ No console errors
- ✅ Styling looks correct (may need adjustments)

---

### Step 7: Update Import Paths (if needed)

If your `components.json` uses a different import alias, update all imports:

**Check `components.json` for:**

```json
{
  "aliases": {
    "components": "@/components",
    "utils": "@/lib/utils"
  }
}
```

**If different, update imports in:**

- `error-loading.tsx`
- `page-not-found.tsx`
- `app.tsx`

---

## Component Reference Guide

### Alert Component

**reactstrap:**

```tsx
<Alert color="success">Message</Alert>
```

**shadcn:**

```tsx
<Alert>
  <AlertDescription>Message</AlertDescription>
</Alert>
```

**Variants:**

- `default` - Standard alert
- `destructive` - Error/danger (replaces `color="danger"`)

**With custom variant:**

```tsx
<Alert variant="destructive">
  <AlertDescription>Error message</AlertDescription>
</Alert>
```

---

### Card Component

**reactstrap:**

```tsx
<Card className="custom-class">Content</Card>
```

**shadcn:**

```tsx
<Card className="custom-class">
  <CardHeader>
    <CardTitle>Title (optional)</CardTitle>
  </CardHeader>
  <CardContent>Content</CardContent>
  <CardFooter>Footer (optional)</CardFooter>
</Card>
```

**Minimal:**

```tsx
<Card>
  <CardContent>Content</CardContent>
</Card>
```

---

## Deliverables Checklist

- [ ] All base shadcn components installed
- [ ] Component mapping helper created
- [ ] `error-loading.tsx` migrated
- [ ] `page-not-found.tsx` migrated
- [ ] `app.tsx` migrated
- [ ] All components tested and working
- [ ] No console errors
- [ ] Import paths verified

---

## Handoff Information

Once this process is complete:

- **Process 3** (Simple Admin Pages) can begin
- **Process 4** (Complex Admin Pages) can begin
- **Process 5** (Navigation) can begin

All processes can work in parallel after this.

---

## Troubleshooting

### Issue: Components not found

**Solution**:

1. Verify components were installed in correct directory
2. Check `components.json` for correct paths
3. Verify import aliases in `tsconfig.json`

### Issue: Styling looks wrong

**Solution**:

1. Ensure Tailwind is processing correctly
2. Check that `@tailwind` directives are in `app.scss`
3. Verify component classes are not being overridden

### Issue: TypeScript errors

**Solution**:

1. Check import paths match your `components.json` config
2. Verify TypeScript can resolve the paths
3. Check `tsconfig.json` paths configuration

---

## Notes

- Keep reactstrap imports until all components are migrated
- Test each component individually
- Document any custom styling needed
- These are the simplest migrations - use as reference for others

---

**Process Owner**: [Agent Name]  
**Start Date**: [Date]  
**Completion Date**: [Date]  
**Status**: ⏳ In Progress / ✅ Complete
