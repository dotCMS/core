package com.dotmarketing.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.util.UUIDGenerator;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the bulk and scoped {@link TreeFactory} operations used when saving
 * contentlets with relationships. The tree table has no foreign keys, so these tests work with
 * synthetic identifiers and a unique relation type per test to stay isolated.
 */
public class TreeFactoryTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private static String newId() {
        return UUIDGenerator.generateUuid();
    }

    private static String newRelationType() {
        return "testRel" + UUIDGenerator.generateUuid().replace("-", "");
    }

    /**
     * Method to test: {@link TreeFactory#insertTrees(List)}
     * When: the list contains two trees with the same {@code (parent, child, relation_type)}
     * primary key but different tree order — e.g. the same related content submitted twice, or
     * in two languages
     * Should: NOT fail with a primary key violation, and keep the last occurrence, matching the
     * legacy {@link TreeFactory#saveTree(Tree)} delete-then-insert behavior
     */
    @Test
    public void insertTreesCollapsesDuplicateKeysToLastOccurrence() {
        final String parent = newId();
        final String child = newId();
        final String relationType = newRelationType();

        TreeFactory.insertTrees(List.of(
                new Tree(parent, child, relationType, 1),
                new Tree(parent, child, relationType, 5)));

        final List<String> relatedIds = TreeFactory
                .getRelatedIdsByParentAndRelationType(parent, relationType);
        assertEquals("Duplicates by PK must collapse into a single row", 1, relatedIds.size());
        assertEquals(child, relatedIds.get(0));

        final Tree saved = TreeFactory.getTree(parent, child, relationType);
        assertEquals("The last occurrence must win", 5, saved.getTreeOrder());
    }

    /**
     * Method to test: {@link TreeFactory#insertTrees(List)}
     * When: a row with the same {@code (parent, child, relation_type)} primary key already
     * exists in the table — e.g. a relationship row preserved by the permission-scoped delete
     * Should: replace the pre-existing row instead of failing with a primary key violation
     */
    @Test
    public void insertTreesReplacesPreExistingRows() {
        final String parent = newId();
        final String child = newId();
        final String relationType = newRelationType();

        TreeFactory.saveTree(new Tree(parent, child, relationType, 1));
        TreeFactory.insertTrees(List.of(new Tree(parent, child, relationType, 9)));

        final List<String> relatedIds = TreeFactory
                .getRelatedIdsByParentAndRelationType(parent, relationType);
        assertEquals(1, relatedIds.size());

        final Tree saved = TreeFactory.getTree(parent, child, relationType);
        assertEquals("The new row must replace the pre-existing one", 9, saved.getTreeOrder());
    }

    /**
     * Method to test: {@link TreeFactory#deleteTreesByParentAndChildrenAndRelationType(String, List, String)}
     * When: only a subset of the children related to a parent is passed in
     * Should: delete only the rows pointing to the listed children, preserving the rest
     */
    @Test
    public void scopedDeleteByParentPreservesUnlistedChildren() {
        final String parent = newId();
        final String child1 = newId();
        final String child2 = newId();
        final String child3 = newId();
        final String relationType = newRelationType();

        TreeFactory.insertTrees(List.of(
                new Tree(parent, child1, relationType, 1),
                new Tree(parent, child2, relationType, 2),
                new Tree(parent, child3, relationType, 3)));

        TreeFactory.deleteTreesByParentAndChildrenAndRelationType(parent,
                List.of(child1, child3), relationType);

        final List<String> remaining = TreeFactory
                .getRelatedIdsByParentAndRelationType(parent, relationType);
        assertEquals(1, remaining.size());
        assertEquals("The unlisted child must be preserved", child2, remaining.get(0));
    }

    /**
     * Method to test: {@link TreeFactory#deleteTreesByChildAndParentsAndRelationType(String, List, String)}
     * When: only a subset of the parents related to a child is passed in
     * Should: delete only the rows pointing to the listed parents, preserving the rest
     */
    @Test
    public void scopedDeleteByChildPreservesUnlistedParents() {
        final String child = newId();
        final String parent1 = newId();
        final String parent2 = newId();
        final String relationType = newRelationType();

        TreeFactory.insertTrees(List.of(
                new Tree(parent1, child, relationType, 1),
                new Tree(parent2, child, relationType, 2)));

        TreeFactory.deleteTreesByChildAndParentsAndRelationType(child,
                List.of(parent1), relationType);

        final List<String> remaining = TreeFactory
                .getRelatedIdsByChildAndRelationType(child, relationType);
        assertEquals(1, remaining.size());
        assertEquals("The unlisted parent must be preserved", parent2, remaining.get(0));
    }

    /**
     * Method to test: {@link TreeFactory#deleteTreesByParentAndChildrenAndRelationType(String, List, String)}
     * When: the list of identifiers exceeds the internal chunk size (500)
     * Should: delete every listed row across multiple chunked statements
     */
    @Test
    public void scopedDeleteHandlesMoreIdsThanChunkSize() {
        final String parent = newId();
        final String relationType = newRelationType();
        final int total = 501;

        final List<Tree> trees = new ArrayList<>(total);
        final List<String> childIds = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            final String child = newId();
            childIds.add(child);
            trees.add(new Tree(parent, child, relationType, i + 1));
        }
        TreeFactory.insertTrees(trees);
        assertEquals(total, TreeFactory
                .getRelatedIdsByParentAndRelationType(parent, relationType).size());

        TreeFactory.deleteTreesByParentAndChildrenAndRelationType(parent, childIds, relationType);

        assertTrue("All listed rows must be deleted across chunks", TreeFactory
                .getRelatedIdsByParentAndRelationType(parent, relationType).isEmpty());
    }

    /**
     * Method to test: {@link TreeFactory#getRelatedIdsByParentAndRelationType(String, String)}
     * and {@link TreeFactory#getRelatedIdsByChildAndRelationType(String, String)}
     * When: rows are inserted with non-sequential tree order values
     * Should: return the identifiers sorted by tree order, without hydrating contentlets
     */
    @Test
    public void getRelatedIdsReturnsIdentifiersInTreeOrder() {
        final String parent = newId();
        final String childA = newId();
        final String childB = newId();
        final String childC = newId();
        final String relationType = newRelationType();

        TreeFactory.insertTrees(List.of(
                new Tree(parent, childC, relationType, 1),
                new Tree(parent, childA, relationType, 2),
                new Tree(parent, childB, relationType, 3)));

        final List<String> children = TreeFactory
                .getRelatedIdsByParentAndRelationType(parent, relationType);
        assertEquals(List.of(childC, childA, childB), children);

        final List<String> parents = TreeFactory
                .getRelatedIdsByChildAndRelationType(childA, relationType);
        assertEquals(List.of(parent), parents);
    }

    /**
     * Method to test: {@link TreeFactory#getNextTreeOrderByParentAndRelationType(String, String)}
     * When: no rows exist for the parent and relation type, and then a row with tree order 7 is
     * inserted
     * Should: return 1 first, and the highest existing tree order plus one afterwards
     */
    @Test
    public void getNextTreeOrderReturnsHighestOrderPlusOne() {
        final String parent = newId();
        final String relationType = newRelationType();

        assertEquals(1,
                TreeFactory.getNextTreeOrderByParentAndRelationType(parent, relationType));

        TreeFactory.insertTrees(List.of(new Tree(parent, newId(), relationType, 7)));
        assertEquals(8,
                TreeFactory.getNextTreeOrderByParentAndRelationType(parent, relationType));
    }

}
