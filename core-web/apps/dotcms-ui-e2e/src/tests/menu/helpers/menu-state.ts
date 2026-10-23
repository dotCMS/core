import { Page } from '@playwright/test';

/** localStorage key the navigation uses to remember whether the bar is collapsed. */
export const MENU_STATUS_KEY = 'dotcms.menu.status';

/**
 * Starts the admin with the bar expanded (or collapsed) the way a returning user would find it.
 *
 * The saved login state already carries `dotcms.menu.status`, so "write it when it is missing"
 * would never run. The value is written once per tab instead, tracked by a `sessionStorage` marker
 * that survives `page.reload()`: a test can then assert that a reload restores what the user
 * last chose, because the second page load does not seed it again.
 */
export async function startWithBar(page: Page, state: 'expanded' | 'collapsed'): Promise<void> {
    const collapsed = state === 'collapsed';
    await page.addInitScript(
        ([key, value]) => {
            const seeded = 'e2e.menu.status.seeded';
            if (window.sessionStorage.getItem(seeded) === null) {
                window.sessionStorage.setItem(seeded, 'true');
                window.localStorage.setItem(key, value);
            }
        },
        [MENU_STATUS_KEY, String(collapsed)] as const
    );
}

/**
 * The modifier keys that mean "open in a new tab" for a link click on the OS running the browser.
 *
 * Chromium uses Cmd on macOS and Ctrl everywhere else. Cmd on Linux is a plain click: the browser
 * itself follows a hash link, with no code of ours involved, so it is not a click to assert on.
 * macOS also gets Ctrl, which there does nothing at all.
 */
export const NEW_TAB_MODIFIERS: readonly ('Control' | 'Meta')[] =
    process.platform === 'darwin' ? ['Control', 'Meta'] : ['Control'];

/** Escapes `text` so it matches literally inside a `RegExp`. */
export function escapeRegExp(text: string): string {
    return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** The `mId` query param of the current hash route, or `null` when the URL has none. */
export function mIdOf(url: string): string | null {
    return new URL(url.replace('#', '')).searchParams.get('mId');
}

/**
 * The route the navigation opens for a menu item: an Angular item keeps its own `url`, every other
 * item opens through the legacy host at `/c/<portlet id>`.
 */
export function menuLinkOf(item: { id: string; url: string; angular: boolean }): string {
    return item.angular ? item.url : `/c/${item.id}`;
}
