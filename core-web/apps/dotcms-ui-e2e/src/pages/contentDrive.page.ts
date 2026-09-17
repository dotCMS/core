import { expect, type Locator, type Page, type Request } from '@playwright/test';
import { Portlet } from '@utils/portlets';

/**
 * Page object for the Content Drive portlet shell.
 */
/**
 * How long an assertion may wait for something a *queued job* has to produce.
 *
 * Generous on purpose. A batch is asynchronous end to end — request, job, completion signal — and
 * the suite runs two workers against one instance, so two tests uploading at once queue behind each
 * other and every outcome arrives later than it would alone. Measured: the same eleven tests take
 * 2.9 minutes with one worker and 13.1 with two, and three of them failed at 30 and 60 seconds
 * purely from that contention.
 *
 * Raising it does not slow a passing run, because every one of these waits returns the moment its
 * condition holds. It does decide how long a *failing* one takes to admit it, and that cost is
 * real: at 120s a degraded instance turned an eleven-test run into 1.6 hours, where the same run
 * takes under a minute healthy. 60s is the compromise — comfortably more than a warm instance
 * needs, and short enough that a sick one is reported rather than waited out.
 */
const OUTCOME_TIMEOUT = 60000;

export class ContentDrivePage {
    readonly toolbar: Locator;
    readonly treeSelector: Locator;
    readonly sidebar: Locator;
    readonly currentSiteHostname: Locator;
    readonly listTitles: Locator;
    readonly treeNodeLabels: Locator;
    readonly allSiteContentRow: Locator;
    readonly systemHostRow: Locator;
    readonly searchField: Locator;
    readonly statusToast: Locator;
    readonly statusToastSummary: Locator;
    readonly scopeBar: Locator;
    readonly scopeBarSummary: Locator;
    readonly scopeBarToggle: Locator;
    readonly toasts: Locator;

    constructor(private page: Page) {
        this.toolbar = page.getByTestId('toolbar');
        this.treeSelector = page.getByTestId('tree-selector');
        this.sidebar = page.getByTestId('sidebar');
        // The text field itself, not its `dot-content-drive-search-input` host: the shortcut under
        // test focuses the input, and only the input can be asserted focused.
        this.searchField = page.getByTestId('search-input-field');
        // The site is named by the tree's own root row rather than a header above it, so the
        // hostname is that row's label, and that row is the first one.
        this.currentSiteHostname = this.sidebar.getByTestId('tree-node-label').first();
        this.listTitles = page.getByTestId('item-title-text');
        this.treeNodeLabels = this.sidebar.getByTestId('tree-node-label');
        // The two entries that are not part of the hierarchy. They carry their own testids and no
        // `tree-node-label`, which is why `currentSiteHostname` above still finds the site row.
        this.allSiteContentRow = this.sidebar.getByTestId('all-site-content');
        this.systemHostRow = this.sidebar.getByTestId('system-host');
        // A run in flight is reported by the status toast, not by the toolbar. The toolbar used to
        // draw an indicator at the end of the filter row (`action-execution-indicator`); that markup
        // is gone, so anything still looking for it is asserting on a testid that cannot appear.
        this.statusToast = page.getByTestId('dot-status-toast');
        this.statusToastSummary = page.getByTestId('status-toast-summary');
        // The bar above the listing: what is being shown, and the one control that changes it.
        // Its slot is always in the DOM and opens by height, so visibility is the question to ask
        // rather than presence.
        this.scopeBar = page.getByTestId('scope-bar');
        this.scopeBarSummary = page.getByTestId('scope-bar-summary');
        this.scopeBarToggle = page.getByTestId('scope-bar-toggle');
        this.toasts = page.locator('.p-toast-message');
    }

