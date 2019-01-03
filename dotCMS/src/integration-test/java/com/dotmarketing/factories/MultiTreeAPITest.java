package com.dotmarketing.factories;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.startup.runonce.Task04315UpdateMultiTreePK;

public class MultiTreeAPITest extends IntegrationTestBase {
    

    private static final String CONTAINER = "CONTAINER";
    private static final String PAGE = "PAGE";
    private static final String CONTENTLET = "CONTENTLET";
    private static final String RELATION_TYPE = "RELATION_TYPE";

    final static int runs =2;
    final static int contentlets =5;
    @BeforeClass
    public static void initData() throws Exception {
        IntegrationTestInitService.getInstance().init();
      //  testUpgradeTask();
        buildInitalData();
    }
    
    public static void testUpgradeTask() throws Exception {
        Task04315UpdateMultiTreePK task = Task04315UpdateMultiTreePK.class.newInstance();
        task.executeUpgrade();
    }
    
    
    public static void buildInitalData() throws Exception {
        for(int i=0;i<runs;i++) {
            for(int j=0;j<contentlets;j++) {
                MultiTree mt = new MultiTree()
                        .setContainer(CONTAINER +i)
                        .setHtmlPage(PAGE)
                        .setContentlet(CONTENTLET + j)
                        .setTreeOrder(j)
                        .setRelationType(RELATION_TYPE + i);

                APILocator.getMultiTreeAPI().saveMultiTree(mt);
            }
        }

    }
    
