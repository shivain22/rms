export interface IDatabaseVersion {
  id?: number;
  databaseId?: number;
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

export const defaultValue: Readonly<IDatabaseVersion> = {
  isSupported: true,
  isRecommended: false,
  active: true,
};
