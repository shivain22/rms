import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import DriverJarManagement from './driver-jar-management';
import DriverJarManagementDetail from './driver-jar-management-detail';
import DriverJarManagementUpdate from './driver-jar-management-update';

const DriverJarManagementRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<DriverJarManagement />} />
    <Route path="new" element={<DriverJarManagementUpdate />} />
    <Route path=":id">
      <Route index element={<DriverJarManagementDetail />} />
      <Route path="edit" element={<DriverJarManagementUpdate />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default DriverJarManagementRoutes;
