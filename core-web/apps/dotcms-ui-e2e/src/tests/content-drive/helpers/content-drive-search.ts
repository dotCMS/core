import { expect, type Locator, type Page } from '@playwright/test';

/**
 * The two values the scope control offers, mirroring `DotContentDriveSearchScope` on the UI side.
 * They are also the `data-testid` suffixes of the panel's options.
 */
export type DriveSearchScope = 'TITLE' | 'ALL_FIELDS';

/**
 * The English trigger label for each scope. The instance runs the default locale, and the label is
 * how a scope change is observed without opening the panel, so the strings live here rather than
 * being inlined at every call site. Source of truth: `content-drive.search.scope.*` in
 * `Language.properties`.
 */
const SCOPE_LABEL: Record<DriveSearchScope, string> = {
    TITLE: 'Title',
    ALL_FIELDS: 'All Fields'
};

/**
 * Locator wrapper for the Content Drive search scope control — the trigger in the toolbar addon
 * and the popover panel it opens (issue #37479).
 *
 * Every `data-testid` lives on `dot-content-drive-search-input` itself, so this stays a locator
 * question: no behavior is re-implemented here, and a testid change fails loudly at the locator.
 */
export class ContentDriveSearchScope {
    readonly trigger: Locator;
    readonly activeLabel: Locator;
    readonly panel: Locator;
    readonly input: Locator;

    constructor(private page: Page) {
        this.trigger = page.getByTestId('search-scope-trigger');
        this.activeLabel = page.getByTestId('search-scope-active');
        this.panel = page.getByTestId('search-scope-panel');
        this.input = page.getByTestId('search-input-field');
    }

    /** An option's inner span, reached through its value-named test id. */
    option(scope: DriveSearchScope): Locator {
        return this.panel.getByTestId(`search-scope-option-${scope}`);
    }

    /** Opens the panel and waits for it to render. */
    async open() {
        await this.trigger.click();
        await expect(this.panel).toBeVisible({ timeout: 5000 });
    }

    /**
     * Picks a scope through the panel and waits for the choice to land: the panel closes and the
     * trigger reads the chosen scope. Choosing re-runs the search (FR-004), so a test that needs
     * the request itself arms a capture around this — see
     * `ContentDrivePage.captureSearchPayload`.
     */
    async choose(scope: DriveSearchScope) {
        await this.open();
        await this.option(scope).click();
        await expect(this.panel).toBeHidden({ timeout: 5000 });
        await this.expectActive(scope);
    }

    /** The trigger reads the active scope as its label (FR-002). */
    async expectActive(scope: DriveSearchScope) {
        await expect(this.activeLabel).toHaveText(SCOPE_LABEL[scope]);
    }

    /** The panel marks the active option, so the user can tell which one is on. */
    async expectOptionMarked(scope: DriveSearchScope) {
        // The option row is reached by role and label — PrimeNG renders `role="option"` with the
        // option's label as its accessible name — and the selection state on `aria-selected`.
        await expect(this.optionRow(scope)).toHaveAttribute('aria-selected', 'true');
    }

    /**
     * The option's explanation comes up as a tooltip on hover (FR-022). Asserted on a stable
     * phrase of the English copy rather than the whole string, so rewording the rest of the
     * sentence does not break the test.
     */
    async expectOptionExplained(scope: DriveSearchScope, phrase: string) {
        await this.option(scope).hover();
        await expect(this.page.locator('.p-tooltip')).toContainText(phrase, { timeout: 5000 });
    }

    /** The listbox option element that carries the option's span. */
    private optionRow(scope: DriveSearchScope): Locator {
        return this.panel.getByRole('option', { name: SCOPE_LABEL[scope] });
    }
}
