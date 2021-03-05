import {
    DotCMSContentTypeField,
    DotCMSContentType,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';
import { EMPTY_FIELD } from '@portlets/shared/dot-content-types-edit/components/fields/util/field-util';

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
    workflows: []
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
