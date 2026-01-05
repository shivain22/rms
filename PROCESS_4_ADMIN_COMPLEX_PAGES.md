# PROCESS 4: Administration Module - Complex Pages Migration

**Status**: 🔄 **Can run after Process 2 completes**  
**Estimated Time**: 4-5 hours per group (can be parallelized)  
**Dependencies**: Process 2 (Core Component Migration)  
**Can Run in Parallel**: ✅ Yes - Health pages and Tenant Management can be done separately

---

## Overview

This process migrates the more complex administration pages that include Modals/Dialogs and more complex component interactions. These can be split into two parallel tasks.

---

## Prerequisites

- ✅ Process 1 completed (Setup)
- ✅ Process 2 completed (Core Components)
- shadcn components: Button, Badge, Table, Row/Col utilities
- **NEW**: Dialog component needed

---

## Pages to Migrate

This process covers 2 groups that can be worked on in parallel:

1. **Health Pages** (Agent A) - 2 files
2. **Tenant Management Pages** (Agent B) - 4 files

---

## TASK 4A: Health Pages Migration

**Files**:

- `src/main/webapp/app/modules/administration/health/health.tsx`
- `src/main/webapp/app/modules/administration/health/health-modal.tsx`

**Estimated Time**: 2-3 hours  
**Components to Replace**: Badge, Button, Row, Col, Table, **Modal → Dialog**

---

### Step 1: Install Dialog Component

```bash
npx shadcn@latest add dialog
```

**Expected Output:**

- `src/components/ui/dialog.tsx` created
- Dialog components: Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogTrigger

---

### Step 2: Migrate health.tsx

#### 2.1 Update Imports

**Remove:**

```tsx
import { Badge, Button, Col, Row, Table } from 'reactstrap';
```

**Add:**

```tsx
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Row, Col } from '@/app/shared/layout/layout-utils'; // Or use Tailwind grid
```

#### 2.2 Replace Badge Component

**Before:**

```tsx
<Badge color={getBadgeType(data[configPropKey].status)}>{data[configPropKey].status}</Badge>
```

**After:**

```tsx
<Badge variant={data[configPropKey].status !== 'UP' ? 'destructive' : 'default'}>{data[configPropKey].status}</Badge>
```

Or update `getBadgeType` function:

```tsx
const getBadgeType = (status: string): string => {
  return status !== 'UP' ? 'destructive' : 'default';
};

// Usage:
<Badge variant={getBadgeType(data[configPropKey].status)}>{data[configPropKey].status}</Badge>;
```

#### 2.3 Replace Button Component

**Before:**

```tsx
<Button onClick={fetchSystemHealth} color={isFetching ? 'btn btn-danger' : 'btn btn-primary'} disabled={isFetching}>
```

**After:**

```tsx
<Button
  onClick={fetchSystemHealth}
  variant={isFetching ? 'destructive' : 'default'}
  disabled={isFetching}
>
```

#### 2.4 Replace Table Component

Follow the same pattern as Process 3:

```tsx
<Table>
  <TableHeader>
    <TableRow>
      <TableHead>Service Name</TableHead>
      <TableHead>Status</TableHead>
      <TableHead>Details</TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    {Object.keys(data).map((configPropKey, configPropIndex) =>
      configPropKey !== 'status' ? (
        <TableRow key={configPropIndex} className="even:bg-muted/50">
          <TableCell>{configPropKey}</TableCell>
          <TableCell>
            <Badge variant={getBadgeType(data[configPropKey].status)}>{data[configPropKey].status}</Badge>
          </TableCell>
          <TableCell>
            {data[configPropKey].details ? (
              <a onClick={getSystemHealthInfo(configPropKey, data[configPropKey])}>
                <FontAwesomeIcon icon="eye" />
              </a>
            ) : null}
          </TableCell>
        </TableRow>
      ) : null,
    )}
  </TableBody>
</Table>
```

#### 2.5 Replace Row/Col

Convert to Tailwind grid or use layout utilities.

---

### Step 3: Migrate health-modal.tsx

#### 3.1 Update Imports

**Remove:**

```tsx
import { Button, Modal, ModalBody, ModalFooter, ModalHeader, Table } from 'reactstrap';
```

**Add:**

```tsx
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
```

#### 3.2 Update Component Props

**Before:**

```tsx
const HealthModal = ({ handleClose, healthObject, showModal }) => {
```

**After:**

```tsx
interface HealthModalProps {
  handleClose: () => void;
  healthObject: any;
  showModal: boolean;
}

const HealthModal = ({ handleClose, healthObject, showModal }: HealthModalProps) => {
```

#### 3.3 Replace Modal with Dialog

**Before:**

