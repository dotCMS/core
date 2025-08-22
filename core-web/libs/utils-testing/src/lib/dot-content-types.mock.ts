import { faker } from '@faker-js/faker';

import {
    DotCMSContentTypeField,
    DotCMSContentType,
    DotCMSContentTypeLayoutRow,
    DotCMSBaseTypesContentTypes,
    DotCMSClazzes
} from '@dotcms/dotcms-models';

import { EMPTY_FIELD } from './field-util';

/**
 * Create a fake content type with the given overrides
 *
 * @export
 * @param {Partial<DotCMSContentType>} [overrides={}]
 * @return {*}  {DotCMSContentType}
 */
export function createFakeContentType(
    overrides: Partial<DotCMSContentType> = {}
): DotCMSContentType {
    return {
        id: faker.string.uuid(),
        name: faker.lorem.word(),
        baseType: DotCMSBaseTypesContentTypes.CONTENT,
        system: false,
        folder: '/',
        host: 'localhost',
        variable: faker.lorem.word(),
        icon: faker.lorem.word(),
        description: faker.lorem.sentence(),
        detailPage: faker.lorem.word(),
        expireDateVar: faker.lorem.word(),
        fields: [],
        fixed: false,
        layout: [],
        modDate: faker.date.recent().getTime(),
        iDate: faker.date.recent().getTime(),
        multilingualable: false,
        nEntries: 0,
        owner: faker.lorem.word(),
        publishDateVar: faker.lorem.word(),
        systemActionMappings: {},
        urlMapPattern: faker.lorem.word(),
        versionable: false,
        workflows: [],
        workflow: [],
        clazz: DotCMSClazzes.SIMPLE_CONTENT_TYPE,
        defaultType: false,
        ...overrides
    };
}

export const dotcmsContentTypeBasicMock: DotCMSContentType = {
    baseType: null,
    clazz: null,
    defaultType: false,
    description: null,
    detailPage: null,
    expireDateVar: null,
    fields: [],
    fixed: false,
    folder: null,
    host: null,
    iDate: null,
    id: null,
    layout: [],
    modDate: null,
    multilingualable: false,
    nEntries: null,
    name: null,
    owner: null,
    publishDateVar: null,
    system: false,
    urlMapPattern: null,
    variable: null,
    versionable: false,
    workflows: [],
    metadata: {}
};

export const dotcmsContentTypeFieldBasicMock: DotCMSContentTypeField = {
    ...EMPTY_FIELD
};

export const fieldsWithBreakColumn: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...dotcmsContentTypeFieldBasicMock
        },
        columns: [
            {
                columnDivider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
                },
                fields: [
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    },
                    {
                        ...dotcmsContentTypeFieldBasicMock,
                        clazz: 'contenttype.column.break',
                        name: 'Column'
                    },
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    }
                ]
            }
        ]
    }
];

export const fieldsBrokenWithColumns: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...dotcmsContentTypeFieldBasicMock
        },
        columns: [
            {
                columnDivider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
                },
                fields: [
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    }
                ]
            },
            {
                columnDivider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
                },
                fields: [
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    }
                ]
            }
        ]
    }
];
