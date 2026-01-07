import React from 'react';

import { Route } from 'react-router';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import Logs from './logs/logs';
import Health from './health/health';
import Metrics from './metrics/metrics';
import Configuration from './configuration/configuration';
import Docs from './docs/docs';
import Gateway from './gateway/gateway';
import TenantManagementRoutes from './tenant-management';
import PlatformManagementRoutes from './tenant-management/platform-management-routes';
import DatabaseVendorManagementRoutes from './database-management/database-vendor-management-routes';
import DatabaseManagementRoutes from './database-management/database-management-routes';
import DatabaseVersionManagementRoutes from './database-management/database-version-management-routes';
import DriverJarManagementRoutes from './database-management/driver-jar-management-routes';

const AdministrationRoutes = () => (
  <div>
    <ErrorBoundaryRoutes>
      <Route path="gateway" element={<Gateway />} />
      <Route path="health" element={<Health />} />
      <Route path="metrics" element={<Metrics />} />
      <Route path="configuration" element={<Configuration />} />
      <Route path="logs" element={<Logs />} />
      <Route path="docs" element={<Docs />} />
      <Route path="tenant-management/*" element={<TenantManagementRoutes />} />
      <Route path="platform-management/*" element={<PlatformManagementRoutes />} />
      <Route path="database-vendor-management/*" element={<DatabaseVendorManagementRoutes />} />
      <Route path="database-management/*" element={<DatabaseManagementRoutes />} />
      <Route path="database-version-management/*" element={<DatabaseVersionManagementRoutes />} />
      <Route path="driver-jar-management/*" element={<DriverJarManagementRoutes />} />
    </ErrorBoundaryRoutes>
  </div>
);

export default AdministrationRoutes;
