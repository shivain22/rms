export interface IDatabaseVendorVersion {
  id?: number;
  vendorId?: number;
  version?: string;
  displayName?: string;
  releaseDate?: string;
  endOfLifeDate?: string;
  releaseNotes?: string;
  isSupported?: boolean;
  isRecommended?: boolean;
  active?: boolean;
  createdDate?: string;
  lastModifiedDate?: string;
}

export const defaultValue: Readonly<IDatabaseVendorVersion> = {
  isSupported: true,
  isRecommended: false,
  active: true,
};
