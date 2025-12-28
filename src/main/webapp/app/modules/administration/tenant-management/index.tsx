import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import TenantManagement from './tenant-management';
import TenantManagementDetail from './tenant-management-detail';
import TenantManagementUpdate from './tenant-management-update';
import TenantManagementDeleteDialog from './tenant-management-delete-dialog';

const TenantManagementRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<TenantManagement />} />
    <Route path="new" element={<TenantManagementUpdate />} />
    <Route path=":id">
      <Route index element={<TenantManagementDetail />} />
      <Route path="edit" element={<TenantManagementUpdate />} />
      <Route path="delete" element={<TenantManagementDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default TenantManagementRoutes;
