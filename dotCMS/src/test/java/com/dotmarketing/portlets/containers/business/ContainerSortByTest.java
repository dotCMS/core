package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotmarketing.portlets.containers.model.Container;
import java.util.Optional;
import org.junit.Test;

/**
 * Verifies how {@link ContainerSortBy} turns the "sort contentlets by" setting of a container into
 * the sort expression used when loading the contentlets of a page container.
 */
public class ContainerSortByTest extends UnitTestBase {

    /**
     * Method to test: {@link ContainerSortBy#resolve(String)}
     * Given: no value
     * Should: resolve to the page container order and count as supported
     */
    @Test
    public void emptyValueIsTheDefaultOrder() {
        assertEquals(Optional.of(ContainerSortBy.DEFAULT_ORDER_BY), ContainerSortBy.resolve(null));
        assertEquals(Optional.of(ContainerSortBy.DEFAULT_ORDER_BY), ContainerSortBy.resolve(""));
        assertEquals(Optional.of(ContainerSortBy.DEFAULT_ORDER_BY), ContainerSortBy.resolve("  "));
        assertTrue(ContainerSortBy.isSupported(null));
    }

    /**
     * Method to test: {@link ContainerSortBy#resolve(String)}
     * Given: a supported column, with and without a direction, in any case or spacing
     * Should: resolve to the qualified column
     */
    @Test
    public void supportedColumnsResolveToQualifiedColumns() {
        assertEquals(Optional.of("multi_tree.tree_order"), ContainerSortBy.resolve("tree_order"));
        assertEquals(Optional.of("multi_tree.tree_order"), ContainerSortBy.resolve(" Tree_Order "));
        assertEquals(Optional.of("contentlet.title"), ContainerSortBy.resolve("title"));
        assertEquals(Optional.of("contentlet.title desc"), ContainerSortBy.resolve("title DESC"));
        assertEquals(Optional.of("contentlet.mod_date desc"), ContainerSortBy.resolve("mod_date   desc"));
        assertEquals(Optional.of("contentlet.mod_date asc"), ContainerSortBy.resolve("mod_date asc"));
        assertEquals(Optional.of("contentlet.sort_order"), ContainerSortBy.resolve("sort_order"));
        assertEquals(Optional.of("contentlet.inode"), ContainerSortBy.resolve("inode"));
        assertTrue(ContainerSortBy.isSupported("title desc"));
    }

    /**
     * Method to test: {@link ContainerSortBy#resolve(String)}
     * Given: a value that is not a single supported column with an optional direction
     * Should: be unsupported
     */
    @Test
    public void anythingElseIsUnsupported() {
        final String[] unsupported = {
                "multi_tree.tree_order",
                "contentlet.title",
                "-title",
                "title, mod_date",
                "tree_order,(select 1)",
                "mod_date,upper(name)",
                "title desc, inode",
                "title descending",
                "name",
                "modDate",
                "not_a_column"
        };
        for (final String value : unsupported) {
            assertEquals(value, Optional.empty(), ContainerSortBy.resolve(value));
            assertFalse(value, ContainerSortBy.isSupported(value));
        }
    }

    /**
     * Method to test: {@link ContainerSortBy#resetIfUnsupported(Container)}
     * Given: containers with a supported and with an unsupported value
     * Should: leave the supported one alone and clear the unsupported one
     */
    @Test
    public void resetIfUnsupportedClearsOnlyUnsupportedValues() {
        final Container supported = new Container();
        supported.setSortContentletsBy("title desc");
        assertFalse(ContainerSortBy.resetIfUnsupported(supported));
        assertEquals("title desc", supported.getSortContentletsBy());

        final Container unsupported = new Container();
        unsupported.setSortContentletsBy("title, mod_date");
        assertTrue(ContainerSortBy.resetIfUnsupported(unsupported));
        assertEquals(null, unsupported.getSortContentletsBy());
    }

    /**
     * Method to test: {@link ContainerSortBy#resolveOrDefault(String)}
     * Given: supported and unsupported values
     * Should: keep the supported ones and fall back to the page container order for the rest
     */
    @Test
    public void resolveOrDefaultNeverFails() {
        assertEquals("contentlet.title desc", ContainerSortBy.resolveOrDefault("title desc"));
        assertEquals(ContainerSortBy.DEFAULT_ORDER_BY, ContainerSortBy.resolveOrDefault(null));
        assertEquals(ContainerSortBy.DEFAULT_ORDER_BY,
                ContainerSortBy.resolveOrDefault("tree_order,(select 1)"));
        assertEquals(ContainerSortBy.DEFAULT_ORDER_BY, ContainerSortBy.resolveOrDefault("-title"));
    }

    /**
     * Method to test: {@link ContainerSortBy#resolveForContentletQuery(String)}
     * Given: the page order with and without a direction, a contentlet column, an unsupported value
     * Should: never name multi_tree; the page order becomes the contentlet sort order, keeping the direction
     */
    @Test
    public void contentletQueryNeverSortsByThePageOrder() {
        assertEquals("contentlet.sort_order", ContainerSortBy.resolveForContentletQuery(null));
        assertEquals("contentlet.sort_order", ContainerSortBy.resolveForContentletQuery("tree_order"));
        assertEquals("contentlet.sort_order desc", ContainerSortBy.resolveForContentletQuery("tree_order desc"));
        assertEquals("contentlet.title asc", ContainerSortBy.resolveForContentletQuery("title asc"));
        assertEquals("contentlet.sort_order", ContainerSortBy.resolveForContentletQuery("title, mod_date"));
    }

    /**
     * Method to test: {@link ContainerSortBy#warnsFor(String)}
     * Given: the same container falling back twice, and a fallback with no container
     * Should: warn the first time only, and never without a container
     */
    @Test
    public void unsupportedValueWarnsOncePerContainer() {
        final String containerId = "container-" + System.nanoTime();

        assertTrue(ContainerSortBy.warnsFor(containerId));
        assertFalse(ContainerSortBy.warnsFor(containerId));
        assertFalse(ContainerSortBy.warnsFor(null));
    }
}
