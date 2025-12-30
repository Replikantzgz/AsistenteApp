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
  // Removed output: 'export' to allow dynamic routes for NextAuth
  images: { unoptimized: true },
};

export default nextConfig;
// export default withPWA(nextConfig);
