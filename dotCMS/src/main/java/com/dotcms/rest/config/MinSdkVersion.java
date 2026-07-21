package com.dotcms.rest.config;

/**
 * The oldest {@code @dotcms/*} SDK version this dotCMS instance still supports, exposed
 * to clients via the {@code X-DotCMS-Min-SDK} response header (see
 * {@link com.dotcms.rest.api.SdkVersionHeaderFilter}).
 *
 * <p>This value is <strong>manually maintained</strong>, not computed. Under date-lockstep
 * SDK versioning (ADR-0019: {@code platform-adrs/decisions/0019-sdk-cms-date-lockstep-versioning.md})
 * most dotCMS releases never change the SDK contract, so this constant does not need to
 * move on every release — only bump it when a change actually breaks compatibility with
 * older {@code @dotcms/*} SDK versions (e.g. a newly-required GraphQL field, a changed
 * {@code postMessage} editor protocol message, a REST response shape the SDK depends on).
 *
 * <p><strong>Bump procedure:</strong> when your PR breaks SDK compatibility, set
 * {@link #VALUE} to whatever {@code @dotcms/client} version is already published on npm
 * as {@code latest} at the time you write the PR. Do not try to guess the exact future
 * dotCMS release version this change will ship in — that date-based version number is
 * only assigned when the release is actually cut, so it isn't known yet at PR time. The
 * goal of this value is only to correctly gate SDKs older than what's needed, not to
 * match the eventual release number exactly.
 */
public final class MinSdkVersion {

    /**
     * No breaking change has been introduced under this mechanism yet, so every
     * previously published SDK version is still considered compatible. The first real
     * bump of this value should replace this baseline.
     */
    public static final String VALUE = "0.0.0";

    private MinSdkVersion() {
        // utility class, no instances
    }

}
