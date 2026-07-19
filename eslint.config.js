const expoConfig = require('eslint-config-expo/flat');
const { defineConfig } = require('eslint/config');

module.exports = defineConfig([
  expoConfig,
  {
    ignores: [
      'dist/**',
      'coverage/**',
      'proxy/node_modules/**',
      'proxy/.wrangler/**',
      'proxy/worker-dist/**',
    ],
  },
]);
