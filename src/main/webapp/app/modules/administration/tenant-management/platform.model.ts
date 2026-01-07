export interface IPlatform {
  id?: number;
  name?: string;
  description?: string;
  active?: boolean;
  createdDate?: string;
  lastModifiedDate?: string;
}

export const defaultValue: Readonly<IPlatform> = {
  active: true,
};
