'use strict';

/**
 * Resolves which @dotcms/* version the example apps on a release branch must be pinned to,
 * and refuses to answer rather than guess when it cannot.
 *
 * Extracted from cicd_comp_release-prepare-phase.yml so the decision is unit-testable
 * without cutting a release: as inline shell it could only be exercised by an actual
 * release, and three separate defects shipped that way (see resolve.test.js "regressions").
 *
 * Deliberately dependency-free CommonJS so the release pipeline can invoke it with a bare
 * `node resolve.js` — no npm install, no build step, nothing that can fail at release time.
 */

/** An exact, non-range pin: 1.2.0, 26.8.3-1, 1.2.0-next.2632. */
const EXACT_VERSION = /^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/;

/**
 * The date-lockstep STABLE form (26.8.7-1) and nothing else. This is what excludes the
 * `-next.<run>` prereleases, the 0.0.1-alpha/beta line and the pre-lockstep 1.x scheme from
 * being picked as a pin for a release branch.
 */
const LOCKSTEP_STABLE = /^\d+\.\d+\.\d+-\d+$/;

const LTS_VERSION = /^(\d{2})\.(\d{2})\.(\d{2})_lts_v\d{1,2}$/;

/**
 * Zero-padded CalVer (26.08.03-01) -> the valid semver npm publishes under (26.8.3-1).
 *
 * Walks the string segment by segment so the original separators and every trailing segment
 * survive. Splitting on [.-] and rebuilding from the first four fields silently truncates
 * the `-next.<run_number>` suffix cicd_3-trunk.yml appends, which makes every `next` publish
 * collide with the stable release version.
 */
function normalizeVersion(version) {
    return String(version)
        .split(/([.-])/)
        .map((part) => (/^\d+$/.test(part) ? String(Number(part)) : part))
        .join('');
}

function isExactVersion(version) {
    return EXACT_VERSION.test(String(version));
}

/** '26.08.08_lts_v1' -> '2026-08-08'. null when the version is not LTS-shaped. */
function ltsDateFromVersion(releaseVersion) {
    const match = LTS_VERSION.exec(String(releaseVersion));
    return match ? `20${match[1]}-${match[2]}-${match[3]}` : null;
}

/**
 * The date the resolution looks back from: the EARLIER of the branch commit's own date and
 * the date encoded in the LTS version.
 *
 * They coincide when a line is named after the release it is cut from. Taking the earlier
 * means a mis-named line errs toward an OLDER SDK — which a server tolerates — rather than a
 * newer one, which is the direction that breaks customers.
 */
function resolveCutoff(commitDate, releaseVersion) {
    const commitDay = String(commitDate).slice(0, 10);
    const ltsDay = ltsDateFromVersion(releaseVersion);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(commitDay)) {
        throw new Error(`Unusable commit date "${commitDate}" — expected an ISO-8601 timestamp.`);
    }
    return ltsDay && ltsDay < commitDay ? ltsDay : commitDay;
}

/**
 * The newest date-lockstep stable version published on or before `cutoffDay`.
 *
 * "On or before" rather than "nearest": for a line dated 26.08.09 the nearest published
 * version is 26.8.10-1, one day AFTER — an SDK ahead of its own server, which is the drift
 * behind Freshdesk #38677.
 *
 * @param {Record<string, string>} registryTime npm's `.time` map: version -> ISO publish date
 */
function selectPublishedOnOrBefore(registryTime, cutoffDay) {
    const cutoff = `${cutoffDay}T23:59:59.999Z`;
    return (
        Object.entries(registryTime || {})
            .filter(([version, published]) => LOCKSTEP_STABLE.test(version) && published <= cutoff)
            .sort((a, b) => (a[1] < b[1] ? -1 : a[1] > b[1] ? 1 : 0))
            .map(([version]) => version)
            .pop() || null
    );
}

/**
 * Ordered comparison by numeric segments, so zero padding does not matter: MinSdkVersion.VALUE
 * is written in the raw release form (26.08.19-01) while a pin is in npm form (26.8.19-1).
 * Mirrors the SDK's own compareVersions in sdk-compatibility.ts.
 */
