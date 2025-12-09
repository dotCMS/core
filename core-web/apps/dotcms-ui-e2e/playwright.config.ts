import { workspaceRoot } from '@nx/devkit';
import { nxE2EPreset } from '@nx/playwright/preset';
import { defineConfig, devices } from '@playwright/test';
import * as dotenv from 'dotenv';

// For CI, you may want to set BASE_URL to the deployed application.
const baseURL = process.env['E2E_BASE_URL'] || 'http://localhost:4200';
const reuseExistingServer = process.env['E2E_REUSE_EXISTING_SERVER'] === 'false' ? false : true;

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
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        baseURL,
        /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
        trace: 'on-first-retry'
    },
    /* Run your local dev server before starting the tests */
    webServer: {
        command: 'yarn nx run dotcms-ui:serve',
        url: `${baseURL}/dotAdmin/#`,
        reuseExistingServer: reuseExistingServer,
        cwd: workspaceRoot
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] }
        },

        {
            name: 'firefox',
            use: { ...devices['Desktop Firefox'] }
        },

        {
            name: 'webkit',
            use: { ...devices['Desktop Safari'] }
        }

        // Uncomment for mobile browsers support
        /* {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] }
},
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] }
}, */

        // Uncomment for branded browsers
        /* {
      name: 'Microsoft Edge',
      use: { ...devices['Desktop Edge'], channel: 'msedge' }
},
    {
      name: 'Google Chrome',
      use: { ...devices['Desktop Chrome'], channel: 'chrome' }
} */
    ]
});
