import { refuseOverPathCeiling, TOO_MANY_FOLDERS_NAMED_KEY } from './folder-delete-ceilings';

/**
 * The courtesy check in front of the server's maximum-paths ceiling (#37063 FR-006, CR-03).
 *
 * Mirrors `upload-ceilings.ts` deliberately — same shape, same reasoning, same refusal to guess a
 * default — because an author meeting both features should meet one behaviour.
 */
describe('refuseOverPathCeiling', () => {
    const paths = (count: number): string[] =>
        Array.from({ length: count }, (_, i) => `//demo.dotcms.com/folder-${i}/`);

    it('should allow a selection under the ceiling', () => {
        expect(refuseOverPathCeiling(paths(5), { maxPaths: 10 })).toBeUndefined();
    });

    it('should allow a selection exactly at the ceiling', () => {
        expect(refuseOverPathCeiling(paths(10), { maxPaths: 10 })).toBeUndefined();
    });

    it('should refuse a selection over the ceiling, naming both numbers', () => {
        // "Fewer" is not an actionable refusal. The author needs to know how many they chose and
        // how many this instance allows.
        expect(refuseOverPathCeiling(paths(11), { maxPaths: 10 })).toEqual({
            key: TOO_MANY_FOLDERS_NAMED_KEY,
            args: ['11', '10']
        });
    });

    it('should treat an absent ceiling as no ceiling', () => {
        // Absent means an instance older than the field, or a configuration request that never
        // landed. A guessed default would be worse than no check: the client would refuse
        // selections the instance accepts, and no author could see why.
        expect(refuseOverPathCeiling(paths(500), undefined)).toBeUndefined();
        expect(refuseOverPathCeiling(paths(500), null)).toBeUndefined();
        expect(refuseOverPathCeiling(paths(500), {})).toBeUndefined();
    });

    it('should treat a zero or negative ceiling as unbounded', () => {
        // How the platform spells unbounded elsewhere.
        expect(refuseOverPathCeiling(paths(500), { maxPaths: 0 })).toBeUndefined();
        expect(refuseOverPathCeiling(paths(500), { maxPaths: -1 })).toBeUndefined();
    });

    it('should never refuse an empty selection', () => {
        // Nothing selected is the action list's business, not the ceiling's.
        expect(refuseOverPathCeiling([], { maxPaths: 10 })).toBeUndefined();
    });

    it('should not hard-code a ceiling of its own', () => {
        // The server stays the enforcement point. With a generous advertised ceiling, a large
        // selection must pass — if this ever fails, someone has baked a number in.
        expect(refuseOverPathCeiling(paths(1000), { maxPaths: 5000 })).toBeUndefined();
    });
});
