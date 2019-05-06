import {
    getDotOptionsFromFieldValue,
    getOriginalStatus,
    updateStatus,
    getClassNames,
    getTagHint,
    getTagError,
    getTagLabel,
    getErrorClass
} from './utils';

describe('getDotOptionsFromFieldValue', () => {
    it('should return label/value', () => {
        const rawItems = 'key1|A,key2|B';
        const items = getDotOptionsFromFieldValue(rawItems);
        expect(items.length).toBe(2);
        expect(items).toEqual([{ label: 'key1', value: 'A' }, { label: 'key2', value: 'B' }]);
    });
});

describe('getOriginalStatus', () => {
    it('should returns initial field Status', () => {
        const status = getOriginalStatus();
        expect(status).toEqual({ dotValid: true, dotTouched: false, dotPristine: true });
    });
    it('should returns field Status with overwrite dotValid equal false', async () => {
        const status = getOriginalStatus(false);
        expect(status).toEqual({ dotValid: false, dotTouched: false, dotPristine: true });
    });
});

describe('updateStatus', () => {
    it('should returns updated field Status', () => {
        const status = { dotValid: false, dotTouched: false, dotPristine: true };
        const changedStatus = { dotValid: true, dotTouched: true, dotPristine: false };
        const newStatus = updateStatus(status, changedStatus);
        expect(newStatus).toEqual(changedStatus);
    });
});

describe('getClassNames', () => {
    it('should returns field CSS classes', () => {
        const status = { dotValid: false, dotTouched: false, dotPristine: true };
        const cssClasses = getClassNames(status, true);
        expect(cssClasses).toEqual({
            'dot-valid': true,
            'dot-invalid': false,
            'dot-pristine': true,
            'dot-dirty': false,
            'dot-touched': false,
            'dot-untouched': true
        });
    });
});

describe('getTagHint', () => {
    it('should returns Hint tag', () => {
        const hint = 'Hint';
        const jsxTag: any = getTagHint(hint);
        expect(jsxTag.vattrs).toEqual({ class: 'dot-field__hint' });
        expect(jsxTag.vchildren).toEqual([{ vtext: 'Hint' }]);
    });
    it('should Not returns Hint tag', () => {
        const hint = '';
        const jsxTag: any = getTagHint(hint);
        expect(jsxTag).toEqual('');
    });
});

describe('getTagError', () => {
    it('should returns Error tag', () => {
        const message = 'Error Msg';
        const jsxTag: any = getTagError(true, message);
        expect(jsxTag.vattrs).toEqual({ class: 'dot-field__error-message' });
        expect(jsxTag.vchildren).toEqual([{ vtext: message }]);
    });
    it('should Not returns Error tag', () => {
        const message = 'Error Msg';
        const jsxTag: any = getTagError(false, message);
        expect(jsxTag).toEqual('');
    });
});

describe('getTagLabel', () => {
    it('should returns Label tag', () => {
        const param = { name: 'Label', label: 'Msg', required: true };
        const jsxTag: any = getTagLabel(param);
        expect(jsxTag.vattrs).toEqual({ class: 'dot-field__label' });
        expect(jsxTag.vchildren[0].vattrs).toEqual({ htmlFor: 'Label' });
        expect(jsxTag.vchildren[0].vchildren).toEqual([{ vtext: 'Msg' }]);
        expect(jsxTag.vchildren[1].vattrs).toEqual({'class': 'dot-field__required-mark'});
        expect(jsxTag.vchildren[1].vchildren).toEqual([{ vtext: '*' }]);
    });
});

describe('getErrorClass', () => {
    it('should returns Error CSS', () => {
        const cssClass = getErrorClass(false);
        expect(cssClass).toEqual('dot-field__error');
    });
    it('should Not returns Error CSS', () => {
        const cssClass = getErrorClass(true);
        expect(cssClass).toEqual('');
    });
});
