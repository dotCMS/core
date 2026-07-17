import { formatSandboxResult } from './format-result';

import type { SandboxResult } from './types';

function ok(value: unknown, logs: string[] = []): SandboxResult {
    return { success: true, value, logs, executionTime: 1 };
}

function err(
    error: { name: string; message: string },
    logs: string[] = []
): SandboxResult {
    return { success: false, error, logs, executionTime: 1 };
}

describe('formatSandboxResult', () => {
    it('pretty-prints a non-string value', () => {
        expect(formatSandboxResult(ok({ a: 1 }))).toBe('{\n  "a": 1\n}');
    });

    it('passes a string value through untouched', () => {
        expect(formatSandboxResult(ok('hello'))).toBe('hello');
    });

    it('appends logs on success', () => {
        expect(formatSandboxResult(ok('v', ['line1', 'line2']))).toBe(
            'v\n\n--- Logs ---\nline1\nline2'
        );
    });

    it('formats an error branch with logs', () => {
        expect(formatSandboxResult(err({ name: 'HttpError', message: 'boom' }, ['ctx']))).toBe(
            'Error: HttpError: boom\nLogs:\nctx'
        );
    });

    it('leaves output under the cap untouched', () => {
        const out = formatSandboxResult(ok('x'.repeat(100)), { maxChars: 200 });
        expect(out).toBe('x'.repeat(100));
        expect(out).not.toContain('truncated');
    });

    it('truncates over-cap success output and appends a notice', () => {
        const out = formatSandboxResult(ok('x'.repeat(500)), { maxChars: 100 });
        expect(out.startsWith('x'.repeat(100))).toBe(true);
        expect(out).toContain('[output truncated at 100 of');
        expect(out).toContain('refine the query');
    });

    it('appends a custom truncation hint', () => {
        const out = formatSandboxResult(ok('x'.repeat(500)), {
            maxChars: 100,
            truncationHint: 'Use resolveRef().'
        });
        expect(out).toContain('Use resolveRef().');
    });

    it('also caps a huge error branch', () => {
        const out = formatSandboxResult(err({ name: 'HttpError', message: 'x'.repeat(500) }), {
            maxChars: 100
        });
        expect(out.length).toBeLessThan(300);
        expect(out).toContain('[output truncated at 100 of');
    });
});
