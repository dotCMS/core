package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    /** Sort expression standing in for the page order where there is no page: the contentlet's own order. */
    public static final String CONTENTLET_SORT_ORDER = "contentlet.sort_order";

    private static final Map<String, String> COLUMNS = Map.of(
            "tree_order", DEFAULT_ORDER_BY,
            "title", "contentlet.title",
            "mod_date", "contentlet.mod_date",
            "sort_order", CONTENTLET_SORT_ORDER,
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
     * Resolves a value for a query that lists contentlets without joining {@code multi_tree}, so
     * the page order cannot apply: the page order, with or without a direction, becomes the
     * contentlet's own sort order, and an unsupported value falls back to it as well.
     *
     * @param value the stored or requested value
     * @return the sort expression to use, never empty
     */
    public static String resolveForContentletQuery(final String value) {
        return resolve(value)
                .map(resolved -> resolved.startsWith(DEFAULT_ORDER_BY)
                        ? resolved.replace(DEFAULT_ORDER_BY, CONTENTLET_SORT_ORDER) : resolved)
                .orElse(CONTENTLET_SORT_ORDER);
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
     * Containers already reported for an unsupported stored value, so a page render logs the
     * warning once per container instead of once per request. Bounded so a site with many such
     * containers cannot grow it without limit; past the bound the warning stops and only the debug
     * line remains.
     */
    private static final Set<String> WARNED_CONTAINERS = ConcurrentHashMap.newKeySet();
    private static final int WARNED_CONTAINERS_LIMIT = 1000;

    /**
     * Resolves a value the same way as {@link #resolve(String)} but never fails: an unsupported
     * value falls back to {@link #DEFAULT_ORDER_BY} and is reported in the debug log. Use
     * {@link #resolveOrDefault(String, String)} when the container is known, so the fallback is
     * also reported once as a warning.
     *
     * @param value the stored or requested value
     * @return the sort expression to use
     */
    public static String resolveOrDefault(final String value) {
        return resolveOrDefault(value, null);
    }

    /**
     * Resolves a value the same way as {@link #resolve(String)} but never fails: an unsupported
     * value falls back to {@link #DEFAULT_ORDER_BY}. The first time a given container falls back
     * a warning is logged; later renders of the same container only leave a debug line, since a
     * value stored before it was validated would otherwise be reported on every request.
     *
     * @param value       the stored or requested value
     * @param containerId the container the value belongs to; may be null
     * @return the sort expression to use
     */
    public static String resolveOrDefault(final String value, final String containerId) {
        return resolve(value).orElseGet(() -> {
            if (warnsFor(containerId)) {
                Logger.warn(ContainerSortBy.class, () -> String.format(
                        "Container %s has an unsupported sort contentlets by value; using the page "
                                + "container order instead (reported once per container)", containerId));
            }
            Logger.debug(ContainerSortBy.class, () -> String.format(
                    "Unsupported sort contentlets by value '%s' on container %s", value, containerId));
            return DEFAULT_ORDER_BY;
        });
    }

    /**
     * Records that the given container fell back to the default order and tells whether that is
     * the first time, which is when the warning is logged.
     *
     * @param containerId the container; null never warns
     * @return true the first time the container is seen, while the record is under its bound
     */
    static boolean warnsFor(final String containerId) {
        if (containerId == null || WARNED_CONTAINERS.size() >= WARNED_CONTAINERS_LIMIT) {
            return false;
        }
        return WARNED_CONTAINERS.add(containerId);
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
