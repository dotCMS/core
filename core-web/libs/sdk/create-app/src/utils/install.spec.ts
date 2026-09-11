/**
 * Contract spec for reporting a failed dependency install (task T032, dotCMS #37262, AC-007).
 *
 * THE BUG (contract X7). `installDependenciesForProject()` returns a `Result`. `Err(val)` is
 * `{ ok: false, val }` — a TRUTHY OBJECT. The caller at `src/index.ts` tested `if (!result)`,
 * which is therefore never true, so the failure branch was unreachable and a failed
 * `npm install` was reported to the user as success.
 *
 * This is the same class of mistake as the rest of this issue: a green signal that does not
 * mean what it claims. The fix is to branch on `result.ok`.
 *
 * API PINNED
 *   export type InstallReport =
 *       | { kind: 'installed' }
 *       | { kind: 'failed'; reason: string };
 *   export function reportInstallResult(result: Result<unknown, unknown>): InstallReport;
 *
 * A string discriminant, deliberately: `strict: false` in this workspace means TypeScript will
 * not narrow a union on a boolean-literal discriminant — and a boolean here would be repeating
 * the very mistake being fixed.
 */

import { Err, Ok } from '@dotcms/http';

import { reportInstallResult } from './install';

describe('reportInstallResult', () => {
    it('reports success when the install succeeded', () => {
        expect(reportInstallResult(Ok(undefined))).toEqual({ kind: 'installed' });
    });

    it('reports FAILURE when the install failed — the branch that was unreachable', () => {
        const report = reportInstallResult(Err(new Error('npm exited with code 1')));

        expect(report.kind).toBe('failed');
    });

    it('surfaces the underlying reason rather than swallowing it', () => {
        const report = reportInstallResult(Err(new Error('ENOENT: npm not found')));

        if (report.kind !== 'failed') {
            throw new Error('expected a failure report');
        }

        expect(report.reason).toContain('npm not found');
    });

    it('handles a non-Error failure value without losing it', () => {
        const report = reportInstallResult(Err('exit status 127'));

        if (report.kind !== 'failed') {
            throw new Error('expected a failure report');
        }

        expect(report.reason).toContain('127');
    });

    /**
     * The trap itself, pinned so nobody reintroduces `if (!result)`.
     *
     * This test asserts a property of `Err`, not of the code under test: an error Result is a
     * truthy object, so negating it can never detect failure. It is here because the original
     * bug is invisible on inspection — `if (!result)` reads like a perfectly ordinary guard.
     */
    it('documents why `if (!result)` could never work: Err() is truthy', () => {
        const failure = Err(new Error('boom'));

        expect(Boolean(failure)).toBe(true);
        expect(!failure).toBe(false);
        // The only correct discriminator:
        expect(failure.ok).toBe(false);
    });
});
