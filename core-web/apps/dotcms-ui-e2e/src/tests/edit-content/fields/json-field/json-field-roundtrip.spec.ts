import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadJSONField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';

/**
 * JSON field round-trip (issue #37670).
 *
 * Guards the one transformation whose output is formatting rather than a value: the JSON
 * resolver stringifies with two-space indentation specifically so the Monaco editor does not
 * show the document as a single flat line. A regression there produces a form that still saves
 * the right data while being unreadable — invisible to a type check, and to a unit test that
 * only compares parsed values.
 *
 * Added when the content model became a discriminated union and the two transformation paths
 * were merged into one (issue #31911).
 */
let contentType: ContentType | null = null;
let contentTypeVariable: string;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2EJsonField${Date.now()}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadJSONField({ name: 'Payload', variable: 'payload', sortOrder: 2 })
        ]
    });
    contentTypeVariable = contentType.variable;
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('a JSON value survives save and reopen, still indented @critical', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    await page.getByTestId('title').fill('JSON round trip');

    const editor = page.locator('dot-edit-content-json-field .monaco-editor').first();
    await expect(editor).toBeVisible({ timeout: 30000 });
    await editor.click();
    await page.keyboard.type('{"alpha": 1, "beta": "two"}');

    await formPage.save();

    await page.reload();
    await expect(editor).toBeVisible({ timeout: 30000 });

    const rendered = await editor.innerText();

    // Both keys survived the round trip...
    expect(rendered).toContain('alpha');
    expect(rendered).toContain('beta');
    // ...and the document is still formatted across lines rather than flattened.
    expect(rendered.split('\n').length).toBeGreaterThan(1);
});
