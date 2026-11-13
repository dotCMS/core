package com.dotcms.rest.api.v1.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.dotcms.UnitTestBase;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.MultiTree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Tests {@link HiddenPageRows}: how a page content save keeps the rows the user could not see in
 * the page editor.
 */
public class HiddenPageRowsTest extends UnitTestBase {

    private static final String PAGE = "page";
    private static final String CONTAINER = "container";
    private static final String DEFAULT = MultiTree.DOT_PERSONALIZATION_DEFAULT;
    private static final String DEFAULT_VARIANT = VariantAPI.DEFAULT_VARIANT.name();

    /**
     * When none of the page's current rows is hidden from the user, the rows the user sent are
     * saved exactly as sent.
     */
    @Test
    public void testKeep_WhenNoRowIsHidden_ShouldReturnTheSentRows() {
        final List<MultiTree> sent = List.of(row("1", "A", 0));

        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "A", 0), row("1", "B", 1)), sent, Set.of(), DEFAULT_VARIANT);

        assertSame(sent, result);
    }

    /**
     * When the user removes the only content they could see from a slot that also holds content
     * they could not see, the hidden content stays in the slot.
     */
    @Test
    public void testKeep_WhenTheVisibleRowIsRemoved_ShouldKeepTheHiddenRow() {
        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "A", 0), row("1", "B", 1)), List.of(), Set.of("B"), DEFAULT_VARIANT);

        assertEquals(List.of("1:B:0"), describe(result));
    }

    /**
     * When the row before a hidden row is removed, the hidden row goes first in its slot, so it
     * stays ahead of the rows that followed it.
     */
    @Test
    public void testKeep_WhenThePrecedingRowIsRemoved_ShouldPutTheHiddenRowFirst() {
        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "A", 0), row("1", "B", 1), row("1", "D", 2)),
                rows(row("1", "D", 0)), Set.of("B"), DEFAULT_VARIANT);

        assertEquals(List.of("1:B:0", "1:D:1"), describe(result));
    }

    /**
     * When the user reorders the visible rows, a hidden row follows the row that preceded it
     * before the save.
     */
    @Test
    public void testKeep_WhenVisibleRowsAreReordered_ShouldKeepTheHiddenRowAfterItsPredecessor() {
        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "A", 0), row("1", "B", 1), row("1", "D", 2)),
                rows(row("1", "D", 0), row("1", "A", 1)), Set.of("B"), DEFAULT_VARIANT);

        assertEquals(List.of("1:D:0", "1:A:1", "1:B:2"), describe(result));
    }

    /**
     * Several hidden rows in one slot keep their order among themselves.
     */
    @Test
    public void testKeep_WhenSeveralRowsAreHidden_ShouldKeepTheirOrder() {
        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "B1", 0), row("1", "A", 1), row("1", "B2", 2)), List.of(),
                Set.of("B1", "B2"), DEFAULT_VARIANT);

        assertEquals(List.of("1:B1:0", "1:B2:1"), describe(result));
    }

    /**
     * The current rows may come in any order; the tree order decides where each one was.
     */
    @Test
    public void testKeep_WhenCurrentRowsAreUnsorted_ShouldUseTheirTreeOrder() {
        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "D", 2), row("1", "B", 1), row("1", "A", 0)),
                rows(row("1", "A", 0), row("1", "D", 1)), Set.of("B"), DEFAULT_VARIANT);

        assertEquals(List.of("1:A:0", "1:B:1", "1:D:2"), describe(result));
    }

    /**
     * A slot the user did not send at all still keeps the rows it holds that the user could not
     * see; the slots the user sent are saved as sent.
     */
    @Test
    public void testKeep_WhenASlotIsNotSent_ShouldKeepItsHiddenRows() {
        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "A", 0), row("2", "B", 0)), rows(row("1", "A", 0), row("1", "D", 1)),
                Set.of("B"), DEFAULT_VARIANT);

        assertEquals(List.of("1:A:0", "1:D:1", "2:B:0"), describe(result));
    }

    /**
     * When the user sends back content they could not see (an API client that knows its id), it
     * is saved where the user put it and not added a second time.
     */
    @Test
    public void testKeep_WhenTheUserSendsHiddenContent_ShouldNotDuplicateIt() {
        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(row("1", "A", 0), row("1", "B", 1)),
                rows(row("1", "B", 0), row("1", "A", 1)), Set.of("B"), DEFAULT_VARIANT);

        assertEquals(List.of("1:B:0", "1:A:1"), describe(result));
    }

    /**
     * When saving in a variant whose tree is the default variant's, the hidden rows are copied
     * into the variant being saved, with their personalization and style properties.
     */
    @Test
    public void testKeep_WhenSavingInAVariant_ShouldCopyTheHiddenRowsIntoIt() {
        final MultiTree hidden = new MultiTree(PAGE, CONTAINER, "B", "1", 1, "dot:persona:p",
                DEFAULT_VARIANT, Map.of("width", "50%"));

        final List<MultiTree> result = HiddenPageRows.keep(
                List.of(new MultiTree(PAGE, CONTAINER, "A", "1", 0, "dot:persona:p", DEFAULT_VARIANT, null),
                        hidden),
                List.of(), Set.of("B"), "variant-1");

        assertEquals(1, result.size());
        final MultiTree kept = result.get(0);
        assertEquals("B", kept.getContentlet());
        assertEquals("variant-1", kept.getVariantId());
        assertEquals("dot:persona:p", kept.getPersonalization());
        assertEquals(Map.of("width", "50%"), kept.getStyleProperties());
        assertEquals(DEFAULT_VARIANT, hidden.getVariantId());
    }

    private static MultiTree row(final String uuid, final String contentId, final int order) {
        return new MultiTree(PAGE, CONTAINER, contentId, uuid, order, DEFAULT, DEFAULT_VARIANT, null);
    }

    private static List<MultiTree> rows(final MultiTree... rows) {
        return new ArrayList<>(List.of(rows));
    }

    /**
     * Describes the rows as {@code slot:content:order}, listed by slot and then by tree order, so
     * an assertion reads in the order the page shows the content.
     */
    private static List<String> describe(final List<MultiTree> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(MultiTree::getRelationType)
                        .thenComparingInt(MultiTree::getTreeOrder))
                .map(row -> row.getRelationType() + ":" + row.getContentlet() + ":" + row.getTreeOrder())
                .collect(Collectors.toList());
    }
}
