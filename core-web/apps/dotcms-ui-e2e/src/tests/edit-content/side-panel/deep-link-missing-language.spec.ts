import { expect, test } from '@playwright/test';
import { createContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { findOrCreateSecondLanguage, type Language } from '@requests/languages';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';

/** The Pages portlet; it reopens the panel its URL names. */
const PAGES = '/dotAdmin/#/pages';

/**
 * #38303, stack review L7-1: a link that names content in a language the content lacks opens the
 * version that exists, and the new editor offers the translation asked for ("Copy from …" or
 * "Start from scratch"), as its own language switch does. The URL keeps the language asked for.
 */
test.describe('A panel link naming a language the content lacks', () => {
    let contentType: ContentType | null = null;
    let identifier: string;
    let otherLanguage: Language;
    let other: Language | undefined;

    // The empty starter CI runs has only the default language: the spec adds one and keeps it.
    test.beforeAll(async ({ request }) => {
        other = await findOrCreateSecondLanguage(request);
    });

    test.beforeEach(async ({ request }) => {
        expect(other, 'the instance needs a second language').toBeDefined();
        otherLanguage = other as Language;

        const suffix = uniqueSuffix();
        // createFakeContentType gives it a Title text field and, with the flag, the new editor.
        contentType = await createFakeContentType(request, {
            name: `E2EMissingLanguage${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: true }
        });
        // In the default language only.
        const contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title: `Missing language ${suffix}`
        });
        identifier = contentlet.identifier;
    });

    test.afterEach(async ({ request }) => {
        // Deleting the content type deletes its contentlet.
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('offers the translation the link asks for, in the new editor @critical', async ({
        page
    }) => {
        const sidePanel = new SidePanel(page);

        await page.goto(`${PAGES}?editContent=${identifier}&editContentLang=${otherLanguage.id}`);

        await sidePanel.expectOpen();
        const startWith = page.getByRole('dialog', { name: 'No translation found' });
        await expect(startWith).toBeVisible({ timeout: 20000 });
        await expect(startWith.getByTestId('option1')).toBeVisible();
        await expect(startWith.getByTestId('option2')).toBeVisible();
        await expect(page).toHaveURL(new RegExp(`editContentLang=${otherLanguage.id}`));
    });
});
