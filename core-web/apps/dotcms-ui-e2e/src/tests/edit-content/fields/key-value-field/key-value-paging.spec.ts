import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadKeyValueField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { KeyValueField } from './helpers/key-value-field';

const VAR = 'keyValueField';

/**
 * Row paging in the shared Key/Value editor (#37191).
 *
 * The list is rendered 40 rows at a time, but the table stays bound to all of it —
 * PrimeNG reorders the array it is given, so a shortened one would silently drop a
 * drag. These tests exist to hold that apart: they check what is on screen AND that
 * the withheld tail survives a reorder and a delete.
 *
 * Reached by the Angular route rather than the content listing, so the legacy
 * portlet is not a prerequisite.
 */
test.describe('key/value paging @smoke', () => {
    let contentType: ContentType | null = null;
    let variable: string;

    test.beforeEach(async ({ request }) => {
        contentType = await createFakeContentType(request, {
            name: `E2EKvPaging${uniqueSuffix()}`,
            fields: [
                createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
                createFakePayloadKeyValueField({ name: 'KV', variable: VAR, sortOrder: 2 })
            ]
        });
        variable = contentType.variable;
    });

    test.afterEach(async ({ request }) => {
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    async function openWith(page: Page, count: number) {
        await page.goto(`/dotAdmin/#/content/new/${variable}`);
        await page.waitForLoadState('domcontentloaded');
        await page.getByTestId('title').waitFor({ state: 'visible', timeout: 20000 });
        const field = new KeyValueField(page, VAR);
        await field.expectVisible();
        for (let i = 0; i < count; i++) {
            await field.addEntry(`key-${String(i).padStart(3, '0')}`, `v${i}`);
        }
        return field;
    }

    const root = (page: Page) => page.getByTestId(`field-${VAR}`);

    test('renders one page and reveals the rest on demand', async ({ page }) => {
        await openWith(page, 45);

        await expect(root(page).getByTestId('dot-key-value-key')).toHaveCount(40);
        await expect(root(page).getByTestId('dot-key-value-load-more')).toBeVisible();

        await root(page).getByTestId('dot-key-value-load-more').click();
        await expect(root(page).getByTestId('dot-key-value-key')).toHaveCount(45);
        await expect(root(page).getByTestId('dot-key-value-load-more')).toHaveCount(0);
        // The row itself is permanent now — it also carries Clear All.
        await expect(root(page).getByTestId('dot-key-value-footer-row')).toHaveCount(1);
    });

    test('no control at all when the list fits one page', async ({ page }) => {
        await openWith(page, 3);
        await expect(root(page).getByTestId('dot-key-value-load-more')).toHaveCount(0);
        // The row itself is permanent now — it also carries Clear All.
        await expect(root(page).getByTestId('dot-key-value-footer-row')).toHaveCount(1);
    });

    test('drag reorders correctly while rows are withheld, losing nothing', async ({ page }) => {
        const field = await openWith(page, 45);
        await expect(root(page).getByTestId('dot-key-value-key')).toHaveCount(40);

        const before = (await root(page).getByTestId('dot-key-value-key').allInnerTexts()).map(
            (t) => t.trim()
        );
        await field.dragRowTo(before[0], 1);

        const after = (await root(page).getByTestId('dot-key-value-key').allInnerTexts()).map((t) =>
            t.trim()
        );
        expect(after[0]).toBe(before[1]);
        expect(after[1]).toBe(before[0]);

        await root(page).getByTestId('dot-key-value-load-more').click();
        const all = (await root(page).getByTestId('dot-key-value-key').allInnerTexts()).map((t) =>
            t.trim()
        );
        expect(all).toHaveLength(45);
        expect(new Set(all).size).toBe(45);
        expect(all[44]).toBe('key-000');
    });

    test('deleting a visible row keeps the withheld tail intact', async ({ page }) => {
        const field = await openWith(page, 45);
        await field.deleteEntryByKey('key-044');

        await expect(root(page).getByTestId('dot-key-value-key')).toHaveCount(40);
        await root(page).getByTestId('dot-key-value-load-more').click();

        const all = (await root(page).getByTestId('dot-key-value-key').allInnerTexts()).map((t) =>
            t.trim()
        );
        expect(all).toHaveLength(44);
        expect(all).not.toContain('key-044');
        expect(all[43]).toBe('key-000');
    });
});
