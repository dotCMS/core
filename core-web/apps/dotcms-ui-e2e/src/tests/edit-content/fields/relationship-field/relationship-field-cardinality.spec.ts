import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import {
    CARDINALITY,
    expect,
    test,
    type TestContentType,
    type TestContentlet
} from '../../../../fixtures/relationship.fixture';

/**
 * Tests that the "Select Existing Content" dialog disables items
 * that are already related to another parent in ONE_TO_ONE and ONE_TO_MANY relationships.
 */
test.describe('Cardinality Constraints', () => {
    let childType: TestContentType;
    let parentType: TestContentType;
    let children: TestContentlet[];

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        // 1. Create child content type
        childType = await apiHelpers.createContentType(apiHelpers.authorPayload(testSuffix));

        // 2. Create parent content type with ONE_TO_MANY relationship to child
        parentType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                testSuffix,
                'E2E_Parent',
                'E2EParent',
                childType.variable,
                'relation',
                CARDINALITY.ONE_TO_MANY
            )
        );

        // 3. Create 3 children (sorted by title)
        children = await apiHelpers.createContentlets(childType.variable, [
            { title: 'Child A' },
            { title: 'Child B' },
            { title: 'Child C' }
        ]);

        // 4. Create Parent A and relate it to Child A
        await apiHelpers.createContentletWithRelationship(
            parentType.variable,
            { title: 'Parent A' },
            { relation: children[0].identifier }
        );
    });

    test('constrained items disabled in ONE_TO_MANY dialog @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        const relField = new RelationshipField(adminPage, 'relation');
        const dialog = new SelectExistingContentDialog(adminPage);

        // Navigate to create Parent B (new content)
        await formPage.goToNew(parentType.variable);

        // Open the "Select Existing Content" dialog
        await relField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();

        // Expect 3 rows in the dialog
        await dialog.expectRowCount(3);

        // Find which row index corresponds to Child A (already taken by Parent A)
        // Children are sorted by title: Child A, Child B, Child C
        // The dialog may sort differently, so find by title text
        const rowCount = await dialog.rows.count();
        let constrainedRowIndex = -1;
        const selectableIndices: number[] = [];

        for (let i = 0; i < rowCount; i++) {
            const rowText = await dialog.rows.nth(i).textContent();
            if (rowText?.includes('Child A')) {
                constrainedRowIndex = i;
            } else {
                selectableIndices.push(i);
            }
        }

        expect(constrainedRowIndex, 'Child A row should exist in dialog').toBeGreaterThanOrEqual(0);
        expect(selectableIndices, 'Should have 2 selectable rows').toHaveLength(2);

        // Child A should be disabled (constrained — already related to Parent A)
        await dialog.expectRowConstrained(constrainedRowIndex);

        // Child B and Child C should be selectable
        for (const idx of selectableIndices) {
            await dialog.expectRowSelectable(idx);
        }
    });

    test('constrained items disabled in ONE_TO_ONE dialog @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // Create a separate ONE_TO_ONE setup
        const childType11 = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`OTO${testSuffix}`)
        );

        const parentType11 = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `OTO${testSuffix}`,
                'E2E_Parent11',
                'E2EParent11',
                childType11.variable,
                'relation',
                CARDINALITY.ONE_TO_ONE
            )
        );

        const children11 = await apiHelpers.createContentlets(childType11.variable, [
            { title: 'Solo Child A' },
            { title: 'Solo Child B' }
        ]);

        // Parent A takes Solo Child A
        await apiHelpers.createContentletWithRelationship(
            parentType11.variable,
            { title: 'Solo Parent A' },
            { relation: children11[0].identifier }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        const relField = new RelationshipField(adminPage, 'relation');
        const dialog = new SelectExistingContentDialog(adminPage);

        // Create new parent — open dialog
        await formPage.goToNew(parentType11.variable);
        await relField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();

        await dialog.expectRowCount(2);

        // Find constrained row (Solo Child A)
        const rowCount = await dialog.rows.count();
        let constrainedIdx = -1;
        let freeIdx = -1;

        for (let i = 0; i < rowCount; i++) {
            const text = await dialog.rows.nth(i).textContent();
            if (text?.includes('Solo Child A')) {
                constrainedIdx = i;
            } else {
                freeIdx = i;
            }
        }

        await dialog.expectRowConstrained(constrainedIdx);
        await dialog.expectRowSelectable(freeIdx);
    });

    test('no constraints in MANY_TO_MANY dialog', async ({ adminPage, apiHelpers, testSuffix }) => {
        // Create MANY_TO_MANY setup — no items should be disabled
        const childTypeMM = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`MM${testSuffix}`)
        );

        const parentTypeMM = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `MM${testSuffix}`,
                'E2E_ParentMM',
                'E2EParentMM',
                childTypeMM.variable,
                'relation',
                CARDINALITY.MANY_TO_MANY
            )
        );

        const childrenMM = await apiHelpers.createContentlets(childTypeMM.variable, [
            { title: 'MM Child A' },
            { title: 'MM Child B' }
        ]);

        // Relate Child A to Parent A — but in M:M this should NOT constrain it
        await apiHelpers.createContentletWithRelationship(
            parentTypeMM.variable,
            { title: 'MM Parent A' },
            { relation: childrenMM[0].identifier }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        const relField = new RelationshipField(adminPage, 'relation');
        const dialog = new SelectExistingContentDialog(adminPage);

        await formPage.goToNew(parentTypeMM.variable);
        await relField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();

        await dialog.expectRowCount(2);

        // Both items should be selectable — no constraints in MANY_TO_MANY
        await dialog.expectRowSelectable(0);
        await dialog.expectRowSelectable(1);
    });
});
