import { expect, test } from '@playwright/test';
import { deleteBundle, pushPublishIntoSeparateBundles } from '@requests/bundles';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';

import { PublishingQueueHelper } from './helpers/publishing-queue';

/**
 * Regression coverage for issue #36861.
 *
 * When the same asset (same operation) is queued in two different bundles rendered on the same
 * Pending page, the asset checkbox id — built from asset + operation only — collides in the dijit
 * widget registry. `dojo/parser` throws, abandons the rest of the parse pass, and every checkbox
 * below the collision point stays an un-upgraded `<input>`. `deleteQueue()` then silently skips
 * them, so Delete becomes a no-op with no error and no server-side log.
 *
 * These tests seed exactly that shape: one contentlet, two bundles, one page.
 */
test.describe('Publishing Queue → Pending — asset queued in multiple bundles', () => {
    let contentType: ContentType | null = null;
    let contentlet: Contentlet | null = null;
    let seededBundleIds: string[] = [];

    test.beforeEach(async ({ request }) => {
        const suffix = Date.now();

        contentType = await createFakeContentType(request, {
            name: `E2EPubQueueType${suffix}`
        });

        contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title: `E2E Pending Duplicate ${suffix}`
        });

        // The defect's precondition: the SAME asset queued in separate bundles on one page.
        //
        // THREE, not two, and that matters. The parser registers the first checkbox, collides on
        // the second and aborts the rest of the pass. With only two bundles the collision is the
        // last thing on the page, so nothing is left un-upgraded and the damage is invisible to
        // a DOM assertion. The third bundle is what renders *below* the collision and never gets
        // upgraded — that is the user-visible breakage this suite has to catch.
        seededBundleIds = await pushPublishIntoSeparateBundles(request, contentlet.identifier, 3);
    });

    test.afterEach(async ({ request }) => {
        // Bundles first, and this is not optional. Deleting the contentlet leaves the bundle and
        // its publishing_queue row behind with a future publish_date, so PublisherQueueJob would
        // eventually attempt a REAL push; and leaked bundles sort ahead of later runs' on the
        // Pending tab (10 per page, ordered by publish_date), eventually pushing a run's own
        // bundles off page one and failing its assertions for an unrelated-looking reason.
        for (const bundleId of seededBundleIds) {
            await deleteBundle(request, bundleId);
        }
        seededBundleIds = [];

        if (contentlet) {
            await deleteContentlets(request, [contentlet.identifier]);
            contentlet = null;
        }

        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('loads with no dojo/parser errors @critical', async ({ page }) => {
        const queue = new PublishingQueueHelper(page);

        // Must be attached before goto() — the parse error fires during the initial render.
        queue.startCollectingConsoleErrors();
        await queue.goto();

        expect(
            queue.dojoParserErrors,
            `Expected no dojo/parser errors, got:\n${queue.dojoParserErrors.join('\n')}`
        ).toEqual([]);
    });

    test('every asset checkbox is upgraded to a dijit widget @critical', async ({ page }) => {
        const queue = new PublishingQueueHelper(page);
        await queue.goto();

        // Any checkbox still rendered as a bare <input> is one the parser never reached.
        const unUpgraded = await queue.unUpgradedCheckboxIds();

        expect(
            unUpgraded,
            `These checkboxes were never upgraded by dijit (parse aborted above them):\n${unUpgraded.join('\n')}`
        ).toEqual([]);
    });

    test('asset checkbox ids are unique and carry their bundle id @critical', async ({ page }) => {
        const queue = new PublishingQueueHelper(page);
        await queue.goto();

        const ids = await queue.assetCheckboxIds();
        const bundleIds = await queue.bundleIds();

        expect(ids.length, 'expected asset rows to be rendered').toBeGreaterThan(0);
        expect(bundleIds.length, 'expected all seeded bundles on one page').toBeGreaterThanOrEqual(
            3
        );

        // AC-003: globally unique. This is the assertion that fails on unfixed code — the same
        // asset in two bundles produces two identical ids.
        expect(new Set(ids).size, `duplicate checkbox ids: ${ids.join(', ')}`).toBe(ids.length);

        // AC-003: each id must be attributable to its owning bundle.
        for (const id of ids) {
            expect(
                bundleIds.some((bundleId) => id.includes(bundleId)),
                `id "${id}" does not contain any rendered bundle id`
            ).toBe(true);
        }
    });

    test('deleting an asset row removes it from that bundle only @critical', async ({ page }) => {
        const queue = new PublishingQueueHelper(page);
        await queue.goto();

        const [bundleA, bundleB] = seededBundleIds;

        // Precondition: the same asset is listed under both bundles.
        expect(await queue.assetIdsForBundle(bundleA)).toContain(contentlet?.identifier);
        expect(await queue.assetIdsForBundle(bundleB)).toContain(contentlet?.identifier);

        await queue.assetCheckboxesForBundle(bundleA).first().check();

        // The delete must name BOTH the asset and its owning bundle. A request that carries the
        // asset alone is the pre-DEC-001 wire format and would clear the asset from every bundle.
        const deleteRequest = page.waitForRequest(
            (request) =>
                request.url().includes('view_publish_queue_list.jsp') &&
                request.url().includes('delete=')
        );
        await queue.deleteButton.click();
        const url = (await deleteRequest).url();

        expect(url, `delete request must identify the bundle: ${url}`).toContain(bundleA);

        // AC-004: bundle A loses the asset, bundle B keeps it.
        await queue.waitForReady();
        expect(await queue.assetIdsForBundle(bundleA)).not.toContain(contentlet?.identifier);
        expect(await queue.assetIdsForBundle(bundleB)).toContain(contentlet?.identifier);
    });

    test('checking a bundle cascades to its asset rows @critical', async ({ page }) => {
        const queue = new PublishingQueueHelper(page);
        await queue.goto();

        const [bundleA] = seededBundleIds;
        const assetBoxes = queue.assetCheckboxesForBundle(bundleA);

        expect(await assetBoxes.count(), 'bundle should have asset rows').toBeGreaterThan(0);

        await queue.bundleCheckbox(bundleA).check();

        // checkAllBundle() both checks AND disables the child rows: a fully selected bundle is
        // deleted through the bundle path, and deleteQueue() deliberately skips disabled nodes so
        // the same rows are not also submitted individually.
        expect(await queue.assetCheckboxStates(bundleA)).toEqual(
            expect.arrayContaining([{ checked: true, disabled: true }])
        );

        await queue.bundleCheckbox(bundleA).uncheck();

        expect(await queue.assetCheckboxStates(bundleA)).toEqual(
            expect.arrayContaining([{ checked: false, disabled: false }])
        );
    });

    test('Delete with nothing selected tells the user @critical', async ({ page }) => {
        const queue = new PublishingQueueHelper(page);
        await queue.goto();

        // Playwright auto-dismisses dialogs, so the handler must be attached before the click or
        // the alert is swallowed and the test sees nothing. The race gives a readable failure
        // instead of a bare 60s test timeout when no dialog fires at all - which is exactly the
        // bug: today Delete reloads the pane silently and says nothing.
        const NO_DIALOG = Symbol('no-dialog');
        const dialogMessage = new Promise<string>((resolve) => {
            page.once('dialog', async (dialog) => {
                const text = dialog.message();
                await dialog.dismiss();
                resolve(text);
            });
        });
        const timeout = new Promise<typeof NO_DIALOG>((resolve) =>
            setTimeout(() => resolve(NO_DIALOG), 10000)
        );

        await queue.deleteButton.click();

        const result = await Promise.race([dialogMessage, timeout]);

        expect(
            result,
            'Delete with an empty selection produced no dialog - the pane just reloaded, which is indistinguishable from success'
        ).not.toBe(NO_DIALOG);

        const message = result as string;

        expect(message.trim(), 'the message must not be empty').not.toBe('');
        // The string has to come from Language.properties via LanguageUtil, not a hardcoded
        // literal - the surrounding portlet localises every user-facing string.
        expect(message, `untranslated language key surfaced to the user: ${message}`).not.toMatch(
            /^[a-z0-9_.]+$/i
        );
    });
});
