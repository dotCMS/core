import { renderSummary } from './ui';

import type { TargetOutcome } from './types';

const ok = (id: string): TargetOutcome => ({
    targetId: id,
    scope: 'folder',
    path: `/tmp/${id}.json`,
    result: 'written',
    reason: null,
    permissionsApplied: true,
    skillsInstalled: 'yes'
});

describe('renderSummary (FR-028, FR-024e, FR-027)', () => {
    it('lists each target with its scope, file and result', () => {
        const out = renderSummary({
            outcomes: [ok('cursor'), ok('claude-code')],
            connection: 'ok'
        });
        expect(out).toContain('cursor');
        expect(out).toContain('claude-code');
        expect(out).toContain('/tmp/cursor.json');
    });

    it('gives a failed target its reason', () => {
        const out = renderSummary({
            outcomes: [{ ...ok('codex'), result: 'failed', reason: 'permission denied' }],
            connection: 'ok'
        });
        expect(out).toContain('permission denied');
    });

    it('does NOT report the run ready when the connection check failed', () => {
        const out = renderSummary({
            outcomes: [ok('cursor')],
            connection: 'failed',
            connectionReason: 'server did not start'
        });
        expect(out).toMatch(/did not start/i);
        expect(out).not.toMatch(/\bready\b/i);
    });

    it('never claims skills landed for a target whose location is unconfirmed', () => {
        const out = renderSummary({
            outcomes: [{ ...ok('vscode'), skillsInstalled: 'unverified' }],
            connection: 'ok'
        });
        expect(out).toMatch(/unverified|not confirmed/i);
    });

    it('does NOT blame permissions for a target that failed to write at all', () => {
        // A failed target has permissionsApplied false because nothing was written, not
        // because the platform refused. Saying otherwise blamed the OS for our own error.
        const out = renderSummary({
            outcomes: [
                { ...ok('cursor'), result: 'failed', reason: 'EISDIR', permissionsApplied: false }
            ],
            connection: 'ok'
        });
        expect(out).toContain('EISDIR');
        expect(out).not.toMatch(/could not restrict/i);
    });

    it('does NOT blame permissions for a skipped target', () => {
        const out = renderSummary({
            outcomes: [{ ...ok('cursor'), result: 'skipped', permissionsApplied: false }],
            connection: 'skipped'
        });
        expect(out).not.toMatch(/could not restrict/i);
    });

    it('says when permissions could not be applied rather than implying protection', () => {
        const out = renderSummary({
            outcomes: [{ ...ok('cursor'), permissionsApplied: false }],
            connection: 'ok'
        });
        expect(out).toMatch(/permission|restrict/i);
    });
});

describe('warnings reach the summary (FR-005a)', () => {
    // The FR-005a defect was a value produced and never consumed. Asserting `runSetup`
    // returns a warning proves half of it; this asserts the other half — that the summary
    // the developer actually reads prints it.
    it('prints each warning', () => {
        const out = renderSummary({
            outcomes: [ok('cursor')],
            connection: 'ok',
            warnings: ['This tool targets dotCMS 26.09.03, but the instance reports 25.01.01.']
        });
        expect(out).toContain('the instance reports 25.01.01');
    });

    it('prints nothing extra when there are none', () => {
        const withNone = renderSummary({
            outcomes: [ok('cursor')],
            connection: 'ok',
            warnings: []
        });
        const withUndefined = renderSummary({ outcomes: [ok('cursor')], connection: 'ok' });
        expect(withNone).toBe(withUndefined);
    });
});
