import React from 'react';
import MenuItem from 'app/shared/layout/menus/menu-item';

import { Translate, translate } from 'react-jhipster';
import { NavDropdown } from './menu-components';

const adminMenuItems = () => (
  <>
    <MenuItem icon="road" to="/admin/gateway">
      <Translate contentKey="global.menu.admin.gateway">Gateway</Translate>
    </MenuItem>
    <MenuItem icon="building" to="/admin/tenant-management">
      <Translate contentKey="global.menu.admin.tenantManagement">Tenant Management</Translate>
    </MenuItem>
    <MenuItem icon="server" to="/admin/platform-management">
      <Translate contentKey="global.menu.admin.platformManagement">Platform Management</Translate>
    </MenuItem>
    <MenuItem icon="database" to="/admin/database-vendor-management">
      <Translate contentKey="global.menu.admin.databaseVendorManagement">Database Vendors</Translate>
    </MenuItem>
    <MenuItem icon="server" to="/admin/database-management">
      <Translate contentKey="global.menu.admin.databaseManagement">Databases</Translate>
    </MenuItem>
    <MenuItem icon="tags" to="/admin/database-version-management">
      <Translate contentKey="global.menu.admin.databaseVersionManagement">Database Versions</Translate>
    </MenuItem>
    <MenuItem icon="upload" to="/admin/driver-jar-management">
      <Translate contentKey="global.menu.admin.driverJarManagement">Driver JARs</Translate>
    </MenuItem>
    <MenuItem icon="tachometer-alt" to="/admin/metrics">
      <Translate contentKey="global.menu.admin.metrics">Metrics</Translate>
    </MenuItem>
    <MenuItem icon="heart" to="/admin/health">
      <Translate contentKey="global.menu.admin.health">Health</Translate>
    </MenuItem>
    <MenuItem icon="cogs" to="/admin/configuration">
      <Translate contentKey="global.menu.admin.configuration">Configuration</Translate>
    </MenuItem>
    <MenuItem icon="tasks" to="/admin/logs">
      <Translate contentKey="global.menu.admin.logs">Logs</Translate>
    </MenuItem>
    {/* jhipster-needle-add-element-to-admin-menu - JHipster will add entities to the admin menu here */}
  </>
);

const openAPIItem = () => (
  <MenuItem icon="book" to="/admin/docs">
    <Translate contentKey="global.menu.admin.apidocs">API</Translate>
  </MenuItem>
);

export const AdminMenu = ({ showOpenAPI }) => (
  <NavDropdown icon="users-cog" name={translate('global.menu.admin.main')} id="admin-menu" data-cy="adminMenu">
    {adminMenuItems()}
    {showOpenAPI && openAPIItem()}
  </NavDropdown>
);

export default AdminMenu;
