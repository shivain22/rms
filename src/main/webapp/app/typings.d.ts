declare const VERSION: string;
declare const SERVER_API_URL: string;
declare const DEVELOPMENT: string;
declare const I18N_HASH: string;
declare const APP_VERSION: string;
declare const APP_BUILD_VERSION: string;
declare const APP_COMMIT_HASH: string;
declare const APP_COMMIT_COUNT: string;
declare const APP_BRANCH: string;
declare const APP_BUILD_TIMESTAMP: string;

declare module '*.json' {
  const value: any;
  export default value;
}
