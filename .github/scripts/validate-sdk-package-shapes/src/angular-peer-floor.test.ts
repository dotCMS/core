import { AngularFloor, deriveAngularFloor, readPartialDeclarations, validateAngularPeerFloor } from './angular-peer-floor';

/**
 * Lines lifted from the fesm2022/dotcms-angular.mjs that shipped the #37680 regression: the
 * highest minVersion is 17.0.0, yet the component uses ChangeDetectionStrategy.Eager, which
 * only linkers from Angular 21.2 onward can parse.
 */
const BUNDLE_37680 = `
class DotCMSEditableTextComponent {
    static { this.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "22.1.0", ngImport: i0, type: DotCMSEditableTextComponent, deps: [], target: i0.ɵɵFactoryTarget.Component }); }
    static { this.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "17.0.0", version: "22.1.0", type: DotCMSEditableTextComponent, isStandalone: true, selector: "dotcms-editable-text", ngImport: i0, template: "", changeDetection: i0.ChangeDetectionStrategy.Eager }); }
}
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "22.1.0", ngImport: i0, type: DotCMSEditableTextComponent, decorators: [] });
class DotCMSShowWhenDirective {
    static { this.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "22.1.0", ngImport: i0, type: DotCMSShowWhenDirective, deps: [], target: i0.ɵɵFactoryTarget.Directive }); }
    static { this.ɵdir = i0.ɵɵngDeclareDirective({ minVersion: "14.0.0", version: "22.1.0", type: DotCMSShowWhenDirective, isStandalone: true, selector: "[dotCMSShowWhen]", ngImport: i0 }); }
}
`;

const FLOOR_22: AngularFloor = {
    floor: '22.0.0',
    highestMinVersion: '17.0.0',
    compilerVersion: '22.1.0',
    declarationCount: 5
};

describe('readPartialDeclarations', () => {
    it('reads the version stamps of every ɵɵngDeclare* call', () => {
        expect(readPartialDeclarations(BUNDLE_37680)).toEqual([
            { kind: 'Factory', minVersion: '12.0.0', version: '22.1.0' },
            { kind: 'Component', minVersion: '17.0.0', version: '22.1.0' },
            { kind: 'ClassMetadata', minVersion: '12.0.0', version: '22.1.0' },
            { kind: 'Factory', minVersion: '12.0.0', version: '22.1.0' },
            { kind: 'Directive', minVersion: '14.0.0', version: '22.1.0' }
        ]);
    });

    it('throws when a declaration does not have the expected shape, instead of silently skipping it', () => {
        const bundle = `${BUNDLE_37680}
            i0.ɵɵngDeclarePipe({ ngImport: i0, version: "23.0.0", minVersion: "23.0.0", type: SomePipe });`;

        expect(() => readPartialDeclarations(bundle)).toThrow(/found 6 ɵɵngDeclare\* calls but could only read version stamps from 5/);
    });

    it('returns an empty list for a bundle with no declarations', () => {
        expect(readPartialDeclarations('export const x = 1;')).toEqual([]);
    });
});

describe('deriveAngularFloor', () => {
    it('uses the compiler major when minVersion under-reports it — the #37680 artifact', () => {
        expect(deriveAngularFloor(readPartialDeclarations(BUNDLE_37680))).toEqual(FLOOR_22);
    });

    it('uses the highest minVersion when it is above the compiler major', () => {
        const floor = deriveAngularFloor([
            { kind: 'Factory', minVersion: '12.0.0', version: '23.4.1' },
            { kind: 'Component', minVersion: '23.2.0', version: '23.4.1' }
        ]);

        expect(floor.floor).toBe('23.2.0');
    });

    it('compares versions semantically, not as strings', () => {
        const floor = deriveAngularFloor([
            { kind: 'Factory', minVersion: '9.0.0', version: '9.1.0' },
            { kind: 'Component', minVersion: '14.0.0', version: '10.0.0' }
        ]);

        expect(floor).toEqual(expect.objectContaining({ highestMinVersion: '14.0.0', compilerVersion: '10.0.0', floor: '14.0.0' }));
    });

    it('throws when there are no declarations to derive a floor from', () => {
        expect(() => deriveAngularFloor([])).toThrow(/no ɵɵngDeclare\* calls found/);
    });

    it.each(['0.0.0-PLACEHOLDER', 'not-a-version'])('throws on the non-release version stamp "%s"', (stamp) => {
        expect(() => deriveAngularFloor([{ kind: 'Component', minVersion: '17.0.0', version: stamp }])).toThrow(
            /not a release version/
        );
    });
});

describe('validateAngularPeerFloor', () => {
    const peers = (range: string) => ({
        peerDependencies: {
            rxjs: '>=7.0.0',
            '@angular/common': range,
            '@angular/core': range,
            '@angular/router': range,
            '@dotcms/client': '0.0.0'
        }
    });

    it('flags every @angular/* peer that admits a version below the floor — the #37680 manifest', () => {
        const violations = validateAngularPeerFloor(peers('>=17.0.0'), FLOOR_22);

        expect(violations).toEqual([
            expect.stringContaining('peerDependencies["@angular/common"] is ">=17.0.0", which admits 17.0.0'),
            expect.stringContaining('peerDependencies["@angular/core"] is ">=17.0.0", which admits 17.0.0'),
            expect.stringContaining('peerDependencies["@angular/router"] is ">=17.0.0", which admits 17.0.0')
        ]);
        expect(violations[0]).toContain('needs Angular 22.0.0 or newer');
    });

    it.each(['>=22.0.0', '^22.0.0', '^22.0.0 || ^23.0.0', '>=22.1.0 <24.0.0', '22.x'])(
        'passes the range "%s", whose lowest version is at or above the floor',
        (range) => {
            expect(validateAngularPeerFloor(peers(range), FLOOR_22)).toEqual([]);
        }
    );

    it.each(['^21.0.0 || ^22.0.0', '>=21.2.0', '*', '>=22.0.0-rc.0'])(
        'flags the range "%s", which admits something below the floor',
        (range) => {
            expect(validateAngularPeerFloor(peers(range), FLOOR_22)).toHaveLength(3);
        }
    );

    it('flags a range it cannot evaluate rather than passing it', () => {
        expect(validateAngularPeerFloor({ peerDependencies: { '@angular/core': 'latest' } }, FLOOR_22)).toEqual([
            expect.stringContaining('not a semver range this check can evaluate')
        ]);
    });

    it('flags a manifest with no @angular/core peer — npm cannot warn about a range that is not declared', () => {
        expect(validateAngularPeerFloor({ peerDependencies: { rxjs: '>=7.0.0' } }, FLOOR_22)).toEqual([
            expect.stringContaining('peerDependencies["@angular/core"] is missing')
        ]);
    });

    it('ignores non-Angular peers, even when their ranges are low', () => {
        expect(validateAngularPeerFloor({ peerDependencies: { '@angular/core': '>=22.0.0', rxjs: '>=1.0.0' } }, FLOOR_22)).toEqual([]);
    });
});
