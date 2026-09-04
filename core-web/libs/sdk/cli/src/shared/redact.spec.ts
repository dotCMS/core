import { redact } from './redact';

describe('redact (FR-022, SC-005)', () => {
    it('shows first 6 and last 4, never the middle', () => {
        const out = redact('dot_abcdefghijklmnopqrstuvwxyz_9999');
        expect(out.startsWith('dot_ab')).toBe(true);
        expect(out.endsWith('9999')).toBe(true);
        expect(out).not.toContain('ghijklmnopqrstuvwxyz');
    });

    it('never returns the input unchanged for a realistic token', () => {
        const token = 'dot_abcdefghijklmnopqrstuvwxyz_9999';
        expect(redact(token)).not.toBe(token);
    });

    it('masks a short secret entirely rather than revealing most of it', () => {
        expect(redact('short')).toBe('*****');
        expect(redact('short')).not.toContain('s'.repeat(2));
    });

    it('handles an empty secret without throwing', () => {
        expect(redact('')).toBe('');
    });
});
