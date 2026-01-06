const webpackMerge = require('webpack-merge').merge;
const BrowserSyncPlugin = require('browser-sync-webpack-plugin');
const SimpleProgressWebpackPlugin = require('simple-progress-webpack-plugin');
const WebpackNotifierPlugin = require('webpack-notifier');
const path = require('path');
const sass = require('sass');

const utils = require('./utils.js');
const commonConfig = require('./webpack.common.js');

const ENV = 'development';

module.exports = async options =>
  webpackMerge(await commonConfig({ env: ENV }), {
    devtool: 'cheap-module-source-map', // https://reactjs.org/docs/cross-origin-errors.html
    mode: ENV,
    entry: ['./src/main/webapp/app/main'],
    output: {
      path: utils.root('target/classes/static/'),
      filename: '[name].[contenthash:8].js',
      chunkFilename: '[name].[chunkhash:8].chunk.js',
    },
    optimization: {
      moduleIds: 'named',
    },
    module: {
      rules: [
        {
          test: /\.(sa|sc|c)ss$/,
          use: [
            'style-loader',
            {
              loader: 'css-loader',
              options: { url: false },
            },
            {
              loader: 'postcss-loader',
            },
            {
              loader: 'sass-loader',
              options: { implementation: sass },
            },
          ],
        },
      ],
    },
    devServer: {
      hot: false,
      static: {
        directory: './target/classes/static/',
      },
      port: 9060,
      proxy: [
        {
          // Match all paths that should be proxied to production backend
          // Use function to ensure all sub-paths are matched
          context: pathname => {
            const shouldProxy =
              pathname.startsWith('/api') ||
              pathname.startsWith('/services') ||
              pathname.startsWith('/management') ||
              pathname.startsWith('/v3/api-docs') ||
              pathname.startsWith('/h2-console') ||
              pathname.startsWith('/auth') ||
              pathname.startsWith('/oauth2') ||
              pathname.startsWith('/login');
            if (shouldProxy) {
              console.log('[Webpack Proxy] Matching path for proxy:', pathname);
            }
            return shouldProxy;
          },
          target: 'https://rmsgateway.atparui.com',
          secure: false, // Set to false to bypass SSL certificate validation (for development only)
          changeOrigin: false, // Don't change origin - preserve original headers
          logLevel: 'debug',
          // Ensure proxy handles all HTTP methods (GET, POST, etc.)
          ws: false, // WebSocket not needed for OAuth2
          // Preserve Origin and Referer headers so backend can detect local frontend
          onProxyReq: (proxyReq, req, res) => {
            console.log('[Webpack Proxy] Proxying request:', req.method, req.url, 'to', 'https://rmsgateway.atparui.com');
            // CRITICAL: For navigation requests (like /oauth2/authorization/oidc),
            // Origin header is not sent by browser. We need to derive it from Referer or set it explicitly.

            // First, try to preserve existing Origin header
            if (req.headers.origin) {
              proxyReq.setHeader('Origin', req.headers.origin);
              console.log('[Webpack Proxy] Preserving Origin header:', req.headers.origin);
            }
            // If no Origin, derive from Referer header
            else if (req.headers.referer) {
              try {
                const url = new URL(req.headers.referer);
                const origin = `${url.protocol}//${url.host}`;
                proxyReq.setHeader('Origin', origin);
                console.log('[Webpack Proxy] Derived Origin from Referer:', origin);
              } catch (e) {
                console.log('[Webpack Proxy] Failed to parse Referer:', req.headers.referer);
                // Fallback: set to localhost:9000
                proxyReq.setHeader('Origin', 'http://localhost:9000');
              }
            }
            // If no Referer, check if request is from localhost:9000 (webpack dev server)
            else if (req.headers.host && req.headers.host.includes('localhost:9000')) {
              proxyReq.setHeader('Origin', 'http://localhost:9000');
              console.log('[Webpack Proxy] Set Origin to localhost:9000 based on Host header');
            }
            // Last resort: always set Origin to localhost:9000 for local dev
            else {
              proxyReq.setHeader('Origin', 'http://localhost:9000');
              console.log('[Webpack Proxy] Set Origin to localhost:9000 (default for local dev)');
            }

            // Always preserve Referer header if present
            if (req.headers.referer) {
              proxyReq.setHeader('Referer', req.headers.referer);
              console.log('[Webpack Proxy] Preserving Referer header:', req.headers.referer);
            } else {
              // Set Referer to localhost:9000 if not present (for navigation requests)
              proxyReq.setHeader('Referer', 'http://localhost:9000/');
              console.log('[Webpack Proxy] Set Referer to localhost:9000 (default for local dev)');
            }

            // Set X-Forwarded headers for production backend
            proxyReq.setHeader('X-Forwarded-Proto', 'https');
            proxyReq.setHeader('X-Forwarded-Host', 'rmsgateway.atparui.com');
          },
          onError: (err, req, res) => {
            console.error('[Webpack Proxy] Proxy error for', req.url, ':', err.message);
            if (!res.headersSent) {
              res.writeHead(500, {
                'Content-Type': 'text/plain',
              });
              res.end('Proxy error: ' + err.message);
            }
          },
          onProxyRes: (proxyRes, req, res) => {
            console.log('[Webpack Proxy] Response:', proxyRes.statusCode, 'for', req.url);
          },
        },
      ],
      historyApiFallback: true,
    },
    stats: process.env.JHI_DISABLE_WEBPACK_LOGS ? 'none' : options.stats,
    plugins: [
      process.env.JHI_DISABLE_WEBPACK_LOGS
        ? null
        : new SimpleProgressWebpackPlugin({
            format: options.stats === 'minimal' ? 'compact' : 'expanded',
          }),
      new BrowserSyncPlugin(
        {
          https: options.tls,
          host: 'localhost',
          port: 9000,
          proxy: {
            target: `http${options.tls ? 's' : ''}://localhost:${options.watch ? '8082' : '9060'}`,
            ws: true,
            proxyOptions: {
              changeOrigin: false, //pass the Host header to the backend unchanged https://github.com/Browsersync/browser-sync/issues/430
            },
          },
          socket: {
            clients: {
              heartbeatTimeout: 60000,
            },
          },
          /*
      ,ghostMode: { // uncomment this part to disable BrowserSync ghostMode; https://github.com/jhipster/generator-jhipster/issues/11116
        clicks: false,
        location: false,
        forms: false,
        scroll: false
      } */
        },
        {
          reload: false,
        },
      ),
      new WebpackNotifierPlugin({
        title: 'Rms',
        contentImage: path.join(__dirname, 'logo-jhipster.png'),
      }),
    ].filter(Boolean),
  });
