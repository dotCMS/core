import { NewEditContentFormPage, LegacyEditContentFormPage } from '@pages';
import { test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadBinaryField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { BinaryField, E2E_IMPORT_URL } from './helpers/binary-field';
import { LegacyBinaryField } from './helpers/legacy-binary-field';

const BINARY_FIELD_VARIABLE = 'binaryField';

async function createBinaryFieldContentType(
    request: Parameters<typeof createFakeContentType>[0],
    options: { contentEditor2Enabled?: boolean } = {}
) {
    const { contentEditor2Enabled = true } = options;

    return createFakeContentType(request, {
        name: `E2EBinaryImageEditor${uniqueSuffix()}`,
        metadata: { CONTENT_EDITOR2_ENABLED: contentEditor2Enabled },
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadBinaryField({
                name: 'Binary Field',
                variable: BINARY_FIELD_VARIABLE,
                sortOrder: 2
            })
        ]
    });
}

test.describe('Binary field image editor — new editor', () => {
    let contentType: ContentType | null = null;
    let contentTypeVariable: string;

    test.beforeEach(async ({ request }) => {
        contentType = await createBinaryFieldContentType(request, {
            contentEditor2Enabled: true
        });
        contentTypeVariable = contentType.variable;
    });

    test.afterEach(async ({ request }) => {
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    // The unified Binary field shows "Edit image" for image files. In the new
    // Edit Content the new Angular image editor opens (the FEATURE_FLAG_NEW_IMAGE_EDITOR
    // gate was removed, so it is always used here; the legacy Dojo editor only runs in
    // the legacy Edit Content — see the describe block below).
    test('import image and Edit opens the new Angular image editor @critical', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.importFromUrl(E2E_IMPORT_URL);
        await field.openImageEditorInNewEditor();
    });
});

test.describe('Binary field image editor — legacy editor', () => {
    let contentType: ContentType | null = null;
    let contentTypeVariable: string;

    test.beforeEach(async ({ request }) => {
        contentType = await createBinaryFieldContentType(request, {
            contentEditor2Enabled: false
        });
        contentTypeVariable = contentType.variable;
    });

    test.afterEach(async ({ request }) => {
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('import image and Edit opens Dojo image editor in legacy form @critical', async ({
        page
    }) => {
        const formPage = new LegacyEditContentFormPage(page);
        await formPage.goToLegacyNew(contentTypeVariable);

        const legacyFrame = await formPage.getLegacyContentFrame();
        const field = new LegacyBinaryField(legacyFrame, page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.importFromUrl(E2E_IMPORT_URL);
        await field.openImageEditorInLegacyEditor();
    });
});
