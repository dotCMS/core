'use strict';

const {
    normalizeVersion,
    isExactVersion,
    ltsDateFromVersion,
    resolveCutoff,
    selectPublishedOnOrBefore,
    compareVersions,
    meetsFloor,
    decide,
    ResolutionError
} = require('./resolve');

/**
 * A slice of @dotcms/client's real npm `.time` map, including the shapes that must never be
 * picked as a pin: the `-next.<run>` prereleases, the 0.0.1 alpha/beta line and the
 * pre-lockstep 1.x scheme.
 */
const REGISTRY_TIME = {
    '0.0.1-alpha.62': '2025-01-15T10:00:00.000Z',
    '0.0.1-beta.46': '2025-05-02T10:00:00.000Z',
    '1.0.0': '2025-07-08T10:00:00.000Z',
    '1.2.0': '2025-10-24T10:00:00.000Z',
    '1.7.0': '2026-05-11T10:00:00.000Z',
    '26.7.31-1': '2026-07-31T10:00:00.000Z',
    '26.8.3-1': '2026-08-03T10:00:00.000Z',
    '26.8.7-1': '2026-08-07T10:00:00.000Z',
    '26.8.10-1': '2026-08-10T10:00:00.000Z',
    '26.8.19-1': '2026-08-19T08:00:00.000Z',
    '26.8.19-2': '2026-08-19T12:00:00.000Z',
    '26.8.19-3': '2026-08-19T18:00:00.000Z',
    '26.9.3-1': '2026-09-03T10:00:00.000Z',
    '26.9.9-1': '2026-09-09T10:00:00.000Z',
    '26.9.9-1-next.2665': '2026-09-09T11:00:00.000Z'
};

const FLOATING = ['latest', 'latest', 'latest'];

const baseInput = (overrides) => ({
    specs: FLOATING,
    isLts: false,
    releaseVersion: '26.09.15-01',
    commitDate: '2026-09-15T12:00:00Z',
    registryTime: REGISTRY_TIME,
    minSdkValue: '0.0.0',
    ...overrides
});

describe('normalizeVersion', () => {
    it.each([
        ['26.08.03-01', '26.8.3-1'],
        ['26.10.20-01', '26.10.20-1'],
        ['2026.06.24-01', '2026.6.24-1'],
        ['26.00.03-01', '26.0.3-1'],
        ['26.9.3-1', '26.9.3-1'],
        ['1.2.0', '1.2.0']
    ])('normalizes %s to %s', (input, expected) => {
        expect(normalizeVersion(input)).toBe(expected);
    });

    it.each([
        ['26.9.3-1-next.2632', '26.9.3-1-next.2632'],
        ['26.08.03-01-next.2632', '26.8.3-1-next.2632'],
        ['25.07.10-1-next.99', '25.7.10-1-next.99']
    ])('preserves the -next.<run> suffix: %s -> %s', (input, expected) => {
        // Rebuilding from the first four [.-] fields dropped this suffix, which made every
        // `next` publish collide with the stable release version.
        expect(normalizeVersion(input)).toBe(expected);
    });
});

describe('isExactVersion', () => {
    it.each(['1.2.0', '26.8.3-1', '26.8.3-1-next.99', '2026.6.24-1'])('accepts %s', (v) => {
        expect(isExactVersion(v)).toBe(true);
    });

    it.each(['latest', 'next', '*', '^26.9.3-1', '~1.2.0', '>=1.0.0', '1.2.x', '26.9.15_lts_v1', ''])(
        'rejects %s',
        (v) => {
            expect(isExactVersion(v)).toBe(false);
        }
    );
});

describe('ltsDateFromVersion', () => {
    it('reads the date out of an LTS version', () => {
        expect(ltsDateFromVersion('26.08.08_lts_v1')).toBe('2026-08-08');
        expect(ltsDateFromVersion('25.07.10_lts_v18')).toBe('2025-07-10');
    });

    it('returns null for a non-LTS version', () => {
        expect(ltsDateFromVersion('26.09.15-01')).toBeNull();
        expect(ltsDateFromVersion('garbage')).toBeNull();
    });
});