```tsx
<Modal isOpen={showModal} modalTransition={{ timeout: 20 }} backdropTransition={{ timeout: 10 }} toggle={handleClose}>
  <ModalHeader toggle={handleClose}>{healthObject.name}</ModalHeader>
  <ModalBody>
    <Table bordered>{/* ... */}</Table>
  </ModalBody>
  <ModalFooter>
    <Button color="primary" onClick={handleClose}>
      Close
    </Button>
  </ModalFooter>
</Modal>
```

**After:**

```tsx
<Dialog open={showModal} onOpenChange={open => !open && handleClose()}>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>{healthObject.name}</DialogTitle>
    </DialogHeader>
    <DialogDescription>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Name</TableHead>
            <TableHead>Value</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {Object.keys(data).map((key, index) => (
            <TableRow key={index}>
              <TableCell>{key}</TableCell>
              <TableCell>{healthObject.name === 'diskSpace' ? formatDiskSpaceOutput(data[key]) : JSON.stringify(data[key])}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DialogDescription>
    <DialogFooter>
      <Button onClick={handleClose}>Close</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

**Key Changes:**

- `isOpen` → `open`
- `toggle` → `onOpenChange` (with logic to call handleClose when closing)
- `ModalHeader` → `DialogHeader` + `DialogTitle`
- `ModalBody` → `DialogDescription` (or just content)
- `ModalFooter` → `DialogFooter`
- Remove transition props (handled by Dialog component)

#### 3.4 Update health.tsx to Pass Correct Props

Ensure `health.tsx` passes props correctly:

```tsx
const renderModal = () => <HealthModal healthObject={healthObject} handleClose={handleClose} showModal={showModal} />;
```

---

### Step 4: Test Health Pages

- ✅ Health page displays correctly
- ✅ Refresh button works
- ✅ Badges show correct status
- ✅ Clicking eye icon opens dialog
- ✅ Dialog displays health details
- ✅ Dialog closes correctly
- ✅ No console errors

---

## TASK 4B: Tenant Management Pages Migration

**Files**:

- `src/main/webapp/app/modules/administration/tenant-management/tenant-management.tsx`
- `src/main/webapp/app/modules/administration/tenant-management/tenant-management-detail.tsx`
- `src/main/webapp/app/modules/administration/tenant-management/tenant-management-update.tsx`
- `src/main/webapp/app/modules/administration/tenant-management/tenant-management-delete-dialog.tsx`

**Estimated Time**: 3-4 hours  
**Components to Replace**: Button, Table, Badge, Row, Col, **Modal → Dialog**, FormText

---

### Step 1: Install Additional Components (if needed)

```bash
# Dialog already installed in Task 4A, but verify
npx shadcn@latest add dialog

# For forms (if using react-hook-form with shadcn)
npx shadcn@latest add form
npx shadcn@latest add label
```

---

### Step 2: Migrate tenant-management.tsx

#### 2.1 Update Imports

**Remove:**

```tsx
import { Button, Table, Badge } from 'reactstrap';
```

**Add:**

```tsx
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
```

#### 2.2 Replace All Components

Follow patterns from Process 3:

- Replace `Button` with shadcn Button
- Replace `Table` with shadcn Table
- Replace `Badge` with shadcn Badge
- Map colors to variants:
  - `color="info"` → `variant="outline"`
  - `color="primary"` → `variant="default"`
  - `color="danger"` → `variant="destructive"`
  - `color="success"` → `variant="default"`

#### 2.3 Update Button with Link

**Before:**

```tsx
<Button tag={Link} to={`/admin/tenant-management/${tenant.id}`} color="link" size="sm">
```

**After:**

```tsx
<Button asChild variant="link" size="sm">
  <Link to={`/admin/tenant-management/${tenant.id}`}>{tenant.id}</Link>
