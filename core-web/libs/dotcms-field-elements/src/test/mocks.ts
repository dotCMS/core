import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';

export const basicField: DotCMSContentTypeField =   {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
    contentTypeId: '',
    dataType: 'TEXT',
    defaultValue: '',
    fieldType: 'Text',
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
    variable: '',
    forceIncludeInApi: true
};

export const dotFormLayoutMock: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...basicField
        },
        columns: [
            {
                columnDivider: {
                    ...basicField
                },
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
        ]
    },
    {
        divider: {
            ...basicField
        },
        columns: [
            {
                columnDivider: {
                    ...basicField
                },
                fields: [
                    {
                        ...basicField,
                        defaultValue: 'key|value,llave|valor',
                        fieldType: 'Key-Value',
                        dataType: 'LONG_TEXT',
                        name: 'Key Value:',
                        required: false,
                        variable: 'keyvalue2',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableKeyValueField'
                    }
                ]
            },
            {
                columnDivider: {
                    ...basicField
                },
                fields: [
                    {
                        ...basicField,
                        defaultValue: '2',
                        fieldType: 'Select',
                        name: 'Dropdwon',
                        required: false,
                        values: '|,labelA|1,labelB|2,labelC|3',
                        variable: 'dropdown3',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField'
                    }
                ]
            }
        ]
    }
];

export const fieldMockNotRequired: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...basicField
        },
        columns: [
            {
                columnDivider: {
                    ...basicField
                },
                fields: [
                    {
                        ...basicField,
                        defaultValue: 'key|value,llave|valor',
                        fieldType: 'Key-Value',
                        name: 'Key Value:',
                        required: false,
                        variable: 'keyvalue2',
                        dataType: 'LONG_TEXT',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableKeyValueField'
                    }
                ]
            }
        ]
    }
];
