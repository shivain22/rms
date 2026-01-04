import { ReducersMapObject } from '@reduxjs/toolkit';
import { loadingBarReducer as loadingBar } from 'react-redux-loading-bar';

import administration from 'app/modules/administration/administration.reducer';
import locale from './locale';
import authentication from './authentication';
import applicationProfile from './application-profile';

import userManagement from './user-management';
import tenantManagement from 'app/modules/administration/tenant-management/tenant-management.reducer';
import databaseVendor from 'app/modules/administration/tenant-management/database-vendor.reducer';
/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

const rootReducer: ReducersMapObject = {
  authentication,
  locale,
  applicationProfile,
  administration,
  userManagement,
  tenantManagement,
  databaseVendor,
  loadingBar,
  /* jhipster-needle-add-reducer-combine - JHipster will add reducer here */
};

export default rootReducer;