    /**
     * Navigates to Content Drive and waits for the sidebar tree to render nodes.
     */
    async goTo() {
        const folderSearch = this.page
            .waitForResponse(
                (r) => r.url().includes('/api/v1/folder/search') && r.status() === 200,
                { timeout: 30000 }
            )
            .catch(() => null);

        await this.page.goto(Portlet.ContentDrive);
        await this.page.waitForLoadState('domcontentloaded');
        await expect(this.toolbar).toBeVisible({ timeout: 20000 });
        await expect(this.treeSelector).toBeVisible({ timeout: 20000 });
        await folderSearch;
        // Wait for tree nodes via projected labels (more specific than the p-tree host test id).
        await expect(this.treeNodeLabels.first()).toBeVisible({ timeout: 20000 });
        // Waited on after the tree, not before it: the hostname is the root node's label, so it only
        // exists once the folder tree has rendered.
        await expect(this.currentSiteHostname).toBeVisible({ timeout: 20000 });
    }

    async expectSiteHostname(hostname: string) {
        await expect(this.currentSiteHostname).toContainText(hostname, {
            timeout: 15000,
            ignoreCase: true
        });
    }

    /**
     * Clicks the tree toggler, which lives in the toolbar in both states. The sidebar used to carry
     * a second copy for the expanded state; it no longer does.
     */
    async toggleTree() {
        await this.toolbar.getByTestId('tree-toggler').click();
    }

    async expectTreeExpanded() {
        await expect
            .poll(async () => (await this.treeSelector.boundingBox())?.width ?? 0, {
                timeout: 10000
            })
            .toBeGreaterThan(100);
        await expect(this.treeNodeLabels.first()).toBeVisible({ timeout: 10000 });
    }

    async expectTreeCollapsed() {
        await expect
            .poll(async () => (await this.treeSelector.boundingBox())?.width ?? 0, {
                timeout: 10000
            })
            .toBeLessThan(10);
    }

    /**
     * Sorts the listing by its first sortable column and waits for the results it re-requests.
     *
     * Which column is immaterial — what matters is that a sort happened, because the table clears
     * its own keyboard anchor as part of one, and that is the state `expectSingleTabStop()` checks
     * afterwards. Picked by position rather than by header text so the assertion does not depend on
     * a translated label.
     *
     * The listing is lazy, so this is a round trip rather than a client-side reorder.
     */
    async sortByFirstColumn() {
        const search = this.page
            .waitForResponse(
                (r) => r.url().includes('/api/v1/drive/search') && r.status() === 200,
                {
                    timeout: 20000
                }
            )
            .catch(() => null);

        await this.page.getByTestId('header-column-sortable').first().click();
        await search;
    }

    async expectListContainsTitle(title: string) {
        await expect(this.listTitles.filter({ hasText: title }).first()).toBeVisible({
            timeout: 20000
        });
    }

    /** Navigates into a folder by clicking its row in the tree. */
    async openFolder(name: string) {
        await this.treeNodeLabels.filter({ hasText: name }).first().click();
        await expect(this.page.getByTestId('dropzone')).toBeVisible({ timeout: 20000 });
    }

    /**
     * Chooses several files through the real file chooser.
     *
     * The only honest proof that `multiple` is on the input: `setInputFiles` with more than one
     * path throws on an input without it, so this fails as a browser error rather than as a
     * missing row.
     */
    async chooseFilesForUpload(names: string[], baseType = 'DOTASSET') {
        const chooser = this.page.waitForEvent('filechooser');

        await this.toolbar.getByTestId('upload-button').click();

        await this.answerUploadTypeIfAsked(baseType);

        await (await chooser).setFiles(names.map((name) => filePayload(name)));
    }

    /**
     * Waits for an uploaded file to reach the listing, forcing fresh loads instead of trusting a
     * push to arrive inside one timeout.
     *
     * The grid refreshes when the run's completion signal lands, so a plain `expect(...)` on the row
     * is really a bet on how quickly the job finishes *and* the signal is delivered. That bet loses
     * in CI, where the same assertion passed against a dev instance and timed out at twenty seconds
     * on the runner — the files were on their way, nothing was watching for them long enough.
     *
     * Reloading is what makes it deterministic: each attempt refetches the folder, so the test
     * depends on the files existing rather than on a message arriving in time.
     */
    async expectUploadedTitle(folderName: string, title: string, timeoutMs = 90000) {
        const row = () => this.listTitles.filter({ hasText: title }).first();
        const deadline = Date.now() + timeoutMs;

        while (Date.now() < deadline) {
            const appeared = await row()
                .waitFor({ state: 'visible', timeout: 15000 })
                .then(() => true)
                .catch(() => false);

            if (appeared) {
                return;
            }

            await this.page.reload();
            await this.page.waitForLoadState('domcontentloaded');
            await this.openFolder(folderName);
        }

        // Left to Playwright, so the failure reads with its usual detail rather than as a bare throw.
        await expect(row()).toBeVisible({ timeout: 15000 });
    }

