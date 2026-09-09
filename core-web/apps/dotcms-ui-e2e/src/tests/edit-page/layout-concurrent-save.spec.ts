import { TemplateBuilderPage } from '@pages';
import { expect, test } from '@playwright/test';
import { createPage, Page as PageContentlet } from '@requests/pages';
import { createTemplate, deleteTemplate, getTemplate } from '@requests/templates';

/**
 * Regression coverage for dotCMS/core#36197 — the UVE Layout tab lost container/content
 * data because the canvas was never locked while a layout save was in flight, and the
 * debounced auto-save and the force-save-on-leave path could both send a layout POST for
 * the same page at the same time.
 *
 * Instead of racing real timers/network (flaky), this holds the first save's POST open via
 * route interception so the "edit while a save is in flight" window is deterministic.
 */

const SYSTEM_CONTAINER = 'SYSTEM_CONTAINER';

// TemplateResource requires `theme` to resolve to a real Folder via FolderAPI.find
// (dotCMS/.../rest/api/v1/template/TemplateResource.java, ~line 587 — "theme must be a
// folder identifier"). Theme.SYSTEM_THEME is a synthetic, non-persisted sentinel (never a
// real folder row) and would fail this check. FolderAPI.SYSTEM_FOLDER, in contrast, is a
// real folder row that dotCMS actively keeps seeded/repaired on every instance
// (FolderAPIImpl's folderIdsNeedFixing/fixFolderIds upgrade check) — safe to use here
// without creating any fixture folder.
const THEME = 'SYSTEM_FOLDER';

let templateIdentifier: string;
let pageContentlet: PageContentlet;

test.beforeEach(async ({ request }) => {
    const suffix = Date.now();

    const template = await createTemplate(request, {
        title: `e2e concurrent-save template ${suffix}`,
        friendlyName: 'e2e concurrent-save template',
        drawed: true,
        body: 'placeholder',
        theme: THEME,
        image: '',
        layout: {
            width: '100%',
            title: `e2e concurrent-save template ${suffix}`,
            header: true,
            footer: true,
            // GridStack uses a 12-column grid: `width` is a 1-12 column span and
            // `leftOffset` is the 1-based starting column (gridstack-utils.ts maps
            // `w: col.width` / `x: col.leftOffset - 1` directly, uncapped). A 50/100-style
            // percentage value here (as an earlier draft of this fixture used) exceeds the
            // 12-column max and made both columns collapse onto the same gs-w="12"
            // position, which is what caused the "element intercepts pointer events"
            // failure on the first CI run — the two boxes were literally stacked on top of
            // each other. width: 6 + leftOffset: 1/7 is a real 50/50 split (columns 1-6
            // and 7-12).
            body: {
                rows: [
                    {
                        columns: [
                            {
                                containers: [{ identifier: SYSTEM_CONTAINER, uuid: '1' }],
                                width: 6,
                                leftOffset: 1
                            },
                            {
                                containers: [{ identifier: SYSTEM_CONTAINER, uuid: '2' }],
                                width: 6,
                                leftOffset: 7
                            }
                        ]
                    }
                ]
            },
            sidebar: { containers: [], location: '', width: '' }
        }
    });

    templateIdentifier = template.identifier;

    const title = `e2e-concurrent-save-${suffix}`;
    pageContentlet = await createPage(request, {
        title,
        url: title,
        friendlyName: title,
        template: templateIdentifier,
        contentType: 'htmlpageasset',
        cachettl: 0
    });
});

test.afterEach(async ({ request }) => {
    if (templateIdentifier) {
        await deleteTemplate(request, [templateIdentifier]);
    }
});

test('editing during an in-flight layout save does not lose container content @critical', async ({
    page,
    request
}) => {
    const templateBuilder = new TemplateBuilderPage(page);

    let layoutPostCount = 0;
    let releaseFirstSave: () => void = () => undefined;
    const firstSaveHeld = new Promise<void>((resolve) => {
        releaseFirstSave = resolve;
    });

    // Hold the FIRST layout save open so we control exactly when it resolves, instead of
    // racing the real 5s debounce + network timing.
    await page.route('**/api/v1/page/*/layout', async (route) => {
        layoutPostCount += 1;

        if (layoutPostCount === 1) {
            await firstSaveHeld;
        }

        await route.continue();
    });

    await templateBuilder.navigateTo(pageContentlet.url);
    await expect(templateBuilder.getBox(1)).toBeVisible();

    // Real edit via a plain click (no GridStack drag needed): delete the container in
    // box 0. This starts the 5s debounce → eventually the first layout POST.
    await templateBuilder.deleteContainerFromBox(0);

    // The overlay appears the instant #layoutSaveInFlight flips true, i.e. exactly when
    // the POST is dispatched — a real, observable signal instead of an arbitrary wait.
    await expect(templateBuilder.getOverlay()).toBeVisible({ timeout: 6000 });
    expect(layoutPostCount).toBe(1);

    // Try to leave the Layout tab while save #1 is still held in flight. This exercises
    // the force-save-on-leave path (AC3) — before the fix, this fired a second, concurrent
    // POST for the same page. If it had, that POST would already have hit the route
    // handler above (synchronously, from the click) and bumped layoutPostCount before we
    // ever get here.
    await templateBuilder.goToContentTab();

    // Let the held save complete and everything settle. toHaveURL's own polling wait
    // gives plenty of real time for a (buggy) second POST to have been intercepted
    // before the count assertion below, without an arbitrary waitForTimeout.
    releaseFirstSave();
    await expect(page).toHaveURL(/edit-page\/content/);

    expect(layoutPostCount).toBe(1);

    // Data integrity: box 0's container was intentionally removed; box 1's must survive
    // untouched — the regression this issue is about is *collateral* container loss.
    const savedTemplate = await getTemplate(request, templateIdentifier);
    const rows = (
        savedTemplate.layout as {
            body: { rows: { columns: { containers: unknown[] }[] }[] };
        }
    ).body.rows;

    expect(rows[0].columns[0].containers).toHaveLength(0);
    expect(rows[0].columns[1].containers).toHaveLength(1);
});
