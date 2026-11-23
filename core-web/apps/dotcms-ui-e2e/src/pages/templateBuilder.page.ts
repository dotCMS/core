import { Locator, Page } from '@playwright/test';

/** Budget for the Layout tab's boot chain (app start, page render, builder config), in ms. */
const BUILDER_BOOT_TIMEOUT = 30_000;

/**
 * Page object for the UVE (Universal Visual Editor) Layout tab — the drawn-template
 * GridStack canvas at /edit-page/layout.
 */
export class TemplateBuilderPage {
    constructor(private page: Page) {}

    /**
     * Opens the Layout tab for the page and resolves once the builder has what it needs to
     * render its boxes.
     *
     * The builder (`TemplateBuilderComponent.ngOnInit`) only builds its rows after
     * `DotContainersService.defaultContainer$` emits, which is the response to
     * `GET /api/v1/configuration/config?keys=DEFAULT_CONTAINER`. That request is the last link
     * of a sequential chain (app boot, then the page render call, then this one), so on a
     * loaded CI runner the boxes can take longer than the 5 s default of an `expect`. Waiting
     * for the response here, with the navigation-sized timeout, leaves the caller's own
     * `expect(...).toBeVisible()` to cover only the render that follows it.
     *
     * The wait is registered before `goto` so the response cannot be missed.
     */
    async navigateTo(pageUrl: string, params: Record<string, string> = {}) {
        const query = new URLSearchParams({ url: pageUrl, ...params }).toString();
        const builderConfigLoaded = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/configuration/config?keys=DEFAULT_CONTAINER') &&
                response.ok(),
            { timeout: BUILDER_BOOT_TIMEOUT }
        );

        await this.page.goto(`/dotAdmin/#/edit-page/layout?${query}`);
        await builderConfigLoaded;
    }

    getOverlay(): Locator {
        return this.page.getByTestId('template-builder-disabled-overlay');
    }

    getBox(index: number): Locator {
        return this.page.getByTestId(`builder-box-${index}`);
    }

    /**
     * Deletes the (single) container in the given box via the trash icon + confirm popup.
     * This is a plain click interaction — no GridStack drag is needed to produce a real
     * templateChange edit.
     */
    async deleteContainerFromBox(boxIndex: number): Promise<void> {
        await this.getBox(boxIndex).getByTestId('btn-trash-container').click();

        // PrimeNG p-confirmPopup default accept button. Confirmed against the installed
        // primeng@21.1.3 package (fesm2022/primeng-config.mjs: `accept: 'Yes'`) — this
        // repo does not override that translation.
        await this.page.getByRole('button', { name: 'Yes' }).click();
    }
}