function compareVersions(a, b) {
    const segments = (v) => String(v).replace(/^v/i, '').split(/[.-]/).map(Number);
    const left = segments(a);
    const right = segments(b);
    for (let i = 0; i < Math.max(left.length, right.length); i++) {
        const x = left[i] || 0;
        const y = right[i] || 0;
        if (x !== y) return x > y ? 1 : -1;
    }
    return 0;
}

/**
 * MinSdkVersion.VALUE is the oldest @dotcms/* SDK a build still supports, served to clients
 * as X-DotCMS-Min-SDK. "0.0.0" is the baseline meaning no breaking change has been declared,
 * so every published SDK is still considered compatible.
 */
function meetsFloor(pin, minSdkValue) {
    if (!minSdkValue || minSdkValue === '0.0.0') return true;
    return compareVersions(pin, minSdkValue) >= 0;
}

class ResolutionError extends Error {}

/**
 * The whole decision, as a pure function of data. Every I/O concern (reading manifests,
 * fetching the registry, reading MinSdkVersion.java) is the caller's job, so this is
 * exhaustively testable.
 *
 * @returns {{action:'none'|'keep'|'pin', reason:string, version?:string,
 *            verifyPublished?:boolean, cutoff?:string}}
 */
function decide({ specs, isLts, releaseVersion, commitDate, registryTime, minSdkValue }) {
    if (!specs || specs.length === 0) {
        return { action: 'none', reason: 'no @dotcms/* dependencies in any example manifest' };
    }

    if (specs.every(isExactVersion)) {
        return {
            action: 'keep',
            reason:
                'example manifests already carry exact pins — branching off an existing LTS lineage, ' +
                'which stays frozen where it was'
        };
    }

    let version;
    let verifyPublished;
    let cutoff = null;
    let reason;

    if (isLts) {
        // cicd_release-sdk.yml skips the SDK publish for LTS, so no SDK exists at this
        // release's own version and 26.09.15_lts_v1 is not valid semver anyway. Resolve by
        // date instead — never npm's `latest`, which is only correct for a line cut from
        // main at that moment and is months ahead for a line cut from an older commit.
        cutoff = resolveCutoff(commitDate, releaseVersion);
        version = selectPublishedOnOrBefore(registryTime, cutoff);
        if (!version) {
            throw new ResolutionError(
                `No @dotcms/client release was published on or before ${cutoff}. This LTS line predates ` +
                    'date-lockstep SDK publishing (ADR-0019), so no pin can be derived — pin its example apps ' +
                    'manually in a reviewed commit, the way PRs #37475/#37476 did for the 25.07.10 line.'
            );
        }
        verifyPublished = true;
        reason = `newest @dotcms/client published on or before ${cutoff}`;
    } else {
        // A normal release publishes its own SDK at the same version (ADR-0019 lockstep),
        // but only AFTER this step: "Create GitHub Release" is what triggers
        // cicd_release-sdk.yml. So the pin is legitimately absent from npm right now and
        // must not be existence-checked.
        version = normalizeVersion(releaseVersion);
        verifyPublished = false;
        reason = "this release's own SDK version, published later in this same run";
    }

    if (!isExactVersion(version)) {
        throw new ResolutionError(
            `Refusing to pin example apps to "${version}" — not an exact version. ` +
                'validate-sdk-package-shapes would reject it on this branch and npm install would fail.'
        );
    }

    if (!meetsFloor(version, minSdkValue)) {
        throw new ResolutionError(
            `Resolved SDK pin ${version} is OLDER than the minimum this build supports ` +
                `(MinSdkVersion.VALUE = ${minSdkValue}). The example apps would ship an SDK this server ` +
                `rejects. Pin them manually to a version at or above ${minSdkValue}.`
        );
    }

    return { action: 'pin', version, verifyPublished, cutoff, reason };
}

module.exports = {
    normalizeVersion,
    isExactVersion,
    ltsDateFromVersion,
    resolveCutoff,
    selectPublishedOnOrBefore,
    compareVersions,
    meetsFloor,
    decide,
    ResolutionError,
    EXACT_VERSION,
    LOCKSTEP_STABLE
};
