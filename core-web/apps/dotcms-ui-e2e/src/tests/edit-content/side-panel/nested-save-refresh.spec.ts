import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';
import { getLegacyFrame } from '@utils/iframe';
import { Portlet } from '@utils/portlets';

import { SidePanel } from './helpers/side-panel';

import { CARDINALITY, expect, test } from '../../../fixtures/relationship.fixture';
import { RelationshipField } from '../fields/relationship-field/helpers/relationship-field';

/**
 * #38303, T174: a save in a panel stacked from inside another refreshes the list under both
 * panels, while they are still open. Here the stacked panel is a Relationship field's "New": the
 * surface opened only the first panel, and the stacked one's save reaches it through the content
 * change notification. Before, that save went only to the field, and the list missed the new
 * content until both panels closed.
 *
 * Opening related content that already exists does not stack a panel: it opens in place, in the
 * same panel, whose saves are the surface's own. Both types use the new editor. Pages has no
 * relationship field on its page types, so it is covered by unit tests only.
 */

const RELATIONSHIP_FIELD = 'authors';

test.describe('A save in a stacked panel refreshes the list underneath', () => {
    let createdTypeIds: string[] = [];
    let childTypeVariable: string;
    let parentIdentifier: string;
    let parentTitle: string;
    let childTitle: string;
    let createdTitle: string;
    /** Every title starts with it, so a title filter shows the parent, the child and the new one. */
    let term: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        term = `zq${testSuffix}`;
        parentTitle = `${term} parent`;
        childTitle = `${term} child`;
        createdTitle = `${term} created`;

        const childType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Notify_${testSuffix}`)
        );
        createdTypeIds.push(childType.id);
        childTypeVariable = childType.variable;

        const parentType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Notify_${testSuffix}`,
                'E2E_Blog_Notify',
                'E2EBlogNotify',
                childType.variable,
                RELATIONSHIP_FIELD,
                CARDINALITY.ONE_TO_MANY
            )
        );
        createdTypeIds.push(parentType.id);

        // The child is there so the list has a row to wait for before anything is saved.
        await apiHelpers.createContentlet(childType.variable, { title: childTitle });
        const parent = await apiHelpers.createContentlet(parentType.variable, {
            title: parentTitle
        });
        parentIdentifier = parent.identifier;
    });

    test.afterEach(async ({ apiHelpers }) => {
        // Parent type first: its relationship field points at the child type. Deleting a type
        // deletes its contentlets.
        for (const id of createdTypeIds.reverse()) {
            await apiHelpers.deleteContentType(id);
        }
        createdTypeIds = [];
    });

    /** Creates related content from the parent's Relationship field, in a panel stacked over it. */
    const createInStackedPanel = async (
        sidePanel: SidePanel,
        relationshipField: RelationshipField
    ): Promise<void> => {
        await relationshipField.clickCreateNew();
        await sidePanel.expectCount(2);

        await sidePanel.form.fillTextField(createdTitle);
        await sidePanel.form.save();
        await expect(relationshipField.table).toContainText(createdTitle);
    };

    /**
     * Closes the stacked panel, then the one under it, with their header X. The parent's new
     * relation is unsaved, so its close asks first; the related content was saved on its own and
     * stays.
     */
    const closeBoth = async (page: Page, sidePanel: SidePanel): Promise<void> => {
        await sidePanel.close();
        await sidePanel.expectCount(1);
        await sidePanel.close();
        await page.getByRole('button', { name: 'Discard changes' }).click();
        await sidePanel.expectClosed();
    };

    test('on the legacy content list @critical', async ({ adminPage }) => {
        const sidePanel = new SidePanel(adminPage);
        const relationshipField = new RelationshipField(adminPage, RELATIONSHIP_FIELD);
        const results = getLegacyFrame(adminPage).locator('#results_table');

        await adminPage.goto(
            `${Portlet.Content}?filter=${childTypeVariable}&editContent=${parentIdentifier}`
        );
        await sidePanel.expectCount(1);
        await expect(sidePanel.title).toContainText(parentTitle);
        await expect(results).toContainText(childTitle, { timeout: 20000 });

        await createInStackedPanel(sidePanel, relationshipField);

        // Both panels are still open, so no close has reloaded the list.
        await sidePanel.expectCount(2);
        await expect(results).toContainText(createdTitle, { timeout: 20000 });

        await closeBoth(adminPage, sidePanel);
        await expect(results).toContainText(createdTitle);
    });

    test('on Content Drive', async ({ adminPage }) => {
        const drive = new ContentDrivePage(adminPage);
        const sidePanel = new SidePanel(adminPage);
        const relationshipField = new RelationshipField(adminPage, RELATIONSHIP_FIELD);

        await adminPage.goto(
            `${Portlet.ContentDrive}?filters=${encodeURIComponent(`title:${term}`)}`
        );
        await expect(drive.toolbar).toBeVisible({ timeout: 20000 });
        await drive.expectListContainsTitle(childTitle);
        await drive.listTitles.filter({ hasText: parentTitle }).first().dblclick();
        await sidePanel.expectCount(1);
        await expect(sidePanel.title).toContainText(parentTitle);

        await createInStackedPanel(sidePanel, relationshipField);

        // Both panels are still open, so no close has reloaded the list.
        await sidePanel.expectCount(2);
        await drive.expectListContainsTitle(createdTitle);

        await closeBoth(adminPage, sidePanel);
        await drive.expectListContainsTitle(createdTitle);
    });
});
