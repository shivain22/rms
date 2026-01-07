import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import DatabaseManagement from './database-management';
import DatabaseManagementDetail from './database-management-detail';
import DatabaseManagementUpdate from './database-management-update';

const DatabaseManagementRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<DatabaseManagement />} />
    <Route path="new" element={<DatabaseManagementUpdate />} />
    <Route path=":id">
      <Route index element={<DatabaseManagementDetail />} />
      <Route path="edit" element={<DatabaseManagementUpdate />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default DatabaseManagementRoutes;
