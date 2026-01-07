export interface IDatabase {
  id?: number;
  vendorId?: number;
  databaseCode?: string;
  displayName?: string;
  description?: string;
  defaultDriverClassName?: string;
  defaultPort?: number;
  jdbcUrlTemplate?: string;
  r2dbcUrlTemplate?: string;
  active?: boolean;
  createdDate?: string;
  lastModifiedDate?: string;
}

export const defaultValue: Readonly<IDatabase> = {
  active: true,
};
