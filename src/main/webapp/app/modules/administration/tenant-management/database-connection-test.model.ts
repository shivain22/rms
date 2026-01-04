export interface IDatabaseConnectionTest {
  vendorCode: string;
  versionId?: number;
  driverId?: number;
  host: string;
  port: number;
  databaseName: string;
  schemaName?: string;
  username: string;
  password: string;
}

export interface IDatabaseConnectionTestResult {
  success: boolean;
  message: string;
  errorDetails?: string;
  connectionTimeMs?: number;
}
