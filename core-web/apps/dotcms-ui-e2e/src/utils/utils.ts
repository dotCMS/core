import { Locator } from '@playwright/test';

/**
 * Wait for the locator to be in the provided state
 * @param locator
 * @param state
 */
export const waitFor = async (
    locator: Locator,
    state: 'attached' | 'detached' | 'visible' | 'hidden'
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
    state: 'attached' | 'detached' | 'visible' | 'hidden',
    callback?: () => Promise<void>
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
    callback?: () => Promise<void>
): Promise<void> => {
    await waitForAndCallback(locator, 'visible', callback);
};
