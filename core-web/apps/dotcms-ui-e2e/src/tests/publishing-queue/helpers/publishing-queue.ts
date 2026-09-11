import { type FrameLocator, type Page } from '@playwright/test';
import { getLegacyFrame } from '@utils/iframe';
import { Portlet } from '@utils/portlets';

/**
 * Focused helper for the Dojo-based Publishing Queue portlet (Pending tab).
 *
 * The portlet renders inside iframe[id="detailFrame"] — use `frame` for all interactions.
 * Modelled on `tests/content-search/helpers/content-listing.ts`.
 */
export class PublishingQueueHelper {
    readonly frame: FrameLocator;

    /**
     * Console errors captured since `startCollectingConsoleErrors()` was called.
     *
     * This is the primary signal for issue #36861: a duplicate dijit widget id makes
     * `dojo/parser::parse()` throw and abandon the rest of the parse pass, which surfaces
     * *only* on the console — the page still renders, just with un-upgraded checkboxes below
     * the collision point. Nothing fails visibly, which is why the bug went unnoticed.
     */
    readonly consoleErrors: string[] = [];

    constructor(private page: Page) {
        this.frame = getLegacyFrame(page);
    }

    /**
     * Starts capturing console errors. Call BEFORE `goto()` — the parse error fires during the
     * portlet's initial render, so a listener attached afterwards misses it entirely.
     */
    startCollectingConsoleErrors() {
        this.page.on('console', (msg) => {
            if (msg.type() === 'error') {
                this.consoleErrors.push(msg.text());
            }
        });
        this.page.on('pageerror', (error) => {
            this.consoleErrors.push(error.message);
        });
    }

    /** Console errors mentioning the Dojo parser or a duplicate widget-id registration. */
    get dojoParserErrors(): string[] {
        return this.consoleErrors.filter(
            (text) =>
                text.includes('dojo/parser') ||
                text.includes('already registered') ||
                text.includes('_ContentSetter')
        );
    }

    get deleteButton() {
        return this.frame.getByRole('button', { name: 'Delete' });
    }

    /** All bundle-level checkboxes, whether or not dijit upgraded them. */
    get bundleCheckboxes() {
        return this.frame.locator('[id^="bundle_to_delete_"]');
    }

    /** All asset-level checkboxes, whether or not dijit upgraded them. */
    get assetCheckboxes() {
        return this.frame.locator('[id^="queue_to_delete_"]');
    }

    /**
     * Bundle ids currently rendered on the page, read off the bundle checkbox ids.
     * The push-publish AJAX action does not return the bundle id it created, so tests
     * discover them here.
     */
    async bundleIds(): Promise<string[]> {
        const ids = await this.bundleCheckboxes.evaluateAll((nodes) =>
            nodes.map((n) => n.id.replace('bundle_to_delete_', ''))
        );

        return ids.filter(Boolean);
    }

    /** Raw `id` attributes of every asset checkbox — the uniqueness assertion for AC-003. */
    async assetCheckboxIds(): Promise<string[]> {
        return this.assetCheckboxes.evaluateAll((nodes) => nodes.map((n) => n.id));
    }

    /**
     * Ids of asset checkboxes that dijit did NOT upgrade into widgets.
     *
     * Asks dijit's own registry rather than inspecting the DOM. `dijit.form.CheckBox` keeps the
     * original `<input>` and carries the widget id on it, so a selector like
     * `input[type=checkbox][id^=...]` matches upgraded and un-upgraded checkboxes alike and
     * proves nothing.
     *
     * The registry is also the thing that actually matters: `deleteQueue()` resolves each node
     * through `dijit.getEnclosingWidget()`, so a checkbox the registry does not know is exactly
     * a checkbox whose selection Delete will silently discard.
     */
    async unUpgradedCheckboxIds(): Promise<string[]> {
        return this.frame.locator('body').evaluate(() => {
            const ids = Array.from(
                document.querySelectorAll('[id^="queue_to_delete_"], [id^="bundle_to_delete_"]')
            ).map((node) => node.id);
            const dijit = (window as unknown as { dijit?: { byId(id: string): unknown } }).dijit;

            if (!dijit) {
                return ids; // dijit never loaded — treat every checkbox as un-upgraded
            }

            return ids.filter((id) => !dijit.byId(id));
        });
    }

