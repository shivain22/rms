# Migration Plan: Bootstrap/Reactstrap to shadcn/ui

## Executive Summary

This document outlines the migration plan from **Bootstrap 5.3.6 + reactstrap 9.2.3** to **shadcn/ui** for the Gateway UI and entire application UI.

### Current State

- **Bootstrap**: 5.3.6
- **reactstrap**: 9.2.3 (React wrapper for Bootstrap)
- **Bootswatch Theme**: Brite
- **React**: 18.3.1
- **TypeScript**: 5.8.3

### Target State

- **shadcn/ui**: Latest (component library built on Radix UI + Tailwind CSS)
- **Tailwind CSS**: Required dependency for shadcn
- **Radix UI**: Peer dependency (installed automatically with shadcn components)

---

## Scope Analysis

### Files Requiring Migration: **20 files**

#### Administration Module (Gateway & Related)

1. `src/main/webapp/app/modules/administration/gateway/gateway.tsx`
2. `src/main/webapp/app/modules/administration/health/health.tsx`
3. `src/main/webapp/app/modules/administration/health/health-modal.tsx`
4. `src/main/webapp/app/modules/administration/configuration/configuration.tsx`
5. `src/main/webapp/app/modules/administration/metrics/metrics.tsx`
6. `src/main/webapp/app/modules/administration/tenant-management/tenant-management.tsx`
7. `src/main/webapp/app/modules/administration/tenant-management/tenant-management-detail.tsx`
8. `src/main/webapp/app/modules/administration/tenant-management/tenant-management-update.tsx`
9. `src/main/webapp/app/modules/administration/tenant-management/tenant-management-delete-dialog.tsx`

#### Layout Components

10. `src/main/webapp/app/shared/layout/header/header.tsx`
11. `src/main/webapp/app/shared/layout/header/header-components.tsx`
12. `src/main/webapp/app/shared/layout/footer/footer.tsx`
13. `src/main/webapp/app/shared/layout/menus/menu-components.tsx`
14. `src/main/webapp/app/shared/layout/menus/account.tsx`
15. `src/main/webapp/app/shared/layout/menus/locale.tsx`
16. `src/main/webapp/app/shared/layout/menus/menu-item.tsx`

#### Core Application

17. `src/main/webapp/app/app.tsx`
18. `src/main/webapp/app/modules/home/home.tsx`
19. `src/main/webapp/app/shared/error/error-loading.tsx`
20. `src/main/webapp/app/shared/error/page-not-found.tsx`

### Components Used (Reactstrap → shadcn Mapping)

| Reactstrap Component                                       | Usage Count | shadcn/ui Equivalent                      | Notes                |
| ---------------------------------------------------------- | ----------- | ----------------------------------------- | -------------------- |
| `Badge`                                                    | 6 files     | `Badge`                                   | Direct replacement   |
| `Button`                                                   | 8 files     | `Button`                                  | Direct replacement   |
| `Table`                                                    | 5 files     | `Table`                                   | Direct replacement   |
| `Modal` + `ModalHeader` + `ModalBody` + `ModalFooter`      | 2 files     | `Dialog`                                  | Different API        |
| `Row` + `Col`                                              | 7 files     | CSS Grid/Flexbox or custom layout         | No direct equivalent |
| `Alert`                                                    | 3 files     | `Alert`                                   | Direct replacement   |
| `Card`                                                     | 1 file      | `Card`                                    | Direct replacement   |
| `Input`                                                    | 1 file      | `Input`                                   | Direct replacement   |
| `Navbar` + `Nav` + `NavbarToggler` + `Collapse`            | 1 file      | Custom navigation with `Sheet` for mobile | Significant change   |
| `NavItem` + `NavLink` + `NavbarBrand`                      | 1 file      | Custom navigation components              | Significant change   |
| `DropdownMenu` + `DropdownToggle` + `UncontrolledDropdown` | 1 file      | `DropdownMenu`                            | Different API        |
| `DropdownItem`                                             | 3 files     | `DropdownMenuItem`                        | Different API        |
| `FormText`                                                 | 1 file      | `FormDescription` or custom text          | Minor change         |

