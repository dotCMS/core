import { ulidTimestamp } from './ulid.util';

describe('ulidTimestamp', () => {
    // Minted by dotCMS's own `com.dotmarketing.util.ULID#nextULID(1781258400000L)`.
    const MINTED_ID = '01KTXMCC8095W38EQCB7P14XP5';
    const MINTED_AT = '2026-06-12T10:00:00.000Z';

    it('decodes the millisecond timestamp from the first 10 characters', () => {
        expect(ulidTimestamp(MINTED_ID)).toBe(MINTED_AT);
    });

    it('is case-insensitive', () => {
        expect(ulidTimestamp(MINTED_ID.toLowerCase())).toBe(MINTED_AT);
    });

    it('returns null for ids that are not ULIDs', () => {
        expect(ulidTimestamp('d5d7a5d8-bc26-4e3d-a5b3-1b2c3d4e5f60')).toBeNull();
        expect(ulidTimestamp('01ARZ3NDEKTSV4RRFFQ69G5FA')).toBeNull(); // 25 chars
        expect(ulidTimestamp('01ARZ3NDEKTSV4RRFFQ69G5FAU')).toBeNull(); // U is not Crockford
        expect(ulidTimestamp('')).toBeNull();
        expect(ulidTimestamp(null)).toBeNull();
    });

    it('returns null when the decoded date is implausible', () => {
        expect(ulidTimestamp('00000000000000000000000000')).toBeNull(); // 1970
        expect(ulidTimestamp('7ZZZZZZZZZZZZZZZZZZZZZZZZZ')).toBeNull(); // far future
    });
});
