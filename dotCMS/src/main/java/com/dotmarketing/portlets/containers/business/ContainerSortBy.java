package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Supported values of the "sort contentlets by" setting of a container.
 * <p>
 * The setting names the column used to order the contentlets of a page container when they are
 * loaded from the database. Only the columns listed here are supported, optionally followed by
 * {@code asc} or {@code desc}; the comparison ignores case and surrounding whitespace. Every other
 * value is unsupported: it is rejected when a container is saved and ignored, in favour of the
 * multi-tree position, when the contentlets are loaded.
 */
public final class ContainerSortBy {

    /** Sort expression used when no supported value is set: the position in the page container. */
    public static final String DEFAULT_ORDER_BY = "multi_tree.tree_order";

    private static final Map<String, String> COLUMNS = Map.of(
            "tree_order", DEFAULT_ORDER_BY,
            "title", "contentlet.title",
            "mod_date", "contentlet.mod_date",
            "sort_order", "contentlet.sort_order",
            "inode", "contentlet.inode");

    private ContainerSortBy() {
    }

    /**
     * Resolves a "sort contentlets by" value into the sort expression used after {@code order by}.
     *
     * @param value the stored or requested value, for example {@code title desc}
     * @return the qualified sort expression, or empty when the value is not supported; an unset
     * value resolves to {@link #DEFAULT_ORDER_BY}
     */
    public static Optional<String> resolve(final String value) {
        if (!UtilMethods.isSet(value)) {
            return Optional.of(DEFAULT_ORDER_BY);
        }
        final String[] parts = value.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length > 2 || !COLUMNS.containsKey(parts[0])) {
            return Optional.empty();
        }
        final String column = COLUMNS.get(parts[0]);
        if (parts.length == 1) {
            return Optional.of(column);
        }
        if ("asc".equals(parts[1]) || "desc".equals(parts[1])) {
            return Optional.of(column + " " + parts[1]);
        }
        return Optional.empty();
    }

    /**
     * Tells whether a "sort contentlets by" value can be stored on a container.
     *
     * @param value the value to check; unset means the default order and is supported
     * @return true when the value resolves to a supported sort expression
     */
    public static boolean isSupported(final String value) {
        return resolve(value).isPresent();
    }

    /**
     * Resolves a value the same way as {@link #resolve(String)} but never fails: an unsupported
     * value falls back to {@link #DEFAULT_ORDER_BY} and leaves a warning in the log.
     *
     * @param value the stored or requested value
     * @return the sort expression to use
     */
    public static String resolveOrDefault(final String value) {
        return resolve(value).orElseGet(() -> {
            Logger.warn(ContainerSortBy.class,
                    "Unsupported sort contentlets by value, using the page container order instead");
            Logger.debug(ContainerSortBy.class, () -> "Unsupported sort contentlets by value: " + value);
            return DEFAULT_ORDER_BY;
        });
    }

    /**
     * Clears the "sort contentlets by" value of a container when it is not supported, so a value
     * stored before it was validated does not stop the container from being saved again, copied
     * or received through push publishing. The reset is reported in the log.
     *
     * @param container the container whose value is checked and, if needed, cleared
     * @return true when the value was cleared
     */
    public static boolean resetIfUnsupported(final Container container) {
        if (isSupported(container.getSortContentletsBy())) {
            return false;
        }
        Logger.warn(ContainerSortBy.class, () -> String.format(
                "Container '%s' [%s] has an unsupported sort contentlets by value; using the default order",
                container.getTitle(), container.getIdentifier()));
        container.setSortContentletsBy(null);
        return true;
    }
}