---

## Migration Complexity Assessment

### **Low Complexity** (Direct 1:1 replacements)

- ✅ Badge → Badge
- ✅ Button → Button
- ✅ Table → Table
- ✅ Alert → Alert
- ✅ Card → Card
- ✅ Input → Input

**Estimated Effort**: 2-3 hours

### **Medium Complexity** (API differences, but straightforward)

- ⚠️ Modal → Dialog (different props: `isOpen` → `open`, `toggle` → `onOpenChange`)
- ⚠️ DropdownMenu → DropdownMenu (different structure, but similar concept)
- ⚠️ FormText → FormDescription (minor text component change)

**Estimated Effort**: 4-6 hours

### **High Complexity** (Requires significant refactoring)

- 🔴 Row/Col Grid System → CSS Grid/Flexbox or custom layout utilities
- 🔴 Navbar/Nav → Custom navigation with Sheet component for mobile menu
- 🔴 Navigation components → Complete rewrite of header navigation

**Estimated Effort**: 8-12 hours

### **Total Estimated Effort**: **14-21 hours** (2-3 days for a developer)

---

## Detailed Migration Plan

### Phase 1: Setup & Infrastructure (2-3 hours)

#### 1.1 Install Dependencies

```bash
# Install Tailwind CSS and dependencies
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Install shadcn/ui CLI
npx shadcn@latest init

# Install required shadcn components (will be done incrementally)
```

#### 1.2 Configure Tailwind CSS

- Create/update `tailwind.config.js`
- Update `postcss.config.js`
- Update `app.scss` to import Tailwind directives instead of Bootstrap
- Remove Bootstrap imports from `app.scss`
- Remove `_bootstrap-variables.scss` (or repurpose for Tailwind theme)

#### 1.3 Update Build Configuration

- Ensure webpack/postcss-loader is configured for Tailwind
- Test build process

#### 1.4 Remove Bootstrap Dependencies

- Remove `bootstrap` from package.json
- Remove `reactstrap` from package.json
- Remove `bootswatch` from package.json
- Run `npm install` to clean up

**Files to Modify:**

- `package.json`
- `tailwind.config.js` (new)
- `postcss.config.js` (new or update)
- `src/main/webapp/app/app.scss`
- `src/main/webapp/app/_bootstrap-variables.scss` (delete or repurpose)

---

### Phase 2: Core Component Migration (4-5 hours)

#### 2.1 Install shadcn Base Components

```bash
npx shadcn@latest add button
npx shadcn@latest add badge
npx shadcn@latest add card
npx shadcn@latest add input
npx shadcn@latest add alert
npx shadcn@latest add table
```

#### 2.2 Create Utility Components

