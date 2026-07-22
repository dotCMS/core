const DOTCMS_VERSION_HEADER = 'x-dotcms-version';
const DOTCMS_MIN_SDK_HEADER = 'x-dotcms-min-sdk';

let hasWarnedOutdatedSdk = false;
let hasWarnedNewerSdk = false;

/**
 * Parses a date-lockstep version string (e.g. "26.7.14-1") into a flat array of numeric
 * segments for ordered comparison. Returns null if any segment isn't a plain integer
 * (e.g. an LTS-shaped version like "26.7.14_lts_v1"), so callers can skip the comparison
 * instead of comparing unrelated formats.
 */
const parseVersionSegments = (version: string): number[] | null => {
    const segments = version
        .trim()
        .replace(/^v/i, '')
        .split(/[.-]/)
        .map((segment) => Number(segment));

    return segments.some((segment) => !Number.isFinite(segment)) ? null : segments;
};

/**
 * Compares two date-lockstep version strings segment by segment as numbers (not as
 * strings), so e.g. "26.10.1" correctly compares greater than "26.7.13". Returns null
 * (instead of throwing) if either version can't be parsed, so callers can fail open.
 */
export const compareVersions = (a: string, b: string): number | null => {
    const segmentsA = parseVersionSegments(a);
    const segmentsB = parseVersionSegments(b);

    if (!segmentsA || !segmentsB) {
        return null;
    }

    const length = Math.max(segmentsA.length, segmentsB.length);

    for (let i = 0; i < length; i++) {
        const partA = segmentsA[i] ?? 0;
        const partB = segmentsB[i] ?? 0;

        if (partA !== partB) {
            return partA > partB ? 1 : -1;
        }
    }

    return 0;
};

/**
 * Resets the once-per-session warning flags. Test-only — production code never needs
 * to warn more than once per page load.
 */
export const resetSdkCompatibilityWarnings = (): void => {
    hasWarnedOutdatedSdk = false;
    hasWarnedNewerSdk = false;
};

/**
 * Reads the dotCMS server's advertised version and minimum supported SDK version off a
 * response (see `SdkVersionWebInterceptor` on the server) and logs a console error/warning
 * if this SDK is outside the compatible range:
 *
 * - `ownVersion < X-DotCMS-Min-SDK` → console.error, the SDK must be upgraded.
 * - `ownVersion > X-DotCMS-Version` → console.warn, the SDK is ahead of this server and
 *   may call APIs it doesn't have yet (e.g. a dev environment newer than the one it's
 *   pointed at).
 *
 * Fails open by design: if the headers are absent (an older server that doesn't send
 * them yet) or unparsable, this silently does nothing — it never throws and never
 * changes request/response behavior. Each kind of warning logs at most once per session
 * so it doesn't spam the console on every request.
 */
export const checkSdkCompatibility = (headers: Headers, ownVersion: string): void => {
    try {
        if (!ownVersion) {
            return;
        }

        const serverVersion = headers.get(DOTCMS_VERSION_HEADER);
        const minSdkVersion = headers.get(DOTCMS_MIN_SDK_HEADER);

        if (!serverVersion || !minSdkVersion) {
            return;
        }

        if (!hasWarnedOutdatedSdk && (compareVersions(ownVersion, minSdkVersion) ?? 0) < 0) {
            hasWarnedOutdatedSdk = true;
            console.error(
                `[dotCMS SDK] SDK ${ownVersion} is not supported by dotCMS ${serverVersion} ` +
                    `(minimum required: ${minSdkVersion}). Install @dotcms/client@${minSdkVersion} or newer.`
            );
        }

        if (!hasWarnedNewerSdk && (compareVersions(ownVersion, serverVersion) ?? 0) > 0) {
            hasWarnedNewerSdk = true;
            console.warn(
                `[dotCMS SDK] SDK ${ownVersion} is newer than dotCMS ${serverVersion} ` +
                    'and may call APIs the server does not have yet.'
            );
        }
    } catch {
        // Never let a compatibility-check failure break the actual request.
    }
};
