import { LegacyEditContentFormPage } from '@pages';
import { expect, request as apiRequest, test } from '@playwright/test';
import {
    Contentlet,
    createContentlet,
    createDotAsset,
    deleteContentlets,
    publishDotAssetVersion
} from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { getDefaultSite, type Site } from '@requests/sites';
import {
    createFakePayloadTextField,
    createFakePayloadWYSIWYGField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { LegacyWysiwygField } from './helpers/legacy-wysiwyg-field';

import { createTestTextFile } from '../fields/file-upload-fields/helpers/file-test-data';

const WYSIWYG_VARIABLE = 'body';
const LINK_TEXT = 'Download the brochure';
const FIRST_VERSION_CONTENT = 'dotAsset version one';
const SECOND_VERSION_CONTENT = 'dotAsset version two';

test.describe('Legacy editor — WYSIWYG link to a dotAsset', () => {
    let site: Site;
    let contentType: ContentType | null = null;
    let contentlet: Contentlet | null = null;
    let dotAsset: Contentlet | null = null;
    let assetName: string;

    test.beforeEach(async ({ request }) => {
        const suffix = uniqueSuffix();
        site = await getDefaultSite(request);

        contentType = await createFakeContentType(request, {
            name: `E2E Legacy Link ${suffix}`,
            // false -> the legacy Dojo/JSP editor, the one whose Link dialog is under test.
            metadata: { CONTENT_EDITOR2_ENABLED: false },
            fields: [
                createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
                createFakePayloadWYSIWYGField({
                    name: 'Body',
                    variable: WYSIWYG_VARIABLE,
                    sortOrder: 2
                })
            ]
        });

        contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title: `Legacy link ${suffix}`,
            [WYSIWYG_VARIABLE]: `<p>${LINK_TEXT}</p>`
        });

        // Two versions BEFORE the link is inserted. The first version's inode is what the Link
        // dialog used to write into the href; the second is what an identifier link must serve.
        assetName = `e2e-link-${suffix}.txt`;
        dotAsset = await createDotAsset(
            request,
            createTestTextFile(assetName, FIRST_VERSION_CONTENT),
            site.identifier
        );
        await publishDotAssetVersion(
            request,
            dotAsset.identifier,
            createTestTextFile(assetName, SECOND_VERSION_CONTENT)
        );
    });

    test.afterEach(async ({ request }) => {
        const identifiers = [contentlet, dotAsset]
            .filter((item): item is Contentlet => item !== null)
            .map((item) => item.identifier);

        if (identifiers.length) {
            await deleteContentlets(request, identifiers);
        }

        contentlet = null;
        dotAsset = null;

        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    /**
     * The Link dialog's file browser used to build the href from the asset's `path`, which for a
     * dotAsset is `/content.<inode>` with the inode of its FIRST version, so the link stayed pinned
     * to that version forever. It has to link by identifier, like the image button does, so the same
     * href keeps serving whatever version is current.
     */
    test('inserts the dotAsset by identifier so the link serves its latest version @critical', async ({
        page
    }) => {
        const asset = dotAsset as Contentlet;
        const expectedHref = `/dA/${asset.identifier}/${assetName}?language_id=${asset.languageId}`;

        const formPage = new LegacyEditContentFormPage(page);
        await formPage.goToLegacyEdit(contentType?.variable as string);
        const legacyFrame = await formPage.getLegacyContentFrame();

        const field = new LegacyWysiwygField(legacyFrame, WYSIWYG_VARIABLE);
        await field.expectReady();
        await field.selectParagraph(LINK_TEXT);

        const linkDialog = await field.openLinkDialog();
        const fileBrowser = await linkDialog.browse();
        await fileBrowser.selectSite(site.hostname);
        await fileBrowser.pick(asset.identifier);

        // What the file browser handed back to the Link dialog: the identifier form, not an inode.
        await expect(linkDialog.urlInput).toHaveValue(expectedHref);
        await linkDialog.okButton.click();

        const link = field.links.first();
        await expect(link).toHaveAttribute('href', expectedHref);
        await expect(link).toHaveText(LINK_TEXT);

        // The same href resolves to the current live version for a site visitor. A fresh, unauthenticated
        // request context is used on purpose: a logged-in back-end user is served the working version,
        // which would not prove that the published version is the one being linked.
        const visitor = await apiRequest.newContext({ baseURL: new URL(page.url()).origin });
        try {
            const served = await visitor.get(expectedHref);
            expect(served.status()).toBe(200);
            expect(await served.text()).toBe(SECOND_VERSION_CONTENT);
        } finally {
            await visitor.dispose();
        }
    });
});
