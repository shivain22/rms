# PROCESS 5: Navigation and Layout Migration

**Status**: 🔄 **Can run after Process 2 completes**  
**Estimated Time**: 6-8 hours  
**Dependencies**: Process 2 (Core Component Migration)  
**Can Run in Parallel**: ⚠️ Partially - Can be split into sub-tasks

---

## Overview

This process migrates the navigation system (header, menus, footer) and home page. This is the most complex migration as it affects the entire application layout. Can be split into parallel sub-tasks.

---

## Prerequisites

- ✅ Process 1 completed (Setup)
- ✅ Process 2 completed (Core Components)
- shadcn components: Button, Badge (already installed)
- **NEW**: DropdownMenu, Sheet components needed

---

## Sub-Tasks (Can be parallelized)

1. **Header Components** (Agent A) - 2 files
2. **Menu Components** (Agent B) - 4 files
3. **Footer and Home Page** (Agent C) - 2 files

---

## TASK 5A: Header Components Migration

**Files**:

- `src/main/webapp/app/shared/layout/header/header.tsx`
- `src/main/webapp/app/shared/layout/header/header-components.tsx`

**Estimated Time**: 3-4 hours  
**Components to Replace**: Navbar, Nav, NavbarToggler, Collapse, NavItem, NavLink, NavbarBrand

---

### Step 1: Install Required Components

```bash
# Install Sheet for mobile menu
npx shadcn@latest add sheet

# Install DropdownMenu (if not already installed)
npx shadcn@latest add dropdown-menu
```

**Expected Output:**

- `src/components/ui/sheet.tsx` created
- `src/components/ui/dropdown-menu.tsx` created (if not already done)

---

### Step 2: Migrate header-components.tsx

#### 2.1 Read Current Implementation

Current file uses:

- `NavbarBrand`
- `NavItem`
- `NavLink`

#### 2.2 Update Imports

**Remove:**

```tsx
import { NavItem, NavLink, NavbarBrand } from 'reactstrap';
```

**Add:**

```tsx
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
```

#### 2.3 Replace NavbarBrand

**Before:**

