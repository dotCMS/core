import { Locator, Page } from '@playwright/test';

/** No persona — the default the editor itself applies when the address does not name one. */
const NO_PERSONA_ID = 'modes.persona.no.persona';

/**
 * The Universal Visual Editor, as far as the Experiments entry points need it (#37478).
 *
 * Deliberately small: this covers the navigation bar, the toolbar's running-experiment badge and
 * the Experiments panel, which is what the two experiments suites assert against. It is not an
 * attempt to model the editor.
 */
export class UveEditorPage {
    constructor(private page: Page) {}

    /**
     * Opens a page in the editor.
     *
     * The address is built rather than clicked through the pages list on purpose: `editEmaGuard`
     * *substitutes* defaults for missing params instead of rejecting, so an incomplete URL opens a
     * plausible-looking wrong page rather than failing. Naming all three keeps the destination
     * unambiguous, which matters for a suite whose whole point is where the editor ends up.
     */
    async open(pageUrl: string, languageId = 1) {
        const params = new URLSearchParams({
            url: pageUrl,
            language_id: String(languageId),
            'com.dotmarketing.persona.id': NO_PERSONA_ID
        });

        await this.page.goto(`/dotAdmin/#/edit-page/content?${params.toString()}`);
    }

    get navBar(): Locator {
        return this.page.getByTestId('ema-nav-bar');
    }

    /** A navigation-bar item, by the accessible name the editor renders for it. */
    navItem(name: string): Locator {
        return this.navBar.getByRole('button', { name, exact: true });
    }

    get experimentsNavItem(): Locator {
        return this.navItem('Experiments');
    }

    /** The toolbar's "running until …" tag, present only while the page has a running experiment. */
    get runningExperimentBadge(): Locator {
        return this.page.getByTestId('runningExperimentTag');
    }

    /** The iframe holding the rendered page. Its `src` is how a reload is detected. */
    get canvas(): Locator {
        return this.page.locator('iframe[data-testid="iframe"]');
    }
}
