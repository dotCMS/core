import { lenientBoolean } from './lenient-boolean';

describe('lenientBoolean', () => {
    const publish = lenientBoolean(true);
    const verify = lenientBoolean(false);

    it('passes real booleans through', () => {
        expect(publish.parse(true)).toBe(true);
        expect(publish.parse(false)).toBe(false);
    });

    it.each([
        ['false', false],
        ['FALSE', false],
        ['0', false],
        ['no', false],
        ['true', true],
        ['TRUE', true],
        ['1', true],
        ['yes', true],
        ['  false  ', false]
    ])('maps the string %p to %p', (input, expected) => {
        // Plain z.coerce.boolean() is wrong here: it uses JS truthiness, so "false" → true.
        expect(publish.parse(input)).toBe(expected);
    });

    it('applies the default when the value is absent', () => {
        expect(publish.parse(undefined)).toBe(true);
        expect(verify.parse(undefined)).toBe(false);
    });

    it('applies the default for an empty string rather than failing validation', () => {
        // `.default()` only substitutes on `undefined`, so returning the original '' from the
        // preprocessor skipped the default entirely and failed with "Expected boolean,
        // received string" — for an argument the caller never set.
        expect(publish.parse('')).toBe(true);
        expect(verify.parse('')).toBe(false);
        expect(publish.parse('   ')).toBe(true);
    });

    it('still rejects a string that means nothing', () => {
        expect(() => publish.parse('maybe')).toThrow();
    });
});
