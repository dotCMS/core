import dotenv from 'dotenv';
import * as path from "node:path";
import { defineConfig, devices } from '@playwright/test';

const resolveEnvs = () => {
  const envFiles = ['.env'];

  if (process.env.CURRENT_ENV === 'local') {
    envFiles.push('.env.local');
  } else if (process.env.CURRENT_ENV === 'ci') {
    envFiles.push('.env.ci');
  }

  envFiles.forEach((file) => {
    dotenv.config({
      path: path.resolve(__dirname, file),
      override: true
    });
  });
};

resolveEnvs();

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['junit'],
    ['github']
  ],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    baseURL: process.env.BASE_URL,
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
    headless: process.env.CI === 'true',
  },
  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },

    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },

    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  webServer: {
    command: 'nx serve dotcms-ui',
    cwd: '../../../core-web',
    url: process.env.BASE_URL + '/dotAdmin',
    reuseExistingServer: !!process.env.CI,
  }
});