    /**
     * How many rows in the listing carry this exact title.
     *
     * The assertion a duplicate-resubmission message cannot make for itself: "nothing was
     * duplicated" is a claim about the folder, and only counting the rows tests it. A copy
     * assertion alone would pass just as happily over a folder holding two of everything.
     */
    async expectTitleCount(title: string, count: number) {
        await expect(this.listTitles.filter({ hasText: title })).toHaveCount(count, {
            timeout: OUTCOME_TIMEOUT
        });
    }

    /**
     * Chooses files and waits for the server to accept the batch.
     *
     * For tests about what happens *after* acceptance. Waiting on the copy instead would couple
     * them to the wording of a message another test already owns, and waiting on rows would be
     * waiting for the wrong thing: acceptance means queued, not created.
     *
     * The response wait is armed before the click that causes it, which is the only order that
     * works.
     */
    async chooseFilesAndAwaitAcceptance(names: string[]) {
        const accepted = this.page.waitForResponse(
            (response) => response.url().includes('/_bulkupload'),
            { timeout: 60000 }
        );

        await this.chooseFilesForUpload(names);

        await accepted;
    }

    /**
     * Answers the upload-type dialog, when the target folder has not already decided.
     *
     * Both routes in need this, which is why it is not inlined in either: a folder that pins a
     * default base type never shows the dialog, and one that does not shows it whether the files
     * arrived through the chooser or through a drop. The drop route missed it, and the batch then
     * waited behind a dialog nobody answered while the test waited for rows that could not appear.
     */
    async answerUploadTypeIfAsked(baseType = 'DOTASSET') {
        const option = this.page.getByTestId(`upload-selector-option-${baseType}`);

        await option
            .waitFor({ state: 'visible', timeout: 5000 })
            .then(() => option.click())
            .catch(() => undefined);
    }

    /**
     * Drops several files onto the listing.
     *
     * Synthesises the `DataTransfer` inside the page: this exercises the same handler a real drag
     * reaches, but it is not a drag from the desktop, which no browser automation can perform.
     */
    async dropFilesOnList(names: string[], baseType = 'DOTASSET') {
        const dropzone = this.page.getByTestId('dropzone');

        await dropzone.dispatchEvent('dragenter');
        await dropzone.dispatchEvent('drop', {
            dataTransfer: await this.page.evaluateHandle((fileNames) => {
                const transfer = new DataTransfer();
                fileNames.forEach((name) =>
                    transfer.items.add(new File([new Uint8Array(4)], name, { type: 'image/png' }))
                );

                return transfer;
            }, names)
        });

        // The files are already chosen here, so the dialog comes *after* the drop rather than
        // before the chooser.
        await this.answerUploadTypeIfAsked(baseType);
    }

    /**
     * Chooses `count` generated files, for the batch sizes a fixture file cannot express.
     *
     * The refusal for too many files starts above `CONTENT_BULK_UPLOAD_MAX_FILES`, which defaults
     * to 100, so proving it means actually choosing 101 of them.
     */
    async chooseGeneratedFilesForUpload(count: number, prefix: string) {
        await this.chooseFilesForUpload(
            Array.from({ length: count }, (_, index) => `${prefix}-${index}.png`)
        );
    }

