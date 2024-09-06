import { DotCMSContentType } from 'dotcms-models';

import { DotApiForm } from './DotApiForm';

const EMPTY = {
    contentTypeId: null,
    dataType: null,
    fieldTypeLabel: null,
    fieldVariables: [],
    fixed: null,
    iDate: null,
    id: null,
    indexed: null,
    listed: null,
    modDate: null,
    readOnly: null,
    searchable: null,
    sortOrder: null,
    unique: null,
    clazz: null,
    regexCheck: null,
    values: null
};

const fieldReturned = [
    {
        ...EMPTY,
        fieldType: 'Text',
        defaultValue: 'defaultValue1',
        hint: 'hint1',
        name: 'field1',
        required: true,
        value: 'value1',
        variable: 'field1'
    },
    {
        ...EMPTY,
        fieldType: 'Text',
        defaultValue: 'defaultValue',
        hint: 'hint2',
        name: 'field2',
        required: true,
        value: 'value2',
        variable: 'field2'
    }
];

const contentTypeReturned: DotCMSContentType = {
    baseType: '',
    clazz: 'A',
    defaultType: true,
    description: '',
    detailPage: '',
    expireDateVar: 'string',
    fields: fieldReturned,
    fixed: true,
    folder: 'FolderA',
    host: 'HostA',
    iDate: 123456789,
    id: '',
    layout: [],
    modDate: 987654321,
    multilingualable: true,
    nEntries: 100,
    name: 'TestA',
    owner: 'me',
    publishDateVar: '',
    system: true,
    systemActionMappings: {},
    urlMapPattern: '',
    variable: 'contentTest1',
    versionable: true,
    workflows: []
};

/** @hidden */
class DotApiContentTypeMock {
    get(): Promise<DotCMSContentType> {
        return new Promise((resolve) => {
            resolve(contentTypeReturned);
        });
    }
}

describe('DotApiForm', () => {
    let dotApiContentType;
    let dotApiForm;

    beforeEach(() => {});

    dotApiContentType = new DotApiContentTypeMock();
    it('should get form content type', (done) => {
        dotApiContentType = new DotApiContentTypeMock();
        const config = {
            identifier: '321'
        };

        dotApiForm = new DotApiForm(dotApiContentType, config);

        dotApiForm.get().then((_result) => {
            done();
        });
    });
});
