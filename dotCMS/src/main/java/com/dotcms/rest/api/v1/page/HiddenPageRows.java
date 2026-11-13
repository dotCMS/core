package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.MultiTree;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps, in a page content save, the page's rows that the user could not see in the page editor.
 * <p>
 * The page editor shows a user only the content they can see and, on save, sends back the content
 * it shows. A save replaces all of a personalization's rows on the page, so without this the
 * content the user could not see (no READ permission, archived) would be removed from the page,
 * and from the live page, by any edit. This class only decides where those rows go; the caller
 * decides which rows the user could not see.
 */
final class HiddenPageRows {

    private HiddenPageRows() {
    }

    /**
     * Adds, to the rows a user sent for one personalization, the page's current rows of that
     * personalization the user could not see.
     * <p>
     * Each kept row goes back into its container slot right after the row that preceded it before
     * the save, or first in the slot when none of the rows before it is still there. Rows the user
     * could not see keep their order among themselves. A slot that gets rows back is renumbered
     * from 0; every other slot keeps the order the user sent. Content the user could not see but
     * sent anyway is saved where the user put it, not added twice. Kept rows are copied into the
     * variant being saved, with their personalization and style properties.
     *
     * @param currentRows      the page's current rows for the personalization, in the tree the
     *                         variant shows (its own rows, or the default variant's when it has
     *                         none), in any order
     * @param sentRows         the rows built from the request for the personalization; this list
     *                         is not modified
     * @param hiddenContentIds identifiers of the contentlets the user could not see
     * @param variantName      the variant being saved
     * @return {@code sentRows} itself when no current row is hidden from the user, otherwise a new
     *         list with the rows to save
     */
    static List<MultiTree> keep(final List<MultiTree> currentRows, final List<MultiTree> sentRows,
            final Set<String> hiddenContentIds, final String variantName) {

        if (hiddenContentIds.isEmpty()
                || currentRows.stream().noneMatch(row -> hiddenContentIds.contains(row.getContentlet()))) {
            return sentRows;
        }

        final Map<String, List<MultiTree>> currentBySlot = currentRows.stream()
                .sorted(Comparator.comparingInt(MultiTree::getTreeOrder))
                .collect(Collectors.groupingBy(HiddenPageRows::slot, LinkedHashMap::new, Collectors.toList()));
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
                slot.add(insertAt, MultiTree.buildMultitreeWithVariant(current, variantName));
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
     * Identifies the container slot a row belongs to: its container and the container's instance
     * on the page.
     */
    private static String slot(final MultiTree row) {
        return row.getContainer() + StringPool.PIPE + row.getRelationType();
    }
}
