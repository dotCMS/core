import {
    DEFAULT_SPLITTER_RATIO,
    dedupeAndCap,
    formatBody,
    formatHistoryLabel,
    getDownloadParams,
    HISTORY_MAX_ENTRIES,
    HISTORY_STORAGE_KEY,
    isValidHistory,
    isValidRatio,
    JSON_PRETTY_PRINT_MAX_BYTES,
    readJson,
    removeKey,
    SPLITTER_STORAGE_KEY,
    VELOCITY_HELP_EXAMPLES,
    writeJson
} from './dot-velocity-playground.utils';

describe('dot-velocity-playground.utils', () => {
    describe('readJson', () => {
        afterEach(() => window.localStorage.clear());

        it('returns the fallback when the key is missing', () => {
            expect(readJson('missing-key', { ok: false })).toEqual({ ok: false });
        });

        it('parses and returns stored JSON', () => {
            window.localStorage.setItem('answer', JSON.stringify(42));
            expect(readJson<number>('answer', 0)).toBe(42);
        });

        it('returns the fallback when the stored payload is not valid JSON', () => {
            window.localStorage.setItem('broken', '{not json');
            expect(readJson('broken', 'fallback')).toBe('fallback');
        });
    });

    describe('writeJson + removeKey', () => {
        afterEach(() => window.localStorage.clear());

        it('round-trips JSON values through localStorage', () => {
            writeJson('roundtrip', { a: 1 });
            expect(JSON.parse(window.localStorage.getItem('roundtrip') ?? '')).toEqual({ a: 1 });
        });

        it('removeKey drops the stored entry', () => {
            window.localStorage.setItem('drop-me', 'x');
            removeKey('drop-me');
            expect(window.localStorage.getItem('drop-me')).toBeNull();
        });
    });

    describe('isValidHistory', () => {
        it('accepts arrays of strings', () => {
            expect(isValidHistory(['$a', '$b'])).toBe(true);
            expect(isValidHistory([])).toBe(true);
        });

        it('rejects non-arrays and mixed-type arrays', () => {
            expect(isValidHistory({ oops: true })).toBe(false);
            expect(isValidHistory(['ok', 42])).toBe(false);
            expect(isValidHistory(null)).toBe(false);
        });
    });

    describe('isValidRatio', () => {
        it('accepts a 2-tuple of finite numbers', () => {
            expect(isValidRatio([50, 50])).toBe(true);
            expect(isValidRatio([0, 100])).toBe(true);
        });

        it('rejects tuples with NaN, infinity, wrong length, or non-numbers', () => {
            expect(isValidRatio([NaN, 50])).toBe(false);
            expect(isValidRatio([Infinity, 50])).toBe(false);
            expect(isValidRatio([50])).toBe(false);
            expect(isValidRatio([50, 50, 50])).toBe(false);
            expect(isValidRatio(['50', '50'])).toBe(false);
        });
    });

    describe('dedupeAndCap', () => {
        it('returns the history unchanged when the entry is blank', () => {
            const history = ['$a', '$b'];
            expect(dedupeAndCap(history, '   ')).toBe(history);
        });

        it('prepends the trimmed entry to the head', () => {
            expect(dedupeAndCap(['$a'], '  $b  ')).toEqual(['$b', '$a']);
        });

        it('collapses entries that differ only by surrounding whitespace', () => {
            expect(dedupeAndCap(['$dup'], '  $dup  ')).toEqual(['$dup']);
        });

        it('caps the resulting list at HISTORY_MAX_ENTRIES', () => {
            const history = Array.from({ length: HISTORY_MAX_ENTRIES }, (_, i) => `$old_${i}`);
            const next = dedupeAndCap(history, '$new');
            expect(next).toHaveLength(HISTORY_MAX_ENTRIES);
            expect(next[0]).toBe('$new');
            expect(next).not.toContain(`$old_${HISTORY_MAX_ENTRIES - 1}`);
        });
    });

    describe('formatBody', () => {
        it('pretty-prints JSON with 2-space indentation', () => {
            expect(formatBody('{"a":1,"b":{"c":2}}', 'json')).toBe(
                '{\n  "a": 1,\n  "b": {\n    "c": 2\n  }\n}'
            );
        });

        it('returns the raw body when the JSON payload is malformed', () => {
            expect(formatBody('{not json', 'json')).toBe('{not json');
        });

        it('returns the raw body for non-JSON content types', () => {
            expect(formatBody('<root><a/></root>', 'xml')).toBe('<root><a/></root>');
            expect(formatBody('plain text', 'plaintext')).toBe('plain text');
        });

        it('returns the raw body when JSON exceeds JSON_PRETTY_PRINT_MAX_BYTES', () => {
            const padding = 'x'.repeat(JSON_PRETTY_PRINT_MAX_BYTES);
            const raw = `{"v":"${padding}"}`;
            expect(formatBody(raw, 'json')).toBe(raw);
        });

        it('returns the raw body when the input is empty / whitespace-only', () => {
            expect(formatBody('', 'json')).toBe('');
            expect(formatBody('   ', 'json')).toBe('   ');
        });
    });

    describe('formatHistoryLabel', () => {
        it('collapses internal whitespace and trims', () => {
            expect(formatHistoryLabel('  $foo   $bar\n$baz ', '(empty)')).toBe('$foo $bar $baz');
        });

        it('truncates to 60 chars and appends ellipsis', () => {
            const long = 'x'.repeat(80);
            const label = formatHistoryLabel(long, '(empty)');
            expect(label).toHaveLength(61);
            expect(label.endsWith('…')).toBe(true);
        });

        it('falls back to the empty label when the entry is blank', () => {
            expect(formatHistoryLabel('   ', '(empty)')).toBe('(empty)');
        });
    });

    describe('getDownloadParams', () => {
        it('maps json content type to a .json extension and application/json mime', () => {
            expect(getDownloadParams('json')).toEqual({ ext: 'json', mime: 'application/json' });
        });

        it('maps xml content type to a .xml extension and application/xml mime', () => {
            expect(getDownloadParams('xml')).toEqual({ ext: 'xml', mime: 'application/xml' });
        });

        it('maps plaintext to .txt and text/plain', () => {
            expect(getDownloadParams('plaintext')).toEqual({ ext: 'txt', mime: 'text/plain' });
        });
    });

    describe('VELOCITY_HELP_EXAMPLES', () => {
        it('is a non-empty list where each entry has a title and code', () => {
            expect(VELOCITY_HELP_EXAMPLES.length).toBeGreaterThan(0);
            for (const ex of VELOCITY_HELP_EXAMPLES) {
                expect(ex.title).toBeTruthy();
                expect(ex.code).toBeTruthy();
            }
        });
    });

    describe('constants surface', () => {
        it('exposes the expected storage keys', () => {
            expect(HISTORY_STORAGE_KEY).toBe('velocityPlayground');
            expect(SPLITTER_STORAGE_KEY).toBe('velocityPlayground.splitterRatio');
        });

        it('keeps the default splitter ratio centered', () => {
            expect(DEFAULT_SPLITTER_RATIO).toEqual([50, 50]);
        });
    });
});