describe('resolveCutoff', () => {
    it('uses the commit date when the line is named after the release it is cut from', () => {
        expect(resolveCutoff('2026-08-08T12:00:00Z', '26.08.08_lts_v1')).toBe('2026-08-08');
    });

    it('uses the EARLIER version date when a line is cut retroactively from newer code', () => {
        // Naming says August, the commit is from September: err toward the older SDK.
        expect(resolveCutoff('2026-09-10T12:00:00Z', '26.08.08_lts_v1')).toBe('2026-08-08');
    });

    it('uses the commit date when the version date is later than the commit', () => {
        expect(resolveCutoff('2026-08-01T12:00:00Z', '26.09.15_lts_v1')).toBe('2026-08-01');
    });

    it('falls back to the commit date for a non-LTS version', () => {
        expect(resolveCutoff('2026-09-15T12:00:00Z', '26.09.15-01')).toBe('2026-09-15');
    });

    it('throws on an unusable commit date rather than resolving from garbage', () => {
        expect(() => resolveCutoff('', '26.08.08_lts_v1')).toThrow(/Unusable commit date/);
    });
});

describe('selectPublishedOnOrBefore', () => {
    it('picks the newest release published on or before the cutoff', () => {
        expect(selectPublishedOnOrBefore(REGISTRY_TIME, '2026-08-08')).toBe('26.8.7-1');
    });

    it('never picks a version published AFTER the cutoff, even when it is nearer', () => {
        // For 26.08.09, 26.8.10-1 is one day after and 26.8.7-1 two days before. The nearer
        // one is ahead of the server — the Freshdesk #38677 drift direction.
        expect(selectPublishedOnOrBefore(REGISTRY_TIME, '2026-08-09')).toBe('26.8.7-1');
    });

    it('picks the last counter of a day that carried several releases', () => {
        expect(selectPublishedOnOrBefore(REGISTRY_TIME, '2026-08-19')).toBe('26.8.19-3');
    });

    it('includes a version published on the cutoff day itself', () => {
        expect(selectPublishedOnOrBefore(REGISTRY_TIME, '2026-08-07')).toBe('26.8.7-1');
    });

    it('never picks a -next prerelease', () => {
        expect(selectPublishedOnOrBefore(REGISTRY_TIME, '2026-09-10')).toBe('26.9.9-1');
    });

    it('never picks an alpha/beta or a pre-lockstep 1.x version', () => {
        expect(selectPublishedOnOrBefore(REGISTRY_TIME, '2026-06-01')).toBeNull();
    });

    it('returns null when nothing was published early enough', () => {
        expect(selectPublishedOnOrBefore(REGISTRY_TIME, '2020-01-01')).toBeNull();
        expect(selectPublishedOnOrBefore({}, '2026-08-08')).toBeNull();
    });
});

describe('compareVersions', () => {
    it('orders by numeric segments, not lexically', () => {
        expect(compareVersions('26.10.1-1', '26.9.9-1')).toBe(1);
        expect(compareVersions('26.8.7-1', '26.8.10-1')).toBe(-1);
        expect(compareVersions('26.8.19-3', '26.8.19-1')).toBe(1);
    });

    it('treats zero padding as equal', () => {
        // MinSdkVersion.VALUE is written raw (26.09.15-01); a pin is in npm form (26.9.15-1).
        expect(compareVersions('26.9.15-1', '26.09.15-01')).toBe(0);
        expect(compareVersions('26.8.3-1', '26.08.03-01')).toBe(0);
    });
});

