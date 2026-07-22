import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import { CARDINALITY, expect, test } from '../../../../fixtures/relationship.fixture';

// ─── Status & Locale Chips (issue #36155) ───────────────────────
//
// Related rows render the Locales value as a `relationship-locale-tag`
// p-tag chip and the Status as a `status-tag` chip (from
// dot-contentlet-status-badge). The status chip carries the PrimeNG
// severity class matching its status. Content is created via
// fire/PUBLISH (default English locale) so its status is deterministically
// Published -> severity `success` -> class `p-tag-success`.

test.describe('Status & Locale Chips', () => {
    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Chips_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Chips_${testSuffix}`,
                'E2E_Blog_Chips',
                'E2EBlogChips',
                authorTypeVariable,
                'authors',
                CARDINALITY.ONE_TO_MANY
            )
        );
        blogTypeVariable = blogType.variable;

        // Published English contentlet -> deterministic status for the color assertion.
        await apiHelpers.createContentlet(authorTypeVariable, {
            title: `Author Chips ${testSuffix}`,
            bio: 'Bio for chip author'
        });
    });

    test('related row renders locale and status chips with correct severity @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        const dialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();
        // ONE_TO_MANY renders checkboxes (multi-select), not radio buttons.
        await dialog.selectItem(0);
        await dialog.clickApply();
        await dialog.expectClosed();

        await relationshipField.expectRowCount(1);

        // Locales cell renders a p-tag chip (not plain text) containing the locale label.
        const localeTag = relationshipField.localeTag(0);
        await expect(localeTag).toBeVisible();
        await expect(localeTag).toHaveClass(/p-tag/);
        await expect(localeTag).toContainText(/English/i);

        // Status cell renders a status-tag chip with the Published severity class.
        const statusTag = relationshipField.statusTag(0);
        await expect(statusTag).toBeVisible();
        await expect(statusTag).toHaveClass(/p-tag/);
        // Robust color assertion: PrimeNG severity CSS class, not computed rgb().
        await expect(statusTag).toHaveClass(/p-tag-success/);
    });
});
