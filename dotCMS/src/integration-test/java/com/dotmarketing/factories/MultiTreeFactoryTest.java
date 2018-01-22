package com.dotmarketing.factories;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.startup.runonce.Task04315UpdateMultiTreePK;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiTreeFactoryTest extends IntegrationTestBase {
    

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

                MultiTreeFactory.saveMultiTree(mt);
            }
        }

    }
    
    @Test
    public  void testDeletes() throws Exception {
        deleteInitialData();
        buildInitalData() ;
        List<MultiTree> all = MultiTreeFactory.getAllMultiTrees();
        
        List<MultiTree> list = MultiTreeFactory.getMultiTrees(PAGE);

        deleteInitialData();
        assertTrue("multiTree deletes", MultiTreeFactory.getAllMultiTrees().size() < all.size() );
        assertTrue("multiTree deletes", MultiTreeFactory.getAllMultiTrees().size() == all.size() - list.size() );
    }
    
    
    @Test
    public  void testReorder() throws Exception {
        deleteInitialData();
        buildInitalData() ;
        MultiTree tree = MultiTreeFactory.getMultiTree(PAGE, CONTAINER+0, CONTENTLET +0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==0 );
        MultiTreeFactory.saveMultiTree(tree.setTreeOrder(7));
        tree = MultiTreeFactory.getMultiTree(PAGE, CONTAINER+ 0, CONTENTLET + 0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==4 );
        MultiTreeFactory.saveMultiTree(tree.setTreeOrder(2));
        List<MultiTree> list = MultiTreeFactory.getMultiTrees(PAGE, CONTAINER+0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", list.get(2).equals(tree));

    }
    
    @Test
    public  void findByChild() throws Exception {
        deleteInitialData();
        buildInitalData() ;
        
        List<MultiTree> list = MultiTreeFactory.getMultiTreesByChild(CONTENTLET + "0");
        
        assertTrue("getByChild returns all results", list.size() == runs );
        
        
        
    }
    
    
    
    @AfterClass
    public static void deleteInitialData() throws Exception {

        List<MultiTree> list = MultiTreeFactory.getMultiTrees(PAGE);

        for(MultiTree tree : list) {
            MultiTreeFactory.deleteMultiTree(tree);
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
        
        MultiTreeFactory.saveMultiTree(mt);
        
        MultiTree mt2 = MultiTreeFactory.getMultiTree(mt.getHtmlPage(), mt.getContainer(), mt.getContentlet(), mt.getRelationType());
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
        MultiTreeFactory.saveMultiTree( multiTree );
        
        
        MultiTree mt2 = MultiTreeFactory.getMultiTree(PAGE+time, CONTAINER +time, CONTENTLET +time, Container.LEGACY_RELATION_TYPE);
        
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
        APILocator.getContainerAPI().findAllContainers(null, false);
        MultiTree mt = new MultiTree()
                .setContainer(CONTAINER)
                .setHtmlPage(PAGE)
                .setContentlet(contentlet.getIdentifier())
                .setTreeOrder(1)
                .setRelationType(RELATION_TYPE);
        MultiTreeFactory.saveMultiTree( mt );


        //Search multitrees for the Contentlet, verify its not empty
        List<MultiTree> multiTrees = MultiTreeFactory.getContainerStructureMultiTree(CONTAINER, contentlet.getStructureInode());
        assertNotNull(multiTrees);
        assertFalse(multiTrees.isEmpty());

        //Delete the multitree
        MultiTreeFactory.deleteMultiTree(mt);

        //Search again the relationship should be gone.
        multiTrees = MultiTreeFactory.getContainerStructureMultiTree(CONTAINER, contentlet.getStructureInode());
        assertTrue(multiTrees.isEmpty());

        MultiTreeFactory.deleteMultiTree(mt);
    }
}
