import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';

export const basicField: DotCMSContentTypeField = {
    clazz: DotCMSClazzes.TEXT,
    contentTypeId: '',
    dataType: '',
    defaultValue: '',
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
                        name: 'Key Value:',
                        required: false,
                        variable: 'keyvalue2'
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
                        variable: 'dropdown3'
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
                        variable: 'keyvalue2'
                    }
                ]
            }
        ]
    }
];
