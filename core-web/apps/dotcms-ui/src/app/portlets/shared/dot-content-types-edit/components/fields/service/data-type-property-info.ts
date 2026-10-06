/** One selectable data type: a message key and the value the field stores. */
export interface DataTypeOption {
    text: string;
    value: string;
}

/** Keyed by a field's `clazz`, which the server supplies, so a lookup can miss. */
export const DATA_TYPE_PROPERTY_INFO: Record<string, DataTypeOption[]> = {
    // Radio inputs: binary, text, date, longText, bool, float, integer
    'com.dotcms.contenttype.model.field.ImmutableRadioField': [
        {
            text: 'contenttypes.field.properties.data_type.values.text',
            value: 'TEXT'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.boolean',
            value: 'BOOL'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.decimal',
            value: 'FLOAT'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.number',
            value: 'INTEGER'
        }
    ],
    'com.dotcms.contenttype.model.field.ImmutableSelectField': [
        {
            text: 'contenttypes.field.properties.data_type.values.text',
            value: 'TEXT'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.boolean',
            value: 'BOOL'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.decimal',
            value: 'FLOAT'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.number',
            value: 'INTEGER'
        }
    ],
    'com.dotcms.contenttype.model.field.ImmutableTextField': [
        {
            text: 'contenttypes.field.properties.data_type.values.text',
            value: 'TEXT'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.decimal',
            value: 'FLOAT'
        },
        {
            text: 'contenttypes.field.properties.data_type.values.number',
            value: 'INTEGER'
        }
    ]
};
