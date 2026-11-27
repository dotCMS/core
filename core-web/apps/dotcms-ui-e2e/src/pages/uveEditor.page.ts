import { expect, type FrameLocator, Locator, Page } from '@playwright/test';

/** No persona — the default the editor itself applies when the address does not name one. */
const NO_PERSONA_ID = 'modes.persona.no.persona';

/**
 * How long one attempt of a pointer-driven gesture waits before it is worth starting over, and how
 * long the gesture keeps being retried (#38717). The editor redraws the contentlet toolbar and the
 * drop zones as the pointer moves, so a single long wait can sit on a node that is already gone or
 * on a state the editor never entered; several short attempts recover from both.
 */
const ONE_GESTURE_MS = 2_000;
const GESTURE_RETRY_MS = 30_000;

/**
 * The Universal Visual Editor, as far as the Experiments entry points (#37478), adding a form to a
 * page and the side-panel flows (#38303) need it.
 *
 * Deliberately small: this covers the navigation bar, the toolbar's running-experiment badge, the
 * Experiments panel, the containers and contentlets on the canvas, the contentlet toolbar's Full
 * Editor button and a drag from the palette, which is what those suites assert against. It is not
 * an attempt to model the editor.
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

    /** The rendered page itself, to find content in it. */
    get canvasFrame(): FrameLocator {
        return this.page.frameLocator('iframe[data-testid="iframe"]');
    }

    /**
     * A contentlet on the page, by its title. The side-panel specs' container (`createUvePage`),
     * like the system container, renders each contentlet's title as its heading.
     */
    contentletHeading(title: string): Locator {
        return this.canvasFrame.getByRole('heading', { name: title, exact: true });
    }

    /**
     * Opens a contentlet in its editor through the contentlet toolbar's Full Editor button (the
     * pencil): hovers the contentlet in the page so the editor draws its toolbar, then clicks it.
     *
     * The toolbar shows its buttons in a row only when the contentlet is at least 600px wide, and
     * folds them into a menu below that, so specs using this run with a desktop-sized viewport.
     * Moving the pointer out of the page and onto the toolbar keeps it: the page script reports
     * no "left" on `pointerleave`, on purpose, so the toolbar stays reachable.
     *
     * The hover and the click are retried together (#38717). The editor rebuilds `hover-actions`
     * whenever the contentlet under the pointer changes, so the button Playwright resolved can be
     * detached before the click lands. Playwright's own retry re-resolves the same chain without
     * hovering again, which re-races the redraw until the test's budget is gone; restarting the
     * whole gesture gives the next attempt a toolbar that is being drawn for this contentlet.
     */
    async openFullEditor(contentletTitle: string) {
        const fullEditor = this.page
            .getByTestId('hover-actions')
            .getByRole('button', { name: 'Full Editor' });

        await expect(async () => {
            await this.contentletHeading(contentletTitle).hover({ timeout: ONE_GESTURE_MS });
            await fullEditor.click({ timeout: ONE_GESTURE_MS });
        }).toPass({ timeout: GESTURE_RETRY_MS });
    }

    /**
     * Drags a content type from the palette onto the first empty container on the page.
     *
     * Driven with the mouse rather than `dragTo`: the drop zones are drawn only once the editor is
     * dragging, so the target does not exist yet when a drag would have to name it. The pointer
     * first enters the page, which switches the editor to dragging, then moves onto the container's
     * zone, away from the canvas's scroll zones, and drops. Chromium turns these mouse events into
     * the HTML5 drag events the editor listens to.
     *
     * The whole gesture is retried, press included (#38717). Repeating only the move cannot
     * recover from the two ways this fails in CI: the palette search is debounced and swaps the
     * list for skeletons while it reloads, so the node that was matched is detached before or
     * during the press, and a press that never reached a live node starts no drag at all, after
     * which no number of moves will draw a zone. Each attempt is bounded so a detached node costs
     * seconds rather than the test's whole budget, and shifts the pointer by a pixel because a
     * move to the coordinates it already holds produces no event.
     *
     * A failed attempt releases the button over the palette, never over the canvas: the specs
     * count how many contentlets were added, and a release on a zone drawn late would add one.
     */
    async dragContentTypeToEmptyContainer(contentTypeName: string) {
        const palette = this.page.getByTestId('palette');
        await palette.getByTestId('palette-search-input').fill(contentTypeName);

        const item = palette.getByTestId('content-type-name').filter({ hasText: contentTypeName });
        await expect(item).toBeVisible({ timeout: 15000 });

        const canvas = await centerOf(this.canvas);

        // The zones are absolutely positioned boxes with no role or test id of their own; their
        // `data-type` is the only thing that tells a container's zone from a contentlet's.
        const zone = this.page.getByTestId('dropzone').locator('[data-type="container"]').first();
        let nudge = 0;

        await expect(async () => {
            nudge = nudge === 0 ? 1 : 0;

            try {
                await item.hover({ timeout: ONE_GESTURE_MS });
                await this.page.mouse.down();
                await this.page.mouse.move(canvas.x + nudge, canvas.y, { steps: 10 });
                await expect(zone).toBeVisible({ timeout: ONE_GESTURE_MS });
            } catch (error) {
                // Back onto the palette before letting go, so the release cannot land on a zone.
                await item.hover({ timeout: ONE_GESTURE_MS }).catch(() => undefined);
                await this.page.mouse.up();
                throw error;
            }
        }).toPass({ timeout: GESTURE_RETRY_MS });

        const target = await dropPointIn(zone, this.canvas);
        await this.page.mouse.move(target.x, target.y, { steps: 5 });
        await this.page.mouse.up();
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

/**
 * How close to the canvas's top or bottom edge a drag scrolls the page instead of hovering a drop
 * zone: the editor's `IFRAME_SCROLL_ZONE`. While it scrolls it hides the zones, and the drag then
 * leaves for the page underneath, so a drop there places nothing.
 */
const CANVAS_SCROLL_ZONE_PX = 100;

/**
 * A point in a drop zone the editor accepts a drop on: inside the zone and out of the canvas's
 * scroll zones, as near the zone's centre as that allows. A container at the top of a page has its
 * centre within the top scroll zone.
 *
 * @param zone - The drop zone
 * @param canvas - The page's iframe
 * @returns The point, on screen
 */
async function dropPointIn(zone: Locator, canvas: Locator): Promise<{ x: number; y: number }> {
    const zoneBox = await zone.boundingBox();
    const canvasBox = await canvas.boundingBox();

    if (!zoneBox || !canvasBox) {
        throw new Error('The drop zone or the canvas has no bounding box');
    }

    const margin = CANVAS_SCROLL_ZONE_PX + 10;
    const top = Math.max(zoneBox.y + 1, canvasBox.y + margin);
    const bottom = Math.min(
        zoneBox.y + zoneBox.height - 1,
        canvasBox.y + canvasBox.height - margin
    );

    if (top > bottom) {
        throw new Error('The drop zone lies entirely within the canvas scroll zones');
    }

    const middle = zoneBox.y + zoneBox.height / 2;

    return { x: zoneBox.x + zoneBox.width / 2, y: Math.min(Math.max(middle, top), bottom) };
}

/** The centre of an element on screen, for the mouse to move to. */
async function centerOf(locator: Locator): Promise<{ x: number; y: number }> {
    const box = await locator.boundingBox();

    if (!box) {
        throw new Error('The element to move the mouse to has no bounding box');
    }

    return { x: box.x + box.width / 2, y: box.y + box.height / 2 };
}
