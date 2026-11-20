package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps, in a page content save, the page's rows that the user could not see in the page editor.
 * <p>
 * The page editor shows a user only the content they can see and, on save, sends back the content
 * it shows. A save replaces all of a personalization's rows on the page, so without this the
 * content the user could not see (no READ permission, archived) would be removed from the page,
 * and from the live page, by any edit. This class only decides where those rows go; the caller
 * decides which rows the user could not see.
 * <p>
 * Rows are matched by container slot: the container and its instance on the page. Both are
 * compared the way the page render reads them, so a container sent as a path matches the same
 * container stored as an identifier, and a slot stored with the legacy instance id matches slot
 * {@code "1"}, which is how the render and the page editor show it.
 * <p>
 * An instance covers the rows of one personalization; it is not thread-safe.
 */
final class HiddenPageRows {

    private final List<MultiTree> currentRows;
    private final List<MultiTree> sentRows;
    private final Function<MultiTree, String> containerIdOf;
    private final Map<String, String> containerIdByContainer = new HashMap<>();

    /**
     * Prepares the merge of the rows a user sent for one personalization with the page's current
     * rows of that personalization.
     *
     * @param currentRows the page's current rows for the personalization, in the tree the variant
     *                    shows (its own rows, or the default variant's when it has none), in any
     *                    order
     * @param sentRows    the rows built from the request for the personalization; the list is not
     *                    modified, but the rows of a slot that gets rows back are renumbered
     */
    HiddenPageRows(final List<MultiTree> currentRows, final List<MultiTree> sentRows) {
        this(currentRows, sentRows, MultiTree::getContainerAsID);
    }

    /**
     * Same as {@link #HiddenPageRows(List, List)}, resolving each row's container identifier with
     * {@code containerIdOf} instead of {@link MultiTree#getContainerAsID()}.
     *
     * @param currentRows   the page's current rows for the personalization
     * @param sentRows      the rows built from the request for the personalization
     * @param containerIdOf returns the identifier of a row's container, whether the row holds the
     *                      identifier or the container's path
     */
    HiddenPageRows(final List<MultiTree> currentRows, final List<MultiTree> sentRows,
            final Function<MultiTree, String> containerIdOf) {
        this.currentRows = currentRows;
        this.sentRows = sentRows;
        this.containerIdOf = containerIdOf;
    }

    /**
     * Returns the contentlets of the current rows that the user did not send back in the same
     * slot. Only these can be kept by {@link #keep}: content the user sent in a slot is saved there
     * as sent, whether or not the user can see it. The caller needs to check visibility only for
     * these, which in a page editor save is the content the editor did not show plus the content
     * the user removed.
     *
     * @return identifiers of the contentlets the caller must check, without {@code null}
     */
    Set<String> unsentContentIds() {
        final Map<String, Set<String>> sentContentIdsBySlot = sentRows.stream().collect(Collectors.groupingBy(
                this::slot, Collectors.mapping(MultiTree::getContentlet, Collectors.toSet())));
        final Set<String> unsentContentIds = new HashSet<>();
        for (final MultiTree current : currentRows) {
            if (null != current.getContentlet() && !sentContentIdsBySlot
                    .getOrDefault(slot(current), Set.of()).contains(current.getContentlet())) {
                unsentContentIds.add(current.getContentlet());
            }
        }
        return unsentContentIds;
    }