```tsx
export const Brand = () => (
  <NavbarBrand tag={Link} to="/" className="brand-logo">
    <BrandIcon />
    <span className="brand-title">
      <Translate contentKey="global.title">Rms</Translate>
    </span>
    <span className="navbar-version">{VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`}</span>
  </NavbarBrand>
);
```

**After:**

```tsx
export const Brand = () => (
  <Link to="/" className="flex items-center space-x-2 brand-logo">
    <BrandIcon />
    <span className="brand-title">
      <Translate contentKey="global.title">Rms</Translate>
    </span>
    <span className="navbar-version">{VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`}</span>
  </Link>
);
```

#### 2.4 Replace NavItem and NavLink

**Before:**

```tsx
export const Home = () => (
  <NavItem>
    <NavLink tag={Link} to="/" className="d-flex align-items-center">
      <FontAwesomeIcon icon="home" />
      <span>
        <Translate contentKey="global.menu.home">Home</Translate>
      </span>
    </NavLink>
  </NavItem>
);
```

**After:**

```tsx
export const Home = () => (
  <Link to="/" className="flex items-center space-x-2 px-3 py-2 rounded-md hover:bg-accent transition-colors">
    <FontAwesomeIcon icon="home" />
    <span>
      <Translate contentKey="global.menu.home">Home</Translate>
    </span>
  </Link>
);
```

---

### Step 3: Migrate header.tsx

#### 3.1 Update Imports

**Remove:**

```tsx
import { Collapse, Nav, Navbar, NavbarToggler } from 'reactstrap';
```

**Add:**

```tsx
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Menu } from 'lucide-react'; // Or use FontAwesome icon
import { cn } from '@/lib/utils';
```

#### 3.2 Replace Navbar Structure

**Before:**

```tsx
<Navbar data-cy="navbar" dark expand="md" fixed="top" className="bg-primary">
  <NavbarToggler aria-label="Menu" onClick={toggleMenu} />
  <Brand />
  <Collapse isOpen={menuOpen} navbar>
    <Nav id="header-tabs" className="ms-auto" navbar>
      <Home />
      {props.isAuthenticated && <EntitiesMenu />}
      {props.isAuthenticated && props.isAdmin && <AdminMenu showOpenAPI={props.isOpenAPIEnabled} />}
      <LocaleMenu currentLocale={props.currentLocale} onClick={handleLocaleChange} />
      <AccountMenu isAuthenticated={props.isAuthenticated} account={props.account} />
    </Nav>
  </Collapse>
</Navbar>
```

**After:**

```tsx
<nav data-cy="navbar" className="fixed top-0 left-0 right-0 z-50 bg-primary text-primary-foreground shadow-md">
  <div className="container mx-auto px-4">
    <div className="flex items-center justify-between h-16">
      {/* Brand */}
      <Brand />

      {/* Desktop Navigation */}
      <div className="hidden md:flex items-center space-x-4 ms-auto">
        <Home />
        {props.isAuthenticated && <EntitiesMenu />}
        {props.isAuthenticated && props.isAdmin && <AdminMenu showOpenAPI={props.isOpenAPIEnabled} />}
        <LocaleMenu currentLocale={props.currentLocale} onClick={handleLocaleChange} />
        <AccountMenu isAuthenticated={props.isAuthenticated} account={props.account} />
      </div>

      {/* Mobile Menu Button */}
      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetTrigger asChild>
          <Button variant="ghost" size="icon" className="md:hidden" aria-label="Menu">
            <Menu className="h-6 w-6" />
          </Button>
        </SheetTrigger>
        <SheetContent side="right" className="w-[300px] sm:w-[400px]">
          <nav className="flex flex-col space-y-4 mt-8">
            <Home />
            {props.isAuthenticated && <EntitiesMenu />}
            {props.isAuthenticated && props.isAdmin && <AdminMenu showOpenAPI={props.isOpenAPIEnabled} />}
            <LocaleMenu currentLocale={props.currentLocale} onClick={handleLocaleChange} />
            <AccountMenu isAuthenticated={props.isAuthenticated} account={props.account} />
          </nav>
        </SheetContent>
      </Sheet>
    </div>
  </div>
</nav>
```

**Key Changes:**

- `Navbar` → Custom `<nav>` with Tailwind classes
- `NavbarToggler` → Sheet trigger button (mobile only)
- `Collapse` → Sheet component for mobile menu
- `Nav` → Flex container for desktop, column for mobile
- Responsive: Desktop shows inline, mobile shows in Sheet

#### 3.3 Update State Management

The `menuOpen` state and `toggleMenu` function should work the same way, but now controlled by Sheet's `open` and `onOpenChange` props.

---

### Step 4: Test Header

- ✅ Brand/logo displays correctly
- ✅ Desktop navigation works
- ✅ Mobile menu button appears on small screens
- ✅ Mobile menu opens/closes correctly
- ✅ All menu items work
- ✅ Navigation links work
- ✅ Responsive design works
- ✅ No console errors

---

## TASK 5B: Menu Components Migration

**Files**:

- `src/main/webapp/app/shared/layout/menus/menu-components.tsx`
- `src/main/webapp/app/shared/layout/menus/account.tsx`
- `src/main/webapp/app/shared/layout/menus/locale.tsx`
- `src/main/webapp/app/shared/layout/menus/menu-item.tsx`

**Estimated Time**: 2-3 hours  
**Components to Replace**: DropdownMenu, DropdownToggle, UncontrolledDropdown, DropdownItem

---

### Step 1: Update menu-components.tsx

#### 1.1 Update Imports

**Remove:**

```tsx
import { DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';
```

**Add:**

```tsx
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { Button } from '@/components/ui/button';
```

#### 1.2 Replace NavDropdown Component

**Before:**

```tsx
export const NavDropdown = props => (
  <UncontrolledDropdown nav inNavbar id={props.id} data-cy={props['data-cy']}>
    <DropdownToggle nav caret className="d-flex align-items-center">
      <FontAwesomeIcon icon={props.icon} />
      <span>{props.name}</span>
    </DropdownToggle>
    <DropdownMenu end style={props.style}>
      {props.children}
    </DropdownMenu>
  </UncontrolledDropdown>
);
```

**After:**

```tsx
export const NavDropdown = (props: {
  id?: string;
  'data-cy'?: string;
  icon: string;
  name: string;
  children: React.ReactNode;
  style?: React.CSSProperties;
}) => (
  <DropdownMenu>
    <DropdownMenuTrigger asChild>
      <Button variant="ghost" className="flex items-center space-x-2" id={props.id} data-cy={props['data-cy']}>
        <FontAwesomeIcon icon={props.icon} />
        <span>{props.name}</span>
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" style={props.style}>
      {props.children}
    </DropdownMenuContent>
  </DropdownMenu>
);
```

---

### Step 2: Update account.tsx, locale.tsx, menu-item.tsx

#### 2.1 Update Imports

**Remove:**

```tsx
import { DropdownItem } from 'reactstrap';
```

**Add:**

```tsx
import { DropdownMenuItem } from '@/components/ui/dropdown-menu';
```

#### 2.2 Replace DropdownItem

**Before:**

```tsx
<DropdownItem tag={Link} to="/path">
  Content
</DropdownItem>
```

**After:**

```tsx
<DropdownMenuItem asChild>
  <Link to="/path">Content</Link>
</DropdownMenuItem>
```

**Or if it's a button:**

```tsx
<DropdownMenuItem onClick={handleClick}>Content</DropdownMenuItem>
```

#### 2.3 Example: account.tsx

**Before:**

```tsx
<DropdownItem tag={Link} to="/account/settings">
  <FontAwesomeIcon icon="wrench" /> Settings
</DropdownItem>
```

**After:**

```tsx
<DropdownMenuItem asChild>
  <Link to="/account/settings" className="flex items-center space-x-2">
    <FontAwesomeIcon icon="wrench" />
    <span>Settings</span>
  </Link>
</DropdownMenuItem>
```

---

### Step 3: Test Menu Components

- ✅ Dropdown menus open/close correctly
- ✅ Menu items are clickable
- ✅ Links navigate correctly
- ✅ Icons display correctly
- ✅ Styling looks good
- ✅ No console errors

---

## TASK 5C: Footer and Home Page Migration

**Files**:

- `src/main/webapp/app/shared/layout/footer/footer.tsx`
- `src/main/webapp/app/modules/home/home.tsx`

**Estimated Time**: 1-2 hours  
**Components to Replace**: Row, Col, Alert

---

### Step 1: Migrate footer.tsx

#### 1.1 Update Imports

**Remove:**

```tsx
import { Col, Row } from 'reactstrap';
```

**Add:**

```tsx
// Use Tailwind grid or layout utilities
import { Row, Col } from '@/app/shared/layout/layout-utils';
```

#### 1.2 Replace Row/Col

**Before:**

```tsx
<Row>
  <Col md="12">Footer content</Col>
</Row>
```

**After (using Tailwind):**

```tsx
<div className="grid grid-cols-12">
  <div className="col-span-12">Footer content</div>
</div>
```

Or use layout utilities if preferred.

---

### Step 2: Migrate home.tsx

#### 2.1 Update Imports

**Remove:**

```tsx
import { Alert, Col, Row } from 'reactstrap';
```

**Add:**

```tsx
import { Alert, AlertDescription } from '@/components/ui/alert';
// Use Tailwind grid or layout utilities
```

#### 2.2 Replace Alert Components

**Before:**

```tsx
<Alert color="success">
  <Translate contentKey="home.logged.message" interpolate={{ username: account.login }}>
    You are logged in as user {account.login}.
  </Translate>
</Alert>
```

**After:**

```tsx
<Alert>
  <AlertDescription>
    <Translate contentKey="home.logged.message" interpolate={{ username: account.login }}>
      You are logged in as user {account.login}.
    </Translate>
  </AlertDescription>
</Alert>
```

**For warning alert:**

```tsx
<Alert variant="default">
  <AlertDescription>{/* Content */}</AlertDescription>
</Alert>
```

#### 2.3 Replace Row/Col Layouts

Convert all Row/Col to Tailwind grid:

```tsx
<div className="grid grid-cols-12 gap-4">
  <div className="col-span-12 md:col-span-3">
    <span className="hipster rounded" />
  </div>
  <div className="col-span-12 md:col-span-9">{/* Content */}</div>
</div>
```

---

### Step 3: Test Footer and Home

- ✅ Footer displays correctly
- ✅ Home page displays correctly
- ✅ Alerts show correctly
- ✅ Layout is responsive
- ✅ No console errors

---

## Common Navigation Patterns

### Pattern 1: Active Link Styling

**reactstrap:**

```tsx
<NavLink active>Active</NavLink>
```

**shadcn/custom:**

```tsx
<Link to="/path" className={cn('px-3 py-2 rounded-md', location.pathname === '/path' && 'bg-accent')}>
  Active
</Link>
```

### Pattern 2: Dropdown with Icon

**reactstrap:**

```tsx
<UncontrolledDropdown>
  <DropdownToggle>
    <FontAwesomeIcon icon="user" /> Menu
  </DropdownToggle>
  <DropdownMenu>
    <DropdownItem>Item 1</DropdownItem>
  </DropdownMenu>
</UncontrolledDropdown>
```

**shadcn:**

```tsx
<DropdownMenu>
  <DropdownMenuTrigger asChild>
    <Button variant="ghost">
      <FontAwesomeIcon icon="user" className="mr-2" />
      Menu
    </Button>
  </DropdownMenuTrigger>
  <DropdownMenuContent>
    <DropdownMenuItem>Item 1</DropdownMenuItem>
  </DropdownMenuContent>
</DropdownMenu>
```

---

## Deliverables Checklist

### Header:

- [ ] Sheet component installed
- [ ] header-components.tsx migrated
- [ ] header.tsx migrated
- [ ] Desktop navigation works
- [ ] Mobile menu works
- [ ] Responsive design works
- [ ] Tested

### Menu Components:

- [ ] DropdownMenu component installed
- [ ] menu-components.tsx migrated
- [ ] account.tsx migrated
- [ ] locale.tsx migrated
- [ ] menu-item.tsx migrated
- [ ] All dropdowns work
- [ ] Tested

### Footer and Home:

- [ ] footer.tsx migrated
- [ ] home.tsx migrated
- [ ] Alerts work correctly
- [ ] Layout responsive
- [ ] Tested

---

## Coordination Notes

- Header and Menu components are closely related - coordinate between Agents A and B
- Test navigation thoroughly as it affects entire app
- Ensure mobile menu works on all screen sizes
- Verify all routes still work after migration

---

## Troubleshooting

### Issue: Mobile menu doesn't open

**Solution**:

1. Verify Sheet component is installed
2. Check `open` and `onOpenChange` props are set correctly
3. Ensure state management is working

### Issue: Dropdown menu positioning wrong

**Solution**: Use `align` prop on `DropdownMenuContent`:

```tsx
<DropdownMenuContent align="end">
```

### Issue: Active link styling not working

**Solution**: Use `useLocation` from react-router-dom to detect active route:

```tsx
import { useLocation } from 'react-router-dom';

const location = useLocation();
const isActive = location.pathname === '/path';
```

---

**Process Owner**: [Agent Names - one per task]  
**Start Date**: [Date]  
**Completion Date**: [Date]  
**Status**: ⏳ In Progress / ✅ Complete
