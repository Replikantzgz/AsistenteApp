import type { NextConfig } from "next";

// Temporarily disable PWA to fix build issues
// const withPWA = require("@ducanh2912/next-pwa").default({
//   dest: "public",
//   cacheOnFrontEndNav: true,
//   aggressiveFrontEndNavCaching: true,
//   reloadOnOnline: true,
//   swcMinify: true,
//   disable: process.env.NODE_ENV === "development", // Disable PWA in dev
//   workboxOptions: {
//     disableDevLogs: true,
//   },
// });

const nextConfig: NextConfig = {
  turbopack: {},
  // output: 'export' solo para build de APK con Capacitor
  ...(process.env.BUILD_TARGET === 'capacitor' ? { output: 'export' } : {}),
  images: { unoptimized: true },
};

export default nextConfig;
// export default withPWA(nextConfig);
