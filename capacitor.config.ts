import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.sillygirl.client',
  appName: 'SillyGirl客户端',
  webDir: 'dist',
  server: {
    url: 'http://localhost:3000',
    cleartext: true,
    androidScheme: 'https'
  }
};

export default config;
