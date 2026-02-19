import {
    fieldCustomProcess,
    getFieldVariableValue,
    getFieldsFromLayout,
    getErrorMessage,
    shouldShowField
} from '.';

import { basicField, dotFormLayoutMock } from '../../../test/mocks';

describe('getFieldVariableValue', () => {
    const variables = [
        {
            clazz: 'a',
            fieldId: '1',
            id: '1a',
            key: 'key1',
            value: 'value1'
        },
        {
            clazz: 'b',
            fieldId: '2',
            id: '2b',
            key: 'key2',
            value: 'value2'
        }
    ];

    it('should return field variable value', () => {
        expect(getFieldVariableValue(variables, 'key2')).toEqual('value2');
    });

    it('should return undefined if variable key not found', () => {
        expect(getFieldVariableValue(variables, 'key3')).toBe(undefined);
    });
});

describe('getErrorMessage', () => {
    it('should return error message', () => {
        const errorString = '{"errors":[{"message":"error test"}]}';
        expect(getErrorMessage(errorString)).toEqual('error test');
    });

    it('should return whole error obj', () => {
        const errorString = '{"errors":[{"code":"404"}]}';
        expect(getErrorMessage(errorString)).toEqual(errorString);
    });
});

describe('fieldCustomProcess', () => {
    it('should return object', () => {
        expect(fieldCustomProcess['DOT-KEY-VALUE']('a|b')).toEqual({ a: 'b' });
    });
});

describe('shouldShowField', () => {
    it('should return true', () => {
        const field = basicField;
        basicField.variable = 'A';
        expect(shouldShowField(field, 'A,B')).toBe(true);
    });

    it('should return false', () => {
        const field = basicField;
        basicField.variable = 'C';
        expect(shouldShowField(field, 'A,B')).toBe(false);
    });
});

describe('getFieldsFromLayout', () => {
    it('should fields array', () => {
        expect(getFieldsFromLayout(dotFormLayoutMock)).toEqual([
            {
                ...basicField,
                variable: 'textfield1',
                required: true,
                name: 'TexField',
                fieldType: 'Text'
            },
            {
                ...basicField,
                defaultValue: 'key|value,llave|valor',
                fieldType: 'Key-Value',
                name: 'Key Value:',
                required: false,
                variable: 'keyvalue2'
            },
            {
                ...basicField,
                defaultValue: '2',
                fieldType: 'Select',
                name: 'Dropdwon',
                required: false,
                values: '|,labelA|1,labelB|2,labelC|3',
                variable: 'dropdown3'
            }
        ]);
    });
});
