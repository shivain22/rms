export interface IDriverJar {
  id?: number;
  versionId?: number;
  driverType?: string;
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

export const defaultValue: Readonly<IDriverJar> = {
  isDefault: false,
  active: true,
};
