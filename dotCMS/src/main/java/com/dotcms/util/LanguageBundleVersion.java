package com.dotcms.util;

import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.util.ReleaseInfo;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Build identity for client-side language bundles. This token changes with the packaged build,
 * not with CMS language-variable edits, the server's start time, locale or timezone.
 */
public final class LanguageBundleVersion {

    /** Prevents instantiation of this shared build-identity utility. */
    private LanguageBundleVersion() {
    }

    /**
     * Returns the SHA-256 identity of this release's version, revision and build timestamp.
     *
     * @return the lowercase hexadecimal language bundle version
     */
    public static String get() {
        return compute(ReleaseInfo.getVersion(), ReleaseInfo.getBuildNumber(),
                ReleaseInfo.getBuildDate().getTime());
    }

    /**
     * Hashes length-prefixed build components so their boundaries are unambiguous.
     *
     * @param version the release version
     * @param buildNumber the build revision
     * @param buildDateEpoch the build timestamp in epoch milliseconds
     * @return the lowercase hexadecimal SHA-256 digest
     */
    @VisibleForTesting
    static String compute(final String version, final String buildNumber, final long buildDateEpoch) {
        return DigestUtils.sha256Hex(version.length() + ":" + version
                + buildNumber.length() + ":" + buildNumber + ":" + buildDateEpoch);
    }
}
