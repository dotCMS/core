import { MockInstance, vi } from 'vitest';

import {
    checkSdkCompatibility,
    compareVersions,
    resetSdkCompatibilityWarnings
} from './sdk-compatibility';

describe('compareVersions', () => {
    it('returns 0 for equal versions', () => {
        expect(compareVersions('26.7.13', '26.7.13')).toBe(0);
    });

    it('compares numerically, not as strings', () => {
        expect(compareVersions('26.10.1', '26.7.13')).toBe(1);
        expect(compareVersions('26.7.13', '26.10.1')).toBe(-1);
    });

    it('compares the counter/prerelease segment too', () => {
        expect(compareVersions('26.7.13-2', '26.7.13-1')).toBe(1);
        expect(compareVersions('26.7.13-1', '26.7.13-2')).toBe(-1);
    });

    it('treats a missing trailing segment as 0', () => {
        expect(compareVersions('26.7.13', '26.7.13-0')).toBe(0);
    });

    it('returns null for unparsable versions instead of throwing', () => {
        expect(compareVersions('26.7.13_lts_v1', '26.7.13')).toBeNull();
        expect(compareVersions('not-a-version', '26.7.13')).toBeNull();
    });

    it('treats a zero-padded version as equal to its normalized form (regression lock)', () => {
        // Defect A: the release pipeline used to write the zero-padded CalVer date
        // (e.g. "26.08.03-01") verbatim into published package.json files, instead of the
        // normalized, valid-semver form npm's registry metadata shows ("26.8.3-1"). This
        // already compares as equal today -- Number("08") is 8 in JS string-to-number
        // conversion, not octal -- so this locks in that correctness against future refactors.
        expect(compareVersions('26.08.03-01', '26.8.3-1')).toBe(0);
    });
});

describe('checkSdkCompatibility', () => {
    let errorSpy: MockInstance;
    let warnSpy: MockInstance;

    beforeEach(() => {
        resetSdkCompatibilityWarnings();
        errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    });

    afterEach(() => {
        errorSpy.mockRestore();
        warnSpy.mockRestore();
    });

    it('does nothing when the headers are absent (older server)', () => {
        checkSdkCompatibility(new Headers(), '26.7.13');

        expect(errorSpy).not.toHaveBeenCalled();
        expect(warnSpy).not.toHaveBeenCalled();
    });

    it('does nothing when versions are compatible', () => {
        const headers = new Headers({
            'x-dotcms-version': '26.7.13',
            'x-dotcms-min-sdk': '26.5.1'
        });

        checkSdkCompatibility(headers, '26.7.13');

        expect(errorSpy).not.toHaveBeenCalled();
        expect(warnSpy).not.toHaveBeenCalled();
    });

    it('logs a console error when the SDK is older than the minimum supported version', () => {
        const headers = new Headers({
            'x-dotcms-version': '26.7.13',
            'x-dotcms-min-sdk': '26.5.1'
        });

        checkSdkCompatibility(headers, '26.1.1');

        expect(errorSpy).toHaveBeenCalledTimes(1);
        expect(errorSpy.mock.calls[0][0]).toContain('26.1.1');
        expect(warnSpy).not.toHaveBeenCalled();
    });

    it('logs a console warning when the SDK is newer than the server', () => {
        const headers = new Headers({
            'x-dotcms-version': '26.7.13',
            'x-dotcms-min-sdk': '26.5.1'
        });

        checkSdkCompatibility(headers, '26.10.1');

        expect(warnSpy).toHaveBeenCalledTimes(1);
        expect(warnSpy.mock.calls[0][0]).toContain('26.10.1');
        expect(errorSpy).not.toHaveBeenCalled();
    });

    it('only logs each warning once per session, across multiple calls', () => {
        const headers = new Headers({
            'x-dotcms-version': '26.7.13',
            'x-dotcms-min-sdk': '26.5.1'
        });

        checkSdkCompatibility(headers, '26.1.1');
        checkSdkCompatibility(headers, '26.1.1');
        checkSdkCompatibility(headers, '26.1.1');

        expect(errorSpy).toHaveBeenCalledTimes(1);
    });

    it('fails open (does nothing) when own version is unparsable/empty', () => {
        const headers = new Headers({
            'x-dotcms-version': '26.7.13',
            'x-dotcms-min-sdk': '26.5.1'
        });

        checkSdkCompatibility(headers, '');

        expect(errorSpy).not.toHaveBeenCalled();
        expect(warnSpy).not.toHaveBeenCalled();
    });
});
