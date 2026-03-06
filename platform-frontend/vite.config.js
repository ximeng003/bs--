import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
export default defineConfig(function (_a) {
    var mode = _a.mode;
    var env = loadEnv(mode, process.cwd(), '');
    var apiTarget = process.env.VITE_API_TARGET || env.VITE_API_TARGET || 'http://127.0.0.1:18080';
    return {
        plugins: [vue()],
        resolve: {
            alias: {
                '@': path.resolve(__dirname, './src'),
            },
        },
        server: {
            port: 5173,
            proxy: {
                '^/api(?:/|$)': {
                    target: apiTarget,
                    changeOrigin: true,
                    secure: false,
                    rewrite: function (p) { return p.replace(/^\/api/, '/api'); },
                    bypass: function (req) {
                        // Do NOT proxy SPA routes like "/api-cases" which are front-end paths
                        var u = req.url || '';
                        if (/^\/api-cases(\/|$)/.test(u) || /^\/web-app(\/|$)/.test(u)) {
                            return '/index.html';
                        }
                        return null;
                    },
                    configure: function (proxy) {
                        proxy.on('error', function (err) {
                            console.log('proxy error', err);
                        });
                        proxy.on('proxyReq', function (_proxyReq, req) {
                            console.log('Sending Request to the Target:', req.method, req.url);
                        });
                        proxy.on('proxyRes', function (proxyRes, req) {
                            console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
                        });
                    },
                },
            },
        },
    };
});
