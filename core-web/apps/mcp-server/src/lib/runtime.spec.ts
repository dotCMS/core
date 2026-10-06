import { AbortError, HttpError, PolicyError, TimeoutError } from '@dotcms/ai/runtime';

import { errorMessage, MAX_ERROR_CHARS, toolFailure, type ToolFailure } from './runtime';

/** Parse what a tool handler actually returns — a JSON string, not an object. */
function parse(result: string): ToolFailure {
    return JSON.parse(result) as ToolFailure;
}

describe('errorMessage', () => {
    it('returns the message of an Error', () => {
        expect(errorMessage(new Error('boom'))).toBe('boom');
    });

    it('stringifies a non-Error throw', () => {
        expect(errorMessage('plain string')).toBe('plain string');
        expect(errorMessage(42)).toBe('42');
    });

    it('caps a huge message and says it was truncated', () => {
        // A dotCMS 5xx returns its full HTML stack-trace page and `HttpError.message` embeds
        // the body verbatim. A transfer manifest keeps one message per FAILED FILE, so 200
        // files against a broken instance would carry 200 copies of that page.
        const huge = 'x'.repeat(MAX_ERROR_CHARS * 3);
        const capped = errorMessage(new Error(huge));

        expect(capped.length).toBeLessThan(huge.length);
        expect(capped).toContain('truncated');
        expect(capped).toContain(String(huge.length));
    });

    it('leaves a message at exactly the cap alone', () => {
        const exact = 'y'.repeat(MAX_ERROR_CHARS);
        expect(errorMessage(new Error(exact))).toBe(exact);
    });
});

describe('toolFailure', () => {
    it('returns JSON carrying the operation, code and prefix', () => {
        const failure = parse(toolFailure('page_verify', new Error('nope')));

        expect(failure.ok).toBe(false);
        expect(failure.operation).toBe('page_verify');
        expect(failure.error).toContain('[MCP Server - page_verify]');
        expect(failure.error).toContain('nope');
        expect(failure.code).toBe('UNKNOWN');
    });

    describe('retryable', () => {
        // `retryable` has to be a FIELD: MCP hands the model a string, so `instanceof` is
        // unavailable on the far side and anything it must branch on has to survive JSON.
        it('is true for a timeout', () => {
            const failure = parse(toolFailure('op', new TimeoutError('too slow', 30_000)));
            expect(failure.retryable).toBe(true);
            expect(failure.code).toBe('TIMEOUT');
        });

        it.each([
            [408, 'Request Timeout'],
            [429, 'Too Many Requests'],
            [500, 'Server Error'],
            [503, 'Service Unavailable']
        ])('is true for a transient HTTP %d', (status, statusText) => {
            const failure = parse(toolFailure('op', new HttpError(status, statusText, 'body')));
            expect(failure.retryable).toBe(true);
            expect(failure.status).toBe(status);
        });

        it.each([
            [400, 'Bad Request'],
            [403, 'Forbidden'],
            [404, 'Not Found'],
            [409, 'Conflict']
        ])('is false for a client-side HTTP %d', (status, statusText) => {
            // A 429 on file 3 of 200 and a permanent 403 read identically once flattened to a
            // message, so the model either abandons a transfer that would have succeeded or
            // retries one that never can.
            const failure = parse(toolFailure('op', new HttpError(status, statusText, 'body')));
            expect(failure.retryable).toBe(false);
            expect(failure.status).toBe(status);
        });

        it('is false for a caller-initiated abort', () => {
            const failure = parse(toolFailure('op', new AbortError('cancelled')));
            expect(failure.retryable).toBe(false);
            expect(failure.code).toBe('ABORT');
        });

        it('is false for a policy rejection', () => {
            const failure = parse(toolFailure('op', new PolicyError('blocked', 'GET', '/x')));
            expect(failure.retryable).toBe(false);
            expect(failure.code).toBe('POLICY');
        });
    });

    it('caps the embedded error body', () => {
        const huge = new HttpError(500, 'Server Error', 'z'.repeat(MAX_ERROR_CHARS * 4));
        const failure = parse(toolFailure('upload_assets', huge));

        expect(failure.error).toContain('truncated');
        expect(failure.error.length).toBeLessThan(MAX_ERROR_CHARS * 2);
    });

    it('merges caller-supplied context without losing the standard fields', () => {
        const failure = parse(toolFailure('op', new Error('x'), { path: '/about-us' }));

        expect(failure['path']).toBe('/about-us');
        expect(failure.ok).toBe(false);
        expect(failure.operation).toBe('op');
    });
});
