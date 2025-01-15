export default {
  testDir: './',
  timeout: 60000,
  use: {
    headless: true,
    browserName: 'chromium' // Browser to use (chromium, firefox, webkit)
  },
  testMatch: '**/*.spec.js' // Pattern to match test files
};