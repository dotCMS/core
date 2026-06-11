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

        await expect(listing.frame.locator('#queryResults')).toBeVisible();
    });

    test.skip('API link in query modal opens new tab', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();
        await listing.openQueryModal();

        const newTabPromise = page.waitForEvent('popup');
        await listing.frame.getByText('API', { exact: true }).click();
        const newTab = await newTabPromise;

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
    let contentlet: Contentlet | null = null;
    let seededTitle = '';

    test.beforeEach(async ({ request }) => {
        const suffix = Date.now();
        seededTitle = `E2E Search Content ${suffix}`;

        contentType = await createFakeContentType(request, {
            name: `E2ESearchType${suffix}`
        });

        contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title: seededTitle
        });
    });

    test.afterEach(async ({ request }) => {
        if (contentlet) {
            await deleteContentlets(request, [contentlet.identifier]);
            contentlet = null;
        }

        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('search filter returns matching content @critical', async ({ page }) => {
        const listing = new ContentListingHelper(page);
        await listing.goto();

        // Wait for the results table to have at least one row before filtering
        await listing.resultsTable
            .locator('tbody tr')
            .first()
            .waitFor({ state: 'visible', timeout: 15000 });

        await listing.searchFor(seededTitle);

        // After filtering, the seeded item should appear in the results
        await expect(listing.frame.getByRole('link', { name: seededTitle }).first()).toBeVisible({
            timeout: 15000
        });
    });
});