    /** The "Pending" tab of the portlet's dijit TabContainer (`#mainTabContainer`). */
    get pendingTab() {
        return this.frame.locator('#mainTabContainer .dijitTab', { hasText: 'Pending' });
    }

    /** The pane the queue list is rendered into (`view_publish_queue_list.jsp` output). */
    get queueResults() {
        return this.frame.locator('#queue_results');
    }

    /**
     * The asset-row checkboxes belonging to one bundle.
     *
     * After the #36861 fix the asset checkbox id ends with its owning bundle id
     * (`queue_to_delete_<asset>$<operation>$<bundleId>`), which is what makes per-bundle
     * selection addressable at all — before the fix the id carried no bundle.
     */
    assetCheckboxesForBundle(bundleId: string) {
        return this.frame.locator(`[id^="queue_to_delete_"][id$="$${bundleId}"]`);
    }

    /** The bundle-level checkbox for one bundle. */
    bundleCheckbox(bundleId: string) {
        return this.frame.locator(`#bundle_to_delete_${bundleId}`);
    }

    /**
     * Checked/disabled state of every asset checkbox under a bundle, read from dijit rather than
     * the DOM.
     *
     * `dijit.form.CheckBox` keeps its own `checked`/`disabled` properties; the underlying
     * `<input>` is not a reliable mirror of them, and `checkAllBundle()` sets the widget
     * properties directly.
     */
    async assetCheckboxStates(
        bundleId: string
    ): Promise<{ checked: boolean; disabled: boolean }[]> {
        return this.frame.locator('body').evaluate((_body, id: string) => {
            const dijit = (
                window as unknown as {
                    dijit?: { byId(widgetId: string): { checked?: boolean; disabled?: boolean } };
                }
            ).dijit;

            return Array.from(document.querySelectorAll('[id^="queue_to_delete_"]'))
                .filter((node) => node.id.endsWith(`$${id}`))
                .map((node) => {
                    const widget = dijit?.byId(node.id);

                    return {
                        checked: Boolean(widget?.checked),
                        disabled: Boolean(widget?.disabled)
                    };
                });
        }, bundleId);
    }

    /** Asset identifiers currently listed under a bundle, derived from the checkbox ids. */
    async assetIdsForBundle(bundleId: string): Promise<string[]> {
        const ids = await this.assetCheckboxesForBundle(bundleId).evaluateAll((nodes) =>
            nodes.map((n) => n.id)
        );

        return ids.map((id) => id.replace('queue_to_delete_', '').split('$')[0]);
    }

    /**
     * Navigates to the Publishing Queue portlet and selects the Pending tab.
     *
     * The portlet opens on "Status / History" (`#audit`), the first pane in `#mainTabContainer`.
     * The queue list — and therefore every checkbox this suite asserts on — only exists once the
     * "Pending" pane (`#queue`) has loaded `view_publish_queue_list.jsp` into `#queue_results`.
     */
    async goto() {
        await this.page.goto(Portlet.PublishingQueue);
        await this.openPendingTab();
    }

    /** Clicks the Pending tab and waits for the queue list to render. */
    async openPendingTab() {
        await this.pendingTab.waitFor({ state: 'visible', timeout: 20000 });
        await this.pendingTab.click();
        await this.waitForReady();
    }

    /** Waits for at least one bundle row to render (queue list fully parsed). */
    async waitForReady() {
        await this.bundleCheckboxes.first().waitFor({ state: 'visible', timeout: 20000 });
    }
}
