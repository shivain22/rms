# PROCESS 3: Administration Module - Simple Pages Migration

**Status**: 🔄 **Can run after Process 2 completes**  
**Estimated Time**: 3-4 hours per page (can be parallelized)  
**Dependencies**: Process 2 (Core Component Migration)  
**Can Run in Parallel**: ✅ Yes - Each page can be done by different agents

---

## Overview

This process migrates the simpler administration pages that use basic components (Badge, Button, Table, Input, Row/Col). These pages can be migrated independently by different agents.

---

## Prerequisites

- ✅ Process 1 completed (Setup)
- ✅ Process 2 completed (Core Components)
- shadcn components installed: Button, Badge, Table, Input
- Layout utilities available (Row/Col from Process 1)

---

## Pages to Migrate

This process covers 3 pages that can be worked on in parallel:

1. **Gateway Page** (Agent A)
2. **Configuration Page** (Agent B)
3. **Metrics Page** (Agent C)

---

## TASK 3A: Gateway Page Migration

**File**: `src/main/webapp/app/modules/administration/gateway/gateway.tsx`  
**Estimated Time**: 1-2 hours  
**Components to Replace**: Badge, Button, Table

---

### Step 1: Read Current Implementation

Read the current file to understand:

- Component structure
- State management
- Event handlers
- Styling

### Step 2: Update Imports

**Remove:**

```tsx
import { Badge, Button, Table } from 'reactstrap';
```

**Add:**

```tsx
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
```

### Step 3: Replace Badge Components

**Before:**

```tsx
<Badge color="success">UP</Badge>
<Badge color="danger">DOWN</Badge>
<Badge color="warning">?</Badge>
```

**After:**

```tsx
<Badge variant="default">UP</Badge>
<Badge variant="destructive">DOWN</Badge>
<Badge variant="secondary">?</Badge>
```

**In metadata function:**

```tsx
<Badge key={`${key.toString()}-containerbadge`} className="fw-normal">
  <Badge key={`${key.toString()}-badge`} variant="outline" className="fw-normal">
    {key}
  </Badge>
  {instance[key]}
</Badge>
```

### Step 4: Replace Button Component

**Before:**

```tsx
<Button onClick={gatewayRoutes} color={isFetching ? 'danger' : 'primary'} disabled={isFetching}>
```

**After:**

```tsx
<Button
  onClick={gatewayRoutes}
  variant={isFetching ? 'destructive' : 'default'}
  disabled={isFetching}
>
```

### Step 5: Replace Table Component

**Before:**

```tsx
<Table striped responsive>
  <thead>
    <tr key="header">
      <th>...</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>...</td>
    </tr>
  </tbody>
</Table>
```

**After:**

```tsx
<Table>
  <TableHeader>
    <TableRow key="header">
      <TableHead>...</TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    <TableRow>
      <TableCell>...</TableCell>
    </TableRow>
  </TableBody>
</Table>
```

**Add striped styling:**

```tsx
<Table>
  <TableHeader>
    <TableRow className="border-b">{/* ... */}</TableRow>
  </TableHeader>
  <TableBody>
    <TableRow className="border-b even:bg-muted/50">{/* ... */}</TableRow>
  </TableBody>
</Table>
```

### Step 6: Test Gateway Page

- ✅ Verify all routes display correctly
- ✅ Refresh button works
- ✅ Badges show correct status
- ✅ Tables are responsive
- ✅ No console errors

---

## TASK 3B: Configuration Page Migration

**File**: `src/main/webapp/app/modules/administration/configuration/configuration.tsx`  
**Estimated Time**: 2-3 hours  
**Components to Replace**: Badge, Input, Row, Col, Table

---

### Step 1: Read Current Implementation

Understand the filter functionality and table structures.

### Step 2: Update Imports

**Remove:**

```tsx
import { Badge, Col, Input, Row, Table } from 'reactstrap';
```

**Add:**

```tsx
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Row, Col } from '@/app/shared/layout/layout-utils'; // From Process 1
```

### Step 3: Replace Input Component

**Before:**

```tsx
<Input type="search" value={filter} onChange={changeFilter} name="search" id="search" />
```

**After:**

```tsx
<Input
  type="search"
  value={filter}
  onChange={changeFilter}
  name="search"
  id="search"
  placeholder="Filter configuration..."
  className="max-w-sm"
/>
```

### Step 4: Replace Table Components

Follow the same pattern as Gateway page, but note:

- Multiple tables in this component
- Nested Row/Col structures inside table cells

**For nested Row/Col in table cells:**

```tsx
<TableCell>
  {Object.keys(property.properties).map((propKey, index) => (
    <div key={index} className="grid grid-cols-12 gap-2 mb-2">
      <div className="col-span-12 md:col-span-4">{propKey}</div>
      <div className="col-span-12 md:col-span-8">
        <Badge className="float-end bg-secondary break">{JSON.stringify(property.properties[propKey])}</Badge>
      </div>
    </div>
  ))}
</TableCell>
```

