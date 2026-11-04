import { expectTypeOf } from 'vitest';

import {
    errorMessage,
    MAX_ERROR_CHARS,
    toToolFailure,
    type ToolFailure,
    type ToolFailureCode
} from './tool-runtime';

import { AbortError, HttpError, NetworkError, PolicyError, TimeoutError } from '../../runtime';

/**
 * A failure as the model receives it: through JSON. MCP hands the model a string, so every
 * field the model must branch on has to survive serialization — asserting on the round-tripped
 * value is what proves that.
 */
function failureFor(operation: string, error: unknown, extra?: Record<string, unknown>) {
    return JSON.parse(JSON.stringify(toToolFailure(operation, error, extra))) as ToolFailure;
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

describe('toToolFailure', () => {
    it('carries the operation, code and prefix', () => {
        const failure = failureFor('page_verify', new Error('nope'));

        expect(failure.ok).toBe(false);
        expect(failure.operation).toBe('page_verify');
        expect(failure.error).toContain('[dotCMS - page_verify]');
        expect(failure.error).toContain('nope');
        expect(failure.code).toBe('UNKNOWN');
    });

    describe('retryable', () => {
        // `retryable` has to be a FIELD: MCP hands the model a string, so `instanceof` is
        // unavailable on the far side and anything it must branch on has to survive JSON.
        it('is true for a timeout', () => {
            const failure = failureFor('op', new TimeoutError('too slow', 30_000));
            expect(failure.retryable).toBe(true);
            expect(failure.code).toBe('TIMEOUT');
        });

        it('is true for a request that never got a response', () => {
            // Refused, reset, DNS: the instance may be restarting. Reported as permanent, this
            // sent the model away from a call that would have worked a moment later.
            const refused = Object.assign(new TypeError('fetch failed'), {
                cause: { code: 'ECONNREFUSED' }
            });
            const failure = failureFor('op', new NetworkError('GET', '/api/v1/site', refused));

            expect(failure.retryable).toBe(true);
            expect(failure.code).toBe('NETWORK');
            expect(failure.error).toContain('ECONNREFUSED');
        });

        it.each([
            [408, 'Request Timeout'],
            [429, 'Too Many Requests'],
            [500, 'Server Error'],
            [503, 'Service Unavailable']
        ])('is true for a transient HTTP %d', (status, statusText) => {
            const failure = failureFor('op', new HttpError(status, statusText, 'body'));
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
            const failure = failureFor('op', new HttpError(status, statusText, 'body'));
            expect(failure.retryable).toBe(false);
            expect(failure.status).toBe(status);
        });

        it('is false for a caller-initiated abort', () => {
            const failure = failureFor('op', new AbortError('cancelled'));
            expect(failure.retryable).toBe(false);
            expect(failure.code).toBe('ABORT');
        });

        it('is false for a policy rejection', () => {
            const failure = failureFor('op', new PolicyError('blocked', 'GET', '/x'));
            expect(failure.retryable).toBe(false);
            expect(failure.code).toBe('POLICY');
        });
    });

    it('caps the embedded error body', () => {
        const huge = new HttpError(500, 'Server Error', 'z'.repeat(MAX_ERROR_CHARS * 4));
        const failure = failureFor('upload_assets', huge);

        expect(failure.error).toContain('truncated');
        expect(failure.error.length).toBeLessThan(MAX_ERROR_CHARS * 2);
    });

    it('types code as the documented set, so a misspelt code does not compile', () => {
        const failure = failureFor('op', new TimeoutError('too slow', 30_000));

        expectTypeOf<ToolFailureCode>().toEqualTypeOf<
            | 'VALIDATION'
            | 'POLICY'
            | 'HTTP'
            | 'NETWORK'
            | 'TIMEOUT'
            | 'ABORT'
            | 'SANDBOX'
            | 'RUNTIME'
            | 'UNKNOWN'
            | 'CONFIGURATION'
        >();
        expectTypeOf(failure.code).toEqualTypeOf<ToolFailureCode>();
        // @ts-expect-error — not a code; with `code: string` this compiled and never matched.
        const typo = failure.code === 'CONFIGURATON';
        expect(typo).toBe(false);
    });

    it('never lets caller context overwrite the code or the other standard fields', () => {
        const failure = failureFor('op', new TimeoutError('too slow', 30_000), {
            code: 'NOT_A_CODE',
            ok: true,
            retryable: false
        });

        expect(failure).toMatchObject({ ok: false, code: 'TIMEOUT', retryable: true });
    });

    it('merges caller-supplied context without losing the standard fields', () => {
        const failure = failureFor('op', new Error('x'), { path: '/about-us' });

        expect(failure['path']).toBe('/about-us');
        expect(failure.ok).toBe(false);
        expect(failure.operation).toBe('op');
    });
});
