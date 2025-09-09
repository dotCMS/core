import { Locator, Page } from "@playwright/test";

/**
 * Wait for the locator to be in the provided state
 * @param locator
 * @param state
 */
export const waitFor = async (
  locator: Locator,
  state: "attached" | "detached" | "visible" | "hidden",
): Promise<void> => {
  await locator.waitFor({ state: state });
};

/**
 * Wait for the locator to be visible
 * @param locator
 * @param state
 * @param callback
 */
export const waitForAndCallback = async (
  locator: Locator,
  state: "attached" | "detached" | "visible" | "hidden",
  callback?: () => Promise<void>,
): Promise<void> => {
  await waitFor(locator, state);
  if (callback) {
    await callback();
  }
};

/**
 * Wait for the locator to be visible and execute the callback
 * @param locator
 * @param callback
 */
export const waitForVisibleAndCallback = async (
  locator: Locator,
  callback?: () => Promise<void>,
): Promise<void> => {
  await waitForAndCallback(locator, "visible", callback);
};

/**
 * Enhanced wait for visible that handles Angular component lifecycle
 * Waits for element to be both present and functionally interactive
 * @param locator
 * @param callback
 * @param options
 */
export const waitForAngularReady = async (
  locator: Locator,
  callback?: () => Promise<void>,
  options?: { timeout?: number; polling?: number }
): Promise<void> => {
  const { timeout = 15000, polling = 500 } = options || {};
  
  // First wait for basic visibility
  await locator.waitFor({ state: "visible", timeout });
  
  // Then wait for Angular to be ready (addresses resource contention issues)
  // Try to get a simple CSS selector from the locator
  const testId = await locator.getAttribute('data-testid');
  const id = await locator.getAttribute('id');
  
  let cssSelector = null;
  if (testId) {
    cssSelector = `[data-testid="${testId}"]`;
  } else if (id) {
    cssSelector = `#${id}`;
  }
  
  // Only run the Angular readiness check if we have a valid CSS selector
  if (cssSelector) {
    await locator.page().waitForFunction(
      (selector) => {
        const element = document.querySelector(selector) as HTMLElement;
        if (!element) return false;
        
        // Check if element is truly interactive (not just visible)
        const isButton = element.tagName === 'BUTTON';
        const isInput = element.tagName === 'INPUT';
        const isSelect = element.tagName === 'SELECT';
        
        if (isButton) {
          return !element.hasAttribute('disabled') && 
                 !element.classList.contains('p-disabled') &&
                 !element.classList.contains('disabled');
        }
        
        if (isInput || isSelect) {
          return !element.hasAttribute('disabled') && 
                 !element.hasAttribute('readonly');
        }
        
        // For other elements, check if they're not hidden by Angular
        return !element.classList.contains('ng-hide') && 
               !element.classList.contains('hidden') &&
               element.offsetHeight > 0 && element.offsetWidth > 0;
      },
      cssSelector,
      { timeout, polling }
    );
  }
  // If no valid CSS selector, just wait for the locator to be stable
  else {
    await locator.waitFor({ state: "attached", timeout });
  }
  
  if (callback) {
    await callback();
  }
};

/**
 * Wait for Angular application to be stable
 * Addresses resource contention and background operations affecting UI state
 * @param page
 * @param timeout
 */
export const waitForAngularStability = async (
  page: Page,
  timeout: number = 10000
): Promise<void> => {
  await page.waitForFunction(
    () => {
      // Wait for any pending Angular operations to complete
      if ((window as any).getAllAngularTestabilities) {
        const testabilities = (window as any).getAllAngularTestabilities();
        return testabilities.every((testability: any) => testability.isStable());
      }
      
      // Fallback: ensure no pending requests or timers
      return (window as any).performance?.now && 
             document.readyState === 'complete' &&
             // Check if any major background operations are running
             !document.querySelector('.loading, .spinner, [data-loading="true"]');
    },
    { timeout, polling: 250 }
  );
};
