import { Locator } from "@playwright/test";

// Note: The waitFor, waitForAndCallback, and waitForVisibleAndCallback functions
// have been removed as Playwright provides native support for these functionalities.
// Use Playwright's auto-waiting and direct waitFor/expect methods instead.

// For example:
// - Instead of waitForVisibleAndCallback, use:
//   await locator.waitFor();
//   await expect(locator).toBeVisible();
//
// - For interactions, use Playwright's auto-waiting:
//   await button.click();  // Playwright automatically waits for the element to be clickable
//
// - For waiting with specific state:
//   await locator.waitFor({ state: "visible" });
