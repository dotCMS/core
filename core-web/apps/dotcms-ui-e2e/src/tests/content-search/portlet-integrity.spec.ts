import { expect, test } from '@playwright/test';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';

import { ContentListingHelper } from './helpers/content-listing';

// ─── Tests that rely on existing system content ───────────────────

test.describe('Content listing portlet — UI integrity', () => {
    test('List and Card view buttons visible, card view switches layout @smoke', async ({
        page
    }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();

        await expect(listing.listViewButton).toBeVisible();
        await expect(listing.cardViewButton).toBeVisible();

        await listing.cardViewButton.click();
        await listing.frame
            .locator('.hydrated > dot-contentlet-thumbnail > .hydrated')
            .first()
            .waitFor({ state: 'visible', timeout: 10000 });
    });

    test('add content dropdown opens Add New Content option @smoke', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();

        await listing.openAddContentDropdown();

        const addNewOption = listing.frame.locator('.dijitMenuItemLabel', {
            hasText: 'Add New Content'
        });
        await addNewOption.waitFor({ state: 'visible', timeout: 10000 });
        await expect(addNewOption).toBeVisible();
    });

    test('bulk workflow actions button disabled then enabled after checkbox @smoke', async ({
        page
    }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();

        await expect(listing.workflowActionsButton).toBeDisabled();

        await listing.firstRowCheckbox.waitFor({ state: 'visible', timeout: 10000 });
        await listing.firstRowCheckbox.check();
        await expect(listing.workflowActionsButton).toBeEnabled();

        await listing.workflowActionsButton.click();
        await expect(listing.frame.locator('.dijitDialog[role="dialog"]').first()).toBeVisible();
    });

    test('show query modal opens and displays query results @smoke', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();
        await listing.openQueryModal();

        // openQueryModal already waits for #queryResults, this just confirms it
        await expect(listing.frame.locator('#queryResults')).toBeVisible();
    });

    test('API link in query modal opens new tab @smoke', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();
        await listing.openQueryModal();

        // The "API" span calls window.open() inside a Dojo XHR callback — browsers block
        // that as a popup because it is no longer a direct user gesture. Intercept the
        // POST so it resolves instantly, keeping window.open() close enough to the click
        // for Playwright (popup blocking disabled) to capture it reliably.
        await page.route('**/api/content/_search', (route) =>
            route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ contentlets: [], total: 0 })
            })
        );

        const newPagePromise = page.context().waitForEvent('page');
        await listing.queryModalApiLink.click();
        const newTab = await newPagePromise;

        await newTab.waitForLoadState();
        expect(newTab.url()).toBeTruthy();

        await newTab.close();
    });

    test('advanced filter clear button resets all selectors @smoke', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();

        await listing.advancedFilterLink.waitFor({ state: 'visible', timeout: 10000 });
        await listing.advancedFilterLink.click();

        // Select System Workflow
        const workflowBtn = listing.frame.locator(
            '#widget_scheme_id [data-dojo-attach-point="_buttonNode"]'
        );
        await workflowBtn.waitFor({ state: 'visible', timeout: 10000 });
        await workflowBtn.click();
        await listing.frame.getByRole('option', { name: 'System Workflow' }).click();

        // Select step — wait for step dropdown to be populated after workflow selection
        const stepBtn = listing.frame.locator(
            "div[id='widget_step_id'] div[data-dojo-attach-point='_buttonNode']"
        );
        await stepBtn.waitFor({ state: 'visible', timeout: 10000 });
        await stepBtn.click();
        const newOption = listing.frame.getByRole('option', { name: 'New' });
        await newOption.waitFor({ state: 'visible', timeout: 5000 });
        await newOption.click();

        // Select Show = Unpublished
        await listing.frame
            .locator('#widget_showingSelect [data-dojo-attach-point="_buttonNode"]')
            .click();
        await listing.frame.getByRole('option', { name: 'Unpublished' }).click();

        // Clear all filters
        await listing.clearFilterButton.click();

        await expect(listing.frame.locator('input[name="scheme_id_select"]')).toHaveAttribute(
            'value',
            'catchall'
        );
        await expect(listing.frame.locator('input[name="step_id_select"]')).toHaveAttribute(
            'value',
            'catchall'
        );
        await expect(listing.frame.locator('#showingSelect')).toHaveAttribute('value', 'All');
    });

    test('advanced filter hide button collapses the filter @smoke', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();

        await expect(listing.frame.getByRole('button', { name: 'Search' })).toBeVisible();

        await listing.advancedFilterLink.waitFor({ state: 'visible', timeout: 10000 });
        await listing.advancedFilterLink.click();

        await expect(listing.advancedFilterLink).toBeHidden();

        await listing.hideFilterLink.waitFor({ state: 'visible', timeout: 5000 });
        await listing.hideFilterLink.click();

        await expect(listing.advancedFilterLink).toBeVisible();
    });
});

// ─── Search filter — requires seeded contentlet ──────────────────

test.describe('Content listing portlet — search filter', () => {
    let contentType: ContentType | null = null;
    let contentlets: Contentlet[] = [];
    let seededTitle = '';
    let otherTitle = '';
    let contentTypeVariable = '';

    test.beforeEach(async ({ request }) => {
        const suffix = Date.now();
        // One token: the listing search matches a multi-word query on any of its words
        seededTitle = `E2ESearchContent${suffix}`;
        otherTitle = `Unrelated Entry ${suffix}`;

        contentType = await createFakeContentType(request, {
            name: `E2ESearchType${suffix}`
        });
        contentTypeVariable = contentType.variable;

        for (const title of [seededTitle, otherTitle]) {
            contentlets.push(
                await createContentlet(request, { contentType: contentTypeVariable, title })
            );
        }
    });

    test.afterEach(async ({ request }) => {
        if (contentlets.length) {
            await deleteContentlets(
                request,
                contentlets.map((c) => c.identifier)
            );
            contentlets = [];
        }

        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('search filter returns matching content @critical', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        // Pin the listing to the seeded type: unfiltered, it opens on whichever type another
        // test last fetched through the shared session, which may hold no content at all.
        await listing.goto(contentTypeVariable);

        await expect(listing.frame.getByRole('link', { name: otherTitle })).toBeVisible({
            timeout: 15000
        });

        await listing.searchFor(seededTitle);

        // After filtering, only the seeded item should remain
        await expect(listing.frame.getByRole('link', { name: seededTitle }).first()).toBeVisible({
            timeout: 15000
        });
        await expect(listing.frame.getByRole('link', { name: otherTitle })).toBeHidden();
    });
});