</Button>
```

**Note**: shadcn Button uses `asChild` prop with Radix Slot for composition.

---

### Step 3: Migrate tenant-management-detail.tsx

#### 3.1 Update Imports

**Remove:**

```tsx
import { Button, Row, Col, Badge } from 'reactstrap';
```

**Add:**

```tsx
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
// Use Tailwind grid or layout utilities
```

#### 3.2 Replace Components

Follow same patterns as other pages.

---

### Step 4: Migrate tenant-management-update.tsx

#### 4.1 Update Imports

**Remove:**

```tsx
import { Button, Row, Col, FormText } from 'reactstrap';
```

**Add:**

```tsx
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
// FormText can be replaced with simple <p> or <span> with className="text-sm text-muted-foreground"
```

#### 4.2 Replace FormText

**Before:**

```tsx
<FormText color="muted">Helper text</FormText>
```

**After:**

```tsx
<p className="text-sm text-muted-foreground">Helper text</p>
```

Or if using shadcn Form:

```tsx
<FormDescription>Helper text</FormDescription>
```

---

### Step 5: Migrate tenant-management-delete-dialog.tsx

#### 5.1 Update Imports

**Remove:**

```tsx
import { Modal, ModalHeader, ModalBody, ModalFooter, Button } from 'reactstrap';
```

**Add:**

```tsx
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
```

#### 5.2 Replace Modal with Dialog

**Before:**

```tsx
<Modal isOpen toggle={handleClose}>
  <ModalHeader toggle={handleClose}>
    <Translate contentKey="entity.delete.title">Confirm delete operation</Translate>
  </ModalHeader>
  <ModalBody>
    <Translate contentKey="tenantManagement.delete.question" interpolate={{ id: tenant.tenantId }}>
      Are you sure you want to delete this Tenant?
    </Translate>
  </ModalBody>
  <ModalFooter>
    <Button color="secondary" onClick={handleClose}>
      Cancel
    </Button>
    <Button color="danger" onClick={confirmDelete}>
      Delete
    </Button>
  </ModalFooter>
</Modal>
```

**After:**

```tsx
<Dialog open={loadModal} onOpenChange={open => !open && handleClose()}>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>
        <Translate contentKey="entity.delete.title">Confirm delete operation</Translate>
      </DialogTitle>
    </DialogHeader>
    <DialogDescription>
      <Translate contentKey="tenantManagement.delete.question" interpolate={{ id: tenant.tenantId }}>
        Are you sure you want to delete this Tenant?
      </Translate>
    </DialogDescription>
    <DialogFooter>
      <Button variant="secondary" onClick={handleClose}>
        <FontAwesomeIcon icon="ban" className="mr-2" />
        <Translate contentKey="entity.action.cancel">Cancel</Translate>
      </Button>
      <Button variant="destructive" onClick={confirmDelete}>
        <FontAwesomeIcon icon="trash" className="mr-2" />
        <Translate contentKey="entity.action.delete">Delete</Translate>
      </Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

**Key Changes:**

- `isOpen` → `open={loadModal}` (use your state variable)
- `toggle` → `onOpenChange` with close handler
- Update button variants

---

### Step 6: Test Tenant Management Pages

- ✅ Tenant list displays correctly
- ✅ Create/Edit/Delete buttons work
- ✅ Detail page displays correctly
- ✅ Update form works
- ✅ Delete dialog opens and closes
- ✅ Delete confirmation works
- ✅ Badges show correct status
- ✅ No console errors

---

## Common Modal → Dialog Patterns

### Pattern 1: Simple Modal

**reactstrap:**

```tsx
<Modal isOpen={show} toggle={handleToggle}>
  <ModalHeader toggle={handleToggle}>Title</ModalHeader>
  <ModalBody>Content</ModalBody>
  <ModalFooter>
    <Button onClick={handleToggle}>Close</Button>
  </ModalFooter>
</Modal>
```

**shadcn:**

```tsx
<Dialog open={show} onOpenChange={setShow}>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>Title</DialogTitle>
    </DialogHeader>
    <DialogDescription>Content</DialogDescription>
    <DialogFooter>
      <Button onClick={() => setShow(false)}>Close</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

### Pattern 2: Modal with External Close Handler

**reactstrap:**

```tsx
<Modal isOpen={show} toggle={handleClose}>
```

**shadcn:**

```tsx
<Dialog open={show} onOpenChange={(open) => !open && handleClose()}>
```

---

## Deliverables Checklist

### Health Pages:

- [ ] Dialog component installed
- [ ] health.tsx migrated
- [ ] health-modal.tsx migrated
- [ ] Modal → Dialog conversion complete
- [ ] All functionality works
- [ ] Tested

### Tenant Management Pages:

- [ ] tenant-management.tsx migrated
- [ ] tenant-management-detail.tsx migrated
- [ ] tenant-management-update.tsx migrated (including FormText)
- [ ] tenant-management-delete-dialog.tsx migrated
- [ ] Modal → Dialog conversion complete
- [ ] All functionality works
- [ ] Tested

---

## Troubleshooting

### Issue: Dialog doesn't close

**Solution**: Ensure `onOpenChange` handler properly updates state:

```tsx
onOpenChange={(open) => {
  if (!open) {
    handleClose();
  }
}}
```

### Issue: Dialog content not displaying

**Solution**: Ensure content is inside `DialogContent`, not directly in `Dialog`.

### Issue: FormText replacement looks wrong

**Solution**: Use appropriate Tailwind classes or shadcn FormDescription component.

---

**Process Owner**: [Agent Names - one per task]  
**Start Date**: [Date]  
**Completion Date**: [Date]  
**Status**: ⏳ In Progress / ✅ Complete
