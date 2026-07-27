package com.dotcms.rest.config;

/**
 * The oldest {@code @dotcms/*} SDK version this dotCMS instance still supports, exposed
 * to clients via the {@code X-DotCMS-Min-SDK} response header (see
 * {@link com.dotcms.filters.interceptor.meta.SdkVersionWebInterceptor}).
 *
 * <p>This value is maintained via a human-reviewed automated PR, not by hand. Under
 * date-lockstep SDK versioning (ADR-0019: {@code platform-adrs/decisions/0019-sdk-cms-date-lockstep-versioning.md})
 * most dotCMS releases never change the SDK contract, so this constant does not need to
 * move on every release — only bump it when a change actually breaks compatibility with
 * older {@code @dotcms/*} SDK versions. See {@code docs/core/SDK_BREAKING_CHANGE_CATEGORIES.md}
 * for concrete categories (removed/renamed GraphQL fields, changed {@code postMessage}
 * editor protocol messages, REST response shape changes, etc.).
 *
 * <p><strong>Bump procedure (automated):</strong> do not edit {@link #VALUE} directly in a
 * PR. Instead:
 * <ol>
 *   <li>Get your PR labeled {@code SDK Breaking Change} — an automated check
 *       ({@code ai_claude-sdk-breaking-change.yml}) evaluates every PR's diff against
 *       {@code docs/core/SDK_BREAKING_CHANGE_CATEGORIES.md} and adds the label (with an
 *       explanatory comment) when it detects a break. The check only ever adds this label,
 *       never removes it — if you disagree with its verdict, remove the label yourself.</li>
 *   <li>At release time, the operator running {@code cicd_6-release.yml} sets its
 *       {@code bump_min_sdk_version} input to {@code true} for a release that includes
 *       such a PR. The release's {@code verify-branch} job fails outright if a
 *       breaking-change-labeled PR merged since the last release is in range and this
 *       input was left {@code false} — forgetting is a loud pipeline failure, not a
 *       silent gap.</li>
 *   <li>Once the release fully succeeds (build AND deployment green — never eagerly),
 *       a dedicated job opens a PR against {@code main} bumping {@link #VALUE} to that
 *       release's version and pings Slack asking a human to review and merge it.
 *       {@code main} is never pushed to directly, and nothing changes on {@code main} if
 *       the release fails partway through — there is nothing to roll back.</li>
 * </ol>
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
