import * as dotenv from "dotenv";
import * as path from "node:path";
import { defineConfig, devices } from "@playwright/test";
import { ReporterDescription } from "playwright/types/test";

const resolveEnvs = () => {
  const envFiles = [".env"];

  if (process.env.CURRENT_ENV === "local") {
    envFiles.push(".env.local");
  } else if (process.env.CURRENT_ENV === "ci") {
    envFiles.push(".env.ci");
  } else if (process.env.CURRENT_ENV === "dev") {
    envFiles.push(".env.dev");
  }

  envFiles.forEach((file) => {
    dotenv.config({
      path: path.resolve(__dirname, file),
      override: true,
    });
  });
};

const resolveReporter = () => {
  const reporter: ReporterDescription[] = [["junit"], ["github"]];
  if (!process.env.INCLUDE_HTML) {
    reporter.push(["html"]);
  }

  return reporter;
};

resolveEnvs();
const reporter = resolveReporter();

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./src/tests",
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: parseInt(process.env.RETRIES),
  /* Opt out of parallel tests on CI. */
  workers: parseInt(process.env.WORKERS),
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  timeout: parseInt(process.env.TIMEOUT),

  reporter,
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    baseURL: process.env.BASE_URL,
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "on-first-retry",
    headless: !!process.env.HEADLESS,
  },
  /* Configure projects for major browsers */
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },

    /*{
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },

    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },*/
  ],
  webServer: {
    command: "nx serve dotcms-ui",
    cwd: "../../../core-web",
    url: process.env.BASE_URL + "/dotAdmin",
    reuseExistingServer: !!process.env.REUSE_SERVER,
  },
});
