export interface ITenant {
  id?: number;
  tenantKey?: string;
  tenantId?: string;
  name?: string;
  subdomain?: string;
  databaseVendorCode?: string;
  databaseVendorVersionId?: number;
  databaseDriverId?: number;
  databaseOwnershipType?: string; // PLATFORM or BYOD (Bring Your Own Database)
  databaseProvisioningMode?: string; // AUTO_CREATE or USE_EXISTING
  databaseHost?: string;
  databasePort?: number;
  databaseName?: string;
  databaseUrl?: string;
  databaseUsername?: string;
  databasePassword?: string;
  schemaName?: string;
  realmName?: string;
  clientId?: string;
  clientSecret?: string;
  defaultRoles?: string;
  platformId?: number;
  isTemplate?: boolean;
  active?: boolean;
}

export const defaultValue: Readonly<ITenant> = {
  active: true,
  databaseOwnershipType: 'PLATFORM',
  databaseVendorCode: 'POSTGRESQL',
  databaseProvisioningMode: 'AUTO_CREATE',
  realmName: '',
  clientId: '',
  clientSecret: '',
  defaultRoles: 'ROLE_ADMIN,ROLE_MANAGER,ROLE_SUPERVISOR,ROLE_WAITER,ROLE_CHEF,ROLE_CASHIER,ROLE_CUSTOMER,ROLE_ANONYMOUS',
  isTemplate: false,
};
