package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HandlerUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link HandlerUtil#setMultiTree(String, String, List, String)}
     * Given Scenario: Sends a set of multi tree with specific tree order
     * ExpectedResult: when recover the multi tree the order should be preserve
     *
     * @throws IOException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void setMultiTree_on_diff_orders_should_not_reorder() throws IOException, DotSecurityException, DotDataException {

        final String pageIdentifier = "e65ce6c5-6b03-45f8-b2a8-90ee80d17987";
        final String pageInode = "001";
        final Long pageLanguage = 1l;
        final List<Map<String, Object>> wrapperMultiTree = new ArrayList<>();
        this.populateMultitree(pageIdentifier, wrapperMultiTree);
        final String modUser = "dotcms.org1";
        HandlerUtil.setMultiTree(pageIdentifier, pageInode, pageLanguage, wrapperMultiTree, modUser);

        final List<MultiTree>  multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(pageIdentifier);

        assertNotNull(multiTrees);
        assertEquals(21, multiTrees.size());
        assertEquals(0, multiTrees.get(0).getTreeOrder());
        assertEquals(0, multiTrees.get(1).getTreeOrder());
        assertEquals(1, multiTrees.get(2).getTreeOrder());
        assertEquals(2, multiTrees.get(3).getTreeOrder());
        assertEquals(3, multiTrees.get(4).getTreeOrder());
        assertEquals(3, multiTrees.get(5).getTreeOrder());
        assertEquals(4, multiTrees.get(6).getTreeOrder());
        assertEquals(5, multiTrees.get(7).getTreeOrder());
        assertEquals(5, multiTrees.get(8).getTreeOrder());
        assertEquals(6, multiTrees.get(9).getTreeOrder());
        assertEquals(6, multiTrees.get(10).getTreeOrder());
        assertEquals(7, multiTrees.get(11).getTreeOrder());
        assertEquals(7, multiTrees.get(12).getTreeOrder());
        assertEquals(8, multiTrees.get(13).getTreeOrder());
        assertEquals(9, multiTrees.get(14).getTreeOrder());
        assertEquals(10, multiTrees.get(15).getTreeOrder());
        assertEquals(10, multiTrees.get(16).getTreeOrder());
        assertEquals(11, multiTrees.get(17).getTreeOrder());
        assertEquals(12, multiTrees.get(18).getTreeOrder());
        assertEquals(13, multiTrees.get(19).getTreeOrder());
        assertEquals(14, multiTrees.get(20).getTreeOrder());


    }

    private void populateMultitree(String pageIdentifier, List<Map<String, Object>> wrapperMultiTree) {
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "3b538995-b85c-4de6-9c63-cf44acca89d2",
                "relation_type", "1617389333051", "tree_order", 0));  // 1
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "d469e5bf-209c-4a80-99ab-fe92510a87c2",
                "relation_type", "1617389333051", "tree_order", 0));  // 2
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "ded6029f-e7af-4acc-b57f-bae252407d30",
                "relation_type", "1617389333051", "tree_order", 1));  // 3
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "469c77fe-ac29-4e90-932a-2668f455ae0a",
                "relation_type", "1617389333051", "tree_order", 2)); // 4
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "cfe9c5d2-3518-4e56-bd5b-78cd4af9c70c",
                "relation_type", "1617389333051", "tree_order", 3)); // 5
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "51104ac9-7b67-48aa-8ffd-0e2b8efb68d7",
                "relation_type", "1617389333051", "tree_order", 3)); // 6
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "2fdd4348-1237-4532-9dd3-6f1358502bfd",
                "relation_type", "1617389333051", "tree_order", 4)); // 7
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "1ea68733-a725-4c13-84d1-49b01cf08de7",
                "relation_type", "1617389333051", "tree_order", 5)); // 8
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "cc433dcc-35af-4da3-8fc6-54aeb413e292",
                "relation_type", "1617389333051", "tree_order", 5)); // 9
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "696fb981-0e19-41c0-9a90-209437127771",
                "relation_type", "1617389333051", "tree_order", 6)); // 10
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "cfad3ddd-5812-419f-af96-27532f2b619e",
                "relation_type", "1617389333051", "tree_order", 6)); // 11
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "3f6ced25-3205-4202-944a-422e176d57a5",
                "relation_type", "1617389333051", "tree_order", 7)); // 12
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "ef12756b-c875-4b3f-94a9-25b92353ee95",
                "relation_type", "1617389333051", "tree_order", 7)); // 13
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "7c0a1775-dff2-4221-88df-18b0dd010746",
                "relation_type", "1617389333051", "tree_order", 8   )); // 14
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "f4089d2d-fc92-44bb-b9cf-c6bb92176cd8",
                "relation_type", "1617389333051", "tree_order", 9)); // 15
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "0c2b2cac-ac3c-4d7d-90f3-1abb70aaec04",
                "relation_type", "1617389333051", "tree_order", 10)); // 16
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "81b132ec-b48d-4838-82da-656acd29d176",
                "relation_type", "1617389333051", "tree_order", 10)); // 17
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "b85fda79-12b6-4046-96aa-ba9b6acebd93",
                "relation_type", "1617389333051", "tree_order", 11)); // 18
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "304993aa-2a66-4931-afe8-797bf3f7c0dd",
                "relation_type", "1617389333051", "tree_order", 12)); // 19
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "1020cfa0-9f80-42d7-8baa-2e2e17733e45",
                "relation_type", "1617389333051", "tree_order", 13)); // 20
        wrapperMultiTree.add(Map.of("parent1", pageIdentifier,
                "parent2", "77907283-a246-4688-bc93-033b5d683057",
                "child", "f863ff25-2ee0-43de-aeba-faa7d74edeae",
                "relation_type", "1617389333051", "tree_order", 14)); // 21
    }
}
