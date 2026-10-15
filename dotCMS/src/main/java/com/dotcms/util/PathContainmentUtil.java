package com.dotcms.util;

import com.dotmarketing.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Helpers to check that a name or a path received from outside stays where it is expected to: a
 * name must be a single plain path segment, and a file must resolve to a location inside a given
 * directory.
 */
public final class PathContainmentUtil {

    private PathContainmentUtil() {
    }

    /**
     * Tells whether a name is a single plain path segment, that is, something that can be used
     * as a file name directly below a directory.
     *
     * @param name The name to check.
     * @return {@code true} if the name is not null, not empty, is neither {@code .} nor
     * {@code ..}, and has no forward slash, backslash or NUL character.
     */
    public static boolean isPlainSegment(final String name) {
        return name != null
                && !name.isEmpty()
                && !".".equals(name)
                && !"..".equals(name)
                && name.indexOf('/') < 0
                && name.indexOf('\\') < 0
                && name.indexOf('\0') < 0;
    }

    /**
     * Tells whether a file resolves to a location strictly inside a directory. Both paths are
     * canonicalized, so {@code .} and {@code ..} segments are collapsed and symbolic links are
     * followed. The comparison includes the trailing separator, so a sibling directory that
     * shares the name prefix of the root is not inside it, and the root itself is not inside
     * itself.
     *
     * @param root      The directory that must contain the file.
     * @param candidate The file to check.
     * @return {@code true} if the candidate is inside the root. {@code false} if it is not, if
     * either argument is null, or if a path cannot be canonicalized.
     */
    public static boolean isContainedIn(final File root, final File candidate) {
        if (root == null || candidate == null) {
            return false;
        }
        try {
            final String canonicalRoot = root.getCanonicalPath();
            return candidate.getCanonicalPath().startsWith(canonicalRoot + File.separator);
        } catch (final IOException e) {
            Logger.error(PathContainmentUtil.class, "A path could not be canonicalized", e);
            return false;
        }
    }

    /**
     * Derives the part of a name that comes before a required suffix, such as a file extension,
     * and only if that part is itself a plain segment per {@link #isPlainSegment(String)}.
     * <p>This is stricter than checking the whole name with {@link #isPlainSegment(String)} and
     * then stripping the suffix: a name like {@code "..tar.gz"} is a plain segment as a whole (it
     * is not literally {@code "."} or {@code ".."}), but the part before the {@code ".tar.gz"}
     * suffix is {@code "."}, and a name like {@code "...tar.gz"} yields {@code ".."}. Using
     * either of those as a path segment on its own, for example as an identifier later joined to
     * a directory, resolves to that directory itself or its parent. This method rejects both.
     *
     * @param name   The full name, for example a received file name.
     * @param suffix The required suffix, for example a file extension.
     * @return The part of {@code name} before {@code suffix}, if {@code name} ends with
     * {@code suffix}, has something before it, and that part is a plain segment. Empty otherwise,
     * including when either argument is {@code null}.
     */
    public static Optional<String> plainSegmentBeforeSuffix(final String name, final String suffix) {
        if (name == null || suffix == null || !name.endsWith(suffix) || name.length() == suffix.length()) {
            return Optional.empty();
        }
        final String stem = name.substring(0, name.length() - suffix.length());
        return isPlainSegment(stem) ? Optional.of(stem) : Optional.empty();
    }

}
