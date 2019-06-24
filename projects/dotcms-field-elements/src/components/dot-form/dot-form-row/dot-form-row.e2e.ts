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
                        defaultValue: 'key|value,llave|valor',
                        fieldType: 'Key-Value',
                        name: 'Key Value:',
                        required: false,
                        variable: 'keyvalue2'
                    }
                ]
            }, {
                fields: [
                    {
                        ...basicField,
                        defaultValue: '2',
                        fieldType: 'Select',
                        name: 'Dropdwon',
                        required: false,
                        values: '|,labelA|1,labelB|2,labelC|3',
                        variable: 'dropdown3'
                    }
                ]
            }
        ],
    }];

describe('dot-form-row', () => {
    let page: E2EPage;
    let element: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-form></dot-form>`
        });
        element = await page.find('dot-form');
    });

    describe('columns', () => {
        beforeEach(async () => {
            element.setProperty('layout', fieldsMock);
            element.setProperty('fieldsToShow', 'test');
            await page.waitForChanges();
        });

        it('should have 2 columns', async () => {
            const columns = await element.findAll('dot-form-column');
            expect(columns.length).toBe(2);
        });

        it('should set values on dot-form-row', async () => {
            const firstColumn = await element.find('dot-form-column');
            expect(await firstColumn.getProperty('column')).toEqual(fieldsMock[0].columns[0]);
            expect(await firstColumn.getProperty('fieldsToShow')).toEqual('test');
        });
    });
});
