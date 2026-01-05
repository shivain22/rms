const { ModuleFederationPlugin } = require('@module-federation/enhanced/webpack');

const packageJson = require('../package.json');
// Microfrontend api, should match across gateway and microservices.
const apiVersion = '0.0.1';

const sharedDefaults = { singleton: true, strictVersion: true, requiredVersion: apiVersion };
const shareMappings = (...mappings) => Object.fromEntries(mappings.map(map => [map, { ...sharedDefaults, version: apiVersion }]));

const shareDependencies = ({ skipList = [] } = {}) =>
  Object.fromEntries(
    Object.entries(packageJson.dependencies)
      .filter(([dependency]) => !skipList.includes(dependency))
      .map(([dependency, version]) => {
        return [dependency, { ...sharedDefaults, version, requiredVersion: version }];
      }),
  );

module.exports = () => {
  return {
    optimization: {
      moduleIds: 'named',
      chunkIds: 'named',
      runtimeChunk: false,
    },

    plugins: [
      new ModuleFederationPlugin({
        shareScope: 'default',
        dts: false,
        manifest: false,
        shared: {
          ...shareDependencies({
            skipList: [
              'reactstrap',
              'bootstrap',
              'bootswatch',
              // Exclude utility libraries from module federation to avoid import issues
              'clsx',
              'tailwind-merge',
              'class-variance-authority',
              // Exclude Radix UI packages - they don't need to be shared
              '@radix-ui/react-dialog',
              '@radix-ui/react-dropdown-menu',
              '@radix-ui/react-slot',
              // Exclude icon library
              'lucide-react',
            ],
          }),
          ...shareMappings(
            'app/config/constants',
            'app/config/store',
            'app/shared/error/error-boundary-routes',
            'app/shared/layout/menus/menu-components',
            'app/shared/layout/menus/menu-item',
            'app/shared/reducers',
            'app/shared/reducers/locale',
            'app/shared/reducers/reducer.utils',
            'app/shared/util/date-utils',
            'app/shared/util/entity-utils',
          ),
        },
      }),
    ],
    output: {
      publicPath: 'auto',
      scriptType: 'text/javascript',
    },
  };
};