describe('meetsFloor', () => {
    it('treats the 0.0.0 baseline as "everything is compatible"', () => {
        expect(meetsFloor('1.0.0', '0.0.0')).toBe(true);
        expect(meetsFloor('1.0.0', null)).toBe(true);
    });

    it('accepts a pin at or above the floor', () => {
        expect(meetsFloor('26.8.7-1', '26.08.03-01')).toBe(true);
        expect(meetsFloor('26.9.15-1', '26.09.15-01')).toBe(true);
    });

    it('rejects a pin below the floor', () => {
        expect(meetsFloor('26.8.7-1', '26.08.19-01')).toBe(false);
    });
});

describe('decide', () => {
    it('does nothing when no example declares an @dotcms/* dependency', () => {
        expect(decide(baseInput({ specs: [] }))).toMatchObject({ action: 'none' });
    });

    it('keeps an already-pinned lineage untouched', () => {
        const result = decide(baseInput({ specs: ['26.8.3-1', '26.8.3-1'], isLts: true }));
        expect(result).toMatchObject({ action: 'keep' });
        expect(result.version).toBeUndefined();
    });

    it('pins a normal release to the version it is about to publish', () => {
        expect(decide(baseInput())).toMatchObject({ action: 'pin', version: '26.9.15-1' });
    });

    it('resolves an LTS line by date, not by npm latest', () => {
        const result = decide(
            baseInput({
                isLts: true,
                releaseVersion: '26.08.08_lts_v1',
                commitDate: '2026-08-08T12:00:00Z'
            })
        );
        expect(result).toMatchObject({ action: 'pin', version: '26.8.7-1', cutoff: '2026-08-08' });
    });

    it('refuses an LTS line that predates date-lockstep publishing', () => {
        expect(() =>
            decide(
                baseInput({
                    isLts: true,
                    releaseVersion: '25.07.10_lts_v19',
                    commitDate: '2025-07-10T12:00:00Z'
                })
            )
        ).toThrow(ResolutionError);
    });

    it('refuses a pin below the server MinSdkVersion floor', () => {
        expect(() =>
            decide(
                baseInput({
                    isLts: true,
                    releaseVersion: '26.08.08_lts_v1',
                    commitDate: '2026-08-08T12:00:00Z',
                    minSdkValue: '26.08.19-01'
                })
            )
        ).toThrow(/OLDER than the minimum this build supports/);
    });

    it('allows a pin exactly at the floor despite different zero padding', () => {
        expect(decide(baseInput({ minSdkValue: '26.09.15-01' }))).toMatchObject({
            action: 'pin',
            version: '26.9.15-1'
        });
    });
});

/**
 * One test per defect that actually shipped or was caught in review. These are the reason
 * this module exists as a module: as inline shell, none of them were reachable without
 * cutting a real release.
 */
describe('regressions', () => {
    it('does NOT require a normal release pin to already exist on npm', () => {
        // The pin is published later in the same run — "Create GitHub Release" is what
        // triggers cicd_release-sdk.yml. An existence check here failed every release.
        const result = decide(baseInput({ releaseVersion: '26.12.31-01', commitDate: '2026-12-31T12:00:00Z' }));
        expect(result).toMatchObject({ action: 'pin', version: '26.12.31-1', verifyPublished: false });
    });

    it('does NOT resolve an LTS cut from older code to the newest published SDK', () => {
        // Pinning npm `latest` gave a line cut from August code a September SDK — the SDK
        // ahead of its own server, which is exactly Freshdesk #38677.
        const result = decide(
            baseInput({
                isLts: true,
                releaseVersion: '26.08.08_lts_v1',
                commitDate: '2026-09-10T12:00:00Z'
            })
        );
        expect(result.version).toBe('26.8.7-1');
        expect(result.version).not.toBe('26.9.9-1');
    });

    it('never emits an LTS-shaped string as a pin', () => {
        // Normalizing an LTS release version yields 26.9.15_lts_v1: not valid semver, not on
        // npm, and rejected by validate-sdk-package-shapes on the branch it was written to.
        expect(() => decide(baseInput({ releaseVersion: '26.09.15_lts_v1', isLts: false }))).toThrow(
            /not an exact version/
        );
    });
});
