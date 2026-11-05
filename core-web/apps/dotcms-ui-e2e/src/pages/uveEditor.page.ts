import { expect, type FrameLocator, Locator, Page } from '@playwright/test';

/** No persona — the default the editor itself applies when the address does not name one. */
const NO_PERSONA_ID = 'modes.persona.no.persona';

/**
 * The Universal Visual Editor, as far as the Experiments entry points (#37478) and the side-panel
 * flows (#38303) need it.
 *
 * Deliberately small: this covers the navigation bar, the toolbar's running-experiment badge, the
 * Experiments panel, the contentlet toolbar's Full Editor button and a drag from the palette. It
 * is not an attempt to model the editor.
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
     */
    async openFullEditor(contentletTitle: string) {
        await this.contentletHeading(contentletTitle).hover();
        await this.page
            .getByTestId('hover-actions')
            .getByRole('button', { name: 'Full Editor' })
            .click();
    }

    /**
     * Drags a content type from the palette onto the first empty container on the page.
     *
     * Driven with the mouse rather than `dragTo`: the drop zones are drawn only once the editor is
     * dragging, so the target does not exist yet when a drag would have to name it. The pointer
     * first enters the page, which switches the editor to dragging, then moves onto the container's
     * zone, away from the canvas's scroll zones, and drops. Chromium turns these mouse events into
     * the HTML5 drag events the editor listens to.
     */
    async dragContentTypeToEmptyContainer(contentTypeName: string) {
        const palette = this.page.getByTestId('palette');
        await palette.getByTestId('palette-search-input').fill(contentTypeName);

        const item = palette.getByTestId('content-type-name').filter({ hasText: contentTypeName });
        await expect(item).toBeVisible({ timeout: 15000 });

        await item.hover();
        await this.page.mouse.down();
        const canvas = await centerOf(this.canvas);
        await this.page.mouse.move(canvas.x, canvas.y, { steps: 10 });

        // The zones are absolutely positioned boxes with no role or test id of their own; their
        // `data-type` is the only thing that tells a container's zone from a contentlet's.
        const zone = this.page.getByTestId('dropzone').locator('[data-type="container"]').first();
        await expect(zone).toBeVisible({ timeout: 10000 });
        const target = await dropPointIn(zone, this.canvas);
        await this.page.mouse.move(target.x, target.y, { steps: 5 });
        await this.page.mouse.up();
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
