import { ContentTypeBuilderPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';

import { KeyValueField } from '../edit-content/fields/key-value-field/helpers/key-value-field';

/**
 * Field Variables — the second consumer of the shared Key/Value editor (#37191).
 *
 * The editor itself is covered by unit tests; this smoke exists because the
 * redesign is a shared-component refactor and the real risk is a regression in
 * one of the three integration points. It drives the dialog end to end and
 * verifies persistence by reopening it.
 */
test.describe('content type builder — field variables', () => {
    let contentType: ContentType | null = null;
    let contentTypeId = '';

    test.beforeEach(async ({ request }) => {
        const suffix = crypto.randomUUID().slice(0, 8);
        contentType = await createFakeContentType(request, {
            name: `E2EFieldVariables${suffix}`
        });
        contentTypeId = contentType.id;
    });

    test.afterEach(async ({ request }) => {
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('add, edit and delete field variables persist across reopen @smoke', async ({ page }) => {
        const builder = new ContentTypeBuilderPage(page);
        await builder.goToBuilder(contentTypeId);

        await builder.openFieldVariables('Title');

        const variables = new KeyValueField(page, builder.fieldVariablesPanel());
        await variables.expectVisible();

        await variables.addEntry('hint', 'Use lowercase keys');
        await variables.addEntry('maxRows', '20');
        await variables.expectEntryCount(2);

        await variables.editEntryValue('maxRows', '40');
        await variables.expectEntry('maxRows', '40');

        await variables.deleteEntryByKey('hint');
        await variables.expectKeyAbsent('hint');

        // Reloading the builder proves each row was persisted against the field, not
        // just held in the dialog's local state. A full reload rather than closing the
        // dialog: nothing survives it in memory, so it is the stronger check, and it
        // does not depend on a modal mask being out of the way — clicking the dialog's
        // close button was intercepted by one in CI.
        await builder.goToBuilder(contentTypeId);
        await builder.openFieldVariables('Title');

        const reopened = new KeyValueField(page, builder.fieldVariablesPanel());
        await reopened.expectEntryCount(1);
        await reopened.expectEntry('maxRows', '40');
        await reopened.expectKeyAbsent('hint');
    });

    test('the editor fits the dialog without scrolling the page @smoke', async ({ page }) => {
        const builder = new ContentTypeBuilderPage(page);
        await builder.goToBuilder(contentTypeId);
        await builder.openFieldVariables('Title');

        const variables = new KeyValueField(page, builder.fieldVariablesPanel());
        await variables.expectVisible();
        await variables.addEntry('aVeryLongKeyNameThatCouldPushTheTableWider', 'x'.repeat(120));

        // FR-020: long content must be absorbed by the editor, never by widening
        // the page.
        const overflows = await page.evaluate(
            () => document.documentElement.scrollWidth > document.documentElement.clientWidth
        );
        expect(overflows).toBe(false);
    });

    test('rows can be reordered @smoke', async ({ page }) => {
        const builder = new ContentTypeBuilderPage(page);
        await builder.goToBuilder(contentTypeId);
        await builder.openFieldVariables('Title');

        const panel = builder.fieldVariablesPanel();
        const variables = new KeyValueField(page, panel);
        await variables.addEntry('alpha', 'first');
        await variables.addEntry('beta', 'second');
        await variables.expectKeyOrder(['beta', 'alpha']);

        await variables.dragRowTo('alpha', 0);
        await variables.expectKeyOrder(['alpha', 'beta']);
    });
});
