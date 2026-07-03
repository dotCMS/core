import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadBinaryField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { BinaryField } from './helpers/binary-field';

const BINARY_FIELD_VARIABLE = 'binaryField';

function makeSystemOptions(options: {
    allowURLImport?: boolean;
    allowCodeWrite?: boolean;
    allowGenerateImg?: boolean;
}) {
    return [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
            fieldId: '',
            id: '',
            key: 'systemOptions',
            value: JSON.stringify(options)
        }
    ];
}

async function createBinaryContentTypeWithOptions(
    request: Parameters<typeof createFakeContentType>[0],
    options: Parameters<typeof makeSystemOptions>[0]
): Promise<ContentType> {
    return createFakeContentType(request, {
        name: `E2EBinarySystemOptions${uniqueSuffix()}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadBinaryField({
                name: 'Binary Field',
                variable: BINARY_FIELD_VARIABLE,
                sortOrder: 2,
                fieldVariables: makeSystemOptions(options)
            })
        ]
    });
}

test.describe('binary field systemOptions', () => {
    test.describe.configure({ mode: 'serial' });

    let contentType: ContentType;

    test.afterEach(async ({ request }) => {
        if (contentType) {
            await deleteContentType(request, contentType.id);
        }
    });

    test('allowURLImport=false hides Import from URL button', async ({ page, request }) => {
        contentType = await createBinaryContentTypeWithOptions(request, { allowURLImport: false });

        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();

        await expect(field.importFromUrlBtn).toBeHidden();
        await expect(field.createNewFileBtn).toBeVisible();
        await expect(field.generateWithAiBtn).toBeVisible();
    });

    test('allowCodeWrite=false hides Create New File button', async ({ page, request }) => {
        contentType = await createBinaryContentTypeWithOptions(request, { allowCodeWrite: false });

        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();

        await expect(field.importFromUrlBtn).toBeVisible();
        await expect(field.createNewFileBtn).toBeHidden();
        await expect(field.generateWithAiBtn).toBeVisible();
    });

    test('allowGenerateImg=false hides Generate with AI button', async ({ page, request }) => {
        contentType = await createBinaryContentTypeWithOptions(request, {
            allowGenerateImg: false
        });

        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();

        await expect(field.importFromUrlBtn).toBeVisible();
        await expect(field.createNewFileBtn).toBeVisible();
        await expect(field.generateWithAiBtn).toBeHidden();
    });

    test('all options disabled hides all action buttons', async ({ page, request }) => {
        contentType = await createBinaryContentTypeWithOptions(request, {
            allowURLImport: false,
            allowCodeWrite: false,
            allowGenerateImg: false
        });

        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();

        await expect(field.importFromUrlBtn).toBeHidden();
        await expect(field.createNewFileBtn).toBeHidden();
        await expect(field.generateWithAiBtn).toBeHidden();
    });
});
