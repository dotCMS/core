import { ContentTypeBuilderPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';

import { KeyValueField } from '../edit-content/fields/key-value-field/helpers/key-value-field';

/** The per-row endpoint this consumer persists through. */
const VARIABLES_API = /\/api\/v1\/contenttype\/.*\/variables/;

/**
 * Runs a mutation and waits for the server to answer it.
 *
 * Every assertion available on screen is optimistic here: the shared editor drops
 * the row from its own list as soon as it is clicked, while the consumer only
 * commits once the request returns. So `expectKeyAbsent` says nothing about whether
 * the write landed, and reloading straight after it races the request — passing on a
 * fast machine and failing in CI, which is exactly what happened.
 */
async function persisted(
    page: import('@playwright/test').Page,
    method: 'POST' | 'PUT' | 'DELETE',
    action: () => Promise<void>
) {
    const [response] = await Promise.all([
        page.waitForResponse(
            (r) => VARIABLES_API.test(r.url()) && r.request().method() === method,
            { timeout: 20000 }
        ),
        action()
    ]);

    expect(response.ok()).toBe(true);
}

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

        await persisted(page, 'POST', () => variables.addEntry('hint', 'Use lowercase keys'));
        await persisted(page, 'POST', () => variables.addEntry('maxRows', '20'));
        await variables.expectEntryCount(2);

        await persisted(page, 'POST', () => variables.editEntryValue('maxRows', '40'));
        await variables.expectEntry('maxRows', '40');

        await persisted(page, 'DELETE', () => variables.deleteEntryByKey('hint'));
        await variables.expectKeyAbsent('hint');

        // Reloading proves each row was persisted against the field, not just held in
        // the dialog's local state. A reload rather than closing the dialog: nothing
        // survives it in memory, so it is the stronger check, and it does not depend on
        // a modal being out of the way — clicking the dialog's close button was
        // intercepted by its mask in CI.
        await builder.reloadBuilder();
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
