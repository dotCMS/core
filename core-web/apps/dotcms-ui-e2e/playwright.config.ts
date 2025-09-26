import { workspaceRoot } from '@nx/devkit';
import { nxE2EPreset } from '@nx/playwright/preset';
import { defineConfig, devices } from '@playwright/test';
import * as dotenv from 'dotenv';

// Environment configuration
const currentEnv = process.env['CURRENT_ENV'] || 'dev';
const baseURL = process.env['E2E_BASE_URL'] || getBaseURL(currentEnv);
const reuseExistingServer = process.env['E2E_REUSE_EXISTING_SERVER'] === 'false' ? false : true;
const headless = process.env['HEADLESS'] === 'true' || currentEnv === 'ci';

console.warn('Playwright config - Current environment:', currentEnv);
console.warn('Playwright config - Base URL:', baseURL);

/**
 * Get base URL based on environment
 */
function getBaseURL(env: string): string {
    switch (env) {
        case 'ci':
            return 'http://localhost:8080'; // dotCMS directly
        case 'dev':
        default:
            return 'http://localhost:4200'; // Angular with proxy to 8080
    }
}

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
dotenv.config();

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
    ...nxE2EPreset(__filename, { testDir: './src' }),
    outputDir: 'test-results',
    /* Run tests in files in parallel */
    fullyParallel: true,
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: !!process.env.CI,
    /* Retry on CI only */
    retries: process.env.CI ? 2 : 0,
    /* Opt out of parallel tests on CI. */
    workers: process.env.CI ? 1 : undefined,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter:
        currentEnv === 'dev'
            ? [
                  ['html', { open: 'always', outputFolder: 'playwright-report' }], // Open report automatically in dev
                  ['junit', { outputFile: 'test-results/junit.xml' }]
              ]
            : [
                  ['html', { outputFolder: 'playwright-report' }],
                  ['junit', { outputFile: 'test-results/junit.xml' }]
              ],
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        baseURL,
        /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        headless: headless
    },
    /* Run your local dev server before starting the tests */
    webServer:
        currentEnv === 'dev'
            ? {
                  command: 'yarn nx run dotcms-ui:serve',
                  url: `${baseURL}/dotAdmin/#/public/login`,
                  reuseExistingServer: reuseExistingServer,
                  cwd: workspaceRoot
              }
            : undefined,
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] }
        }

        // {
        //   name: 'firefox',
        //   use: { ...devices['Desktop Firefox'] }
        // },

        // {
        //   name: 'webkit',
        //   use: { ...devices['Desktop Safari'] }
        // }
    ]
});
