import { NewEditContentFormPage } from '@pages';

import { AssetPickerDialog } from '@components/asset-picker-dialog.component';

import { expect, test } from '../../../fixtures/asset-picker.fixture';
import { ImageField } from '../fields/file-upload-fields/image-field/helpers/image-field';

/**
 * The AssetPicker's left column (#37208): a site selector, a folder search scoped to that site, and
 * a folder tree rooted at a single `All` node.
 *
 * A **second site** is seeded here rather than read from the catalog. The two assertions this suite
 * exists for — that changing site re-scopes everything, and that folder results never cross sites —
 * are unfalsifiable on a single-site install: they would pass while testing nothing. A stock demo
 * install has exactly one site.
 */
const IMAGE_FIELD_VARIABLE = 'image';

test.describe('AssetPicker sidebar — site selector', () => {
    let contentTypeId: string;
    let contentTypeVariable: string;
    let homeSite: string;
    let otherSiteId: string;
    let otherSiteHost: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.assetPickerPayload(testSuffix)
        );
        contentTypeId = contentType.id;
        contentTypeVariable = contentType.variable;

        homeSite = (await apiHelpers.getDefaultSite()).hostname;

        otherSiteHost = `e2e-picker-${testSuffix}.dotcms.com`;
        const created = await apiHelpers.createSite(otherSiteHost);
        otherSiteId = created.identifier;

        // A folder on each site, named distinctly, so "did the tree follow the site?" has a visible
        // answer rather than two empty trees that look alike. The home site also gets a *nested*
        // folder whose name only matches mid-string, which is what proves the search is recursive
        // and matches on contains rather than prefix.
        await apiHelpers.createFolders(homeSite, [
            `/home-only-${testSuffix}`,
            `/zz-parent-${testSuffix}/deep-nested-${testSuffix}`
        ]);
        await apiHelpers.createFolders(otherSiteHost, [`/other-only-${testSuffix}`]);
    });

    test.afterEach(async ({ apiHelpers }) => {
        await apiHelpers.deleteContentType(contentTypeId);
        // A leaked site stays in every later run's selector and silently changes what it shows.
        await apiHelpers.deleteSite(otherSiteId);
    });

    test('names the site being browsed, above the folder search @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(adminPage, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(adminPage);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        await picker.expectBrowsingSite(homeSite);
        // The globe from the design. Asserted here rather than in Jest: it renders inside
        // `p-select`'s closed label, which the unit harness does not paint at all.
        await expect(picker.siteSelector.getByTestId('dot-site-icon')).toBeVisible();
        await expect(picker.folderSearch).toBeVisible();
        await expect(picker.folderTree).toBeVisible();
    });

    test('its dropdown lists sites only — never folders', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(adminPage, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(adminPage);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        await picker.openSiteSelector();
        await picker.filterSites('e2e-picker');

        await expect(picker.siteOptions.filter({ hasText: otherSiteHost })).toHaveCount(1);
        // The folders seeded above must not leak into a *site* list. This is the regression the
        // whole split exists to prevent: one control used to search sites and folders at once.
        await expect(picker.siteOptions.filter({ hasText: 'home-only' })).toHaveCount(0);
        await expect(picker.siteOptions.filter({ hasText: 'other-only' })).toHaveCount(0);
    });

    test('the tree has a single root that is not a site row @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(adminPage, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(adminPage);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        // Exactly one root, and the seeded folder hanging off it — sites are no longer roots.
        await expect(picker.folderTree.locator('.p-tree-root > .p-tree-node')).toHaveCount(1);
        await expect(picker.treeNodes.filter({ hasText: `home-only-${testSuffix}` })).toHaveCount(
            1
        );

        // The root must not repeat the hostname the selector already shows. Asserted as an absence
        // rather than as the literal "All", because that label comes from a message key the running
        // dotCMS only serves once its Language.properties has been rebuilt.
        await expect(picker.treeNodes.filter({ hasText: homeSite })).toHaveCount(0);
    });

    test('changing site re-roots the tree and re-scopes the asset list @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(adminPage, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(adminPage);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        await expect(picker.treeNodes.filter({ hasText: `home-only-${testSuffix}` })).toHaveCount(
            1
        );

        await picker.chooseSite(otherSiteHost);

        await expect(picker.treeNodes.filter({ hasText: `other-only-${testSuffix}` })).toHaveCount(
            1
        );
        await expect(picker.treeNodes.filter({ hasText: `home-only-${testSuffix}` })).toHaveCount(
            0
        );
    });

    test('finds a nested folder by a mid-name fragment, scoped to this site @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(adminPage, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(adminPage);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        // `nested` sits in the middle of `deep-nested-<suffix>`, one level down. A prefix match or a
        // non-recursive query would both miss it.
        await picker.searchFolders('nested');

        await expect(picker.searchResults).toBeVisible();
        await expect(picker.folderTree).toBeHidden();
        await expect(
            picker.folderResultRows.filter({ hasText: `deep-nested-${testSuffix}` })
        ).toHaveCount(1);

        // Scoped to this site: the other site's folder must not appear for a term that matches it.
        await picker.clearFolderSearch();
        await picker.searchFolders('other-only');
        await expect(picker.folderResultRows).toHaveCount(0);
    });

    test('picking a result keeps the list open, and clearing returns to the tree', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(adminPage, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(adminPage);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        await picker.searchFolders('nested');
        const row = picker.folderResultRows.filter({ hasText: `deep-nested-${testSuffix}` });
        await row.click();

        // The whole point of keeping it open: jump to another match without retyping.
        await expect(picker.searchResults).toBeVisible();
        await expect(row).toHaveAttribute('aria-current', 'true');

        await picker.clearFolderSearch();
        await expect(picker.folderTree).toBeVisible();
        await expect(picker.searchResults).toBeHidden();
    });
});
