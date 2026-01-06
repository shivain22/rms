const webpackMerge = require('webpack-merge').merge;
const BrowserSyncPlugin = require('browser-sync-webpack-plugin');
const SimpleProgressWebpackPlugin = require('simple-progress-webpack-plugin');
const WebpackNotifierPlugin = require('webpack-notifier');
const path = require('path');
const sass = require('sass');

const utils = require('./utils.js');
const commonConfig = require('./webpack.common.js');

const ENV = 'development';

// Handle unhandled promise rejections to prevent AggregateError from crashing the process
process.on('unhandledRejection', (reason, promise) => {
  if (reason && reason.name === 'AggregateError') {
    console.warn('[Webpack] BrowserSync error caught (non-fatal):', reason.message);
    if (reason.errors && Array.isArray(reason.errors)) {
      reason.errors.forEach((err, index) => {
        console.warn(`[Webpack] Error ${index + 1}:`, err.message);
      });
    }
    console.warn('[Webpack] Continuing without BrowserSync. You can access the app at http://localhost:9060');
    // Don't exit - let webpack dev server continue
  } else {
    console.error('[Webpack] Unhandled rejection:', reason);
  }
});

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
          // CRITICAL: Preserve cookies for session management
          cookieDomainRewrite: '', // Keep original domain
          cookiePathRewrite: '', // Keep original path
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
            // If no Referer, check if request is from localhost:9000 or localhost:9060 (webpack dev server)
            else if (req.headers.host && (req.headers.host.includes('localhost:9000') || req.headers.host.includes('localhost:9060'))) {
              const origin = `http://${req.headers.host.split(':')[0]}:${req.headers.host.includes(':9060') ? '9060' : '9000'}`;
              proxyReq.setHeader('Origin', origin);
              console.log('[Webpack Proxy] Set Origin based on Host header:', origin);
            }
            // Last resort: check if request came through BrowserSync (port 9000) or direct (port 9060)
            else {
              // Default to 9060 if we can't determine (webpack dev server direct access)
              const defaultOrigin =
                req.url.startsWith('/api') || req.url.startsWith('/management') ? 'http://localhost:9060' : 'http://localhost:9000';
              proxyReq.setHeader('Origin', defaultOrigin);
              console.log('[Webpack Proxy] Set Origin to default for local dev:', defaultOrigin);
            }

            // Always preserve Referer header if present
            if (req.headers.referer) {
              proxyReq.setHeader('Referer', req.headers.referer);
              console.log('[Webpack Proxy] Preserving Referer header:', req.headers.referer);
            } else {
              // Set Referer based on Host header or default to 9060 (webpack dev server direct access)
              const refererPort =
                req.headers.host && req.headers.host.includes(':9060')
                  ? '9060'
                  : req.headers.host && req.headers.host.includes(':9000')
                    ? '9000'
                    : '9060';
              proxyReq.setHeader('Referer', `http://localhost:${refererPort}/`);
              console.log('[Webpack Proxy] Set Referer to localhost:' + refererPort + ' (default for local dev)');
            }

            // Set X-Forwarded headers for production backend
            proxyReq.setHeader('X-Forwarded-Proto', 'https');
            proxyReq.setHeader('X-Forwarded-Host', 'rmsgateway.atparui.com');

            // Log cookies being sent (for debugging)
            if (req.headers.cookie) {
              console.log('[Webpack Proxy] Forwarding cookies:', req.headers.cookie.substring(0, 100) + '...');
            } else {
              console.warn('[Webpack Proxy] No cookies in request - session may not be preserved');
            }
          },
          onProxyRes: (proxyRes, req, res) => {
            console.log('[Webpack Proxy] Response:', proxyRes.statusCode, 'for', req.url);

            // Log Set-Cookie headers from backend (for debugging)
            if (proxyRes.headers['set-cookie']) {
              console.log('[Webpack Proxy] Backend set cookies:', proxyRes.headers['set-cookie']);
              // Rewrite cookie domain from rmsgateway.atparui.com to localhost for local dev
              if (Array.isArray(proxyRes.headers['set-cookie'])) {
                proxyRes.headers['set-cookie'] = proxyRes.headers['set-cookie'].map(cookie => {
                  // Remove domain restriction so cookie works on localhost
                  return cookie.replace(/;\s*[Dd]omain=[^;]+/gi, '');
                });
                console.log('[Webpack Proxy] Rewritten cookies for localhost:', proxyRes.headers['set-cookie']);
              }
            }
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
          // Use middleware to ensure all requests are forwarded (including API calls)
          middleware: [
            (req, res, next) => {
              // Log API requests for debugging
              if (
                req.url.startsWith('/api') ||
                req.url.startsWith('/management') ||
                req.url.startsWith('/oauth2') ||
                req.url.startsWith('/login')
              ) {
                console.log('[BrowserSync] Forwarding API request to webpack dev server:', req.method, req.url);
              }
              // Continue to BrowserSync's proxy handler
              next();
            },
          ],
          socket: {
            clients: {
              heartbeatTimeout: 60000,
            },
          },
          // Disable notifications and auto-open to prevent errors
          notify: false,
          open: false,
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
          // Add name to help with debugging
          name: 'browser-sync',
        },
      ),
      new WebpackNotifierPlugin({
        title: 'Rms',
        contentImage: path.join(__dirname, 'logo-jhipster.png'),
      }),
    ].filter(Boolean),
  });
