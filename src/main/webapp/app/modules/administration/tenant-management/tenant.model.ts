export interface ITenant {
  id?: number;
  tenantKey?: string;
  tenantId?: string;
  name?: string;
  subdomain?: string;
  databaseVendorCode?: string;
  databaseUrl?: string;
  databaseUsername?: string;
  databasePassword?: string;
  schemaName?: string;
  realmName?: string;
  clientId?: string;
  clientSecret?: string;
  defaultRoles?: string;
  active?: boolean;
}

export const defaultValue: Readonly<ITenant> = {
  active: true,
  databaseVendorCode: 'POSTGRESQL',
  realmName: '',
  clientId: '',
  clientSecret: '',
  defaultRoles: 'ROLE_ADMIN,ROLE_MANAGER,ROLE_SUPERVISOR,ROLE_WAITER,ROLE_CHEF,ROLE_CASHIER,ROLE_CUSTOMER,ROLE_ANONYMOUS',
};
