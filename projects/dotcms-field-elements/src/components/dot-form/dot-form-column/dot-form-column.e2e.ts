import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';
import { DotCMSContentTypeField, DotCMSContentTypeRow } from './../models';

const basicField: DotCMSContentTypeField = {
    clazz: '',
    contentTypeId: '',
    dataType: '',
    defaultValue: '',
    fieldContentTypeProperties: [],
    fieldType: '',
    fieldTypeLabel: '',
    fieldVariables: [],
    fixed: true,
    hint: '',
    iDate: 100,
    id: '',
    indexed: true,
    listed: true,
    modDate: 100,
    name: '',
    readOnly: true,
    regexCheck: '',
    required: true,
    searchable: true,
    sortOrder: 100,
    unique: true,
    values: '',
    variable: ''
};

const fieldsMock: DotCMSContentTypeRow[] = [
    {
        columns: [
            {
                fields: [
                    {
                        ...basicField,
                        variable: 'textfield1',
                        required: true,
                        name: 'TexField',
                        fieldType: 'Text'
                    }
                ]
            }
        ],
    }, {
        columns: [
            {
                fields: [
                    {
                        ...basicField,
                        defaultValue: 'key|value,llave|valor',
                        fieldType: 'Key-Value',
                        name: 'Key Value:',
                        required: false,
                        variable: 'keyvalue2'
                    }
                ]
            }
        ],
    }];

describe('dot-form-column', () => {
    let page: E2EPage;
    let element: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-form></dot-form>`
        });
        element = await page.find('dot-form');
    });

    describe('columns and fields', () => {
        beforeEach(async () => {
            element.setProperty('layout', fieldsMock);
            await page.waitForChanges();
        });

        it('should have 2 fields', async () => {
            const fields = await element.findAll('dot-form-column');
            expect(fields.length).toBe(2);
        });

        it('should have CSS class on field', async () => {
            const firstField = await element.find('dot-form-column');
            expect(firstField).toBeDefined();
        });
    });

    describe('@Props', () => {
        describe('column', () => {
            it('should render textfield and keyValue fields', async () => {
                element.setProperty('layout', fieldsMock);
                await page.waitForChanges();

                const textfield = await element.find('dot-textfield');
                const keyValue = await element.find('dot-key-value');
                expect(textfield).not.toBeNull();
                expect(keyValue).not.toBeNull();
            });

            it('should not render any fields', async () => {
                const fields = await element.findAll('dot-form-column');
                expect(fields.length).toBe(0);
            });
        });

        describe('fieldsToShow', () => {
            it('should only render textfield field', async () => {
                element.setProperty('layout', fieldsMock);
                element.setProperty('fieldsToShow', 'textfield1');
                await page.waitForChanges();

                const textfield = await element.find('dot-textfield');
                const keyValue = await element.find('dot-key-value');
                expect(textfield).not.toBeNull();
                expect(keyValue).toBeNull();
            });

            it('should render all fields (2)', async () => {
                element.setProperty('layout', fieldsMock);
                await page.waitForChanges();

                const fields = await element.findAll('dot-form-column');
                expect(fields.length).toBe(2);
            });
        });
    });
});
