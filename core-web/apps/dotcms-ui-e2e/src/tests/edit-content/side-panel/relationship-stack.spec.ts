import { ContentDrivePage } from '@pages';

import { SidePanel } from './helpers/side-panel';

import { CARDINALITY, expect, test } from '../../../fixtures/relationship.fixture';
import { RelationshipField } from '../fields/relationship-field/helpers/relationship-field';

/**
 * #38303, US4: a Relationship field inside a side panel creates related content in a second panel
 * stacked over the first. Saving the new item lists it in the field; Escape closes only the front
 * panel and leaves the parent open.
 *
 * Both types use the new editor; the parent is opened from Content Drive's edit link.
 */

const RELATIONSHIP_FIELD = 'authors';

test.describe('Relationship field stacks a panel to create related content', () => {
    let createdTypeIds: string[] = [];
    let parentIdentifier: string;
    let parentTitle: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const childType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Stack_${testSuffix}`)
        );
        createdTypeIds.push(childType.id);

        const parentType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Stack_${testSuffix}`,
                'E2E_Blog_Stack',
                'E2EBlogStack',
                childType.variable,
                RELATIONSHIP_FIELD,
                CARDINALITY.ONE_TO_MANY
            )
        );
        createdTypeIds.push(parentType.id);

        parentTitle = `Stack parent ${testSuffix}`;
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

    test('creates related content in a stacked panel and closes back to the parent @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const drive = new ContentDrivePage(adminPage);
        const sidePanel = new SidePanel(adminPage);
        const relationshipField = new RelationshipField(adminPage, RELATIONSHIP_FIELD);
        const childTitle = `Stack child ${testSuffix}`;

        await drive.goToEditContent(parentIdentifier);
        await sidePanel.expectCount(1);
        await expect(sidePanel.title).toContainText(parentTitle);

        await relationshipField.clickCreateNew();
        await sidePanel.expectCount(2);

        await sidePanel.form.fillTextField(childTitle);
        await sidePanel.form.save();
        await relationshipField.expectRowCount(1);
        await expect(relationshipField.table).toContainText(childTitle);

        await adminPage.keyboard.press('Escape');
        await sidePanel.expectCount(1);
        await expect(sidePanel.title).toContainText(parentTitle);
    });
});
