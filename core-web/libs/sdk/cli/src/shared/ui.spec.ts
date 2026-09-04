import { renderSummary } from './ui';

import type { TargetOutcome } from './types';

const ok = (id: string): TargetOutcome => ({
    targetId: id, scope: 'folder', path: `/tmp/${id}.json`, result: 'written',
    reason: null, permissionsApplied: true, skillsInstalled: 'yes'
});

describe('renderSummary (FR-028, FR-024e, FR-027)', () => {
    it('lists each target with its scope, file and result', () => {
        const out = renderSummary({ outcomes: [ok('cursor'), ok('claude-code')], connection: 'ok' });
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
            outcomes: [ok('cursor')], connection: 'failed', connectionReason: 'server did not start'
        });
        expect(out).toMatch(/did not start/i);
        expect(out).not.toMatch(/\bready\b/i);
    });

    it('never claims skills landed for a target whose location is unconfirmed', () => {
        const out = renderSummary({
            outcomes: [{ ...ok('vscode'), skillsInstalled: 'unverified' }], connection: 'ok'
        });
        expect(out).toMatch(/unverified|not confirmed/i);
    });

    it('says when permissions could not be applied rather than implying protection', () => {
        const out = renderSummary({
            outcomes: [{ ...ok('cursor'), permissionsApplied: false }], connection: 'ok'
        });
        expect(out).toMatch(/permission|restrict/i);
    });
});
