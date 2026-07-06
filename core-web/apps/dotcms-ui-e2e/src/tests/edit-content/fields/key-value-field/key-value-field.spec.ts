import { faker } from '@faker-js/faker';
import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadCustomField,
    createFakePayloadKeyValueField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { KeyValueField } from './helpers/key-value-field';

const KEY_VALUE_FIELD_VARIABLE = 'keyValueField';
const CUSTOM_FIELD_VARIABLE = 'customField';

async function saveAndReload(page: import('@playwright/test').Page, title: string) {
    const formPage = new NewEditContentFormPage(page);
    await formPage.fillTextField(title);
    await formPage.save();

    await page.waitForURL(/\/content\/([a-f0-9-]+)/);
    const [, savedContentIdentifier] = page
        .url()
        .match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
    expect(savedContentIdentifier).toBeTruthy();

    await page.goto(`/dotAdmin/#/content/${savedContentIdentifier}`);
    await page.waitForLoadState('domcontentloaded');
    await page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });

    return savedContentIdentifier;
}

test.describe('Key Value Field', () => {
    test.describe('Isolated content type', () => {
        let contentType: ContentType | null = null;
        let contentTypeVariable: string;

        test.beforeEach(async ({ request }) => {
            contentType = await createFakeContentType(request, {
                name: `E2EKeyValueField${uniqueSuffix()}`,
                fields: [
                    createFakePayloadTextField({
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    }),
                    createFakePayloadKeyValueField({
                        name: 'Key Value Field',
                        variable: KEY_VALUE_FIELD_VARIABLE,
                        sortOrder: 2
                    })
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

        test('first key-value entry is not split into characters @critical', async ({ page }) => {
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentTypeVariable);

            const field = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await field.expectVisible();
            await field.addEntry('mykey', 'myvalue');

            await field.expectEntryCount(1);
            await field.expectEntry('mykey', 'myvalue');
            await field.expectKeyAbsent('m');
            await field.expectKeyAbsent('y');
            await field.expectKeyAbsent('k');
            await field.expectKeyAbsent('e');
        });

        test('save reloads and persists first key-value entry @critical', async ({ page }) => {
            const title = `E2E KV ${faker.lorem.word()}`;
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentTypeVariable);

            const field = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await field.expectVisible();
            await field.addEntry('mykey', 'myvalue');

            await saveAndReload(page, title);

            const reloadedField = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await reloadedField.expectVisible();
            await reloadedField.expectEntryCount(1);
            await reloadedField.expectEntry('mykey', 'myvalue');
        });

        test('save reloads and persists multiple key-value entries @critical', async ({ page }) => {
            const title = `E2E KV Multi ${faker.lorem.word()}`;
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentTypeVariable);

            const field = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await field.expectVisible();
            await field.addEntry('firstKey', 'firstValue');
            await field.addEntry('secondKey', 'secondValue');

            await field.expectEntryCount(2);
            await field.expectEntry('firstKey', 'firstValue');
            await field.expectEntry('secondKey', 'secondValue');

            await saveAndReload(page, title);

            const reloadedField = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await reloadedField.expectEntryCount(2);
            await reloadedField.expectEntry('firstKey', 'firstValue');
            await reloadedField.expectEntry('secondKey', 'secondValue');
        });

        test('edit key-value entry value persists after save and reload @smoke', async ({
            page
        }) => {
            const title = `E2E KV Edit ${faker.lorem.word()}`;
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentTypeVariable);

            const field = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await field.addEntry('editKey', 'originalValue');
            await field.editEntryValue('editKey', 'updatedValue');
            await field.expectEntry('editKey', 'updatedValue');

            await saveAndReload(page, title);

            const reloadedField = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await reloadedField.expectEntry('editKey', 'updatedValue');
        });

        test('delete key-value entry persists after save and reload @smoke', async ({ page }) => {
            const title = `E2E KV Delete ${faker.lorem.word()}`;
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentTypeVariable);

            const field = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await field.addEntry('keepKey', 'keepValue');
            await field.addEntry('removeKey', 'removeValue');
            await field.expectEntryCount(2);

            await field.deleteEntry(0);
            await field.expectEntryCount(1);
            await field.expectEntry('keepKey', 'keepValue');
            await field.expectKeyAbsent('removeKey');

            await saveAndReload(page, title);

            const reloadedField = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await reloadedField.expectEntryCount(1);
            await reloadedField.expectEntry('keepKey', 'keepValue');
            await reloadedField.expectKeyAbsent('removeKey');
        });
    });

    test.describe('Regression #36318 with custom field', () => {
        let contentType: ContentType | null = null;
        let contentTypeVariable: string;

        test.beforeEach(async ({ request }) => {
            contentType = await createFakeContentType(request, {
                name: `E2EKeyValueCustom${uniqueSuffix()}`,
                fields: [
                    createFakePayloadTextField({
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    }),
                    createFakePayloadCustomField({
                        name: 'Custom Field',
                        variable: CUSTOM_FIELD_VARIABLE,
                        sortOrder: 2
                    }),
                    createFakePayloadKeyValueField({
                        name: 'Key Value Field',
                        variable: KEY_VALUE_FIELD_VARIABLE,
                        sortOrder: 3
                    })
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

        test('first key-value entry is not split when custom field is present @critical', async ({
            page
        }) => {
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentTypeVariable);

            await expect(page.getByTestId(`field-${CUSTOM_FIELD_VARIABLE}`)).toBeVisible({
                timeout: 15000
            });

            const field = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await field.expectVisible();
            await field.addEntry('mykey', 'myvalue');

            await field.expectEntryCount(1);
            await field.expectEntry('mykey', 'myvalue');
            await field.expectKeyAbsent('m');
            await field.expectKeyAbsent('y');
            await field.expectKeyAbsent('k');
            await field.expectKeyAbsent('e');
        });

        test('save reloads and persists key-value entry with custom field present @critical', async ({
            page
        }) => {
            const title = `E2E KV Custom ${faker.lorem.word()}`;
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentTypeVariable);

            await expect(page.getByTestId(`field-${CUSTOM_FIELD_VARIABLE}`)).toBeVisible({
                timeout: 15000
            });

            const field = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await field.addEntry('mykey', 'myvalue');

            await saveAndReload(page, title);

            const reloadedField = new KeyValueField(page, KEY_VALUE_FIELD_VARIABLE);
            await reloadedField.expectEntryCount(1);
            await reloadedField.expectEntry('mykey', 'myvalue');
        });
    });
});
