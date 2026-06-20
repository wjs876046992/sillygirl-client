import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.sillygirl.client',
  appName: 'SillyGirl客户端',
  webDir: 'dist',
  server: {
    hostname: 'localhost',
    androidScheme: 'http'
  }
};

export default config;
