module.exports = {
  testDir: './', 
  timeout: 60000, 
  use: {
    headless: false, 
    browserName: 'chromium' // Browser to use (chromium, firefox, webkit)
  },
  testMatch: '**/*.spec.js' // Pattern to match test files
};