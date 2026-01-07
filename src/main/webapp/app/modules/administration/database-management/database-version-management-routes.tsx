import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import DatabaseVersionManagement from './database-version-management';
import DatabaseVersionManagementDetail from './database-version-management-detail';
import DatabaseVersionManagementUpdate from './database-version-management-update';

const DatabaseVersionManagementRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<DatabaseVersionManagement />} />
    <Route path="new" element={<DatabaseVersionManagementUpdate />} />
    <Route path=":id">
      <Route index element={<DatabaseVersionManagementDetail />} />
      <Route path="edit" element={<DatabaseVersionManagementUpdate />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default DatabaseVersionManagementRoutes;
