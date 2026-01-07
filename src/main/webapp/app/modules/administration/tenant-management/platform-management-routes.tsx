import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import PlatformManagement from './platform-management';
import PlatformManagementDetail from './platform-management-detail';
import PlatformManagementUpdate from './platform-management-update';
import PlatformManagementDeleteDialog from './platform-management-delete-dialog';

const PlatformManagementRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<PlatformManagement />} />
    <Route path="new" element={<PlatformManagementUpdate />} />
    <Route path=":id">
      <Route index element={<PlatformManagementDetail />} />
      <Route path="edit" element={<PlatformManagementUpdate />} />
      <Route path="delete" element={<PlatformManagementDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default PlatformManagementRoutes;