### Step 5: Replace Badge Components

**Before:**

```tsx
<Badge className="float-end bg-secondary break">{...}</Badge>
```

**After:**

```tsx
<Badge variant="secondary" className="float-end break">{...}</Badge>
```

### Step 6: Replace Row/Col Layouts

Use the layout utilities from Process 1, or convert to Tailwind grid:

**Option 1: Use Layout Utilities**

```tsx
<Row>
  <Col md="4">...</Col>
  <Col md="8">...</Col>
</Row>
```

**Option 2: Use Tailwind Grid (Recommended)**

```tsx
<div className="grid grid-cols-12 gap-4">
  <div className="col-span-12 md:col-span-4">...</div>
  <div className="col-span-12 md:col-span-8">...</div>
</div>
```

### Step 7: Test Configuration Page

- ✅ Filter input works
- ✅ Tables display correctly
- ✅ Nested layouts work
- ✅ Responsive design maintained
- ✅ No console errors

---

## TASK 3C: Metrics Page Migration

**File**: `src/main/webapp/app/modules/administration/metrics/metrics.tsx`  
**Estimated Time**: 1-2 hours  
**Components to Replace**: Button, Row, Col

---

### Step 1: Read Current Implementation

Note: This page uses components from `react-jhipster` (JvmMemory, etc.) - these should remain unchanged.

### Step 2: Update Imports

**Remove:**

```tsx
import { Button, Col, Row } from 'reactstrap';
```

**Add:**

```tsx
import { Button } from '@/components/ui/button';
import { Row, Col } from '@/app/shared/layout/layout-utils'; // Or use Tailwind grid
```

### Step 3: Replace Button Component

**Before:**

```tsx
<Button onClick={getMetrics} color={isFetching ? 'btn btn-danger' : 'btn btn-primary'} disabled={isFetching}>
```

**After:**

```tsx
<Button
  onClick={getMetrics}
  variant={isFetching ? 'destructive' : 'default'}
  disabled={isFetching}
>
```

### Step 4: Replace Row/Col Layouts

**Before:**

```tsx
<Row>
  <Col sm="12">
    <h3>...</h3>
    <Row>
      <Col md="4">...</Col>
      <Col md="4">...</Col>
      <Col md="4">...</Col>
    </Row>
  </Col>
</Row>
```

**After (using Tailwind Grid):**

```tsx
<div className="grid grid-cols-12 gap-4">
  <div className="col-span-12">
    <h3>...</h3>
    <div className="grid grid-cols-12 gap-4 mt-4">
      <div className="col-span-12 md:col-span-4">...</div>
      <div className="col-span-12 md:col-span-4">...</div>
      <div className="col-span-12 md:col-span-4">...</div>
    </div>
  </div>
</div>
```

### Step 5: Test Metrics Page

- ✅ Refresh button works
- ✅ All metric components display
- ✅ Layout is responsive
- ✅ No console errors

---

## Common Patterns and Solutions

### Pattern 1: Striped Tables

**reactstrap:**

```tsx
<Table striped>
```

**shadcn:**

```tsx
<Table>
  <TableBody>
    <TableRow className="even:bg-muted/50">
```

### Pattern 2: Responsive Tables

**reactstrap:**

```tsx
<Table responsive>
```

**shadcn:**

```tsx
<div className="overflow-x-auto">
  <Table>{/* ... */}</Table>
</div>
```

### Pattern 3: Button with Icon

**reactstrap:**

```tsx
<Button color="primary">
  <FontAwesomeIcon icon="sync" />
  &nbsp;
  <Translate>Refresh</Translate>
</Button>
```

**shadcn:**

```tsx
<Button variant="default">
  <FontAwesomeIcon icon="sync" className="mr-2" />
  <Translate>Refresh</Translate>
</Button>
```

---

## Deliverables Checklist (Per Page)

- [ ] Imports updated
- [ ] All components replaced
- [ ] Functionality preserved
- [ ] Styling maintained/improved
- [ ] Responsive design works
- [ ] No console errors
- [ ] Tested manually

---

## Coordination Notes

- Each agent should work on a separate page
- Use feature branches: `feature/migrate-gateway`, `feature/migrate-configuration`, `feature/migrate-metrics`
- Coordinate merge order if there are shared utilities
- Test after each page migration

---

## Troubleshooting

### Issue: Table not responsive

**Solution**: Wrap table in `<div className="overflow-x-auto">`

### Issue: Row/Col layout broken

**Solution**:

1. Use Tailwind grid classes directly
2. Or verify layout utilities are imported correctly
3. Check responsive breakpoints

### Issue: Badge colors don't match

**Solution**: Adjust variant or add custom className with Tailwind colors

---

**Process Owner**: [Agent Names - one per page]  
**Start Date**: [Date]  
**Completion Date**: [Date]  
**Status**: ⏳ In Progress / ✅ Complete
