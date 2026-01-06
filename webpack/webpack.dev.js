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
  if (reason && (reason.name === 'AggregateError' || reason.constructor.name === 'AggregateError')) {
    console.warn('[Webpack] BrowserSync error caught (non-fatal):', reason.message || 'Unknown error');
    if (reason.errors && Array.isArray(reason.errors)) {
      reason.errors.forEach((err, index) => {
        console.warn(`[Webpack] Error ${index + 1}:`, err.message || err);
      });
    }
    console.warn('[Webpack] Continuing without BrowserSync. You can access the app at http://localhost:9060');
    // Don't exit - let webpack dev server continue
    return; // Prevent default behavior
  }
  // For other unhandled rejections, log but don't crash
  console.error('[Webpack] Unhandled rejection (non-fatal):', reason);
});

// Also handle uncaught exceptions to prevent crashes
process.on('uncaughtException', err => {
  if (err.name === 'AggregateError' || err.constructor.name === 'AggregateError') {
    console.warn('[Webpack] BrowserSync AggregateError caught (non-fatal):', err.message);
    console.warn('[Webpack] Continuing without BrowserSync. You can access the app at http://localhost:9060');
    // Don't exit - let webpack dev server continue
    return;
  }
  // For other exceptions, log but don't crash in development
  console.error('[Webpack] Uncaught exception (non-fatal in dev):', err);
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
      client: {
        overlay: {
          errors: true,
          warnings: false, // Disable warning overlay - only show errors
        },
      },
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
          changeOrigin: true, // CRITICAL: Change Host header to match target, otherwise backend returns 404
          logLevel: 'debug',
          // CRITICAL: Preserve cookies for session management
          cookieDomainRewrite: '', // Keep original domain
          cookiePathRewrite: '', // Keep original path
          // Ensure proxy handles all HTTP methods (GET, POST, etc.)
          ws: false, // WebSocket not needed for OAuth2
          // Preserve Origin and Referer headers so backend can detect local frontend
          onProxyReq: (proxyReq, req, res) => {
            // CRITICAL: Capture original host BEFORE webpack changes it (changeOrigin: true will change Host header)
            const originalHost = req.headers.host || 'localhost:9000';
            const originalOrigin = req.headers.origin;
            const originalReferer = req.headers.referer;

            console.log('[Webpack Proxy] Proxying request:', req.method, req.url, 'to', 'https://rmsgateway.atparui.com');
            console.log('[Webpack Proxy] Original Host:', originalHost, '| Origin:', originalOrigin, '| Referer:', originalReferer);

            // CRITICAL: For navigation requests (like /oauth2/authorization/oidc),
            // Origin header is not sent by browser. We need to derive it from Referer or set it explicitly.

            // First, try to preserve existing Origin header
            if (originalOrigin) {
              proxyReq.setHeader('Origin', originalOrigin);
              console.log('[Webpack Proxy] Preserving Origin header:', originalOrigin);
            }
            // If no Origin, derive from Referer header
            else if (originalReferer) {
              try {
                const url = new URL(originalReferer);
                const origin = `${url.protocol}//${url.host}`;
                proxyReq.setHeader('Origin', origin);
                console.log('[Webpack Proxy] Derived Origin from Referer:', origin);
              } catch (e) {
                console.log('[Webpack Proxy] Failed to parse Referer:', originalReferer);
                // Fallback: set to localhost:9000
                proxyReq.setHeader('Origin', 'http://localhost:9000');
              }
            }
            // If no Referer, check if request is from localhost:9000 or localhost:9060 (webpack dev server)
            else if (originalHost && (originalHost.includes('localhost:9000') || originalHost.includes('localhost:9060'))) {
              const origin = `http://${originalHost.split(':')[0]}:${originalHost.includes(':9060') ? '9060' : '9000'}`;
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
            if (originalReferer) {
              proxyReq.setHeader('Referer', originalReferer);
              console.log('[Webpack Proxy] Preserving Referer header:', originalReferer);
            } else {
              // Set Referer based on Host header or default to 9060 (webpack dev server direct access)
              const refererPort =
                originalHost && originalHost.includes(':9060') ? '9060' : originalHost && originalHost.includes(':9000') ? '9000' : '9060';
              proxyReq.setHeader('Referer', `http://localhost:${refererPort}/`);
              console.log('[Webpack Proxy] Set Referer to localhost:' + refererPort + ' (default for local dev)');
            }

            // Set X-Forwarded headers for production backend
            // X-Forwarded-Proto should be 'https' since backend expects HTTPS
            proxyReq.setHeader('X-Forwarded-Proto', 'https');
            // X-Forwarded-Host should be the original client host (localhost:9000 or localhost:9060)
            // This is critical for OAuth2 redirect URI generation - backend uses this to determine redirect URI
            proxyReq.setHeader('X-Forwarded-Host', originalHost);
            console.log('[Webpack Proxy] Set X-Forwarded-Host to original client host:', originalHost);
            // Also set X-Forwarded-For for proper proxy detection
            const forwardedFor = req.headers['x-forwarded-for'] || req.socket.remoteAddress || '127.0.0.1';
            proxyReq.setHeader('X-Forwarded-For', forwardedFor);

            // NOTE: With changeOrigin: true, webpack will automatically change the Host header to 'rmsgateway.atparui.com'
            // This is required for the backend to accept the request (otherwise it returns 404)
            // But X-Forwarded-Host preserves the original client host for OAuth2 redirect URI generation

            // Log cookies being sent (for debugging)
            if (req.headers.cookie) {
              const cookies = req.headers.cookie;
              console.log('[Webpack Proxy] Forwarding cookies:', cookies);
              // Check for session cookie specifically
              if (cookies.includes('JSESSIONID') || cookies.includes('SESSION')) {
                console.log('[Webpack Proxy] ✓ Session cookie found in request');
              } else {
                console.warn(
                  '[Webpack Proxy] ⚠ No session cookie (JSESSIONID/SESSION) found - only:',
                  cookies
                    .split(';')
                    .map(c => c.trim().split('=')[0])
                    .join(', '),
                );
              }
            } else {
              console.warn('[Webpack Proxy] ⚠ No cookies in request - session may not be preserved');
            }
          },
          onProxyRes: (proxyRes, req, res) => {
            console.log('[Webpack Proxy] Response:', proxyRes.statusCode, 'for', req.url);

            // Log Set-Cookie headers from backend (for debugging)
            if (proxyRes.headers['set-cookie']) {
              console.log('[Webpack Proxy] Backend set cookies (raw):', proxyRes.headers['set-cookie']);
              // Check for session cookie
              const setCookies = Array.isArray(proxyRes.headers['set-cookie'])
                ? proxyRes.headers['set-cookie']
                : [proxyRes.headers['set-cookie']];
              const hasSessionCookie = setCookies.some(
                cookie => cookie.includes('JSESSIONID') || cookie.includes('SESSION') || cookie.toLowerCase().includes('session'),
              );
              if (hasSessionCookie) {
                console.log('[Webpack Proxy] ✓ Session cookie detected in Set-Cookie headers');
              } else {
                console.warn(
                  '[Webpack Proxy] ⚠ No session cookie detected - only:',
                  setCookies.map(c => c.split(';')[0].split('=')[0]).join(', '),
                );
              }

              // Rewrite cookies for localhost development
              if (Array.isArray(proxyRes.headers['set-cookie'])) {
                proxyRes.headers['set-cookie'] = proxyRes.headers['set-cookie'].map(cookie => {
                  const original = cookie;
                  let rewritten = cookie;
                  // Remove domain restriction so cookie works on localhost
                  rewritten = rewritten.replace(/;\s*[Dd]omain=[^;]+/gi, '');
                  // Remove Secure flag (HTTPS only) so cookie works on HTTP localhost
                  rewritten = rewritten.replace(/;\s*[Ss]ecure/gi, '');
                  // Remove SameSite=None (requires Secure) - replace with SameSite=Lax for localhost
                  if (rewritten.includes('SameSite=None')) {
                    rewritten = rewritten.replace(/;\s*[Ss]ame[Ss]ite=None/gi, '; SameSite=Lax');
                  }
                  if (original !== rewritten) {
                    console.log('[Webpack Proxy] Cookie rewritten:', original.split(';')[0], '->', rewritten.split(';')[0]);
                  }
                  return rewritten;
                });
                console.log(
                  '[Webpack Proxy] All cookies rewritten for localhost:',
                  proxyRes.headers['set-cookie'].map(c => c.split(';')[0]).join(', '),
                );
              } else {
                // Handle single cookie string (shouldn't happen but just in case)
                let rewritten = proxyRes.headers['set-cookie'];
                const original = rewritten;
                rewritten = rewritten.replace(/;\s*[Dd]omain=[^;]+/gi, '');
                rewritten = rewritten.replace(/;\s*[Ss]ecure/gi, '');
                if (rewritten.includes('SameSite=None')) {
                  rewritten = rewritten.replace(/;\s*[Ss]ame[Ss]ite=None/gi, '; SameSite=Lax');
                }
                proxyRes.headers['set-cookie'] = rewritten;
                if (original !== rewritten) {
                  console.log('[Webpack Proxy] Cookie rewritten:', original.split(';')[0], '->', rewritten.split(';')[0]);
                }
              }
            } else {
              // Check if this is an OAuth2 callback that should set a session cookie
              if (req.url.includes('/login/oauth2/code/') || req.url.includes('/oauth2/authorization/')) {
                console.warn('[Webpack Proxy] ⚠ OAuth2 endpoint but no Set-Cookie header - session cookie may not be set');
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
      // Wrap BrowserSync in try-catch to handle initialization errors gracefully
      (() => {
        try {
          return new BrowserSyncPlugin(
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
          );
        } catch (err) {
          console.warn('[Webpack] BrowserSync initialization failed (non-fatal):', err.message);
          console.warn('[Webpack] Continuing without BrowserSync. You can access the app at http://localhost:9060');
          // Return a no-op plugin that does nothing
          return {
            apply: () => {
              // No-op - BrowserSync failed but webpack dev server continues
            },
          };
        }
      })(),
      new WebpackNotifierPlugin({
        title: 'Rms',
        contentImage: path.join(__dirname, 'logo-jhipster.png'),
      }),
    ].filter(Boolean),
  });
