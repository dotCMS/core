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

    const title = page.getByTestId('title');
    await title.fill('JSON round trip');

    // `.monaco-editor` alone is not enough: the ngx-monaco-editor wrapper renders an empty
    // `div.monaco-editor` placeholder before Monaco's lazily loaded bundle creates the real editor,
    // so it is visible while keystrokes still go nowhere. `.view-lines` only exists once
    // `monaco.editor.create` has run.
    const editorLines = page.locator('dot-edit-content-json-field .monaco-editor .view-lines');
    await expect(editorLines).toBeVisible({ timeout: 30000 });
    await editorLines.click();
    await page.keyboard.type('{"alpha": 1, "beta": "two"}');

    await expect(title).toHaveValue('JSON round trip');
    await expect(editorLines).toContainText('beta');
    // The editor's validator reads the JSON worker's markers, which update asynchronously. Saving
    // while a marker from a half-typed state is still there blocks the save as an invalid form.
    await expect(page.locator('dot-edit-content-json-field .squiggly-error')).toHaveCount(0);

    await formPage.save();

    // `save()` only waits for the workflow API response, not for the editor to navigate from
    // /content/new/<type> to the saved contentlet. Reloading before that lands re-opens the *new*
    // content form — an empty one — which is why this read back nothing. Same wait the image,
    // file and binary specs already use.
    await page.waitForURL(/\/content\/([a-f0-9-]+)/);
    const [, savedContentIdentifier] = page
        .url()
        .match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
    expect(savedContentIdentifier).toBeTruthy();

    await formPage.goToContent(savedContentIdentifier);

    // Retrying assertions: the editor paints its lines after it becomes visible, so a one-shot
    // read right after `toBeVisible` can return an empty string.
    // Both keys survived the round trip...
    await expect(editorLines).toContainText('alpha', { timeout: 30000 });
    await expect(editorLines).toContainText('beta');
    // ...and the document is still formatted across lines rather than flattened.
    await expect.poll(() => editorLines.locator('.view-line').count()).toBeGreaterThan(1);
});
