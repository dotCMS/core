export const basicField = {
    clazz: '',
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
export const dotFormLayoutMock = [
    {
        divider: Object.assign({}, basicField),
        columns: [
            {
                columnDivider: Object.assign({}, basicField),
                fields: [
                    Object.assign({}, basicField, { variable: 'textfield1', required: true, name: 'TexField', fieldType: 'Text' })
                ]
            }
        ]
    },
    {
        divider: Object.assign({}, basicField),
        columns: [
            {
                columnDivider: Object.assign({}, basicField),
                fields: [
                    Object.assign({}, basicField, { defaultValue: 'key|value,llave|valor', fieldType: 'Key-Value', name: 'Key Value:', required: false, variable: 'keyvalue2' })
                ]
            },
            {
                columnDivider: Object.assign({}, basicField),
                fields: [
                    Object.assign({}, basicField, { defaultValue: '2', fieldType: 'Select', name: 'Dropdwon', required: false, values: '|,labelA|1,labelB|2,labelC|3', variable: 'dropdown3' })
                ]
            }
        ]
    }
];
export const fieldMockNotRequired = [
    {
        divider: Object.assign({}, basicField),
        columns: [
            {
                columnDivider: Object.assign({}, basicField),
                fields: [
                    Object.assign({}, basicField, { defaultValue: 'key|value,llave|valor', fieldType: 'Key-Value', name: 'Key Value:', required: false, variable: 'keyvalue2' })
                ],
            }
        ]
    }
];
