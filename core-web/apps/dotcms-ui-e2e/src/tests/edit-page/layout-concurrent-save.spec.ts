import { TemplateBuilderPage } from '@pages';
import { expect, test } from '@playwright/test';
import { createPage, Page as PageContentlet } from '@requests/pages';
import { createTemplate, deleteTemplate, getTemplate } from '@requests/templates';

/**
 * Regression coverage for dotCMS/core#36197 — the UVE Layout tab lost container/content
 * data because the canvas was never locked while a layout save was in flight.
 *
 * This covers the AC1/AC2 promise end-to-end in a real browser: a real edit starts a real
 * 5s debounce, the canvas locks for the entire save+reload cycle (verified by holding the
 * POST open via route interception, deterministic rather than racing real timing), and no
 * container is collaterally lost.
 *
 * The AC3 guarantee — the debounced auto-save and the force-save-on-leave path can never
 * both have a POST in flight for the same page — is covered instead by the 3 unit tests in
 * edit-ema-layout.component.spec.ts. That's deliberate, not a gap: driving an in-app
 * CanDeactivate-guard-blocked navigation from Playwright (via a real click, `{ force: true
 * }`, and `dispatchEvent` — all three tried) reliably hung the browser context past any
 * timeout budget, while the same race is trivial to simulate deterministically with fake
 * timers in Jest.
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
    //
    // Trailing `**` is required: DotPageLayoutService.save() appends `?variantName=...`
    // whenever a variant is active (this test environment defaults to `DEFAULT`), and a
    // glob without a trailing wildcard must match the URL exactly up to its end — so
    // `**/layout` (no trailing `**`) never matched `.../layout?variantName=DEFAULT` and
    // silently missed every request.
    await page.route('**/api/v1/page/*/layout**', async (route) => {
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

    // Check the overlay first — confirms the deletion click really flipped
    // #layoutSaveInFlight (i.e. a save was genuinely triggered), independent of whether
    // our route pattern below actually matches the request.
    await expect(templateBuilder.getOverlay()).toBeVisible({ timeout: 6000 });

    // Poll layoutPostCount rather than asserting it immediately: the DOM update above and
    // the network request reaching Playwright's routing layer travel through different
    // channels (rendering vs. CDP network events) and aren't strictly ordered relative to
    // each other.
    await expect.poll(() => layoutPostCount, { timeout: 6000 }).toBe(1);

    // AC2: the lock must span the whole save + reload cycle, not just the POST — release
    // the held response and confirm the overlay only comes down once the page has actually
    // re-hydrated, not the instant the response arrives. Generous timeout: pageReload()
    // does a real re-fetch + re-render, and this CI environment has repeatedly shown it
    // can take well over Playwright's 5s assertion default under load.
    releaseFirstSave();
    await expect(templateBuilder.getOverlay()).toBeHidden({ timeout: 20_000 });

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
