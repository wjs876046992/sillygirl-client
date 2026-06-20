import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    // 开发模式：代理 /api 到 sillyGirl 服务器
    // 浏览器请求 localhost:3000/api/... → 同域 → cookie 自动生效
    proxy: {
      '/api': {
        target: 'http://192.168.1.12:8081',
        changeOrigin: true,
        // 让 cookie 随当前域名自动存储（不指定 Domain，浏览器用请求域名）
        configure: (proxy, options) => {
          proxy.on('proxyRes', (proxyRes, req) => {
            const cookie = proxyRes.headers['set-cookie'];
            if (cookie) {
              proxyRes.headers['set-cookie'] = (
                Array.isArray(cookie)
                  ? cookie
                  : [cookie]
              ).map((c) => {
                // 去掉 Domain= 指定，让浏览器用当前请求域名自动匹配
                if (c.includes('Domain=')) {
                  return c.replace(/;\s*Domain=[^;]*/, '');
                }
                return c;
              });
            }
          });
        },
      },
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          antd: ['antd-mobile'],
          utils: ['zustand', 'axios', 'socket.io-client'],
        },
      },
    },
  },
})
