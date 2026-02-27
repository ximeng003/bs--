import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:18080',
                changeOrigin: true,
                // rewrite: (path) => path.replace(/^\/api/, '') // 如果后端不需要 /api 前缀，可以取消注释
            }
        }
    }
});