    /**
     * Adds, to the rows the user sent, the current rows the user could not see.
     * <p>
     * Each kept row goes back into its container slot right after the row that preceded it before
     * the save, or first in the slot when none of the rows before it is still there. Rows the user
     * could not see keep their order among themselves. A slot that gets rows back is renumbered
     * from 0; every other slot keeps the order the user sent.
     * <p>
     * Content the user could not see but sent in its slot anyway is saved where the user put it in
     * that slot, not added twice. Sending it to another slot adds it there and keeps it where it
     * was: the same content can sit in several containers of a page, and a placement the user
     * could not see is never removed.
     * <p>
     * Kept rows are copied into the variant being saved, with their personalization and style
     * properties. A kept row stored with the legacy instance id is written with instance id
     * {@code "1"}, the slot the page editor sends for it, so the slot is not split in two.
     *
     * @param hiddenContentIds identifiers of the contentlets the user could not see
     * @param variantName      the variant being saved
     * @return the sent rows themselves when no current row is hidden from the user, otherwise a
     *         new list with the rows to save
     */
    List<MultiTree> keep(final Set<String> hiddenContentIds, final String variantName) {

        if (hiddenContentIds.isEmpty()
                || currentRows.stream().noneMatch(row -> hiddenContentIds.contains(row.getContentlet()))) {
            return sentRows;
        }

        final Map<String, List<MultiTree>> currentBySlot = currentRows.stream()
                .sorted(Comparator.comparingInt(MultiTree::getTreeOrder))
                .collect(Collectors.groupingBy(this::slot, LinkedHashMap::new, Collectors.toList()));
        final Map<String, List<MultiTree>> rowsBySlot = new LinkedHashMap<>();
        sentRows.forEach(row -> rowsBySlot.computeIfAbsent(slot(row), key -> new ArrayList<>()).add(row));

        for (final Map.Entry<String, List<MultiTree>> slotRows : currentBySlot.entrySet()) {
            if (slotRows.getValue().stream().noneMatch(row -> hiddenContentIds.contains(row.getContentlet()))) {
                continue;
            }
            final List<MultiTree> slot = rowsBySlot.computeIfAbsent(slotRows.getKey(), key -> new ArrayList<>());
            putBackHiddenRows(slotRows.getValue(), slot, hiddenContentIds, variantName);
            for (int order = 0; order < slot.size(); order++) {
                slot.get(order).setTreeOrder(order);
            }
        }

        return rowsBySlot.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    /**
     * Inserts the hidden rows of one slot into the rows to save for that slot. Walks the slot's
     * current rows in tree order, remembering where the last row still present in {@code slot}
     * sits, and puts each hidden row right after it.
     *
     * @param currentSlotRows  the slot's current rows, in tree order
     * @param slot             the rows to save for the slot; hidden rows are inserted into it
     * @param hiddenContentIds identifiers of the contentlets the user could not see
     * @param variantName      the variant being saved
     */
    private static void putBackHiddenRows(final List<MultiTree> currentSlotRows, final List<MultiTree> slot,
            final Set<String> hiddenContentIds, final String variantName) {

        final Set<String> sentContentIds = slot.stream().map(MultiTree::getContentlet).collect(Collectors.toSet());
        int insertAt = 0;
        for (final MultiTree current : currentSlotRows) {
            if (hiddenContentIds.contains(current.getContentlet())
                    && !sentContentIds.contains(current.getContentlet())) {
                final MultiTree kept = MultiTree.buildMultitreeWithVariant(current, variantName);
                kept.setInstanceId(instanceId(current));
                slot.add(insertAt, kept);
                insertAt++;
            } else {
                final int position = indexOfContent(slot, current.getContentlet());
                if (position >= 0) {
                    insertAt = position + 1;
                }
            }
        }
    }

    /**
     * Returns the position of the first row of {@code rows} that places the contentlet, or -1.
     */
    private static int indexOfContent(final List<MultiTree> rows, final String contentId) {
        for (int index = 0; index < rows.size(); index++) {
            if (Objects.equals(contentId, rows.get(index).getContentlet())) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Identifies the container slot a row belongs to, the way the page render reads it: the
     * container's identifier and the container's instance on the page.
     */
    private String slot(final MultiTree row) {
        final String containerId = containerIdByContainer.computeIfAbsent(row.getContainer(),
                container -> containerIdOf.apply(row));
        return containerId + StringPool.PIPE + instanceId(row);
    }

    /**
     * Returns the row's container instance id, reading the legacy instance id as {@code "1"}, as
     * the page render does.
     */
    private static String instanceId(final MultiTree row) {
        return ContainerUUID.UUID_LEGACY_VALUE.equals(row.getRelationType())
                ? ContainerUUID.UUID_START_VALUE
                : row.getRelationType();
    }
}
