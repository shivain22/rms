module.exports = {
  // APP_VERSION is passed as an environment variable from the Gradle / Maven build tasks.
  VERSION: process.env.APP_VERSION || 'DEV',
  APP_VERSION: process.env.APP_VERSION || 'DEV',
  APP_BUILD_VERSION: process.env.APP_BUILD_VERSION || 'DEV',
  APP_COMMIT_HASH: process.env.APP_COMMIT_HASH || 'unknown',
  APP_COMMIT_COUNT: process.env.APP_COMMIT_COUNT || '0',
  APP_BRANCH: process.env.APP_BRANCH || 'unknown',
  APP_BUILD_TIMESTAMP: process.env.APP_BUILD_TIMESTAMP || 'unknown',
  // The root URL for API calls, ending with a '/' - for example: `"https://www.jhipster.tech:8081/myservice/"`.
  // If this URL is left empty (""), then it will be relative to the current context.
  // If you use an API server, in `prod` mode, you will need to enable CORS
  // (see the `jhipster.cors` common JHipster property in the `application-*.yml` configurations)
  SERVER_API_URL: '',
};
