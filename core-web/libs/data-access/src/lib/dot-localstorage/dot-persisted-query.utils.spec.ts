import {
    buildPersistedQueryKey,
    PERSISTED_QUERY_KEY_PREFIX,
    readJson,
    removeKey,
    writeJson
} from './dot-persisted-query.utils';

describe('dot-persisted-query.utils', () => {
    const TEST_KEY = 'test.persisted-query.spec';

    beforeEach(() => {
        window.localStorage.clear();
    });

    afterEach(() => {
        window.localStorage.clear();
    });

    describe('buildPersistedQueryKey', () => {
        it('produces the documented storage key convention', () => {
            expect(buildPersistedQueryKey('query-tool')).toBe(
                `${PERSISTED_QUERY_KEY_PREFIX}.query-tool.lastQuery`
            );
        });
    });

    describe('writeJson / readJson', () => {
        it('round-trips a string value', () => {
            writeJson(TEST_KEY, 'select *');
            expect(readJson<string>(TEST_KEY, '')).toBe('select *');
        });

        it('round-trips a structured value', () => {
            writeJson(TEST_KEY, { a: 1, b: [true, 'two'] });
            expect(
                readJson<{ a: number; b: [boolean, string] }>(TEST_KEY, { a: 0, b: [false, ''] })
            ).toEqual({ a: 1, b: [true, 'two'] });
        });

        it('returns the fallback when the key is missing', () => {
            expect(readJson<string>('nonexistent-key', 'fallback')).toBe('fallback');
        });

        it('returns the fallback when the stored payload is not valid JSON', () => {
            window.localStorage.setItem(TEST_KEY, '{not-json');
            expect(readJson<string>(TEST_KEY, 'fallback')).toBe('fallback');
        });
    });

    describe('removeKey', () => {
        it('deletes a stored entry', () => {
            writeJson(TEST_KEY, 'to-remove');
            removeKey(TEST_KEY);
            expect(window.localStorage.getItem(TEST_KEY)).toBeNull();
        });

        it('is a noop when the key does not exist', () => {
            expect(() => removeKey('nonexistent-key')).not.toThrow();
        });
    });
});
