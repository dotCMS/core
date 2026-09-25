import { faker } from '@faker-js/faker';
import { UveEditorPage } from '@pages';
import { expect, test } from '@playwright/test';
import { actionsPageWorkflow, createPage, Page } from '@requests/pages';

/**
 * `FEATURE_FLAG_EXPERIMENTS_PORTLET` off — the shipped default (#37478, US3, FR-043, SC-009).
 *
 * **Nothing is flipped here, and that is the point.** The flag ships off and is read fail-closed,
 * so a stock instance *is* the flag-off case. These are characterization tests: they pass on the
 * build before this feature and must go on passing after it. They cannot go Red, which is why the
 * plan's Red gate for this story covers only the flag-**on** assertions, which live in the unit
 * suite (`dot-ema-shell.component.spec.ts`).
 *
 * The flag-on end of the switch is not exercised here: turning it on is a server-side
 * configuration change that this harness has no way to make per-test.
 *
 * @see specs/37478-uve-experiments-panel/quickstart.md — V16
 */

let pageContentlet: Page;

test.beforeEach(async ({ request }) => {
    const title = faker.lorem.words(3);
    const url = title.split(' ').join('-');

    pageContentlet = await createPage(request, {
        title,
        url,
        friendlyName: title,
        template: 'SYSTEM_TEMPLATE',
        contentType: 'htmlpageasset',
        cachettl: 0
    });
});

test.afterEach(async ({ request }) => {
    if (pageContentlet) {
        await actionsPageWorkflow(request, pageContentlet.inode, [
            'Unpublish',
            'Archive',
            'Destroy'
        ]);
    }
});

test('Experiments leads to the legacy per-page screens @critical', async ({ page }) => {
    const editor = new UveEditorPage(page);
    await editor.open(`/${pageContentlet.url}`);

    await expect(editor.experimentsNavItem).toBeVisible();
    await editor.experimentsNavItem.click();

    /**
     * The route the item carried before this feature existed, and must still carry. The panel
     * writes nothing to the address, so a URL assertion is what tells the two behaviours apart:
     * a destination navigates, an action does not.
     *
     * The segment after `experiments/` is deliberately not pinned. It is `{pageId}` on an instance
     * with Analytics configured, and `analytic-app-misconfiguration` on one without — this harness
     * is the second kind, as the note at the foot of this file already records. Either way the
     * editor has left `/edit-page/content` for the legacy screen, which is the whole claim. Pinning
     * the page id made this assert the instance's Analytics setup instead.
     */
    await expect(page).toHaveURL(/\/edit-page\/experiments\//);
    await expect(page).not.toHaveURL(/\/edit-page\/content/);
});

test('no Experiments panel exists @critical', async ({ page }) => {
    const editor = new UveEditorPage(page);
    await editor.open(`/${pageContentlet.url}`);

    await editor.experimentsNavItem.click();

    // FR-044: none of the panel's behaviour is reachable or observable with the flag off.
    await expect(page.getByTestId('experiments-panel')).toHaveCount(0);
});

/**
 * FR-025d — the toolbar badge keeps its legacy deep link.
 *
 * Not implemented: the badge only renders while the page has a **running** experiment, and
 * seeding one end to end means creating the experiment, adding a variant, setting a goal and
 * starting it — and a started experiment needs Analytics configured on the instance, which the
 * e2e environment does not guarantee. Covered instead by the unit suite at T074, which asserts
 * the `routerLink` survives with the flag off.
 */
test.fixme('the toolbar badge keeps its legacy deep link', async () => {
    // Needs a running-experiment seeding fixture; see the note above.
});

/**
 * FR-045 — the full portlet stays reachable and unfiltered from the main navigation.
 *
 * Not implemented: the Experiments portlet is **opt-in**. It is declared in `portlet.xml` but no
 * upgrade task adds it to any layout, so on a stock instance `/experiments` redirects to `/start`
 * and the administration menu carries no Experiments entry at all. Asserting this needs a layout
 * seeded for the test user, which this harness has no helper for.
 */
test.fixme('the full portlet is reachable and unfiltered from the main navigation', async () => {
    // Needs a layout-seeding fixture; see the note above.
});