    /**
     * The handle has arrived: the author has been told the batch is theirs to leave, and the
     * indicator is still reporting it.
     *
     * Both halves matter and they fail for different reasons. Without the message the author never
     * learns the rules changed; without the indicator the batch looks finished when it is not.
     */
    async expectHandedToBackground() {
        // One surface says both halves now. The status toast carries the in-flight wording, which
        // for a backgrounded batch is the sentence that tells the author the batch is theirs to
        // leave — so its presence is the indicator, and there is no second element to check.
        // It used to be two: a wide advisory toast plus the toolbar's indicator, which meant a
        // backgrounded upload announced itself twice.
        await expect(this.statusToastSummary.filter({ hasText: 'in the background' })).toBeVisible({
            timeout: OUTCOME_TIMEOUT
        });
    }

    /** What the status toast is saying right now, if anything. */
    async expectStatusToastContaining(text: string) {
        await expect(this.statusToastSummary.filter({ hasText: text }).first()).toBeVisible({
            timeout: OUTCOME_TIMEOUT
        });
    }

    /**
     * The status toast has stopped reporting.
     *
     * A run that never clears its toast leaves the portlet claiming work is in flight forever, and
     * the toast is sticky precisely so it cannot time itself out -- which makes it the store's job
     * to end it, and therefore worth asserting.
     */
    async expectStatusToastGone() {
        await expect(this.statusToastSummary).toHaveCount(0, { timeout: OUTCOME_TIMEOUT });
    }

    /**
     * Whether the status toast is what a click would land on at its own centre.
     *
     * Asked of the browser's hit-testing rather than by clicking a control underneath. The first
     * version of this clicked the listing's rows-per-page box on the claim that it is "always
     * enabled"; it is not -- an empty folder disables it, and a test that seeds its own folder
     * always starts empty, so the check failed on the control rather than on the toast.
     *
     * `elementFromPoint` asks the question directly and needs nothing beneath the toast at all.
     */
    async statusToastTakesClicksAtItsCentre(): Promise<boolean> {
        const box = await this.statusToast.boundingBox();

        if (!box) {
            throw new Error('no status toast on screen to test');
        }

        return this.page.evaluate(
            ([x, y]) =>
                !!document.elementFromPoint(x, y)?.closest('[data-testid="dot-status-toast"]'),
            [box.x + box.width / 2, box.y + box.height / 2]
        );
    }

    /** Whether the scope bar is open, which is a question about height rather than presence. */
    async scopeBarIsOpen(): Promise<boolean> {
        const slot = this.page.getByTestId('scope-bar-slot');
        const box = await slot.boundingBox();

        return (box?.height ?? 0) > 0;
    }

    /** Flips the System Host toggle and waits for the listing it re-requests. */
    async toggleSystemHostInScopeBar() {
        const listing = this.page.waitForResponse(
            (response) => response.url().includes('/v1/drive/search') && response.ok()
        );
        await this.scopeBarToggle.click();
        await listing;
    }

    /** Opens the New menu and returns the labels it offers. */
    async openNewMenu(): Promise<string[]> {
        await this.toolbar.getByTestId('add-new-button').click();
        const items = this.page.getByRole('menuitem');
        await expect(items.first()).toBeVisible({ timeout: 10000 });

        return items.allInnerTexts();
    }

    /**
     * The path the folder dialog says a new folder will land on.
     *
     * Read rather than asserted here because the wrong value is not a missing element: the builder
     * used to paste a location that is not a folder path straight after the hostname, so the field
     * was populated and confidently wrong.
     */
    async folderDialogPath(): Promise<string> {
        const path = this.page.getByTestId('folder-path-preview');
        await expect(path).toBeVisible({ timeout: 10000 });

        return (await path.innerText()).trim();
    }

    /** A message the author can read, whatever severity it arrived with. */
    /**
     * Runs `body` and fails if any batch was submitted while it did.
     *
     * The evidence that a refusal happened *in front of* the server rather than behind it. The copy
     * alone cannot tell the two apart, and which side refused is the whole point of the client
     * reading the advertised ceiling: an author refused before the upload has waited for nothing.
     */
    async expectNothingUploadedWhile(body: () => Promise<void>) {
        const submissions: string[] = [];
        const record = (request: Request) => {
            if (request.url().includes('/_bulkupload')) {
                submissions.push(request.url());
            }
        };

        this.page.on('request', record);

        try {
            await body();
        } finally {
            this.page.off('request', record);
        }

        expect(
            submissions,
            'the batch reached the server, so it was not refused before the upload'
        ).toEqual([]);
    }

