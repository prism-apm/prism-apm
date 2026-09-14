import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue2';
import * as path from 'path';
import compression from "vite-plugin-compression2";

// noinspection JSUnusedGlobalSymbols
export default defineConfig({
    plugins: [
        vue(),
        compression({
            threshold: 10240, // compress > 10kb
            algorithms: ['gzip'],
            deleteOriginalAssets: false
        })
    ],
    resolve: {
        extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
        alias: {
            'vue$': 'vue/dist/vue.esm.js',
            '@': path.resolve(__dirname, 'src')
        }
    },
    server: {
        host: '127.0.0.1',
        port: 8066,
        open: false,
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true
            }
        }
    },
    build: {
        // minify: 'terser',
        // terserOptions: {
        //   compress: { drop_console: true, drop_debugger: true },
        //   format: { comments: false }
        // },
        chunkSizeWarningLimit: 1500,
        rollupOptions: {
            output: {
                entryFileNames: 'js/[name].[hash].js',
                chunkFileNames: 'js/[name].[hash].js',
                assetFileNames: (assetInfo) => {
                    const assetName = assetInfo.names?.[0] || '';

                    if (assetName.endsWith('.css')) {
                        return 'css/[name].[hash][extname]';
                    }
                    if (['.woff', '.woff2', '.ttf', '.eot'].some(ext => assetName.endsWith(ext))) {
                        return 'fonts/[name].[hash][extname]';
                    }
                    if (['.png', '.jpg', '.svg', '.gif', '.webp'].some(ext => assetName.endsWith(ext))) {
                        return 'img/[name].[hash][extname]';
                    }
                    return 'assets/[name].[hash][extname]';
                },

        manualChunks(id) {
          if (id.includes('node_modules')) {
            // Vite ids are absolute paths; normalize them to strings.
            const idStr = id.toString();

                        // Vue
                        if (/[\\/](vue|vue-router|pinia|vue-i18n|pinia-plugin-persistedstate)[\\/]/.test(idStr)) {
                            return 'chunk-vue';
                        }
                        // iView
                        if (/[\\/]iview[\\/]/.test(idStr)) {
                            return 'chunk-iview';
                        }
                        // Echarts
                        if (/[\\/](echarts|zrender)[\\/]/.test(idStr)) {
                            return 'chunk-echarts';
                        }
                        // AntV G6
                        if (/[\\/](@antv|regl|dagre|gl-matrix|graphlib)[\\/]/.test(idStr)) {
                            return 'chunk-antv';
                        }

                        // Others (lodash, axios, qs, dayjs ...)
                        return 'chunk-vendors';
                    }
                }
            }
        }
    }
});
