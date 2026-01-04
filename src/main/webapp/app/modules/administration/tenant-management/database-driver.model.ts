export interface IDatabaseDriver {
  id?: number;
  vendorId?: number;
  versionId?: number;
  driverType?: string; // JDBC or R2DBC
  filePath?: string;
  fileName?: string;
  fileSize?: number;
  driverClassName?: string;
  md5Hash?: string;
  description?: string;
  isDefault?: boolean;
  uploadedBy?: string;
  active?: boolean;
  createdDate?: string;
  lastModifiedDate?: string;
}

export const defaultValue: Readonly<IDatabaseDriver> = {
  isDefault: false,
  active: true,
};