- Create layout utilities to replace Row/Col (or use Tailwind's grid system)
- Create navigation components to replace Navbar structure

#### 2.3 Migrate Simple Components

Start with files using only simple components:

- `error-loading.tsx` (Alert only)
- `page-not-found.tsx` (Alert only)
- `app.tsx` (Card only)

**Files to Modify:**

- `src/main/webapp/app/shared/error/error-loading.tsx`
- `src/main/webapp/app/shared/error/page-not-found.tsx`
- `src/main/webapp/app/app.tsx`
- Create: `src/main/webapp/app/shared/layout/layout-utils.tsx` (for Row/Col replacement)

---

### Phase 3: Administration Module - Simple Pages (3-4 hours)

#### 3.1 Gateway Page

- Replace `Badge`, `Button`, `Table`
- Update styling to use Tailwind classes
- Test functionality

**Files to Modify:**

- `src/main/webapp/app/modules/administration/gateway/gateway.tsx`

#### 3.2 Configuration Page

- Replace `Badge`, `Input`, `Row`, `Col`, `Table`
- Migrate filter input
- Update table layouts

**Files to Modify:**

- `src/main/webapp/app/modules/administration/configuration/configuration.tsx`

#### 3.3 Metrics Page

- Replace `Button`, `Row`, `Col`
- Update layout structure

**Files to Modify:**

- `src/main/webapp/app/modules/administration/metrics/metrics.tsx`

---

### Phase 4: Administration Module - Complex Pages (4-5 hours)

#### 4.1 Health Pages

- Replace `Badge`, `Button`, `Row`, `Col`, `Table` in `health.tsx`
- Replace `Modal` components with `Dialog` in `health-modal.tsx`
- Update modal state management (isOpen → open, toggle → onOpenChange)

**Files to Modify:**

- `src/main/webapp/app/modules/administration/health/health.tsx`
- `src/main/webapp/app/modules/administration/health/health-modal.tsx`

#### 4.2 Tenant Management Pages

- Replace all components in tenant-management.tsx
- Replace Modal in tenant-management-delete-dialog.tsx
- Update tenant-management-detail.tsx
- Update tenant-management-update.tsx (including FormText)

**Files to Modify:**

- `src/main/webapp/app/modules/administration/tenant-management/tenant-management.tsx`
- `src/main/webapp/app/modules/administration/tenant-management/tenant-management-detail.tsx`
- `src/main/webapp/app/modules/administration/tenant-management/tenant-management-update.tsx`
- `src/main/webapp/app/modules/administration/tenant-management/tenant-management-delete-dialog.tsx`

---

### Phase 5: Navigation & Layout (6-8 hours)

#### 5.1 Install Navigation Components

```bash
npx shadcn@latest add dropdown-menu
npx shadcn@latest add sheet  # For mobile menu
npx shadcn@latest add dialog  # Already done, but verify
```

#### 5.2 Migrate Header Components

- Replace `Navbar`, `Nav`, `NavbarToggler`, `Collapse` with custom navigation
- Use `Sheet` component for mobile menu
- Replace `NavItem`, `NavLink`, `NavbarBrand` with custom components
- Maintain existing functionality (menu toggle, routing)

**Files to Modify:**

- `src/main/webapp/app/shared/layout/header/header.tsx`
- `src/main/webapp/app/shared/layout/header/header-components.tsx`

#### 5.3 Migrate Menu Components

- Replace `DropdownMenu`, `DropdownToggle`, `UncontrolledDropdown`
- Replace `DropdownItem` with `DropdownMenuItem`
- Update menu-components.tsx
- Update account.tsx, locale.tsx, menu-item.tsx

**Files to Modify:**

- `src/main/webapp/app/shared/layout/menus/menu-components.tsx`
- `src/main/webapp/app/shared/layout/menus/account.tsx`
- `src/main/webapp/app/shared/layout/menus/locale.tsx`
- `src/main/webapp/app/shared/layout/menus/menu-item.tsx`

#### 5.4 Migrate Footer

- Replace `Row`, `Col` with Tailwind grid/flexbox

**Files to Modify:**

- `src/main/webapp/app/shared/layout/footer/footer.tsx`

#### 5.5 Migrate Home Page

- Replace `Alert`, `Row`, `Col`
- Update layout structure

**Files to Modify:**

- `src/main/webapp/app/modules/home/home.tsx`

---

### Phase 6: Styling & Polish (2-3 hours)

#### 6.1 Global Styles

- Update `app.scss` to remove Bootstrap-specific styles
- Add custom Tailwind utilities if needed
- Update color scheme to match existing theme (or adopt shadcn default)
- Ensure responsive breakpoints work correctly

#### 6.2 Component-Specific Styles

- Review each migrated component for styling issues
- Ensure hover states, focus states, and transitions work
- Fix any layout issues from Row/Col migration

#### 6.3 Theme Customization

- Configure shadcn theme to match existing color scheme (if needed)
- Update `tailwind.config.js` with custom colors
- Test dark mode (if applicable)

**Files to Modify:**

- `src/main/webapp/app/app.scss`
- `tailwind.config.js`
- `components.json` (shadcn config)

---

### Phase 7: Testing & Validation (3-4 hours)

#### 7.1 Functional Testing

- Test all administration pages
- Test navigation (desktop and mobile)
- Test modals/dialogs
- Test forms
- Test responsive layouts

#### 7.2 Visual Testing

- Compare before/after screenshots
- Ensure no visual regressions
- Test on different screen sizes
- Verify accessibility (keyboard navigation, screen readers)

#### 7.3 Integration Testing

- Run existing test suite
- Update tests that reference Bootstrap classes
- Add new tests for shadcn components if needed

**Files to Review:**

- All test files (`.spec.tsx`, `.test.tsx`)
- Cypress E2E tests (if any)

---

## Component Mapping Reference

### Direct Replacements

#### Badge

```tsx
// Before (reactstrap)
<Badge color="success">Active</Badge>

// After (shadcn)
<Badge variant="default">Active</Badge>
// Note: shadcn uses variants: default, secondary, destructive, outline
// May need to map: success → default, danger → destructive, warning → secondary
```

#### Button

```tsx
// Before (reactstrap)
<Button color="primary" size="sm">Click</Button>

// After (shadcn)
<Button variant="default" size="sm">Click</Button>
// Note: shadcn variants: default, destructive, outline, secondary, ghost, link
```

#### Table

```tsx
// Before (reactstrap)
<Table striped responsive>
  <thead>...</thead>
  <tbody>...</tbody>
</Table>

// After (shadcn)
<Table>
  <TableHeader>...</TableHeader>
  <TableBody>...</TableBody>
</Table>
// Note: striped/responsive handled via Tailwind classes
```

#### Alert

```tsx
// Before (reactstrap)
<Alert color="success">Message</Alert>

// After (shadcn)
<Alert>
  <AlertDescription>Message</AlertDescription>
</Alert>
// Note: Different structure, variants via className
```

#### Card

```tsx
// Before (reactstrap)
<Card className="jh-card">Content</Card>

// After (shadcn)
<Card>
  <CardContent>Content</CardContent>
</Card>
```

### Complex Replacements

#### Modal → Dialog

```tsx
// Before (reactstrap)
<Modal isOpen={showModal} toggle={handleClose}>
  <ModalHeader toggle={handleClose}>Title</ModalHeader>
  <ModalBody>Content</ModalBody>
  <ModalFooter>
    <Button onClick={handleClose}>Close</Button>
  </ModalFooter>
</Modal>

// After (shadcn)
<Dialog open={showModal} onOpenChange={setShowModal}>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>Title</DialogTitle>
    </DialogHeader>
    <DialogDescription>Content</DialogDescription>
    <DialogFooter>
      <Button onClick={handleClose}>Close</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

#### Row/Col → Tailwind Grid

```tsx
// Before (reactstrap)
<Row>
  <Col md="4">Content 1</Col>
  <Col md="8">Content 2</Col>
</Row>

// After (Tailwind)
<div className="grid grid-cols-12 gap-4">
  <div className="col-span-12 md:col-span-4">Content 1</div>
  <div className="col-span-12 md:col-span-8">Content 2</div>
</div>
```

#### Navbar → Custom Navigation

```tsx
// Before (reactstrap)
<Navbar expand="md">
  <NavbarToggler onClick={toggleMenu} />
  <Collapse isOpen={menuOpen} navbar>
    <Nav>...</Nav>
  </Collapse>
</Navbar>

// After (shadcn + custom)
<nav className="flex items-center justify-between">
  <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
    <SheetTrigger>Menu</SheetTrigger>
    <SheetContent>
      <nav>...</nav>
    </SheetContent>
  </Sheet>
</nav>
```

---

## Risk Assessment

### High Risk Areas

1. **Navigation System**: Complete rewrite required, potential for breaking changes
2. **Layout System**: Row/Col migration may cause layout shifts
3. **Modal/Dialog**: State management changes may introduce bugs
4. **Responsive Design**: Need to ensure mobile experience is maintained

### Mitigation Strategies

1. Migrate one component/page at a time
2. Keep Bootstrap in package.json until migration is complete (can run both temporarily)
3. Create comprehensive test coverage before migration
4. Use feature flags if possible to toggle between old/new UI
5. Maintain backup branches for each phase

---

## Dependencies to Add

```json
{
  "devDependencies": {
    "tailwindcss": "^3.4.0",
    "postcss": "^8.4.0",
    "autoprefixer": "^10.4.0"
  },
  "dependencies": {
    "@radix-ui/react-dialog": "^1.0.0",
    "@radix-ui/react-dropdown-menu": "^2.0.0",
    "@radix-ui/react-slot": "^1.0.0",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.1.0",
    "lucide-react": "^0.400.0",
    "tailwind-merge": "^2.2.0"
  }
}
```

## Dependencies to Remove

```json
{
  "dependencies": {
    "bootstrap": "5.3.6", // Remove
    "reactstrap": "9.2.3", // Remove
    "bootswatch": "5.3.5" // Remove
  }
}
```

---

## Post-Migration Tasks

1. **Performance Audit**

   - Check bundle size reduction
   - Verify runtime performance
   - Optimize Tailwind CSS purge configuration

2. **Documentation Update**

   - Update component usage guidelines
   - Document new styling approach
   - Update developer onboarding docs

3. **Accessibility Audit**

   - Verify ARIA attributes
   - Test with screen readers
   - Ensure keyboard navigation works

4. **Browser Compatibility**
   - Test on all supported browsers
   - Verify CSS Grid support
   - Check for any polyfills needed

---

## Estimated Timeline

| Phase                    | Duration        | Dependencies     |
| ------------------------ | --------------- | ---------------- |
| Phase 1: Setup           | 2-3 hours       | None             |
| Phase 2: Core Components | 4-5 hours       | Phase 1          |
| Phase 3: Simple Pages    | 3-4 hours       | Phase 2          |
| Phase 4: Complex Pages   | 4-5 hours       | Phase 2, Phase 3 |
| Phase 5: Navigation      | 6-8 hours       | Phase 2          |
| Phase 6: Styling         | 2-3 hours       | All previous     |
| Phase 7: Testing         | 3-4 hours       | All previous     |
| **Total**                | **24-32 hours** | **3-4 days**     |

---

## Success Criteria

✅ All Bootstrap/reactstrap components removed  
✅ All functionality preserved  
✅ No visual regressions  
✅ Responsive design maintained  
✅ Accessibility standards met  
✅ Test suite passes  
✅ Bundle size reduced or maintained  
✅ Performance maintained or improved

---

## Notes

- shadcn/ui is not a traditional npm package - components are copied into your project
- This allows for full customization of components
- Components use Tailwind CSS for styling, which is more flexible than Bootstrap
- The migration will result in a more modern, maintainable codebase
- Consider setting up Storybook or similar for component documentation post-migration

---

## Questions to Consider

1. **Theme Customization**: Do you want to maintain the current Bootswatch "Brite" theme colors, or adopt shadcn's default theme?
2. **Migration Strategy**: Big bang (all at once) or incremental (page by page)?
3. **Backward Compatibility**: Do you need to support both systems during migration?
4. **Testing**: What's your current test coverage? Will you need to update many tests?
5. **Team Training**: Does the team need training on Tailwind CSS and shadcn patterns?

---

**Document Version**: 1.0  
**Created**: Migration Planning Phase  
**Last Updated**: [Current Date]
