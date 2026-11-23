import { UveEditorPage } from '@pages';
import { expect, test } from '@playwright/test';
import { findWorkingContentlet } from '@requests/contentlets';
import { type ContentType, createPageContentType, deleteContentType } from '@requests/contentType';
import { getLanguages, type Language } from '@requests/languages';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';
import { createUvePage, removeUvePage, type UvePage, uvePagePath } from './helpers/uve-page';

/**
 * #38303, T178: the UVE's "create a new language version" opens the editor the page's type uses.
 * On a page type with the new editor, the page opens in it in the asked language, and the editor
 * offers to copy the page into the translation or start from scratch, as its own locale switch does.
 * The first save creates the translation and the UVE shows the page in that language.
 */
test.describe('The UVE translates a page in the editor its type uses', () => {
    let pageType: ContentType | null = null;
    let uvePage: UvePage | null = null;
    let otherLanguage: Language;

    test.beforeEach(async ({ request }) => {
        const languages = await getLanguages(request);
        const other = languages.find((language) => !language.defaultLanguage);
        expect(other, 'the instance needs a second language to translate into').toBeDefined();
        otherLanguage = other as Language;

        const suffix = uniqueSuffix();
        pageType = await createPageContentType(request, `E2ETranslatePage${suffix}`, true);
        uvePage = await createUvePage(request, suffix, {
            contentTypeIds: [],
            pageType: pageType.variable
        });
    });

    test.afterEach(async ({ request }) => {
        if (uvePage) {
            await removeUvePage(request, uvePage);
            uvePage = null;
        }

        if (pageType) {
            await deleteContentType(request, pageType.id);
            pageType = null;
        }
    });

    test('creates the translation in the new editor, copied from the page @critical', async ({
        page,
        request
    }) => {
        const uve = new UveEditorPage(page);
        const sidePanel = new SidePanel(page);
        const created = uvePage as UvePage;

        // The page has no version in this language: the UVE asks to create one.
        await uve.open(uvePagePath(created), otherLanguage.id);
        const createVersion = page.getByRole('alertdialog', {
            name: 'Create New Language Version?'
        });
        await expect(createVersion).toBeVisible({ timeout: 20000 });
        await createVersion.getByRole('button', { name: 'Yes' }).click();

        // The new editor, not the legacy one, offering to copy the page or start from scratch.
        await sidePanel.expectOpen();
        const startWith = page.getByRole('dialog', { name: 'No translation found' });
        await expect(startWith).toBeVisible({ timeout: 20000 });
        await expect(sidePanel.front.getByTestId('legacy-screen-iframe')).toHaveCount(0);

        await startWith.getByTestId('option1').click();
        await startWith.getByRole('button', { name: 'Continue' }).click();
        await expect(startWith).toBeHidden();

        // Copied from the page: its URL is already there.
        await expect(sidePanel.front.getByTestId('url')).toHaveValue(
            new RegExp(created.page.title)
        );

        await sidePanel.form.save();

        // The translation exists, copied from the page, and the UVE shows the page in that
        // language.
        await expect
            .poll(
                async () =>
                    (
                        await findWorkingContentlet(request, {
                            identifier: created.page.identifier,
                            languageId: otherLanguage.id
                        })
                    )?.title ?? null,
                { timeout: 20000 }
            )
            .toBe(created.page.title);
        await expect(page).toHaveURL(new RegExp(`language_id=${otherLanguage.id}`));
        await expect(createVersion).toBeHidden();
    });
});
