import { HttpErrorResponse } from '@angular/common/http';

import {
    DEFAULT_SPLITTER_RATIO,
    dedupeAndCap,
    firstLine,
    formatBody,
    formatErrorTrace,
    formatHistoryLabel,
    formatWarnings,
    getDownloadParams,
    HISTORY_MAX_ENTRIES,
    HISTORY_STORAGE_KEY,
    isValidHistory,
    isValidRatio,
    JSON_PRETTY_PRINT_MAX_BYTES,
    parseVelocityError,
    parseWarningsHeader,
    readJson,
    removeKey,
    SPLITTER_STORAGE_KEY,
    UNKNOWN_ERROR_KEY,
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

    describe('parseVelocityError', () => {
        const makeError = (body: unknown, status = 400): HttpErrorResponse =>
            new HttpErrorResponse({ error: body, status, statusText: 'error' });

        it('parses the structured 400 body from a JSON string (responseType: text)', () => {
            const detail = {
                message: 'Encountered "#end" — expected #if',
                errorType: 'ParseErrorException',
                templateName: 'dynamic velocity',
                line: 12,
                column: 3
            };

            const result = parseVelocityError(makeError(JSON.stringify({ errors: [detail] })));

            expect(result.isVelocityError).toBe(true);
            expect(result.error).toEqual({
                message: detail.message,
                structured: detail,
                warnings: []
            });
        });

        it('includes warnings from the structured 400 body', () => {
            const detail = { message: 'boom', errorType: 'ParseErrorException' };
            const warning = {
                type: 'UNDEFINED_REFERENCE',
                message: "Undefined reference '$x'",
                reference: '$x',
                line: 2
            };

            const result = parseVelocityError(
                makeError(JSON.stringify({ errors: [detail], warnings: [warning] }))
            );

            expect(result.error.warnings).toEqual([warning]);
        });

        it('parses the structured body when it arrives already as an object', () => {
            const detail = { message: 'boom', errorType: 'MethodInvocationException' };

            const result = parseVelocityError(makeError({ errors: [detail] }));

            expect(result.isVelocityError).toBe(true);
            expect(result.error.structured).toEqual(detail);
        });

        it('takes only the first error when several are returned', () => {
            const first = { message: 'first', line: 1 };
            const result = parseVelocityError(
                makeError(JSON.stringify({ errors: [first, { message: 'second' }] }))
            );

            expect(result.error.structured).toEqual(first);
        });

        it('falls back to the raw text body for unstructured errors (not a velocity error)', () => {
            const result = parseVelocityError(makeError('Something went wrong', 500));

            expect(result.isVelocityError).toBe(false);
            expect(result.error).toEqual({
                message: 'Something went wrong',
                structured: null,
                warnings: []
            });
        });

        it('falls back to a nested error.message when there is no errors array', () => {
            const result = parseVelocityError(makeError({ message: 'nested detail' }, 500));

            expect(result.isVelocityError).toBe(false);
            expect(result.error).toEqual({
                message: 'nested detail',
                structured: null,
                warnings: []
            });
        });

        it('falls back to the unknown i18n key when nothing usable is present', () => {
            const result = parseVelocityError(makeError(null, 0));

            expect(result.isVelocityError).toBe(false);
            expect(result.error.structured).toBeNull();
            // HttpErrorResponse synthesizes a generic message for status 0; when the body is
            // empty we still guarantee a non-empty message for the banner.
            expect(result.error.message.length).toBeGreaterThan(0);
        });

        it('does not treat an empty errors array as a velocity error', () => {
            const result = parseVelocityError(makeError(JSON.stringify({ errors: [] })));

            expect(result.isVelocityError).toBe(false);
        });

        it('ignores a malformed JSON string body and does not throw', () => {
            const result = parseVelocityError(makeError('{ not valid json', 500));

            expect(result.isVelocityError).toBe(false);
            expect(result.error.message).toBe('{ not valid json');
        });

        it('uses the unknown key constant for a null error', () => {
            const result = parseVelocityError(null);

            expect(result.error.message).toBe(UNKNOWN_ERROR_KEY);
            expect(result.error.structured).toBeNull();
        });
    });

    describe('firstLine', () => {
        it('returns the message unchanged when it is a single line', () => {
            expect(firstLine('Something went wrong')).toBe('Something went wrong');
        });

        it('returns only the first non-empty line of a multi-line message', () => {
            const multi =
                'Encountered "<EOF>" at line 5, column 39\nWas expecting one of:\n  "[" ...\n  "(" ...';
            expect(firstLine(multi)).toBe('Encountered "<EOF>" at line 5, column 39');
        });

        it('skips leading blank lines', () => {
            expect(firstLine('\n\n  real message\nmore')).toBe('real message');
        });

        it('trims surrounding whitespace', () => {
            expect(firstLine('   padded   ')).toBe('padded');
        });
    });

    describe('formatErrorTrace', () => {
        it('returns just the resolved message for an unstructured error', () => {
            const trace = formatErrorTrace(
                { message: 'raw', structured: null, warnings: [] },
                'Something went wrong'
            );

            expect(trace).toBe('Something went wrong');
        });

        it('prefixes the header with the error type and appends template + location lines', () => {
            const trace = formatErrorTrace(
                {
                    message: 'ignored — resolvedMessage wins',
                    warnings: [],
                    structured: {
                        message: 'Encountered "#end"',
                        errorType: 'ParseErrorException',
                        templateName: 'dynamic velocity',
                        line: 12,
                        column: 3
                    }
                },
                'Encountered "#end"'
            );

            expect(trace).toBe(
                [
                    'ParseErrorException: Encountered "#end"',
                    '    at template "dynamic velocity"',
                    '    at line 12, column 3'
                ].join('\n')
            );
        });

        it('uses the full detail (not the summary) as the header body when present', () => {
            const trace = formatErrorTrace(
                {
                    message: 'Encountered "<EOF>" at line 6, column 39',
                    warnings: [],
                    structured: {
                        message: 'Encountered "<EOF>" at line 6, column 39',
                        errorType: 'ParseErrorException',
                        detail: 'Encountered "<EOF>" at line 6, column 39\nWas expecting one of:\n  "[" ...'
                    }
                },
                'Encountered "<EOF>" at line 6, column 39'
            );

            expect(trace).toBe(
                'ParseErrorException: Encountered "<EOF>" at line 6, column 39\nWas expecting one of:\n  "[" ...'
            );
        });

        it('omits the column segment when only a line is reported', () => {
            const trace = formatErrorTrace(
                {
                    message: 'boom',
                    warnings: [],
                    structured: { message: 'boom', errorType: 'MethodInvocationException', line: 5 }
                },
                'boom'
            );

            expect(trace).toBe(
                ['MethodInvocationException: boom', '    at line 5'].join('\n')
            );
        });

        it('renders the message alone when structured detail has no type or location', () => {
            const trace = formatErrorTrace(
                { message: 'boom', structured: { message: 'boom' }, warnings: [] },
                'boom'
            );

            expect(trace).toBe('boom');
        });

        it('appends collected warnings after the error', () => {
            const trace = formatErrorTrace(
                {
                    message: 'boom',
                    structured: { message: 'boom', errorType: 'ParseErrorException' },
                    warnings: [
                        {
                            type: 'UNDEFINED_REFERENCE',
                            message: "Undefined reference '$x'",
                            reference: '$x',
                            line: 2,
                            column: 1
                        }
                    ]
                },
                'boom'
            );

            expect(trace).toBe(
                [
                    'ParseErrorException: boom',
                    '',
                    '1 warning:',
                    "  - [UNDEFINED_REFERENCE] Undefined reference '$x' (line 2, column 1)"
                ].join('\n')
            );
        });
    });

    describe('formatWarnings', () => {
        it('returns an empty string for no warnings', () => {
            expect(formatWarnings([])).toBe('');
        });

        it('formats a single warning with a singular header and location', () => {
            expect(
                formatWarnings([
                    {
                        type: 'INVALID_METHOD',
                        message: 'nope() missing',
                        reference: '$o',
                        line: 4,
                        column: 2
                    }
                ])
            ).toBe(['1 warning:', '  - [INVALID_METHOD] nope() missing (line 4, column 2)'].join('\n'));
        });

        it('uses a plural header and omits location when unavailable', () => {
            expect(
                formatWarnings([
                    { type: 'UNDEFINED_REFERENCE', message: 'a' },
                    { type: 'NULL_SET', message: 'b' }
                ])
            ).toBe(['2 warnings:', '  - [UNDEFINED_REFERENCE] a', '  - [NULL_SET] b'].join('\n'));
        });
    });

    describe('parseWarningsHeader', () => {
        it('returns [] for null/empty', () => {
            expect(parseWarningsHeader(null)).toEqual([]);
            expect(parseWarningsHeader('')).toEqual([]);
            expect(parseWarningsHeader('   ')).toEqual([]);
        });

        it('parses a JSON array of warnings', () => {
            const warnings = [{ type: 'UNDEFINED_REFERENCE', message: 'a', reference: '$a' }];
            expect(parseWarningsHeader(JSON.stringify(warnings))).toEqual(warnings);
        });

        it('returns [] for malformed JSON or a non-array', () => {
            expect(parseWarningsHeader('{ not json')).toEqual([]);
            expect(parseWarningsHeader('{"not":"array"}')).toEqual([]);
        });
    });
});
