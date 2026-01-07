import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import DatabaseVendorManagement from './database-vendor-management';
import DatabaseVendorManagementDetail from './database-vendor-management-detail';
import DatabaseVendorManagementUpdate from './database-vendor-management-update';

const DatabaseVendorManagementRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<DatabaseVendorManagement />} />
    <Route path="new" element={<DatabaseVendorManagementUpdate />} />
    <Route path=":id">
      <Route index element={<DatabaseVendorManagementDetail />} />
      <Route path="edit" element={<DatabaseVendorManagementUpdate />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default DatabaseVendorManagementRoutes;
