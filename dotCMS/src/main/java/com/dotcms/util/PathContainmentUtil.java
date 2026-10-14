package com.dotcms.util;

import com.dotmarketing.util.Logger;

import java.io.File;
import java.io.IOException;

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

}
