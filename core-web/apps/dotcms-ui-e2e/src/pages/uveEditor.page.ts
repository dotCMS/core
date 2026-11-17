import { expect, Locator, Page } from '@playwright/test';

/** No persona — the default the editor itself applies when the address does not name one. */
const NO_PERSONA_ID = 'modes.persona.no.persona';

/**
 * The Universal Visual Editor, as far as the Experiments entry points (#37478) and adding a form
 * to a page need it.
 *
 * Deliberately small: this covers the navigation bar, the toolbar's running-experiment badge, the
 * Experiments panel, and the containers and contentlets on the canvas, which is what those suites
 * assert against. It is not an attempt to model the editor.
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

    /**
     * The Experiments entry point.
     *
     * Named "A/B" in the navigation bar, not "Experiments" — `editema.editor.navbar.experiments`
     * resolves to `A/B`. The accessible name is what the item actually renders, so that is what
     * this locator has to ask for.
     */
    get experimentsNavItem(): Locator {
        return this.navItem('A/B');
    }

    /** The toolbar's "running until …" tag, present only while the page has a running experiment. */
    get runningExperimentBadge(): Locator {
        return this.page.getByTestId('runningExperimentTag');
    }

    /** The iframe holding the rendered page. Its `src` is how a reload is detected. */
    get canvas(): Locator {
        return this.page.locator('iframe[data-testid="iframe"]');
    }

    /** A container on the rendered page, by its position among the page's containers. */
    container(index: number): Locator {
        return this.page
            .frameLocator('iframe[data-testid="iframe"]')
            .locator('[data-dot-object="container"]')
            .nth(index);
    }

    /** Every place a contentlet is rendered on the page. */
    contentlet(identifier: string): Locator {
        return this.page
            .frameLocator('iframe[data-testid="iframe"]')
            .locator(`[data-dot-object="contentlet"][data-dot-identifier="${identifier}"]`);
    }

    /**
     * Adds a form to a container the way an editor does: the container's add menu, then the form
     * in the form selector.
     *
     * The add button, and the menu it opens, exist only while the editor holds a hovered area. It
     * sets one when the pointer moves inside the canvas, and drops it whenever the canvas scrolls,
     * resizes or reloads its content, which happens on its own just after the page opens or a
     * form is placed. Nothing brings the button back until the pointer moves again, so a click
     * on a button that has just gone waits out the test. The whole sequence (move off the canvas
     * and back, open the menu, pick Form) is therefore retried until Form is clicked. A retry
     * cannot add a second form: the form selector only opens on that last click.
     */
    async addForm(containerIndex: number, formName: string) {
        const addContent = this.page.getByRole('button', { name: 'Add content' });
        const formItem = this.page.getByRole('menuitem', { name: 'Form' });
        await expect(async () => {
            await this.page.mouse.move(0, 0);
            await this.container(containerIndex).hover();
            if (!(await formItem.isVisible())) {
                await addContent.click({ timeout: 2000 });
            }

            await formItem.click({ timeout: 2000 });
        }).toPass({ timeout: 20000 });
        await this.page
            .getByRole('row', { name: formName })
            .getByTestId('form-select-button')
            .click();
    }
}