    @Test
    public  void testDeletes() throws Exception {
        deleteInitialData();
        buildInitalData() ;
        List<MultiTree> all = APILocator.getMultiTreeAPI().getAllMultiTrees();
        
        List<MultiTree> list = APILocator.getMultiTreeAPI().getMultiTrees(PAGE);

        deleteInitialData();
        assertTrue("multiTree deletes", APILocator.getMultiTreeAPI().getAllMultiTrees().size() < all.size() );
        assertTrue("multiTree deletes", APILocator.getMultiTreeAPI().getAllMultiTrees().size() == all.size() - list.size() );
    }
    
    
    @Test
    public  void testReorder() throws Exception {
        deleteInitialData();
        buildInitalData() ;
        MultiTree tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+0, CONTENTLET +0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==0 );
        APILocator.getMultiTreeAPI().saveMultiTree(tree.setTreeOrder(7));
        tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+ 0, CONTENTLET + 0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==4 );
        APILocator.getMultiTreeAPI().saveMultiTree(tree.setTreeOrder(2));
        List<MultiTree> list = APILocator.getMultiTreeAPI().getMultiTrees(PAGE, CONTAINER+0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", list.get(2).equals(tree));

    }
    
    @Test
    public  void findByChild() throws Exception {
        deleteInitialData();
        buildInitalData() ;
        
        List<MultiTree> list = APILocator.getMultiTreeAPI().getMultiTreesByChild(CONTENTLET + "0");
        
        assertTrue("getByChild returns all results", list.size() == runs );
        
        
        
    }
    
    
    
    @AfterClass
    public static void deleteInitialData() throws Exception {

        List<MultiTree> list = APILocator.getMultiTreeAPI().getMultiTrees(PAGE);

        for(MultiTree tree : list) {
            APILocator.getMultiTreeAPI().deleteMultiTree(tree);
        }

    }
    
    
    
    
    
    @Test
    public  void testSaveMultiTree() throws Exception {
        MultiTree mt = new MultiTree()
                .setContainer(CONTAINER)
                .setHtmlPage(PAGE)
                .setContentlet("NEW_ONE")
                .setTreeOrder(0)
                .setRelationType(RELATION_TYPE + 0);
        
        APILocator.getMultiTreeAPI().saveMultiTree(mt);
        
        MultiTree mt2 = APILocator.getMultiTreeAPI().getMultiTree(mt.getHtmlPage(), mt.getContainer(), mt.getContentlet(), mt.getRelationType());
        assertTrue("multiTree save and get equals", mt.equals(mt2));
    }
    
    
    

    
    
    
    @Test
    public void testLegacyMultiTreeSave() throws Exception {

        
        long time = System.currentTimeMillis();

        
        MultiTree multiTree = new MultiTree();
        multiTree.setParent1( PAGE+time);
        multiTree.setParent2( CONTAINER +time);
        multiTree.setChild( CONTENTLET +time);
        multiTree.setTreeOrder( 1 );
        APILocator.getMultiTreeAPI().saveMultiTree( multiTree );
        
        
        MultiTree mt2 = APILocator.getMultiTreeAPI().getMultiTree(PAGE+time, CONTAINER +time, CONTENTLET +time, Container.LEGACY_RELATION_TYPE);
        
        assertTrue("multiTree save without relationtype and get equals", multiTree.equals(mt2));
    }
    
    
    @Test
    public void testGetMultiTreeIdentifierIdentifierIdentifierString() throws Exception {

        
        
        
    }

    @Test
    public void testGetMultiTreeInode() throws Exception {

    }

    @Test
    public void testMultiTreeForContainerStructure() throws Exception {

        //Search for an existing Container Structure (contentlet)
        final Contentlet contentlet = APILocator.getContentletAPIImpl().findAllContent(0,1).get(0);

        //Create a MultiTree and relate it to that Contentlet
        MultiTree mt = new MultiTree()
                .setContainer(CONTAINER)
                .setHtmlPage(PAGE)
                .setContentlet(contentlet.getIdentifier())
                .setTreeOrder(1)
                .setRelationType(RELATION_TYPE);
        APILocator.getMultiTreeAPI().saveMultiTree( mt );


        //Search multitrees for the Contentlet, verify its not empty
        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getContainerStructureMultiTree(CONTAINER, contentlet.getStructureInode());
        assertNotNull(multiTrees);
        assertFalse(multiTrees.isEmpty());

        //Delete the multitree
        APILocator.getMultiTreeAPI().deleteMultiTree(mt);

        //Search again the relationship should be gone.
        multiTrees = APILocator.getMultiTreeAPI().getContainerStructureMultiTree(CONTAINER, contentlet.getStructureInode());
        assertTrue(multiTrees.isEmpty());

        APILocator.getMultiTreeAPI().deleteMultiTree(mt);
    }

    @Test
    public void testUpdateMultiTree_GivenSomeMultiTreeWithRelationType_ShouldUpdateRelationType() throws Exception {
        final String NEW_RELATION_TYPE = "New Relation Type";
        final String containerId = CONTAINER + "0";
        final String relationType = RELATION_TYPE + "0";

        APILocator.getMultiTreeAPI().updateMultiTree(PAGE, containerId, relationType, NEW_RELATION_TYPE);
        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(PAGE, containerId, relationType);

        assertTrue(multiTrees.isEmpty());

        List<MultiTree> multiTreesNewRelationType = APILocator.getMultiTreeAPI().getMultiTrees(PAGE, containerId, NEW_RELATION_TYPE);

        assertFalse(multiTreesNewRelationType.isEmpty());
        assertTrue(multiTreesNewRelationType.size() == 5);

        for (MultiTree multiTree : multiTreesNewRelationType) {
            assertEquals(NEW_RELATION_TYPE, multiTree.getRelationType());
        }
    }


    @Test
    public void testMultiTreesSave() throws Exception {


        long time = System.currentTimeMillis();

        final String parent1 = PAGE + time;

        MultiTree multiTree1 = new MultiTree();
        multiTree1.setParent1( parent1 );
        multiTree1.setParent2( CONTAINER +time);
        multiTree1.setChild( CONTENTLET +time);
        multiTree1.setRelationType("1");
        multiTree1.setTreeOrder( 1 );

        long time2 = time + 1;

        MultiTree multiTree2 = new MultiTree();
        multiTree2.setParent1( parent1 );
        multiTree2.setParent2( CONTAINER + time2);
        multiTree2.setChild( CONTENTLET + time2);
        multiTree2.setRelationType("1");
        multiTree2.setTreeOrder( 2 );

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list(multiTree1, multiTree2) );

        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(parent1);

        assertEquals(2, multiTrees.size());

        MultiTree mtFromDB1 = APILocator.getMultiTreeAPI().getMultiTree(parent1, multiTree1.getContainer(),
                multiTree1.getContentlet(), multiTree1.getRelationType());
        MultiTree mtFromDB2 = APILocator.getMultiTreeAPI().getMultiTree(parent1, multiTree2.getContainer(),
                multiTree2.getContentlet(), multiTree2.getRelationType());


        assertEquals(multiTree1, mtFromDB1);
        assertEquals(multiTree2, mtFromDB2);
    }

    @Test
    public void testMultiTreesSaveWithEmpty() throws Exception {


        long time = System.currentTimeMillis();

        final String parent1 = PAGE + time;

        MultiTree multiTree1 = new MultiTree();
        multiTree1.setHtmlPage( parent1 );
        multiTree1.setContainer( CONTAINER +time);
        multiTree1.setContentlet( CONTENTLET +time);
        multiTree1.setRelationType("1");
        multiTree1.setTreeOrder( 1 );

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list(multiTree1) );
        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list() );

        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(parent1);

        assertEquals(0, multiTrees.size());
    }

    @Test
    public void testMultiTreesSaveDeleting() throws Exception {


        long time = System.currentTimeMillis();

        final String parent1 = PAGE + time;

        MultiTree multiTree1 = new MultiTree();
        multiTree1.setHtmlPage( parent1 );
        multiTree1.setContainer( CONTAINER +time);
        multiTree1.setContentlet( CONTENTLET +time);
        multiTree1.setRelationType("1");
        multiTree1.setTreeOrder( 1 );

        long time2 = time + 1;

        MultiTree multiTree2 = new MultiTree();
        multiTree2.setParent1( parent1 );
        multiTree2.setParent2( CONTAINER + time2);
        multiTree2.setChild( CONTENTLET + time2);
        multiTree2.setRelationType("1");
        multiTree2.setTreeOrder( 2 );

        long time3 = time + 2;

        MultiTree multiTree3 = new MultiTree();
        multiTree3.setParent1( parent1 );
        multiTree3.setParent2( CONTAINER + time3);
        multiTree3.setChild( CONTENTLET + time3);
        multiTree3.setRelationType("-1");
        multiTree3.setTreeOrder( 3 );

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list(multiTree1, multiTree2, multiTree3) );
        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list(multiTree1) );

        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(parent1);

        assertEquals(2, multiTrees.size());
        assertTrue(multiTrees.contains(multiTree1));
        assertTrue(multiTrees.contains(multiTree3));
    }
}
