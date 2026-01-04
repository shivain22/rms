export interface IDatabaseVendor {
  id?: number;
  vendorCode?: string;
  displayName?: string;
  defaultPort?: number;
  driverKey?: string;
  description?: string;
  jdbcUrlTemplate?: string;
  r2dbcUrlTemplate?: string;
  driverClassName?: string;
  active?: boolean;
}

export const defaultValue: Readonly<IDatabaseVendor> = {
  active: true,
};
