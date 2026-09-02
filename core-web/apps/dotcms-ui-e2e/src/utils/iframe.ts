import { type FrameLocator, type Page } from '@playwright/test';

/**
 * Returns a FrameLocator for the legacy Dojo iframe (id="detailFrame").
 * Most legacy portlets render inside this iframe.
 */
export function getLegacyFrame(page: Page): FrameLocator {
    return page.frameLocator('#detailFrame');
}