    async expectToastContaining(text: string) {
        await expect(this.toasts.filter({ hasText: text }).first()).toBeVisible({
            timeout: OUTCOME_TIMEOUT
        });
    }

    /**
     * The outcome for a batch that has finished server-side.
     *
     * Waits on the message rather than on the listing: the run reports through a pushed signal
     * that arrives when the job resolves, and a listing that has not refreshed yet is not evidence
     * either way.
     */
    async expectOutcomeContaining(text: string) {
        await this.expectToastContaining(text);
    }

    /**
     * Whether the batch's outcome reached the author who left the portlet.
     *
     * A different surface answers this than the one inside the portlet: the toast belongs to a
     * component that is destroyed on navigation, so what survives is the durable notification, and
     * the bell is where it lands.
     */
    async expectNotificationContaining(text: string) {
        // The panel renders each notification's *message*, never its title, so asserting on
        // "Upload Finished" matched nothing while the outcome was sitting right there. Cost a whole
        // run to learn.
        await this.page.locator('.pi-bell').first().click();

        await expect(
            this.page
                .locator('#dot-toolbar-notifications-content')
                .filter({ hasText: text })
                .first()
        ).toBeVisible({ timeout: OUTCOME_TIMEOUT });
    }

    /**
     * The warning that only one file will be uploaded must be gone.
     *
     * Asserted on the toast's severity rather than on its wording, so it keeps holding when the
     * copy is reworded, and fails if the old message is merely reworded rather than removed.
     */
    async expectNoSingleFileWarning() {
        await expect(this.page.locator('.p-toast-message-warn')).toHaveCount(0);
    }

    /** Selects the All Site Content entry and waits for the listing it triggers. */
    async selectAllSiteContent() {
        await this.selectSidebarEntry(this.allSiteContentRow);
    }

    /** Selects the System Host entry and waits for the listing it triggers. */
    async selectSystemHost() {
        await this.selectSidebarEntry(this.systemHostRow);
    }

    /**
     * Clicks a sidebar entry and waits for the listing request the click sets off.
     *
     * Armed before the click, not after: the response can land first, and then a wait registered
     * afterwards never resolves.
     */
    private async selectSidebarEntry(row: Locator) {
        // Clicking the entry you are already on changes no location, so the store re-requests
        // nothing and a wait for the listing never resolves -- the test then dies on its own
        // timeout with "Page closed", which says nothing about the entry. The drive lands on all
        // site content, so this is the ordinary case for a test that starts there.
        if ((await row.getAttribute('aria-current')) === 'true') {
            return;
        }

        const listing = this.page.waitForResponse(
            (response) => response.url().includes('/v1/drive/search') && response.ok()
        );
        await row.click();
        await listing;
    }

    /**
     * Asserts which sidebar entry reads as the current one.
     *
     * `aria-current` rather than a class: the rows announce selection to assistive tech through
     * it, so asserting on it checks the thing that actually has to be right.
     */
    async expectSelectedEntry(entry: 'all' | 'system-host' | 'neither') {
        const row = entry === 'system-host' ? this.systemHostRow : this.allSiteContentRow;
        await expect(row).toHaveAttribute('aria-current', entry === 'neither' ? /^$/ : 'true', {
            timeout: entry === 'neither' ? 2000 : undefined
        });
    }

    /** Whether an entry currently announces itself as the selected one. */
    async isEntrySelected(entry: 'all' | 'system-host') {
        const row = entry === 'system-host' ? this.systemHostRow : this.allSiteContentRow;

        return (await row.getAttribute('aria-current')) === 'true';
    }
}

/** A tiny in-memory PNG, so the tests carry no fixture files. */
function filePayload(name: string) {
    return { name, mimeType: 'image/png', buffer: Buffer.from([0, 0, 0, 0]) };
}
