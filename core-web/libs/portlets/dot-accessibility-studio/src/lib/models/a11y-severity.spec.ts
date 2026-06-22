import { A11yGroup } from './a11y-groups';
import { impactToSeverity, severityBreakdown, SEVERITY_ORDER } from './a11y-severity';

describe('a11y-severity', () => {
    describe('impactToSeverity', () => {
        it('maps each axe impact to its bucket', () => {
            expect(impactToSeverity('critical')).toBe('critical');
            expect(impactToSeverity('serious')).toBe('serious');
            expect(impactToSeverity('moderate')).toBe('moderate');
            expect(impactToSeverity('minor')).toBe('minor');
        });

        it('buckets a null/absent impact as minor', () => {
            expect(impactToSeverity(null)).toBe('minor');
        });
    });

    describe('severityBreakdown', () => {
        const group = (impact: A11yGroup['impact'], count: number): A11yGroup => ({
            code: 'x',
            type: 'error',
            message: 'm',
            impact,
            helpUrl: '',
            items: [],
            count
        });

        it('sums element counts per severity bucket', () => {
            const counts = severityBreakdown([
                group('critical', 3),
                group('critical', 1),
                group('serious', 2),
                group('moderate', 1),
                group(null, 4) // → minor
            ]);
            expect(counts).toEqual({ critical: 4, serious: 2, moderate: 1, minor: 4 });
        });

        it('returns all-zero for no groups', () => {
            expect(severityBreakdown([])).toEqual({
                critical: 0,
                serious: 0,
                moderate: 0,
                minor: 0
            });
        });
    });

    it('orders severities high → low', () => {
        expect(SEVERITY_ORDER).toEqual(['critical', 'serious', 'moderate', 'minor']);
    });
});
