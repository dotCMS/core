import {
    getClassNames,
    getDotOptionsFromFieldValue,
    getErrorClass,
    getHintId,
    getId,
    getLabelId,
    getOriginalStatus,
    getStringFromDotKeyArray,
    getTagError,
    getTagHint,
    isFileAllowed,
    updateStatus
} from './utils';

describe('getClassNames', () => {
    it('should return field CSS classes', () => {
        let status = { dotValid: false, dotTouched: false, dotPristine: true };
        expect(getClassNames(status, true)).toEqual({
            'dot-valid': true,
            'dot-invalid': false,
            'dot-pristine': true,
            'dot-dirty': false,
            'dot-touched': false,
            'dot-untouched': true
        });

        status = { dotValid: true, dotTouched: true, dotPristine: false };
        expect(getClassNames(status, true)).toEqual({
            'dot-dirty': true,
            'dot-invalid': false,
            'dot-pristine': false,
            'dot-required': undefined,
            'dot-touched': true,
            'dot-untouched': false,
            'dot-valid': true
        });
    });
});

describe('getDotOptionsFromFieldValue', () => {
    it('should return label/value', () => {
        const items = getDotOptionsFromFieldValue('key1|A,key2|B');
        expect(items.length).toBe(2);
        expect(items).toEqual([
            { label: 'key1', value: 'A' },
            { label: 'key2', value: 'B' }
        ]);
    });

    it('should support \r\n as option splitter', () => {
        const items = getDotOptionsFromFieldValue('key1|A\r\nkey2|B');
        expect(items.length).toBe(2);
        expect(items).toEqual([
            { label: 'key1', value: 'A' },
            { label: 'key2', value: 'B' }
        ]);
    });

    it('should support \r\n and semicolon as option splitter', () => {
        const items = getDotOptionsFromFieldValue('key1|A\r\nkey2|B,key3|C');
        expect(items.length).toBe(3);
        expect(items).toEqual([
            { label: 'key1', value: 'A' },
            { label: 'key2', value: 'B' },
            { label: 'key3', value: 'C' }
        ]);
    });

    it('should empty array when invalid format', () => {
        const items = getDotOptionsFromFieldValue('key1A, key2/B, @');
        expect(items.length).toBe(0);
    });

    it('should handle other type', () => {
        const items = getDotOptionsFromFieldValue(null);
        expect(items.length).toBe(0);
    });
});

describe('getErrorClass', () => {
    it('should return error CSS', () => {
        expect(getErrorClass(false)).toEqual('dot-field__error');
    });
    it('should not return error CSS', () => {
        expect(getErrorClass(true)).toBeUndefined();
    });
});

describe('getHintId', () => {
    it('should return hint id correctly', () => {
        expect(getHintId('***^^^HelloWorld123$$$###')).toEqual('hint-helloworld123');
    });

    it('should return undefined', () => {
        expect(getHintId('')).toBeUndefined();
    });
});

describe('getId', () => {
    it('should return id', () => {
        expect(getId('some123Name#$%^&')).toBe('dot-some123name');
    });
});

describe('getLabelId', () => {
    it('should return label id correctly', () => {
        expect(getLabelId('***^^^HelloWorld123$$$###')).toEqual('label-helloworld123');
    });

    it('should return undefined', () => {
        expect(getLabelId('')).toBeUndefined();
    });
});

describe('getOriginalStatus', () => {
    it('should return initial field Status', () => {
        expect(getOriginalStatus()).toEqual({
            dotValid: true,
            dotTouched: false,
            dotPristine: true
        });
    });
    it('should return field Status with overwrite dotValid equal false', () => {
        expect(getOriginalStatus(false)).toEqual({
            dotValid: false,
            dotTouched: false,
            dotPristine: true
        });
    });
});

describe('getStringFromDotKeyArray', () => {
    it('should transform to string', () => {
        expect(
            getStringFromDotKeyArray([
                {
                    key: 'some1',
                    value: 'val1'
                },
                {
                    key: 'some45',
                    value: 'val99'
                }
            ])
        ).toBe('some1|val1,some45|val99');
    });
});

describe('getTagError', () => {
    it('should return error tag', () => {
        const message = 'Error Msg';
        const jsxTag = getTagError(true, message);
        expect(jsxTag['$attrs$']).toEqual({ class: 'dot-field__error-message' });
        expect(jsxTag['$children$'][0]['$text$']).toEqual(message);
    });
    it('should not return Error tag', () => {
        expect(getTagError(false, 'Error Msg')).toEqual(null);
    });
});

describe('getTagHint', () => {
    it('should return Hint tag', () => {
        const meessage = 'this is a hint';
        const jsxTag: any = getTagHint(meessage);
        console.log(jsxTag);
        expect(jsxTag['$attrs$']).toEqual({ class: 'dot-field__hint', id: 'hint-this-is-a-hint' });
        expect(jsxTag['$children$'][0]['$text$']).toEqual(meessage);
    });
    it('should not return Hint tag', () => {
        expect(getTagHint('')).toBeNull();
    });
});

describe('updateStatus', () => {
    it('should return updated field Status', () => {
        const status = { dotValid: false, dotTouched: false, dotPristine: true };
        expect(updateStatus(status, { dotTouched: true })).toEqual({
            dotValid: false,
            dotTouched: true,
            dotPristine: true
        });
    });
});

xdescribe('isValidURL', () => {
    // new URL is not available in headless browser.
});

describe('isFileAllowed', () => {
    it('should return true when file extension is valid', () => {
        expect(isFileAllowed('file.pdf', '.png, .pdf')).toBe(true);
    });

    it('should return true when allowedExtensions are any', () => {
        expect(isFileAllowed('file.pdf', '*')).toBe(true);
    });

    it('should return true when allowedExtensions are empty', () => {
        expect(isFileAllowed('file.pdf', '')).toBe(true);
    });

    it('should return false when file extension is not valid', () => {
        expect(isFileAllowed('file.pdf', '.png')).toBe(false);
    });
});
