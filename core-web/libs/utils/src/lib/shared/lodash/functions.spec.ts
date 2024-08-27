import { camelCase, isEmpty, isEqual } from './functions';

describe('Utility Functions', () => {
    describe('isEmpty', () => {
        it('should return true for null or undefined', () => {
            expect(isEmpty(null)).toBe(true);
            expect(isEmpty(undefined)).toBe(true);
        });

        it('should return true for an empty object', () => {
            expect(isEmpty({})).toBe(true);
        });

        it('should return false for a non-empty object', () => {
            expect(isEmpty({ key: 'value' })).toBe(false);
        });

        it('should return true for an empty array', () => {
            expect(isEmpty([])).toBe(true);
        });

        it('should return false for a non-empty array', () => {
            expect(isEmpty([1, 2, 3])).toBe(false);
        });

        it('should return true for an empty string', () => {
            expect(isEmpty('')).toBe(true);
            expect(isEmpty('   ')).toBe(true);
        });

        it('should return false for a non-empty string', () => {
            expect(isEmpty('text')).toBe(false);
        });
    });

    describe('isEqual', () => {
        it('should return true for identical values', () => {
            expect(isEqual(1, 1)).toBe(true);
            expect(isEqual('string', 'string')).toBe(true);
            expect(isEqual([1, 2, 3], [1, 2, 3])).toBe(true);
            expect(isEqual({ a: 1 }, { a: 1 })).toBe(true);
        });

        it('should return false for different types', () => {
            expect(isEqual(1, '1')).toBe(false);
            expect(isEqual({}, [])).toBe(false);
        });

        it('should return false for different values', () => {
            expect(isEqual(1, 2)).toBe(false);
            expect(isEqual('a', 'b')).toBe(false);
            expect(isEqual([1, 2, 3], [3, 2, 1])).toBe(false);
            expect(isEqual({ a: 1 }, { a: 2 })).toBe(false);
        });

        it('should return true for deeply equal objects', () => {
            expect(isEqual({ a: { b: 1 } }, { a: { b: 1 } })).toBe(true);
        });

        it('should return false for deeply unequal objects', () => {
            expect(isEqual({ a: { b: 1 } }, { a: { b: 2 } })).toBe(false);
        });
    });

    describe('camelCase', () => {
        it('should convert a string to camelCase', () => {
            expect(camelCase('hello world')).toBe('helloWorld');
            expect(camelCase('Hello World')).toBe('helloWorld');
        });

        it('should return an empty string if input is empty', () => {
            expect(camelCase('')).toBe('');
        });

        it('should handle single word strings', () => {
            expect(camelCase('word')).toBe('word');
        });

        it('should handle strings with multiple spaces', () => {
            expect(camelCase('  hello   world  ')).toBe('helloWorld');
        });
    });
});
