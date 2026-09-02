import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadKeyValueField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { KeyValueField } from './helpers/key-value-field';

const VAR = 'keyValueField';

/**
 * Renaming a key in place (#37191).
 *
 * The key was read-only after creation until this was asked for, so what is worth
 * holding here is the part a unit test cannot show: that a rename keeps the pair's
 * value, and that the collision and Escape paths leave the list as it was.
 */
test.describe('editing the key in place @smoke', () => {
    let ct: ContentType | null = null;
    let variable: string;

    test.beforeEach(async ({ request }) => {
        ct = await createFakeContentType(request, {
            name: `E2EKeyEdit${uniqueSuffix()}`,
            fields: [
                createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
                createFakePayloadKeyValueField({ name: 'KV', variable: VAR, sortOrder: 2 })
            ]
        });
        variable = ct.variable;
    });

    test.afterEach(async ({ request }) => {
        if (ct) {
            await deleteContentType(request, ct.id);
            ct = null;
        }
    });

    const open = async (page) => {
        await page.goto(`/dotAdmin/#/content/new/${variable}`);
        await page.waitForLoadState('domcontentloaded');
        await page.getByTestId('title').waitFor({ state: 'visible', timeout: 20000 });
        const f = new KeyValueField(page, VAR);
        await f.expectVisible();

        return { f, root: page.getByTestId(`field-${VAR}`) };
    };

    const keys = async (root) =>
        (await root.getByTestId('dot-key-value-key-output').allInnerTexts()).map((t: string) =>
            t.trim()
        );

    const renameFirst = async (root, to: string, key = 'Enter') => {
        await root.getByTestId('dot-key-value-key-output').first().click();
        const input = root.getByTestId('dot-key-value-key-input').first();
        await expect(input).toBeVisible();
        await input.fill(to);
        await input.press(key);
    };

    test('click renames the key and keeps the value', async ({ page }) => {
        const { f, root } = await open(page);
        await f.addEntry('oldKey', 'theValue');

        await renameFirst(root, 'newKey');

        const values = (await root.getByTestId('dot-key-value-value-output').allInnerTexts()).map(
            (t) => t.trim()
        );
        // The rename must carry the pair's value across, not reset it.
        expect(await keys(root)).toEqual(['newKey']);
        expect(values).toEqual(['theValue']);
    });

    test('refuses a rename onto an existing key', async ({ page }) => {
        const { f, root } = await open(page);
        await f.addEntry('alpha', '1');
        await f.addEntry('beta', '2');

        await renameFirst(root, 'alpha');
        expect(await keys(root)).toEqual(['beta', 'alpha']);
    });

    test('escape leaves the key alone', async ({ page }) => {
        const { f, root } = await open(page);
        await f.addEntry('keepMe', 'v');

        await renameFirst(root, 'discarded', 'Escape');
        expect(await keys(root)).toEqual(['keepMe']);
    });
});
