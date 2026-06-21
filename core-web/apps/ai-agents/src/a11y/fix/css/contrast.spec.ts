import {
    contrastRatio,
    nudgeToClear,
    parseColor,
    parseTargetRatio,
    relativeLuminance,
    toHex
} from './contrast';

describe('contrast math', () => {
    describe('parseColor', () => {
        it('parses #rrggbb, #rgb, rgb()', () => {
            expect(parseColor('#e76300')).toEqual({ r: 231, g: 99, b: 0 });
            expect(parseColor('#fff')).toEqual({ r: 255, g: 255, b: 255 });
            expect(parseColor('rgb(0, 128, 255)')).toEqual({ r: 0, g: 128, b: 255 });
            expect(parseColor('rgba(10,20,30,0.5)')).toEqual({ r: 10, g: 20, b: 30 });
        });
        it('returns null for unsupported', () => {
            expect(parseColor('hotpink')).toBeNull();
            expect(parseColor('var(--x)')).toBeNull();
        });
    });

    it('computes known contrast ratios', () => {
        // black on white = 21:1
        expect(contrastRatio({ r: 0, g: 0, b: 0 }, { r: 255, g: 255, b: 255 })).toBeCloseTo(21, 0);
        // white on white = 1:1
        expect(contrastRatio({ r: 255, g: 255, b: 255 }, { r: 255, g: 255, b: 255 })).toBeCloseTo(
            1,
            5
        );
    });

    it('relativeLuminance: black 0, white 1', () => {
        expect(relativeLuminance({ r: 0, g: 0, b: 0 })).toBeCloseTo(0, 5);
        expect(relativeLuminance({ r: 255, g: 255, b: 255 })).toBeCloseTo(1, 5);
    });

    it('parseTargetRatio reads "4.5:1" / "3:1" / default', () => {
        expect(parseTargetRatio('4.5:1')).toBe(4.5);
        expect(parseTargetRatio('3:1')).toBe(3);
        expect(parseTargetRatio(undefined)).toBe(4.5);
    });

    describe('nudgeToClear', () => {
        it('darkens a too-light color against a white counterpart until it clears', () => {
            // #e76300 (orange) on #ffffff ≈ 2.6:1, fails AA. Counterpart is white →
            // darken the orange toward black.
            const orange = parseColor('#e76300')!;
            const white = parseColor('#ffffff')!;
            const fix = nudgeToClear(orange, white, 4.5);
            expect(fix).not.toBeNull();
            expect(fix!.achievedRatio).toBeGreaterThanOrEqual(4.5);
            // the nudged color must actually clear against white
            expect(contrastRatio(parseColor(fix!.newColor)!, white)).toBeGreaterThanOrEqual(4.5);
        });

        it('returns the original when it already passes', () => {
            const black = parseColor('#000000')!;
            const white = parseColor('#ffffff')!;
            const fix = nudgeToClear(black, white, 4.5);
            expect(fix?.newColor).toBe('#000000');
        });

        it('finds a MINIMAL nudge (not just pure black)', () => {
            const orange = parseColor('#e76300')!;
            const white = parseColor('#ffffff')!;
            const fix = nudgeToClear(orange, white, 4.5)!;
            // should not collapse all the way to black — stays a recognizable dark orange
            expect(fix.newColor).not.toBe('#000000');
            const c = parseColor(fix.newColor)!;
            expect(c.r).toBeGreaterThan(c.g); // still red-dominant (hue preserved-ish)
        });

        it('returns null when the target is unreachable by nudging (mid-grey counterpart)', () => {
            // Against mid-grey, neither pure black nor white reaches 7:1.
            const grey = parseColor('#777777')!;
            const start = parseColor('#888888')!;
            expect(nudgeToClear(start, grey, 7)).toBeNull();
        });

        it('toHex clamps and formats', () => {
            expect(toHex({ r: -5, g: 300, b: 128 })).toBe('#00ff80');
        });
    });
});
