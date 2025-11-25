package com.dotmarketing.factories;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.transform.TransformerLocator;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.LayoutChanges;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.startup.runonce.Task04315UpdateMultiTreePK;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.junit.*;

import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.beans.MultiTree.DOT_PERSONALIZATION_DEFAULT;
import static org.junit.Assert.*;

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
        Task04315UpdateMultiTreePK task = Task04315UpdateMultiTreePK.class.getDeclaredConstructor().newInstance();
        task.executeUpgrade();
    }


    public static void buildInitalData() throws Exception {
        buildInitalData(VariantAPI.DEFAULT_VARIANT.name());

    }

    public static void buildInitalData(final String variantName) throws Exception {
        for(int i=0;i<runs;i++) {
            for(int j=0;j<contentlets;j++) {
                MultiTree mt = new MultiTree()
                        .setContainer(CONTAINER +i)
                        .setHtmlPage(PAGE)
                        .setContentlet(CONTENTLET + j)
                        .setTreeOrder(j)
                        .setInstanceId(RELATION_TYPE + i)
                        .setVariantId(variantName);

                APILocator.getMultiTreeAPI().saveMultiTree(mt);
            }
        }

    }

    /**
     * This test checks if the uuid does exact match on the table (same keys)
     * @throws Exception
     */
    @Test
    public  void testDoesPageContentsHaveContainer_no_prefix() throws Exception {

        final Table<String, String, Set<PersonalizedContentlet>> pageContents = HashBasedTable.create();
        final String  containerIdentifier = "12345";
        final String  containeruuid       = "xxx";
        final ContainerUUID containerUUID = new ContainerUUID(containerIdentifier, containeruuid);
        final Container container         = new Container();
        final MultiTreeAPIImpl multiTreeAPI = new MultiTreeAPIImpl();

        container.setIdentifier(containerIdentifier);
        pageContents.put(containerIdentifier, containeruuid, Sets.newConcurrentHashSet());

        Assert.assertTrue("Should has the container", multiTreeAPI.doesPageContentsHaveContainer(pageContents, containerUUID, container));
    }

    /**
     * This test checks if the uuid does match on the table but the table has a diff keys (with a dotParser_ prefix)
     * @throws Exception
     */
    @Test
    public  void testDoesPageContentsHaveContainer_with_prefix() throws Exception {

        final Table<String, String, Set<PersonalizedContentlet>> pageContents = HashBasedTable.create();
        final String  containerIdentifier = "12345";
        final String  containeruuid       = "xxx";
        final ContainerUUID containerUUID = new ContainerUUID(containerIdentifier, containeruuid);
        final Container container         = new Container();
        final MultiTreeAPIImpl multiTreeAPI = new MultiTreeAPIImpl();

        container.setIdentifier(containerIdentifier);
        pageContents.put(containerIdentifier, ParseContainer.PARSE_CONTAINER_UUID_PREFIX + containeruuid, Sets.newConcurrentHashSet());

        Assert.assertTrue("Should has the container", multiTreeAPI.doesPageContentsHaveContainer(pageContents, containerUUID, container));
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
        buildInitalData();

        MultiTree tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+0, CONTENTLET +0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==0 );
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(tree.setTreeOrder(7));
        tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+ 0, CONTENTLET + 0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==4 );
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(tree.setTreeOrder(2));

        List<MultiTree> list = APILocator.getMultiTreeAPI().getMultiTrees(PAGE, CONTAINER+0, RELATION_TYPE+0);

        assertTrue("multiTree reorders", list.get(0).getContentlet().equals("CONTENTLET1"));
        assertTrue("multiTree reorders", list.get(1).getContentlet().equals("CONTENTLET2"));
        assertTrue("multiTree reorders", list.get(2).equals(tree));
        assertTrue("multiTree reorders", list.get(3).getContentlet().equals("CONTENTLET3"));
        assertTrue("multiTree reorders", list.get(4).getContentlet().equals("CONTENTLET4"));
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#saveMultiTreeAndReorder(MultiTree)}
     * When: A {@link MultiTree} is saved with not the last treeorder expected with a specific {@link Variant}
     * Should: Save it and reorder all the {@link MultiTree} that already exists to this {@link Variant}
     * @throws Exception
     */
    @Test
    public  void testReorderWithVariant() throws Exception {
        final Variant variant = new VariantDataGen().nextPersisted();

        deleteInitialData();
        buildInitalData();
        buildInitalData(variant.name());

        MultiTree tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+0, CONTENTLET +0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==0 );
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(tree.setTreeOrder(7));
        tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+ 0, CONTENTLET + 0, RELATION_TYPE+0);
        assertTrue("multiTree reorders", tree.getTreeOrder()==4 );
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(tree.setTreeOrder(2));

        final ArrayList arrayList = new DotConnect()
                .setSQL("select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? and variant_id = 'DEFAULT' order by tree_order")
                .addParam(PAGE)
                .addParam(CONTAINER+0)
                .addParam(RELATION_TYPE+0)
                .loadResults();
        assertTrue("multiTree reorders", ((Map) arrayList.get(0)).get("child").equals("CONTENTLET1"));
        assertTrue("multiTree reorders", ((Map) arrayList.get(1)).get("child").equals("CONTENTLET2"));
        assertTrue("multiTree reorders", ((Map) arrayList.get(2)).get("child").equals("CONTENTLET0"));
        assertTrue("multiTree reorders", ((Map) arrayList.get(3)).get("child").equals("CONTENTLET3"));
        assertTrue("multiTree reorders", ((Map) arrayList.get(4)).get("child").equals("CONTENTLET4"));

        final ArrayList arrayList_2 = new DotConnect()
                .setSQL("select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? and variant_id = '" + variant.name() + "' order by tree_order")
                .addParam(PAGE)
                .addParam(CONTAINER+0)
                .addParam(RELATION_TYPE+0)
                .loadResults();

        assertTrue("multiTree reorders", ((Map) arrayList_2.get(0)).get("child").equals("CONTENTLET0"));
        assertTrue("multiTree reorders", ((Map) arrayList_2.get(1)).get("child").equals("CONTENTLET1"));
        assertTrue("multiTree reorders", ((Map) arrayList_2.get(2)).get("child").equals("CONTENTLET2"));
        assertTrue("multiTree reorders", ((Map) arrayList_2.get(3)).get("child").equals("CONTENTLET3"));
        assertTrue("multiTree reorders", ((Map) arrayList_2.get(4)).get("child").equals("CONTENTLET4"));

        tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+0, CONTENTLET +0, RELATION_TYPE+0, DOT_PERSONALIZATION_DEFAULT, variant.name());
        assertTrue("multiTree reorders", tree.getTreeOrder()==0 );
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(tree.setTreeOrder(7));
        tree = APILocator.getMultiTreeAPI().getMultiTree(PAGE, CONTAINER+ 0, CONTENTLET + 0, RELATION_TYPE+0, DOT_PERSONALIZATION_DEFAULT, variant.name());
        assertTrue("multiTree reorders", tree.getTreeOrder()==4 );
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(tree.setTreeOrder(2));

        final ArrayList arrayList_3 = new DotConnect()
                .setSQL("select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? and variant_id = 'DEFAULT' order by tree_order")
                .addParam(PAGE)
                .addParam(CONTAINER+0)
                .addParam(RELATION_TYPE+0)
                .loadResults();
        assertTrue("multiTree reorders", ((Map) arrayList_3.get(0)).get("child").equals("CONTENTLET1"));
        assertTrue("multiTree reorders", ((Map) arrayList_3.get(1)).get("child").equals("CONTENTLET2"));
        assertTrue("multiTree reorders", ((Map) arrayList_3.get(2)).get("child").equals("CONTENTLET0"));
        assertTrue("multiTree reorders", ((Map) arrayList_3.get(3)).get("child").equals("CONTENTLET3"));
        assertTrue("multiTree reorders", ((Map) arrayList_3.get(4)).get("child").equals("CONTENTLET4"));
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
                .setInstanceId(RELATION_TYPE + 0);
        
        APILocator.getMultiTreeAPI().saveMultiTree(mt);
        
        MultiTree mt2 = APILocator.getMultiTreeAPI().getMultiTree(mt.getHtmlPage(), mt.getContainer(), mt.getContentlet(), mt.getRelationType());
        assertTrue("multiTree save and get equals", mt.equals(mt2));
    }
    
    
    

    
    
    
    @Test
    public void testLegacyMultiTreeSave() throws Exception {

        
        long time = System.currentTimeMillis();

        
        MultiTree multiTree = new MultiTree();
        multiTree.setHtmlPage( PAGE+time);
        multiTree.setContainer( CONTAINER +time);
        multiTree.setContentlet( CONTENTLET +time);
        multiTree.setTreeOrder( 1 );
        APILocator.getMultiTreeAPI().saveMultiTree( multiTree );
        
        
        MultiTree mt2 = APILocator.getMultiTreeAPI().getMultiTree(PAGE+time, CONTAINER +time, CONTENTLET +time, Container.LEGACY_RELATION_TYPE);
        
        assertTrue("multiTree save without relationtype and get equals", multiTree.equals(mt2));
    }
    
    
    @Test
    public void testGetPageMultiTrees() throws Exception {
        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode()).nextPersisted();

        MultiTree multiTree = new MultiTree();
        multiTree.setHtmlPage(page);
        multiTree.setContainer(container);
        multiTree.setContentlet(content);
        multiTree.setInstanceId("abc");
        multiTree.setPersonalization(DOT_PERSONALIZATION_DEFAULT);
        multiTree.setTreeOrder( 1 );
        
        //delete out any previous relation
        APILocator.getMultiTreeAPI().deleteMultiTree(multiTree);
        CacheLocator.getMultiTreeCache().clearCache();
        Table<String, String, Set<PersonalizedContentlet>> trees= APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);
        
        Table<String, String, Set<PersonalizedContentlet>> cachedTrees= APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

        Logger.info(this, "\n\n**** cachedTrees: " + cachedTrees);
        // should be the same object coming from in memory cache
        assert(trees==cachedTrees);

        CacheLocator.getMultiTreeCache().removePageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name());

        trees= APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);
        
        // cache flush forced a cache reload, so different objects in memory
        assert(trees!=cachedTrees);
        
        // but the objects should contain the same data
        assert(trees.equals(cachedTrees));

        // there is no container entry 
        assert(!(cachedTrees.rowKeySet().contains(container.getIdentifier())));


        // check cache flush on save
        APILocator.getMultiTreeAPI().saveMultiTree( multiTree );
        Table<String, String, Set<PersonalizedContentlet>> addedTrees= APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);
        assert(cachedTrees!=addedTrees);
        
        // did we get a new object from the cache?
        Assert.assertNotNull(cachedTrees);
        Assert.assertNotNull(addedTrees);
        Logger.info(this, "\n\n**** cachedTrees: " + cachedTrees);
        Logger.info(this, "\n\n**** addedTrees: " + addedTrees);
        assertNotEquals(cachedTrees, addedTrees);
        assert(addedTrees.rowKeySet().contains(container.getIdentifier()));
        
        // check cache flush on delete
        APILocator.getMultiTreeAPI().deleteMultiTree(multiTree );
        Table<String, String, Set<PersonalizedContentlet>> deletedTrees= APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);
        
        // did we get a new object from the cache?
        assert(!(addedTrees.equals(deletedTrees)));
        assert(!(deletedTrees.rowKeySet().contains(container.getIdentifier())));
    }

    
    
  /**
   * This test makes sure that if you have a container that accepts 1 contentlet, then that container
   * will take 1 contentlet for each persona and not just 1 contentlet in total.  See:
   * https://github.com/dotCMS/core/issues/17181
   * 
   * @throws Exception
   */
  @Test
  public void test_personalize_page_respects_max_contentlet_value_per_persona() throws Exception {

    final Template template = new TemplateDataGen().body("body").nextPersisted();
    final Folder folder = new FolderDataGen().nextPersisted();
    final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
    final Structure structure = new StructureDataGen().nextPersisted();
    final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();
    final Contentlet content1 = new ContentletDataGen(structure.getInode()).nextPersisted();
    final Contentlet content2 = new ContentletDataGen(structure.getInode()).nextPersisted();

    final Persona persona = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
    final String uniqueId = UUIDGenerator.shorty();

    MultiTree multiTree = new MultiTree();
    multiTree.setHtmlPage(page);
    multiTree.setContainer(container);
    multiTree.setContentlet(content1);
    multiTree.setInstanceId(uniqueId);
    multiTree.setPersonalization(DOT_PERSONALIZATION_DEFAULT);
    multiTree.setTreeOrder(1);
    APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

    multiTree = new MultiTree();
    multiTree.setHtmlPage(page);
    multiTree.setContainer(container);
    multiTree.setContentlet(content2);
    multiTree.setInstanceId(uniqueId);
    multiTree.setPersonalization(persona.getKeyTag());
    multiTree.setTreeOrder(1);
    APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

    Table<String, String, Set<PersonalizedContentlet>> pageContents = APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

    for (final String containerId : pageContents.rowKeySet()) {
      assertEquals("containers match. Saved:" + container.getIdentifier() + ", got:" + containerId, containerId, container.getIdentifier());

      for (final String uuid : pageContents.row(containerId).keySet()) {
        assertEquals("containers uuids match. Saved:" + uniqueId + ", got:" + uuid, uniqueId, uuid);
        final Set<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);

        assertTrue("container should have 2 personalized contents - got :" + personalizedContentletSet.size(),
            personalizedContentletSet.size() == 2);
        assertTrue("container should have contentlet for keyTag:" + DOT_PERSONALIZATION_DEFAULT, personalizedContentletSet
            .contains(new PersonalizedContentlet(content1.getIdentifier(), DOT_PERSONALIZATION_DEFAULT)));
        assertTrue("container should have contentlet for persona:" + persona.getKeyTag(),
            personalizedContentletSet.contains(new PersonalizedContentlet(content2.getIdentifier(), persona.getKeyTag())));
      }

    }

  }

    @Test
    public void testMultiTreeForContainerStructure() throws Exception {

        //THIS USES THE INDEX WHICH IS SOMETIMES NOT THERE LOCALLY
        //final Contentlet contentlet = APILocator.getContentletAPIImpl().findAllContent(0,1).get(0);
        
        Map<String, Object> map = new DotConnect().setSQL("select * from contentlet_version_info").setMaxRows(1).loadObjectResults().get(0);
        final Contentlet contentlet = APILocator.getContentletAPIImpl().find(map.get("working_inode").toString(), APILocator.systemUser(), false);
        
        

        //Create a MultiTree and relate it to that Contentlet
        MultiTree mt = new MultiTree()
                .setContainer(CONTAINER)
                .setHtmlPage(PAGE)
                .setContentlet(contentlet.getIdentifier())
                .setTreeOrder(1)
                .setInstanceId(RELATION_TYPE);
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
    public void testMultiTreesSave() throws Exception {


        long time = System.currentTimeMillis();

        final String parent1 = PAGE + time;

        MultiTree multiTree1 = new MultiTree()
        .setHtmlPage(parent1 )
        .setContainer( CONTAINER +time)
        .setContentlet( CONTENTLET +time)
        .setInstanceId("1")
        .setTreeOrder( 1 );

        long time2 = time + 1;

        MultiTree multiTree2 = new MultiTree()
        .setHtmlPage( parent1 )
        .setContainer( CONTAINER + time2)
        .setContentlet( CONTENTLET + time2)
        .setInstanceId("1")
        .setTreeOrder( 2 );

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, VariantAPI.DEFAULT_VARIANT.name(),
                list(multiTree1, multiTree2) );

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
        multiTree1.setInstanceId("1");
        multiTree1.setTreeOrder( 1 );

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, VariantAPI.DEFAULT_VARIANT.name(), list(multiTree1) );
        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, VariantAPI.DEFAULT_VARIANT.name(), list() );

        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(parent1);

        assertEquals(0, multiTrees.size());
    }

    /**
     * Method to test: {@link MultiTreeAPI#copyPersonalizationForPage(String, String, String)}
     * When: A Page has Personalizations for the DEFAULT Variant and default Persona
     * and try to copy the Personalizations to the Default Variant but another Persona
     * Should: Copy from the DEFAULT VARIANT and default Persona to the Specific Persona and Default Variant
     */
    @Test
    public void testGetMultiTreesByPersonalizedPage() throws Exception {

        final MultiTreeAPI multiTreeAPI = new MultiTreeAPIImpl();
        final String htmlPage           = UUIDGenerator.generateUuid();
        final String container          = UUIDGenerator.generateUuid();
        final String content1           = UUIDGenerator.generateUuid();
        final String content2           = UUIDGenerator.generateUuid();
        final String personalization    = "dot:somepersona";
        final String newPersonalization = "dot:newpersona";

        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content1, UUIDGenerator.generateUuid(), 1)); // dot:default
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content2, UUIDGenerator.generateUuid(), 1)); // dot:default
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content1, UUIDGenerator.generateUuid(), 2, personalization)); // dot:somepersona

        List<MultiTree> multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage,
                DOT_PERSONALIZATION_DEFAULT, VariantAPI.DEFAULT_VARIANT.name());

        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());

        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, personalization,
                VariantAPI.DEFAULT_VARIANT.name());

        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(1, multiTrees.size());

        multiTrees = multiTreeAPI.copyPersonalizationForPage(htmlPage, DOT_PERSONALIZATION_DEFAULT,
                newPersonalization, VariantAPI.DEFAULT_VARIANT.name());
        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());
        org.junit.Assert.assertEquals(newPersonalization, multiTrees.get(0).getPersonalization());
        org.junit.Assert.assertEquals(newPersonalization, multiTrees.get(1).getPersonalization());

        multiTreeAPI.deletePersonalizationForPage(htmlPage, newPersonalization, VariantAPI.DEFAULT_VARIANT.name());
        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, newPersonalization,
                VariantAPI.DEFAULT_VARIANT.name());

        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(0, multiTrees.size());
    }

    @Test
    public void testMultiTreesSaveAndPersonalizationForPage() throws Exception {

        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode()).nextPersisted();

        
        
        
        final MultiTreeAPI multiTreeAPI = new MultiTreeAPIImpl();

        final String personalization    = "dot:persona:somepersona";

        multiTreeAPI.saveMultiTree(new MultiTree(page.getIdentifier(), container.getIdentifier(), content.getIdentifier(), UUIDGenerator.generateUuid(), 1)); // dot:default
        multiTreeAPI.saveMultiTree(new MultiTree(page.getIdentifier(), container.getIdentifier(), content.getIdentifier(), UUIDGenerator.generateUuid(), 2, personalization)); // dot:somepersona

        final Set<String> personalizationSet = multiTreeAPI.getPersonalizationsForPage(page);

        org.junit.Assert.assertNotNull(personalizationSet);
        org.junit.Assert.assertEquals(2, personalizationSet.size());
        org.junit.Assert.assertTrue(personalizationSet.contains(DOT_PERSONALIZATION_DEFAULT));
        org.junit.Assert.assertTrue(personalizationSet.contains(personalization));

        final Set<String> allPersonalizationSet = multiTreeAPI.getPersonalizations();

        org.junit.Assert.assertNotNull(allPersonalizationSet);
        org.junit.Assert.assertTrue(allPersonalizationSet.size() >= 2);
        org.junit.Assert.assertTrue(allPersonalizationSet.contains(DOT_PERSONALIZATION_DEFAULT));
        org.junit.Assert.assertTrue(allPersonalizationSet.contains(personalization));
    }

    @Test
    public void testCleanUpUnusedPersonalization() throws Exception {

        this.testMultiTreesSaveAndPersonalizationForPage();
        final MultiTreeAPI multiTreeAPI = new MultiTreeAPIImpl();
        multiTreeAPI.cleanUpUnusedPersonalization(personalization -> personalization.startsWith("dot:persona:"));
        final Set<String> allPersonalizationSet = multiTreeAPI.getPersonalizations();

        org.junit.Assert.assertNotNull(allPersonalizationSet);
        org.junit.Assert.assertTrue(allPersonalizationSet.stream().noneMatch(personalization-> personalization.startsWith("dot:persona:")));
        org.junit.Assert.assertTrue(allPersonalizationSet.contains(DOT_PERSONALIZATION_DEFAULT));
    }


    @Test
    public void testMultiTreesSavingWithPersonalizationForPage() throws Exception {

        final MultiTreeAPI multiTreeAPI = new MultiTreeAPIImpl();
        final String htmlPage           = UUIDGenerator.generateUuid();
        final String container          = UUIDGenerator.generateUuid();
        final String content            = UUIDGenerator.generateUuid();
        final String instance1          = UUIDGenerator.generateUuid();
        final String instance2          = UUIDGenerator.generateUuid();
        final String instance3          = UUIDGenerator.generateUuid();
        final String instance4          = UUIDGenerator.generateUuid();
        final String personalization1   = "dot:persona:one";
        final String personalization2   = "dot:persona:two";

        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content, instance1, 1, personalization1)); // dot:persona:one
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content, instance2, 2, personalization1)); // dot:persona:one
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content, instance3, 3, personalization2)); // dot:persona:two
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content, instance4, 4, personalization2)); // dot:persona:two

        final MultiTree multiTree1 = multiTreeAPI.getMultiTree(htmlPage, container, content, instance1, personalization1);
        final MultiTree multiTree2 = multiTreeAPI.getMultiTree(htmlPage, container, content, instance2, personalization1);
        final MultiTree multiTree3 = multiTreeAPI.getMultiTree(htmlPage, container, content, instance3, personalization2);
        final MultiTree multiTree4 = multiTreeAPI.getMultiTree(htmlPage, container, content, instance4, personalization2);


        org.junit.Assert.assertNotNull(multiTree1);
        org.junit.Assert.assertNotNull(multiTree2);
        org.junit.Assert.assertNotNull(multiTree3);
        org.junit.Assert.assertNotNull(multiTree4);

        org.junit.Assert.assertEquals(personalization1, multiTree1.getPersonalization());
        org.junit.Assert.assertEquals(personalization1, multiTree2.getPersonalization());
        org.junit.Assert.assertEquals(personalization2, multiTree3.getPersonalization());
        org.junit.Assert.assertEquals(personalization2, multiTree4.getPersonalization());
    }

    @Test
    public void testMultiTreesSaveDeleting() throws Exception {


        long time = System.currentTimeMillis();

        final String parent1 = PAGE + time;

        MultiTree multiTree1 = new MultiTree();
        multiTree1.setHtmlPage( parent1 );
        multiTree1.setContainer( CONTAINER +time);
        multiTree1.setContentlet( CONTENTLET +time);
        multiTree1.setInstanceId("1");
        multiTree1.setTreeOrder( 1 );

        long time2 = time + 1;

        MultiTree multiTree2 = new MultiTree();
        multiTree2.setHtmlPage( parent1 );
        multiTree2.setContainer( CONTAINER + time2);
        multiTree2.setContentlet( CONTENTLET + time2);
        multiTree2.setInstanceId("1");
        multiTree2.setTreeOrder( 2 );

        long time3 = time + 2;

        MultiTree multiTree3 = new MultiTree();
        multiTree3.setHtmlPage( parent1 );
        multiTree3.setContainer( CONTAINER + time3);
        multiTree3.setContentlet( CONTENTLET + time3);
        multiTree3.setInstanceId("-1");
        multiTree3.setTreeOrder( 3 );

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, VariantAPI.DEFAULT_VARIANT.name(), list(multiTree1, multiTree2, multiTree3) );
        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, VariantAPI.DEFAULT_VARIANT.name(), list(multiTree1) );

        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(parent1);

        assertEquals(2, multiTrees.size());
        assertTrue(multiTrees.contains(multiTree1));
        assertTrue(multiTrees.contains(multiTree3));
    }

    /**
     * Method to Test: {@link MultiTreeAPI#overridesMultitreesByPersonalization(String, String, List, Optional)} )}
     * When: A Page with content in spanish and english, is trying to add the spanish content again into the same container, but
     * it's editing the english version of the page
     * Should: Throw an exception saying that the content already exists in that container.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_overridesMultitreesByPersonalization_AddContentTwiceDiffLangEditing_throwException() throws Exception {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language espLanguage = new LanguageDataGen().country("ESP").languageCode("esp").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet enContentlet = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet espContentlet = new ContentletDataGen(contentType.id())
                .languageId(espLanguage.getId())
                .nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final String uniqueId = UUIDGenerator.shorty();

        final MultiTree multiTreeContentEN =new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(enContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        final MultiTree multiTreeContentES = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(espContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .nextPersisted();


        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                DOT_PERSONALIZATION_DEFAULT,
                list(multiTreeContentEN,multiTreeContentES),
                Optional.of(defaultLanguage.getId())
        );
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#saveMultiTree(MultiTree)}
     * When: try to save two {@link MultiTree} with different {@link Variant}
     * Should: save both
     * @throws DotDataException
     */
    @Test
    public void saveUpdateMultiTree() throws DotDataException {

        final Variant variantA = new VariantDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet defaultContentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final MultiTree multiTree = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(defaultContentlet)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                .getMultiTrees(page.getIdentifier());

        assertEquals(1, multiTrees.size());
        assertEquals(page.getIdentifier(), multiTrees.get(0).getHtmlPage());
        assertEquals(container.getIdentifier(), multiTrees.get(0).getContainer());
        assertEquals(defaultContentlet.getIdentifier(), multiTrees.get(0).getContentlet());
        assertEquals(DOT_PERSONALIZATION_DEFAULT, multiTrees.get(0).getPersonalization());
        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), multiTrees.get(0).getVariantId());

        final MultiTree multiTree2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(defaultContentlet)
                .setVariant(variantA)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().saveMultiTree(multiTree2);

        final List<MultiTree> multiTrees_2 = getMultiTrees(page.getIdentifier());

        assertEquals(2, multiTrees_2.size());
        assertEquals(page.getIdentifier(), multiTrees_2.get(0).getHtmlPage());
        assertEquals(container.getIdentifier(), multiTrees_2.get(0).getContainer());
        assertEquals(defaultContentlet.getIdentifier(), multiTrees_2.get(0).getContentlet());
        assertEquals(DOT_PERSONALIZATION_DEFAULT, multiTrees_2.get(0).getPersonalization());
        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), multiTrees_2.get(0).getVariantId());

        assertEquals(page.getIdentifier(), multiTrees_2.get(1).getHtmlPage());
        assertEquals(container.getIdentifier(), multiTrees_2.get(1).getContainer());
        assertEquals(defaultContentlet.getIdentifier(), multiTrees_2.get(1).getContentlet());
        assertEquals(DOT_PERSONALIZATION_DEFAULT, multiTrees_2.get(1).getPersonalization());
        assertEquals(variantA.name(), multiTrees_2.get(1).getVariantId());
    }


    private List<MultiTree> getMultiTrees(final String pageId) throws DotDataException {
        final ArrayList arrayList = new DotConnect()
                .setSQL("select * from multi_tree where parent1 = ? order by tree_order")
                .addParam(pageId)
                .loadResults();
        return TransformerLocator.createMultiTreeTransformer(arrayList).asList();
    }

    /**
     * Method to Test: {@link MultiTreeAPI#overridesMultitreesByPersonalization(String, String, List, Optional, String)}
     * When: A Page with content in a specific variants and personalization try to update the MulTree just for the one specific variant and personalization
     * Should: Should keep the DEFAULT variants and DEFAULT persona versions
     */
    @Test
    public void shouldReplaceVariantAndPersonalizationMultiTree() throws Exception {
        final Variant variantA = new VariantDataGen().nextPersisted();
        final Persona persona = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet defaultContentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final Contentlet variantContentlet_1 = new ContentletDataGen(contentType.id())
                .variant(variantA)
                .nextPersisted();

        final Contentlet variantContentlet_2 = new ContentletDataGen(contentType.id())
                .variant(variantA)
                .nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final String uniqueId = UUIDGenerator.shorty();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(defaultContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(defaultContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(persona.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(variantContentlet_1)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(variantContentlet_1)
                .setInstanceID(uniqueId)
                .setPersonalization(persona.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(variantContentlet_2)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(variantContentlet_2)
                .setInstanceID(uniqueId)
                .setPersonalization(persona.getKeyTag())
                .setTreeOrder(2)
                .setVariant(variantA)
                .nextPersisted();

        final Contentlet newEnContentlet = new ContentletDataGen(contentType.id())
                .variant(variantA)
                .nextPersisted();

        final MultiTree newMultiTreeEnContent = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(newEnContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(persona.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variantA)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                persona.getKeyTag(),
                list(newMultiTreeEnContent),
                Optional.of(defaultLanguage.getId()),
                variantA.name()
        );


        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTreesByVariant(page.getIdentifier(), variantA.name());

        assertEquals(4, multiTrees.size());

        for (MultiTree multiTree : multiTrees) {
            if (multiTree.getContentlet().equals(defaultContentlet.getIdentifier())) {
                assertEquals(variantA.name(), multiTree.getVariantId());
                assertEquals(DOT_PERSONALIZATION_DEFAULT, multiTree.getPersonalization());
            } else if (multiTree.getContentlet().equals(newEnContentlet.getIdentifier())) {
                assertEquals(variantA.name(), multiTree.getVariantId());
                assertEquals(persona.getKeyTag(), multiTree.getPersonalization());
            } else if (multiTree.getContentlet().equals(variantContentlet_1.getIdentifier())) {
                assertEquals(variantA.name(), multiTree.getVariantId());
                assertEquals(DOT_PERSONALIZATION_DEFAULT, multiTree.getPersonalization());
            } else if (multiTree.getContentlet().equals(variantContentlet_2.getIdentifier())) {
                assertEquals(variantA.name(), multiTree.getVariantId());
                assertEquals(DOT_PERSONALIZATION_DEFAULT, multiTree.getPersonalization());
            } else {
                throw new AssertionError("Contentlet not expected");
            }
        }
    }

    /**
     * Method to Test: {@link MultiTreeAPI#getMultiTreesByVariant(String, String)} (String, String)}
     * When: Create a Page with {@link MultiTree} is different {@link Variant} and call the 
     * {@link MultiTreeAPI#getMultiTreesByVariant(String, String)} method just for a specific {@link Variant}
     * Should: Return just the {@link MultiTree} for that {@link Variant}
     */
    @Test
    public void getMultiTreesByVariant() throws Exception {
        final Variant variantA = new VariantDataGen().nextPersisted();
        final Variant variantB = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType.id()).nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variantB)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variantB)
                .nextPersisted();

        List<String> contentsId = getContentsId(page, VariantAPI.DEFAULT_VARIANT.name());

        assertEquals(2, contentsId.size());
        assertTrue(contentsId.contains(contentlet_1.getIdentifier()));
        assertTrue(contentsId.contains(contentlet_2.getIdentifier()));

        contentsId = getContentsId(page, variantA.name());

        assertEquals(2, contentsId.size());
        assertTrue(contentsId.contains(contentlet_2.getIdentifier()));
        assertTrue(contentsId.contains(contentlet_3.getIdentifier()));

        contentsId = getContentsId(page, variantB.name());

        assertEquals(2, contentsId.size());
        assertTrue(contentsId.contains(contentlet_3.getIdentifier()));
        assertTrue(contentsId.contains(contentlet_1.getIdentifier()));

    }

    /**
     * Method to Test: {@link MultiTreeAPI#getMultiTreesByVariant(String, String)} (String, String)}
     * When: Create a Page with {@link MultiTree} just in  {@link VariantAPI#DEFAULT_VARIANT} and call the
     * {@link MultiTreeAPI#getMultiTreesByVariant(String, String)} method just for a specific {@link Variant}
     * Should: Return the {@link MultiTree} for that {@link VariantAPI#DEFAULT_VARIANT}
     */
    @Test
    public void getMultiTreesByVariantWithFallback() throws Exception {
        final Variant variantA = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType.id()).nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setInstanceID("1")
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees = APILocator.getMultiTreeAPI()
                .getPageMultiTrees(page, variantA.name(), false);

        assertFalse(pageMultiTrees.isEmpty());

        final List<String> contentsID = new ArrayList<>();

        for (String containerId : pageMultiTrees.rowKeySet()) {
            for (final String containerUUID : pageMultiTrees.row(containerId).keySet()) {
                final Set<PersonalizedContentlet> personalizedContentlets = pageMultiTrees.get(
                        containerId, containerUUID);

                contentsID.addAll(personalizedContentlets.stream()
                        .map(personalizedContentlet -> personalizedContentlet.getContentletId())
                        .collect(Collectors.toList()));
            }
        }

        assertEquals(3, contentsID.size());
        assertTrue("multiTree reorders", contentsID.contains(contentlet_1.getIdentifier()));
        assertTrue("multiTree reorders", contentsID.contains(contentlet_2.getIdentifier()));
        assertTrue("multiTree reorders", contentsID.contains(contentlet_3.getIdentifier()));
    }

    private List<String> getContentsId(final HTMLPageAsset page, final String variantName)
            throws DotDataException, DotSecurityException {
        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees = APILocator.getMultiTreeAPI()
                .getPageMultiTrees(page, variantName, false);

        final List<String> contentsId = new ArrayList<>();

        for (final String containerId : pageMultiTrees.rowKeySet()) {
            for (final String uniqueId : pageMultiTrees.row(containerId).keySet()) {
                final Collection<PersonalizedContentlet> personalizedContentletSet = pageMultiTrees.get(containerId, uniqueId);

                for (PersonalizedContentlet personalizedContentlet : personalizedContentletSet) {
                    contentsId.add(personalizedContentlet.getContentletId());
                }
            }
        }
        return contentsId;
    }

    /**
     * Method to Test: {@link MultiTreeAPI#overridesMultitreesByPersonalization(String, String, List, Optional, String)}
     * When: Try to save the same contentlet twice but for different variants
     * Should: save both multi_tree
     */
    @Test
    public void savaDifferent() throws Exception {
        final Variant variantA = new VariantDataGen().nextPersisted();
        final Variant variantB = new VariantDataGen().nextPersisted();
        final Persona persona = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet defaultContentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final MultiTree firstMultiTree = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(defaultContentlet)
                .setInstanceID("1")
                .setPersonalization(persona.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variantA)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                persona.getKeyTag(),
                list(firstMultiTree),
                Optional.of(defaultLanguage.getId()),
                variantA.name()
        );

        final List<MultiTree> multiTrees_1 = getMultiTrees(page.getIdentifier());
        assertEquals(1, multiTrees_1.size());

        final MultiTree newMultiTreeEnContent = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(defaultContentlet)
                .setInstanceID("1")
                .setPersonalization(persona.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variantB)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                persona.getKeyTag(),
                list(newMultiTreeEnContent),
                Optional.of(defaultLanguage.getId()),
                variantB.name()
        );

        final List<MultiTree> multiTrees_2 = getMultiTrees(page.getIdentifier());

        assertEquals(2, multiTrees_2.size());

        for (MultiTree multiTree : multiTrees_2) {
            assertEquals(defaultContentlet.getIdentifier(), multiTree.getContentlet());
            assertEquals(persona.getKeyTag(), multiTree.getPersonalization());
            assertTrue(multiTree.getVariantId().equals(variantA.name()) || multiTree.getVariantId().equals(variantB.name()));
        }
    }

    /**
     * Method to Test: {@link MultiTreeAPI#overridesMultitreesByPersonalization(String, String, List, Optional, String)}
     * When: A Page with content in different variants (A and B) try to update the MulTree just for the A variant
     * Should: Should keep the B and Default content and replace the A content
     */
    @Test
    public void shouldReplaceVariantMultiTree() throws Exception {
        final Variant variantA = new VariantDataGen().nextPersisted();
        final Variant variantB = new VariantDataGen().nextPersisted();

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet defaultContentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final Contentlet variantAContentlet = new ContentletDataGen(contentType.id())
                .variant(variantA)
                .nextPersisted();

        final Contentlet variantBContentlet = new ContentletDataGen(contentType.id())
                .variant(variantB)
                .nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final String uniqueId = UUIDGenerator.shorty();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(defaultContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(variantAContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variantA)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(variantBContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variantB)
                .nextPersisted();

        final Contentlet newEnContentlet = new ContentletDataGen(contentType.id())
                .variant(variantA)
                .nextPersisted();

        final MultiTree newMultiTreeEnContent = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(newEnContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variantA)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                DOT_PERSONALIZATION_DEFAULT,
                list(newMultiTreeEnContent),
                Optional.of(defaultLanguage.getId()),
                variantA.name()
        );

        final List<MultiTree> multiTrees = getMultiTrees(page.getIdentifier());

        final List<String> multiTreeContentlets = multiTrees.stream()
                .map(multiTree -> multiTree.getContentlet())
                .collect(Collectors.toList());

        assertEquals(3, multiTreeContentlets.size());
        assertTrue(multiTreeContentlets.contains(defaultContentlet.getIdentifier()));
        assertTrue(multiTreeContentlets.contains(newEnContentlet.getIdentifier()));
        assertTrue(multiTreeContentlets.contains(variantBContentlet.getIdentifier()));
        assertFalse(multiTreeContentlets.contains(variantAContentlet.getIdentifier()));
    }


    /**
     * Method to Test: {@link MultiTreeAPI#overridesMultitreesByPersonalization(String, String, List, Optional)} )}
     * When: A Page with content in Spanish and English try to update the MulTree just for English lang
     * Should: Should keep the Spanish content and replace the English content
     */
    @Test
    public void shouldReplaceEnglishMultiTree() throws Exception {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language espLanguage = new LanguageDataGen().country("ESP").languageCode("esp").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet enContentlet = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet enContentlet2 = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet espContentlet = new ContentletDataGen(contentType.id())
                .languageId(espLanguage.getId())
                .nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final String uniqueId = UUIDGenerator.shorty();

        new MultiTreeDataGen()
            .setPage(page)
            .setContainer(container)
            .setContentlet(enContentlet)
            .setInstanceID(uniqueId)
            .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
            .setTreeOrder(1)
            .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(enContentlet2)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(espContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .nextPersisted();

        final Contentlet newEnContentlet = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final MultiTree newMultiTreeEnContent = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(newEnContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                DOT_PERSONALIZATION_DEFAULT,
                list(newMultiTreeEnContent),
                Optional.of(defaultLanguage.getId())
        );

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());

        final List<String> multiTreeContentlets = multiTrees.stream()
                .map(multiTree -> multiTree.getContentlet())
                .collect(Collectors.toList());

        assertEquals(2, multiTreeContentlets.size());
        assertTrue(multiTreeContentlets.contains(espContentlet.getIdentifier()));
        assertTrue(multiTreeContentlets.contains(newEnContentlet.getIdentifier()));
        assertFalse(multiTreeContentlets.contains(enContentlet.getIdentifier()));
    }

    /**
     * Method to Test: {@link MultiTreeAPI#overridesMultitreesByPersonalization(String, String, List, Optional)} )}
     * When: A Page with content in Spanish and English try to update the MulTree but without lang
     * Should: Should replace all the {@link MultiTree}
     */
    @Test
    public void shouldReplaceAllMultiTree() throws Exception {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language espLanguage = new LanguageDataGen().country("ESP").languageCode("esp").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet enContentlet = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet espContentlet = new ContentletDataGen(contentType.id())
                .languageId(espLanguage.getId())
                .nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final String uniqueId = UUIDGenerator.shorty();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(enContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(espContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .nextPersisted();

        final Contentlet newEnContentlet = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final MultiTree newMultiTreeEnContent = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(newEnContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                DOT_PERSONALIZATION_DEFAULT,
                list(newMultiTreeEnContent),
                Optional.empty()
        );

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());

        final List<String> multiTreeContentlets = multiTrees.stream()
                .map(multiTree -> multiTree.getContentlet())
                .collect(Collectors.toList());

        assertEquals(1, multiTreeContentlets.size());
        assertTrue(multiTreeContentlets.contains(newEnContentlet.getIdentifier()));
        assertFalse(multiTreeContentlets.contains(espContentlet.getIdentifier()));
        assertFalse(multiTreeContentlets.contains(enContentlet.getIdentifier()));
    }

    /**
     * Method to Test: {@link MultiTreeAPI#getPageMultiTrees(IHTMLPage, boolean)}
     * When: Advanced Template with 2 {@link FileAssetContainer} (one empty) and 2 {@link Container} (one empty)
     * Should: Return the 4 containers
     * */
    @Test
    public void testEmptyContainersInAdvancedTemplate() throws DotDataException, DotSecurityException {
        final Container container = new ContainerDataGen().nextPersisted();
        FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen().nextPersisted();
        fileAssetContainer = (FileAssetContainer) APILocator.getContainerAPI().find(fileAssetContainer.getInode(), APILocator.systemUser(), false);

        final Container emptyContainer = new ContainerDataGen().nextPersisted();
        FileAssetContainer emptyFileAssetContainer = new ContainerAsFileDataGen().nextPersisted();
        emptyFileAssetContainer = (FileAssetContainer) APILocator.getContainerAPI().find(emptyFileAssetContainer.getInode(), APILocator.systemUser(), false);

        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_START_VALUE)
                .withContainer(fileAssetContainer, ContainerUUID.UUID_START_VALUE)
                .withContainer(emptyContainer, ContainerUUID.UUID_START_VALUE)
                .withContainer(emptyFileAssetContainer, ContainerUUID.UUID_START_VALUE)
                .nextPersisted();

        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        createContentAndMultiTree(container, fileAssetContainer, page);

        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees = APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

        assertTrue(pageMultiTrees.rowKeySet().contains(container.getIdentifier()));
        assertTrue(pageMultiTrees.rowKeySet().contains(emptyContainer.getIdentifier()));
        assertTrue(pageMultiTrees.rowKeySet().contains(fileAssetContainer.getIdentifier()));
        assertTrue(pageMultiTrees.rowKeySet().contains(emptyFileAssetContainer.getIdentifier()));
    }

    /**
     * Method to Test: {@link MultiTreeAPI#getPageMultiTrees(IHTMLPage, boolean)}
     * When: Drawed Template with 2 {@link FileAssetContainer} (one empty) and 2 {@link Container} (one empty)
     * Should: Return the 4 containers
     * */
    @Test
    public void testEmptyContainersInDrawedTemplate() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().site(site).nextPersisted();
        FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen().host(site).nextPersisted();
        fileAssetContainer = (FileAssetContainer) APILocator.getContainerAPI().find(fileAssetContainer.getInode(), APILocator.systemUser(), false);

        final Container emptyContainer = new ContainerDataGen().site(site).nextPersisted();
        FileAssetContainer emptyFileAssetContainer = new ContainerAsFileDataGen().host(site).nextPersisted();
        emptyFileAssetContainer = (FileAssetContainer) APILocator.getContainerAPI().find(emptyFileAssetContainer.getInode(), APILocator.systemUser(), false);

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container)
                .withContainer(fileAssetContainer)
                .withContainer(emptyContainer)
                .withContainer(emptyFileAssetContainer)
                .next();

        final Template template = new TemplateDataGen()
                .host(site)
                .drawedBody(templateLayout)
                .nextPersisted();

        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        createContentAndMultiTree(container, fileAssetContainer, page);

        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees = APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

        assertTrue(pageMultiTrees.rowKeySet().contains(container.getIdentifier()));
        assertTrue(pageMultiTrees.rowKeySet().contains(emptyContainer.getIdentifier()));
        assertTrue(pageMultiTrees.rowKeySet().contains(fileAssetContainer.getIdentifier()));
        assertTrue(pageMultiTrees.rowKeySet().contains(emptyFileAssetContainer.getIdentifier()));
    }

    private void createContentAndMultiTree(Container container, FileAssetContainer fileAssetContainer, HTMLPageAsset page) {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setInstanceID(ContainerUUID.UUID_START_VALUE)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(fileAssetContainer)
                .setContentlet(contentlet_2)
                .setInstanceID(ContainerUUID.UUID_START_VALUE)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();
    }

    ///
    /**
     * Method to Test: {@link MultiTreeAPI#overridesMultitreesByPersonalization(String, String, List, Optional)}}
     * When: Two pages shares the same contentlets, them one of these pages overrides their multitree
     * Should: Make sure the multitree of the another page still there (previously the share contentlets were removed on both tree, but just added in the one that overrides)
     */
    @Test
    public void after_override_multitrees_should_keep_other_pages_contentlet_references() throws Exception {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet1 = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page1 = new HTMLPageDataGen(folder, template).nextPersisted();
        final HTMLPageAsset page2 = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1).withStructure(structure, "").nextPersisted();

        final MultiTree newMultiTreeEnContent1 = new MultiTreeDataGen()
                .setPage(page1)
                .setContainer(container)
                .setContentlet(contentlet1)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        final MultiTree newMultiTreeEnContent2 = new MultiTreeDataGen()
                .setPage(page1)
                .setContainer(container)
                .setContentlet(contentlet2)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .nextPersisted();

        //
        new MultiTreeDataGen()
                .setPage(page2)
                .setContainer(container)
                .setContentlet(contentlet1)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page2)
                .setContainer(container)
                .setContentlet(contentlet2)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .nextPersisted();

        final Contentlet newEnContentlet = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final MultiTree newMultiTreeEnContent = new MultiTreeDataGen()
                .setPage(page1)
                .setContainer(container)
                .setContentlet(newEnContentlet)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page1.getIdentifier(),
                DOT_PERSONALIZATION_DEFAULT,
                list(newMultiTreeEnContent1, newMultiTreeEnContent2, newMultiTreeEnContent),
                Optional.of(defaultLanguage.getId())
        );

        CacheLocator.getMultiTreeCache().clearCache();

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(page1.getIdentifier());

        final Set<String> multiTreeContentlets = multiTrees.stream()
                .map(multiTree -> multiTree.getContentlet())
                .collect(Collectors.toSet());

        assertEquals(3, multiTreeContentlets.size());
        assertTrue(multiTreeContentlets.contains(newEnContentlet.getIdentifier()));
        assertTrue(multiTreeContentlets.contains(contentlet2.getIdentifier()));
        assertTrue(multiTreeContentlets.contains(contentlet1.getIdentifier()));

        final List<MultiTree> multiTrees2 = APILocator.getMultiTreeAPI().getMultiTrees(page2.getIdentifier());

        final Set<String> multiTreeContentlets2 = multiTrees2.stream()
                .map(multiTree -> multiTree.getContentlet())
                .collect(Collectors.toSet());

        assertEquals(2, multiTreeContentlets2.size());
        assertTrue(multiTreeContentlets.contains(contentlet1.getIdentifier()));
        assertTrue(multiTreeContentlets.contains(contentlet2.getIdentifier()));
    }

    /**
     * Method to Test: {@link MultiTreeAPI#copyMultiTree(String, List, String)}}
     * When:
     * - Have a Page with several MultiTree into the DEFAULT Variant and different Perzonalization.
     * - And copy the MUltiTree to a new {@link Variant}
     *
     * Should: Copy all the MultiTree to the new Variant and must keep the same Peraonalization
     */
    @Test
    public void copyMultiTreeToNewVariant() throws DotDataException {
        final Variant targetVariant = new VariantDataGen().nextPersisted();
        final Variant extraVariant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .nextPersisted();

        final Contentlet contentlet_3 = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .nextPersisted();

        final Contentlet contentlet_4 = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .nextPersisted();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Container container = new ContainerDataGen().maxContentlets(1).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization("another_persona")
                .setTreeOrder(2)
                .nextPersisted();

        final MultiTree multiTree_3 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .nextPersisted();

        final MultiTree multiTree_4 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_4)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization("personalization")
                .setTreeOrder(1)
                .setVariant(extraVariant)
                .nextPersisted();

        APILocator.getMultiTreeAPI().copyMultiTree(page.getIdentifier(),
                list(multiTree_1, multiTree_2, multiTree_3), targetVariant.name());

        final List<Map<String, String>> multiTrees = new DotConnect().setSQL(
                        "SELECT * FROM multi_tree WHERE parent1 = ?")
                .addParam(page.getIdentifier())
                .loadResults();

        assertEquals(7, multiTrees.size());

        final Set<String> contentletsTargetVariant = multiTrees.stream()
                .filter(multiTreeMap -> targetVariant.name().equals(multiTreeMap.get("variant_id")))
                .map(multiTreeMap -> multiTreeMap.get("child"))
                .collect(Collectors.toSet());

        assertEquals(3, contentletsTargetVariant.size());
        assertTrue(contentletsTargetVariant.contains(contentlet_1.getIdentifier()));
        assertTrue(contentletsTargetVariant.contains(contentlet_2.getIdentifier()));
        assertTrue(contentletsTargetVariant.contains(contentlet_3.getIdentifier()));

        final List<Map<String, String>> extraVariantMap = multiTrees.stream()
                .filter(multiTreeMap -> extraVariant.name().equals(multiTreeMap.get("variant_id")))
                .collect(Collectors.toList());

        assertEquals(1, extraVariantMap.size());
        assertTrue(extraVariantMap.get(0).get("child").contains(contentlet_4.getIdentifier()));
        assertTrue(extraVariantMap.get(0).get("variant_id").contains(extraVariant.name()));
        assertTrue(extraVariantMap.get(0).get("personalization").contains("personalization"));

        final Collection<Map<String, String>> contentletsDefaultVariant = multiTrees.stream()
                .filter(multiTreeMap -> VariantAPI.DEFAULT_VARIANT.name().equals(multiTreeMap.get("variant_id")))
                .collect(Collectors.toSet());

        assertEquals(3, contentletsDefaultVariant.size());

        for (final Map<String, String> multiTreeMap : contentletsDefaultVariant) {
            if (multiTreeMap.get("child").equals(contentlet_1.getIdentifier())) {
                assertTrue(multiTreeMap.get("variant_id").equals(VariantAPI.DEFAULT_VARIANT.name()));
                assertTrue(multiTreeMap.get("personalization").equals(DOT_PERSONALIZATION_DEFAULT));
            } else if (multiTreeMap.get("child").equals(contentlet_2.getIdentifier())) {
                assertTrue(multiTreeMap.get("variant_id").equals(VariantAPI.DEFAULT_VARIANT.name()));
                assertTrue(multiTreeMap.get("personalization").equals("another_persona"));
            } else if (multiTreeMap.get("child").equals(contentlet_3.getIdentifier())) {
                assertTrue(multiTreeMap.get("variant_id").equals(VariantAPI.DEFAULT_VARIANT.name()));
                assertTrue(multiTreeMap.get("personalization").equals(DOT_PERSONALIZATION_DEFAULT));
            } else {
                fail("Contentlet not found");
            }
        }

    }

    /**
     * Method to Test: {@link MultiTreeAPI#getMultiTrees(Variant)}
     * When: You have MultiTree inside a Variant for different pages
     * Should: return all the {@link MultiTree} for the Variant
     */
    @Test
    public void multiTreesByVariant() throws DotDataException {
        final Variant variant_1 = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset htmlPageAsset_1 = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset htmlPageAsset_2 = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset htmlPageAsset_3 = new HTMLPageDataGen(host, template).nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Contentlet contentlet_1_default = new ContentletDataGen(contentType.id())
                .nextPersisted();
        final Contentlet contentlet_2_default = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3_default = new ContentletDataGen(contentType.id()).nextPersisted();

        final Contentlet contentlet_1_variant_1 = new ContentletDataGen(contentType.id())
                .nextPersisted();

        final Contentlet contentlet_2_variant_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3_variant_1 = new ContentletDataGen(contentType.id()).nextPersisted();

        final Contentlet contentlet_1_variant_2 = new ContentletDataGen(contentType.id())
                .nextPersisted();
        final Contentlet contentlet_2_variant_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3_variant_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_1)
                .setContainer(container)
                .setContentlet(contentlet_1_default)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_2)
                .setContainer(container)
                .setContentlet(contentlet_2_default)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_3)
                .setContainer(container)
                .setContentlet(contentlet_3_default)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_1)
                .setContainer(container)
                .setContentlet(contentlet_1_variant_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_2)
                .setContainer(container)
                .setContentlet(contentlet_2_variant_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variant_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_3)
                .setContainer(container)
                .setContentlet(contentlet_3_variant_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .setVariant(variant_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_1)
                .setContainer(container)
                .setContentlet(contentlet_1_variant_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant_2)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_2)
                .setContainer(container)
                .setContentlet(contentlet_2_variant_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variant_2)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_3)
                .setContainer(container)
                .setContentlet(contentlet_3_variant_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .setVariant(variant_2)
                .nextPersisted();

        final List<MultiTree> multiTreeList = APILocator.getMultiTreeAPI().getMultiTrees(variant_1);

        assertEquals(3, multiTreeList.size());
        multiTreeList.stream().map(MultiTree::getContentlet).forEach(contentletId ->
            assertTrue(contentlet_1_variant_1.getIdentifier().equals(contentletId)
                    || contentlet_2_variant_1.getIdentifier().equals(contentletId)
                    || contentlet_3_variant_1.getIdentifier().equals(contentletId))
        );

        final List<MultiTree> multiTreeList_2 = APILocator.getMultiTreeAPI().getMultiTrees(variant_2);

        assertEquals(3, multiTreeList_2.size());
        multiTreeList_2.stream().map(MultiTree::getContentlet).forEach(contentletId ->
            assertTrue(contentlet_1_variant_2.getIdentifier().equals(contentletId)
                    || contentlet_2_variant_2.getIdentifier().equals(contentletId)
                    || contentlet_3_variant_2.getIdentifier().equals(contentletId))
        );

    }

    /**
     * Method to Test: {@link MultiTreeAPI#deleteMultiTree(String, String)}
     * When: You have MultiTree inside a Variant for different pages and you try to delete
     * them just for a Page and {@link Variant}
     * Should:delete the MultiTrees for the Page and {@link Variant} but not for the other pages
     */
    @Test
    public void deleteMultiTreeByPageAndVariant() throws DotDataException, DotSecurityException {
        final Variant variant_1 = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset htmlPageAsset_1 = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset htmlPageAsset_2 = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset htmlPageAsset_3 = new HTMLPageDataGen(host, template).nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Contentlet contentlet_1_default = new ContentletDataGen(contentType.id())
                .nextPersisted();
        final Contentlet contentlet_2_default = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3_default = new ContentletDataGen(contentType.id()).nextPersisted();

        final Contentlet contentlet_1_variant_1 = new ContentletDataGen(contentType.id())
                .nextPersisted();

        final Contentlet contentlet_2_variant_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3_variant_1 = new ContentletDataGen(contentType.id()).nextPersisted();

        final Contentlet contentlet_1_variant_2 = new ContentletDataGen(contentType.id())
                .nextPersisted();
        final Contentlet contentlet_2_variant_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3_variant_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_1)
                .setContainer(container)
                .setContentlet(contentlet_1_default)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_2)
                .setContainer(container)
                .setContentlet(contentlet_2_default)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_3)
                .setContainer(container)
                .setContentlet(contentlet_3_default)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_1)
                .setContainer(container)
                .setContentlet(contentlet_1_variant_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_2)
                .setContainer(container)
                .setContentlet(contentlet_2_variant_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variant_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_3)
                .setContainer(container)
                .setContentlet(contentlet_3_variant_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .setVariant(variant_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_1)
                .setContainer(container)
                .setContentlet(contentlet_1_variant_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant_2)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_2)
                .setContainer(container)
                .setContentlet(contentlet_2_variant_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .setVariant(variant_2)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(htmlPageAsset_3)
                .setContainer(container)
                .setContentlet(contentlet_3_variant_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(3)
                .setVariant(variant_2)
                .nextPersisted();

        APILocator.getMultiTreeAPI().getPageMultiTrees(htmlPageAsset_1, variant_1.name(), false);

        assertEquals(1, countMultiTree(variant_1, htmlPageAsset_1));
        APILocator.getMultiTreeAPI().deleteMultiTree(htmlPageAsset_1.getIdentifier(), variant_1.name());
        assertEquals(0, countMultiTree(variant_1, htmlPageAsset_1));

        assertEquals(1, countMultiTree(variant_2, htmlPageAsset_1));

        APILocator.getMultiTreeAPI().deleteMultiTree(htmlPageAsset_1.getIdentifier(), variant_2.name());
        assertEquals(0, countMultiTree(variant_2, htmlPageAsset_1));

        assertEquals(1, countMultiTree(variant_2, htmlPageAsset_2));
        assertEquals(1, countMultiTree(variant_1, htmlPageAsset_2));

        assertEquals(1, countMultiTree(variant_2, htmlPageAsset_3));
        assertEquals(1, countMultiTree(variant_1, htmlPageAsset_3));

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees = CacheLocator.getMultiTreeCache()
                .getPageMultiTrees(htmlPageAsset_1.getIdentifier(), variant_1.name(), false);

        assertTrue(pageMultiTrees.isEmpty());
    }

    private static int countMultiTree(Variant variant_1, HTMLPageAsset htmlPageAsset_1)
            throws DotDataException {
        final List<Map<String, String>> results = new DotConnect().setSQL(
                        "SELECT COUNT(*) FROM multi_tree WHERE parent1 = ? AND variant_id = ?")
                .addParam(htmlPageAsset_1.getIdentifier())
                .addParam(variant_1.name())
                .loadResults();

        return results.isEmpty() ? 0 : Integer.parseInt(results.get(0).get("count"));
    }

    /**
     * Method to test: {@link MultiTreeAPI#getPersonalizationsForPage(IHTMLPage, String)}
     * When: A Page has Personalizations for a Specific Variant and does not have a Default Variant
     * Personalization
     * Should: Return the Personalizations for the Specific Variant
     */
    @Test
    public void getPersonalizationsForPageForSpecificVariantWhenNoDefault()
            throws DotDataException {

        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_2 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String noDefaultVariantName = experiment.trafficProportion().variants()
                .stream()
                .filter(variant -> !variant.id().equals(VariantAPI.DEFAULT_VARIANT.name()))
                .map(experimentVariant -> experimentVariant.id())
                .findFirst()
                .orElseThrow();

        final Variant noDefaultVariant = APILocator.getVariantAPI().get(noDefaultVariantName).orElseThrow();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(noDefaultVariant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(persona_2.getKeyTag())
                .setTreeOrder(1)
                .setVariant(noDefaultVariant)
                .nextPersisted();

        final Set<String> personalizationsForPage = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(experimentPage, noDefaultVariant.name());

        assertEquals(2, personalizationsForPage.size());
        assertTrue(personalizationsForPage.contains(persona_1.getKeyTag()));
        assertTrue(personalizationsForPage.contains(persona_2.getKeyTag()));
    }

    /**
     * Method to test: {@link MultiTreeAPI#getPersonalizationsForPage(IHTMLPage, String)}
     * When: A Page has Personalizations for a Specific Variant and also have others Personalization to
     * Default Variant
     * Should: Return the Personalizations for the Specific and DEFAULT Variants
     */
    @Test
    public void getPersonalizationsForPageForSpecificVariantAndDefault() throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_2 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_3 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String noDefaultVariantName = experiment.trafficProportion().variants()
                .stream()
                .filter(variant -> !variant.id().equals(VariantAPI.DEFAULT_VARIANT.name()))
                .map(experimentVariant -> experimentVariant.id())
                .findFirst()
                .orElseThrow();

        final Variant noDefaultVariant = APILocator.getVariantAPI().get(noDefaultVariantName).orElseThrow();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(noDefaultVariant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(persona_2.getKeyTag())
                .setTreeOrder(1)
                .setVariant(noDefaultVariant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(persona_3.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        final Set<String> personalizationsForPageNoDefaultVariant = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(experimentPage, noDefaultVariant.name());

        assertEquals(2, personalizationsForPageNoDefaultVariant.size());
        assertTrue(personalizationsForPageNoDefaultVariant.contains(persona_1.getKeyTag()));
        assertTrue(personalizationsForPageNoDefaultVariant.contains(persona_2.getKeyTag()));

        final Set<String> personalizationsForPageDefaultVariant = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(experimentPage, VariantAPI.DEFAULT_VARIANT.name());

        assertEquals(2, personalizationsForPageDefaultVariant.size());
        assertTrue(personalizationsForPageDefaultVariant.contains(persona_1.getKeyTag()));
        assertTrue(personalizationsForPageDefaultVariant.contains(persona_3.getKeyTag()));

    }

    /**
     * Method to test: {@link MultiTreeAPI#getPersonalizationsForPage(IHTMLPage, String)}
     * When: A Page has Personalizations for the DEFAULT Variant and does not have for a Specific Variant
     * Personalization
     * Should: Return the Personalizations for the Default Variant
     */
    @Test
    public void getPersonalizationsForPageForDefaultVariantWhenNoSpecific()
            throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_2 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_3 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String noDefaultVariantName = experiment.trafficProportion().variants()
                .stream()
                .filter(variant -> !variant.id().equals(VariantAPI.DEFAULT_VARIANT.name()))
                .map(experimentVariant -> experimentVariant.id())
                .findFirst()
                .orElseThrow();

        final Variant noDefaultVariant = APILocator.getVariantAPI().get(noDefaultVariantName).orElseThrow();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(experimentPage)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(persona_3.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        final Set<String> personalizationsForPageNoDefaultVariant = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(experimentPage, noDefaultVariant.name());

        assertEquals(2, personalizationsForPageNoDefaultVariant.size());
        assertTrue(personalizationsForPageNoDefaultVariant.contains(persona_1.getKeyTag()));
        assertTrue(personalizationsForPageNoDefaultVariant.contains(persona_3.getKeyTag()));

        final Set<String> personalizationsForPageDefaultVariant = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(experimentPage, VariantAPI.DEFAULT_VARIANT.name());

        assertEquals(2, personalizationsForPageDefaultVariant.size());
        assertTrue(personalizationsForPageDefaultVariant.contains(persona_1.getKeyTag()));
        assertTrue(personalizationsForPageDefaultVariant.contains(persona_3.getKeyTag()));
    }

    /**
     * Method to test: {@link MultiTreeAPI#getPersonalizationsForPage(IHTMLPage, String)}
     * When: A Page don't have any Personalizations
     * Should: Return the default Persona
     */
    @Test
    public void getPersonalizationsForPageWithoutPersonalization() throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_2 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_3 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String noDefaultVariantName = experiment.trafficProportion().variants()
                .stream()
                .filter(variant -> !variant.id().equals(VariantAPI.DEFAULT_VARIANT.name()))
                .map(experimentVariant -> experimentVariant.id())
                .findFirst()
                .orElseThrow();

        final Variant noDefaultVariant = APILocator.getVariantAPI().get(noDefaultVariantName).orElseThrow();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final Set<String> personalizationsForPageNoDefaultVariant = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(experimentPage, noDefaultVariant.name());

        assertTrue(personalizationsForPageNoDefaultVariant.isEmpty());

        final Set<String> personalizationsForPageDefaultVariant = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(experimentPage, VariantAPI.DEFAULT_VARIANT.name());

        assertTrue(personalizationsForPageDefaultVariant.isEmpty());

    }

    /**
     * Method to test: {@link MultiTreeAPI#copyPersonalizationForPage(String, String, String)}
     * When: A Page has
     * - Personalizations for the Specific Variant for the default Persona
     * - and Personalizations for the Default Variant for the default Persona
     * and  try to copy the Personalizations to the same Specific Variant for a different Persona
     * Should: Copy from the Specific VARIANT and default Persona to the Specific Variant and Persona
     */
    @Test
    public void copyFromDefaultToSpecificVariant() throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_2 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(persona_2.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_4)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));


        APILocator.getMultiTreeAPI().copyPersonalizationForPage(page.getIdentifier(),
                persona_1.getKeyTag(), variant.name());

        final List<String> contentsVariantPersona1 = getContentsId(persona_1.getKeyTag(), page, variant);

        assertEquals(2, contentsVariantPersona1.size());

        assertTrue(contentsVariantPersona1.contains(contentlet_3.getIdentifier()));
        assertTrue(contentsVariantPersona1.contains(contentlet_4.getIdentifier()));

        final List<String> contentsDefaultVariantPersona1 = getContentsId(persona_1.getKeyTag(), page,
                VariantAPI.DEFAULT_VARIANT);

        assertEquals(1, contentsDefaultVariantPersona1.size());
        assertTrue(contentsDefaultVariantPersona1.contains(contentlet_1.getIdentifier()));

        final List<String> contentsDefaultVariantPersona2 = getContentsId(persona_2.getKeyTag(), page,
                VariantAPI.DEFAULT_VARIANT);

        assertEquals(1, contentsDefaultVariantPersona2.size());
        assertTrue(contentsDefaultVariantPersona2.contains(contentlet_2.getIdentifier()));

        final List<String> contentsDefaultVariantDefaultPersona = getContentsId(DOT_PERSONALIZATION_DEFAULT, page,
                VariantAPI.DEFAULT_VARIANT);

        assertEquals(2, contentsDefaultVariantDefaultPersona.size());
        assertTrue(contentsDefaultVariantDefaultPersona.contains(contentlet_3.getIdentifier()));
        assertTrue(contentsDefaultVariantDefaultPersona.contains(contentlet_4.getIdentifier()));

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(2, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(2, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));

    }

    /**
     * Method to test: {@link MultiTreeAPI#copyPersonalizationForPage(String, String, String)}
     * When: A Page has Personalizations for the Specific Variant for the default Persona and try
     * to copy the Personalizations to the same Specific Variant for a different Persona
     * Should: Copy from the Specific VARIANT and default Persona to the Specific Variant and Persona
     */
    @Test
    public void copyFromSpecificVariantToSpecificVariant() throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final Persona persona_2 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_4)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_5)
                .setPersonalization(persona_2.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_5.getIdentifier()));


        APILocator.getMultiTreeAPI().copyPersonalizationForPage(page.getIdentifier(),
                persona_1.getKeyTag(), variant.name());

        final List<String> contentsVariantPersona1 = getContentsId(persona_1.getKeyTag(), page, variant);

        assertEquals(2, contentsVariantPersona1.size());
        assertTrue(contentsVariantPersona1.contains(contentlet_2.getIdentifier()));
        assertTrue(contentsVariantPersona1.contains(contentlet_3.getIdentifier()));

        final List<String> contentsDefaultVariantPersona1 = getContentsId(persona_1.getKeyTag(), page,
                VariantAPI.DEFAULT_VARIANT);

        assertEquals(1, contentsDefaultVariantPersona1.size());
        assertTrue(contentsDefaultVariantPersona1.contains(contentlet_1.getIdentifier()));

        final List<String> contentsDefaultVariantDefaultPersona = getContentsId(DOT_PERSONALIZATION_DEFAULT, page,
                VariantAPI.DEFAULT_VARIANT);

        assertEquals(1, contentsDefaultVariantDefaultPersona.size());
        assertTrue(contentsDefaultVariantDefaultPersona.contains(contentlet_4.getIdentifier()));

        final List<String> contentsVariantDefaultPersona = getContentsId(DOT_PERSONALIZATION_DEFAULT, page,
                variant);

        assertEquals(2, contentsVariantDefaultPersona.size());
        assertTrue(contentsVariantDefaultPersona.contains(contentlet_2.getIdentifier()));
        assertTrue(contentsVariantDefaultPersona.contains(contentlet_3.getIdentifier()));

        final List<String> contentsVariantPersona2 = getContentsId(persona_2.getKeyTag(), page,
                variant);

        assertEquals(1, contentsVariantPersona2.size());
        assertTrue(contentsVariantPersona2.contains(contentlet_5.getIdentifier()));

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(2, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(2, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_5.getIdentifier()));

    }

    /**
     * Method to test: {@link MultiTreeAPI#copyPersonalizationForPage(String, String, String)}
     * When: A Page has:
     * - Personalizations for the Specific Variant for the default Persona.
     * - Don't have any Personalizations for the DEFAULT Variant
     * and try to copy the Personalizations to the same Specific Variant for a different Persona
     * Should: Copy from the Specific VARIANT and default Persona to the Specific Variant and Persona
     */
    @Test
    public void copyFromSpecificVariantToSpecificVariantNotDefaultPeronalizations() throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();


        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));

        APILocator.getMultiTreeAPI().copyPersonalizationForPage(page.getIdentifier(),
                persona_1.getKeyTag(), variant.name());

        final List<String> contentsVariantDefaultPersona = getContentsId(DOT_PERSONALIZATION_DEFAULT, page,
                variant);

        assertEquals(2, contentsVariantDefaultPersona.size());
        assertTrue(contentsVariantDefaultPersona.contains(contentlet_1.getIdentifier()));
        assertTrue(contentsVariantDefaultPersona.contains(contentlet_2.getIdentifier()));

        final List<String> contentsVariantDefaultPersona1 = getContentsId(persona_1.getKeyTag(), page,
                variant);

        assertEquals(2, contentsVariantDefaultPersona1.size());
        assertTrue(contentsVariantDefaultPersona1.contains(contentlet_1.getIdentifier()));
        assertTrue(contentsVariantDefaultPersona1.contains(contentlet_2.getIdentifier()));

        assertEquals(2, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(2, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));

    }

    @NotNull
    private static List<String> getContentsId(final String personaKey, final HTMLPageAsset page,
            final Variant variant) throws DotDataException {
        final List<String> contents = new DotConnect().setSQL(
                        "SELECT child FROM multi_tree WHERE variant_id = ? AND parent1 = ? AND personalization = ?")
                .addParam(variant.name())
                .addParam(page.getIdentifier())
                .addParam(personaKey)
                .loadObjectResults()
                .stream().map(row -> row.get("child").toString())
                .collect(Collectors.toList());
        return contents;
    }


    /**
     * Method to test: {@link MultiTreeAPI#deletePersonalizationForPage(String, String, String)}
     * When: A Page has Personalizations for the Default Variant for a specific Persona and try
     * to delete it.
     * Should: Delete All the MultiTree for the Default Variant and specific Persona
     */
    @Test
    public void deletePersonalizationDefaultVariant() throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_4)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));


        APILocator.getMultiTreeAPI().deletePersonalizationForPage(page.getIdentifier(), persona_1.getKeyTag(),
                VariantAPI.DEFAULT_VARIANT.name());

        final List<String> contentsDefaultVariantPersona1 = getContentsId(persona_1.getKeyTag(), page,
                VariantAPI.DEFAULT_VARIANT);
        assertTrue(contentsDefaultVariantPersona1.isEmpty());

        final List<String> contentsDefaultVariantDefaultPersona = getContentsId(DOT_PERSONALIZATION_DEFAULT,
                page, VariantAPI.DEFAULT_VARIANT);

        assertEquals(1, contentsDefaultVariantDefaultPersona.size());
        assertTrue(contentsDefaultVariantDefaultPersona.contains(contentlet_1.getIdentifier()));

        final List<String> contentsVariantPersona1 = getContentsId(persona_1.getKeyTag(),
                page, variant);

        assertEquals(1, contentsVariantPersona1.size());
        assertTrue(contentsVariantPersona1.contains(contentlet_4.getIdentifier()));

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(0, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(0, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));

    }

    /**
     * Method to test: {@link MultiTreeAPI#deletePersonalizationForPage(String, String, String)}
     * When: A Page has Personalizations for the Specific Variant for a specific Persona and try
     * to delete it.
     * Should: Delete All the MultiTree for the Specific Variant and Persona
     */
    @Test
    public void deletePersonalizationSpecificVariant() throws DotDataException {
        final Persona persona_1 = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container, "1").nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType.id()).nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_1)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_5)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_2)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_3)
                .setPersonalization(persona_1.getKeyTag())
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet_4)
                .setPersonalization(DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .setVariant(variant)
                .nextPersisted();

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_5.getIdentifier()));

        APILocator.getMultiTreeAPI().deletePersonalizationForPage(page.getIdentifier(), persona_1.getKeyTag(),
                variant.name());

        final List<String> contentsVariantPersona1 = getContentsId(persona_1.getKeyTag(), page,
                variant);
        assertTrue(contentsVariantPersona1.isEmpty());

        final List<String> contentsDefaultVariantDefaultPersona = getContentsId(DOT_PERSONALIZATION_DEFAULT,
                page, VariantAPI.DEFAULT_VARIANT);

        assertEquals(1, contentsDefaultVariantDefaultPersona.size());
        assertTrue(contentsDefaultVariantDefaultPersona.contains(contentlet_1.getIdentifier()));

        final List<String> contentsDefaultVariantPersona1 = getContentsId(persona_1.getKeyTag(),
                page, VariantAPI.DEFAULT_VARIANT);

        assertEquals(1, contentsDefaultVariantPersona1.size());
        assertTrue(contentsDefaultVariantPersona1.contains(contentlet_5.getIdentifier()));

        final List<String> contentsVariantDefaultPersona = getContentsId(DOT_PERSONALIZATION_DEFAULT,
                page, variant);

        assertEquals(1, contentsVariantDefaultPersona.size());
        assertTrue(contentsVariantDefaultPersona.contains(contentlet_4.getIdentifier()));

        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_1.getIdentifier()));
        assertEquals(0, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_2.getIdentifier()));
        assertEquals(0, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_3.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_4.getIdentifier()));
        assertEquals(1, APILocator.getMultiTreeAPI().getAllContentletReferencesCount(contentlet_5.getIdentifier()));
    }


    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have a Page with 4 containers all of them are different instances of the same containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * And you move the last instance to be the first one
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : Add to the instance 2
     * - Contentlet_2 : Add to the instance 3
     * - Contentlet_3 : Add to the instance 4
     * - Contentlet_5 : Add to the instance 4
     * - Contentlet_4 : Add to the instance 1
     *
     * @throws DotDataException
     */
    @Test
    public void moveContainerUpdateMultiTrees() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                .withContainer(container, "2")
                .withContainer(container, "3")
                .withContainer(container, "4")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        createMultiTrees(page, container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .change(container.getIdentifier(), "4", "1")
            .change(container.getIdentifier(), "1", "2")
            .change(container.getIdentifier(), "2", "3")
            .change(container.getIdentifier(), "3", "4")
            .build();

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, list(page.getIdentifier()),
                VariantAPI.DEFAULT_VARIANT.name());

        final List<MultiTree> multiTreesFromDB = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
        assertEquals(5, multiTreesFromDB.size());

        final Map<String, List<MultiTree>> groupedByInstanceId = multiTreesFromDB.stream()
                .collect(Collectors.groupingBy(MultiTree::getRelationType));

        for (final String intanceId : groupedByInstanceId.keySet()) {
            final List<MultiTree> multiTrees = groupedByInstanceId.get(intanceId);

            switch (intanceId){
                case "1":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_4.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                case "2":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_1.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                case "3":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_2.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                case "4":
                    assertEquals(2, multiTrees.size());

                    List<String> contentlets = multiTrees.stream().map(MultiTree::getContentlet)
                            .collect(Collectors.toList());
                    assertTrue(contentlets.contains(contentlet_3.getIdentifier()));
                    assertTrue(contentlets.contains(contentlet_5.getIdentifier()));
                    break;
                default:
                    throw new AssertionError("UUID not expected: " + intanceId);
            }
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have a Page with 4 containers all of them are different instances of the same containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * And you move the instance 2 and 3
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 3
     * - Contentlet_3 : Add to the instance 2
     * - Contentlet_5 : Add to the instance 2
     * - Contentlet_4 : Add to the instance 4
     *
     * @throws DotDataException
     */
    @Test
    public void moveJustTwoContainerUpdateMultiTrees() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                .withContainer(container, "2")
                .withContainer(container, "3")
                .withContainer(container, "4")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        createMultiTrees(page, container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .change(container.getIdentifier(), "2", "3")
            .change(container.getIdentifier(), "3", "2")
            .build();

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, list(page.getIdentifier()), VariantAPI.DEFAULT_VARIANT.name());

        final List<MultiTree> multiTreesFromDB = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
        assertEquals(5, multiTreesFromDB.size());

        final Map<String, List<MultiTree>> groupedByInstanceId = multiTreesFromDB.stream()
                .collect(Collectors.groupingBy(MultiTree::getRelationType));

        for (final String intanceId : groupedByInstanceId.keySet()) {
            final List<MultiTree> multiTrees = groupedByInstanceId.get(intanceId);

            switch (intanceId){
                case "1":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_1.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                case "2":
                    assertEquals(2, multiTrees.size());

                    List<String> contentlets = multiTrees.stream().map(MultiTree::getContentlet)
                            .collect(Collectors.toList());
                    assertTrue(contentlets.contains(contentlet_3.getIdentifier()));
                    assertTrue(contentlets.contains(contentlet_5.getIdentifier()));
                    break;
                case "3":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_2.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                case "4":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_4.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                default:
                    throw new AssertionError("UUID not expected: " + intanceId);
            }
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have a Page with 4 containers all of them are different instances of the same containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * And you remove the instance 1
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : removed
     * - Contentlet_2 : Add to the instance 1
     * - Contentlet_3 : Add to the instance 2
     * - Contentlet_5 : Add to the instance 2
     * - Contentlet_4 : Add to the instance 3
     *
     * @throws DotDataException
     */
    @Test
   public void removeContainerUpdateMultiTrees() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                .withContainer(container, "2")
                .withContainer(container, "3")
                .withContainer(container, "4")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        createMultiTrees(page, container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .remove(container.getIdentifier(), "1")
            .change(container.getIdentifier(), "2", "1")
            .change(container.getIdentifier(), "3", "2")
            .change(container.getIdentifier(), "4", "3")
            .build();

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, list(page.getIdentifier()), VariantAPI.DEFAULT_VARIANT.name());

        final List<MultiTree> multiTreesFromDB = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
        assertEquals(4, multiTreesFromDB.size());

        final Map<String, List<MultiTree>> groupedByInstanceId = multiTreesFromDB.stream()
                .collect(Collectors.groupingBy(MultiTree::getRelationType));

        for (final String intanceId : groupedByInstanceId.keySet()) {
            final List<MultiTree> multiTrees = groupedByInstanceId.get(intanceId);

            switch (intanceId){
                case "1":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_2.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                case "2":
                    assertEquals(2, multiTrees.size());

                    List<String> contentlets = multiTrees.stream().map(multiTree -> multiTree.getContentlet())
                            .collect(Collectors.toList());
                    assertTrue(contentlets.contains(contentlet_3.getIdentifier()));
                    assertTrue(contentlets.contains(contentlet_5.getIdentifier()));
                    break;
                case "3":
                    assertEquals(1, multiTrees.size());
                    assertEquals(contentlet_4.getIdentifier(), multiTrees.get(0).getContentlet());
                    break;
                default:
                    throw new AssertionError("UUID not expected: " + intanceId);
            }
        }
    }


    private static void createMultiTrees(final HTMLPageAsset page,
                                         Container container, Contentlet... contentlets) {

        final Contentlet contentlet_1 = contentlets[0];
        final Contentlet contentlet_2 = contentlets[1];
        final Contentlet contentlet_3 = contentlets[2];
        final Contentlet contentlet_4 = contentlets[3];
        final Contentlet contentlet_5 = contentlets[4];

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setInstanceID("1")
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setInstanceID("2")
                .setContentlet(contentlet_2)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setInstanceID("3")
                .setContentlet(contentlet_3)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setInstanceID("3")
                .setContentlet(contentlet_5)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setInstanceID("4")
                .setContentlet(contentlet_4)
                .nextPersisted();
    }

    private Map<String, List<Map<String, Object>>> getMultiTreeByPageOrderByInstanceId(final String pageId,
                                                                                       final ArrayList<Map<String, Object>> multiTreeResult,
                                                                                       final int multiTreeCurrentIndex) {
        int innerMultiCurrentIndex = multiTreeCurrentIndex;
        final Map<String, List<Map<String, Object>>> multiTreesFromDB = new HashMap<>();

        while (innerMultiCurrentIndex < multiTreeResult.size()) {
            Map<String, Object> multiTreeMap = multiTreeResult.get(innerMultiCurrentIndex);

            if (pageId.equals(multiTreeMap.get("parent1").toString())) {
                String relationType = multiTreeMap.get("relation_type").toString();
                innerMultiCurrentIndex++;

                List<Map<String, Object>> contentlets = multiTreesFromDB.get(relationType);

                if (contentlets == null) {
                    contentlets = new ArrayList<>();

                }
                contentlets.add(multiTreeMap);
                multiTreesFromDB.put(relationType, contentlets);
            } else {
                break;
            }
        }

        return multiTreesFromDB;
    }

    private List<Map<String, Object>>getMultiTreeByPageOrder(final String pageId,
                                                           final ArrayList<Map<String, Object>> multiTreeResult,
                                                           final int multiTreeCurrentIndex) {
        int innerMultiCurrentIndex = multiTreeCurrentIndex;
        final List<Map<String, Object>> multiTreesFromDB = new ArrayList<>();

        while (innerMultiCurrentIndex < multiTreeResult.size()) {
            Map<String, Object> multiTreeMap = multiTreeResult.get(innerMultiCurrentIndex);

            if (pageId.equals(multiTreeMap.get("parent1").toString())) {
                innerMultiCurrentIndex++;
                multiTreesFromDB.add(multiTreeMap);
            } else {
                break;
            }
        }

        return multiTreesFromDB;
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have 10 Page with 4 containers all of them are different instances of the same containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * And you move last the instance to be the first one
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : Add to the instance 2
     * - Contentlet_2 : Add to the instance 3
     * - Contentlet_3 : Add to the instance 4
     * - Contentlet_5 : Add to the instance 4
     * - Contentlet_4 : Add to the instance 1
     *
     * @throws DotDataException
     */
    @Test
    public void moveContainerUpdateMultiTreesOnMultiplePages() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                .withContainer(container, "2")
                .withContainer(container, "3")
                .withContainer(container, "4")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .change(container.getIdentifier(), "4", "1")
            .change(container.getIdentifier(), "1", "2")
            .change(container.getIdentifier(), "2", "3")
            .change(container.getIdentifier(), "3", "4")
            .build();

        final int pagesNumber = 20;
        final HTMLPageAsset[] pages = new HTMLPageAsset[pagesNumber];

        for (int i = 0; i < pagesNumber; i++) {
            pages[i] = new HTMLPageDataGen(host, template).nextPersisted();
            createMultiTrees(pages[i], container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

        }

        final List<String> pageIds = Arrays.stream(pages)
                .map(page -> page.getIdentifier())
                .sorted(String::compareTo)
                        .collect(Collectors.toList());

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, pageIds, VariantAPI.DEFAULT_VARIANT.name());

        final String getMultiTreeQuery = "SELECT parent1,parent2,child,relation_type FROM multi_tree WHERE parent1 in (?" +
                new String(new char[pageIds.size() - 1]).replace("\0", ", ?") + ") order by parent1";

        final DotConnect dotConnect = new DotConnect().setSQL(getMultiTreeQuery);

        pageIds.stream().forEach(pageId -> dotConnect.addParam(pageId));
        final ArrayList<Map<String, Object>> multiTreeResult = dotConnect.loadResults();
        int multiTreeCurrentIndex = 0;

        for (int i = 0; i < pagesNumber; i++) {

            final Map<String, List<Map<String, Object>>> groupedByInstanceId = getMultiTreeByPageOrderByInstanceId(pageIds.get(i),
                    multiTreeResult, multiTreeCurrentIndex);

            final int size = groupedByInstanceId.values().stream().map(List::size).reduce(0, Integer::sum);
            multiTreeCurrentIndex = multiTreeCurrentIndex + size;

            assertEquals(5, size);

            for (final String intanceId : groupedByInstanceId.keySet()) {
                final List<Map<String, Object>> multiTrees = groupedByInstanceId.get(intanceId);

                switch (intanceId) {
                    case "1":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_4.getIdentifier(), multiTrees.get(0).get("child"));
                        break;
                    case "2":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_1.getIdentifier(), multiTrees.get(0).get("child"));
                        break;
                    case "3":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_2.getIdentifier(), multiTrees.get(0).get("child"));
                        break;
                    case "4":
                        assertEquals(2, multiTrees.size());

                        List<String> contentlets = multiTrees.stream().map(multiTree -> multiTree.get("child").toString())
                                .collect(Collectors.toList());
                        assertTrue(contentlets.contains(contentlet_3.getIdentifier()));
                        assertTrue(contentlets.contains(contentlet_5.getIdentifier()));
                        break;
                    default:
                        throw new AssertionError("UUID not expected: " + intanceId);
                }
            }
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have 100 Page with 4 containers all of them are different instances of the same containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * Should: Return the list of pages under 100 millis
     *
     * @throws DotDataException
     */
    @Test
    @Ignore
    public void moveContainerUpdateMultiTreesOnMultiplePagesPerformance() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                .withContainer(container, "2")
                .withContainer(container, "3")
                .withContainer(container, "4")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .change(container.getIdentifier(), "4", "1")
            .change(container.getIdentifier(), "1", "2")
            .change(container.getIdentifier(), "2", "3")
            .change(container.getIdentifier(), "3", "4")
            .build();

        final int pagesNumber = 100;
        final HTMLPageAsset[] pages = new HTMLPageAsset[pagesNumber];

        for (int i = 0; i < pagesNumber; i++) {
            pages[i] = new HTMLPageDataGen(host, template).nextPersisted();
            createMultiTrees(pages[i], container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

        }

        final List<String> pageIds = Arrays.stream(pages)
                .map(page -> page.getIdentifier())
                .sorted(String::compareTo)
                .collect(Collectors.toList());

        long start = System.currentTimeMillis();
        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, pageIds, VariantAPI.DEFAULT_VARIANT.name());
        long end = System.currentTimeMillis();

        assertTrue((end - start) < 100);
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have 20 Pages with 4 containers all of them are different instances of the same containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * And you remove the instance 1
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : removed
     * - Contentlet_2 : Add to the instance 1
     * - Contentlet_3 : Add to the instance 2
     * - Contentlet_5 : Add to the instance 2
     * - Contentlet_4 : Add to the instance 3
     *
     * @throws DotDataException
     */
    @Test
    public void removeContainerUpdateMultiTreesMultiPages() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                .withContainer(container, "2")
                .withContainer(container, "3")
                .withContainer(container, "4")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final int pagesNumber = 20;
        final HTMLPageAsset[] pages = new HTMLPageAsset[pagesNumber];

        for (int i = 0; i < pagesNumber; i++) {
            pages[i] = new HTMLPageDataGen(host, template).nextPersisted();
            createMultiTrees(pages[i], container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

        }

        final List<String> pageIds = Arrays.stream(pages)
                .map(page -> page.getIdentifier())
                .sorted(String::compareTo)
                .collect(Collectors.toList());

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .remove(container.getIdentifier(), "1")
            .change(container.getIdentifier(), "2", "1")
            .change(container.getIdentifier(), "3", "2")
            .change(container.getIdentifier(), "4", "3")
            .build();

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, pageIds, VariantAPI.DEFAULT_VARIANT.name());

        final String getMultiTreeQuery = "SELECT parent1,parent2,child,relation_type FROM multi_tree WHERE parent1 in (?" +
                new String(new char[pageIds.size() - 1]).replace("\0", ", ?") + ") order by parent1";

        final DotConnect dotConnect = new DotConnect().setSQL(getMultiTreeQuery);

        pageIds.stream().forEach(pageId -> dotConnect.addParam(pageId));

        final ArrayList<Map<String, Object>> multiTreeResult = dotConnect.loadResults();
        int multiTreeCurrentIndex = 0;

        for (int i = 0; i < pagesNumber; i++) {

            final Map<String, List<Map<String, Object>>> groupedByInstanceId = getMultiTreeByPageOrderByInstanceId(pageIds.get(i), multiTreeResult,
                    multiTreeCurrentIndex);

            final int size = groupedByInstanceId.values().stream().map(List::size).reduce(0, Integer::sum);
            multiTreeCurrentIndex = multiTreeCurrentIndex + size;

            assertEquals(4, size);

            for (final String intanceId : groupedByInstanceId.keySet()) {
                final List<Map<String, Object>> multiTrees = groupedByInstanceId.get(intanceId);

                switch (intanceId) {
                    case "1":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_2.getIdentifier(), multiTrees.get(0).get("child").toString());
                        break;
                    case "2":
                        assertEquals(2, multiTrees.size());

                        List<String> contentlets = multiTrees.stream().map(multiTree -> multiTree.get("child").toString())
                                .collect(Collectors.toList());
                        assertTrue(contentlets.contains(contentlet_3.getIdentifier()));
                        assertTrue(contentlets.contains(contentlet_5.getIdentifier()));
                        break;
                    case "3":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_4.getIdentifier(), multiTrees.get(0).get("child").toString());
                        break;
                    default:
                        throw new AssertionError("UUID not expected: " + intanceId);
                }
            }
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have a Page with 5 containers instances from 3 different containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1, Container 1
     * - Contentlet_2 : Add to the instance 1, Container 2
     * - Contentlet_3 : Add to the instance 2, Container 2
     * - Contentlet_4 : Add to the instance 1, Container 3
     * - Contentlet_5 : Add to the instance 2, Container 1
     *
     * And you move the Inatnce 2 of the Container 1  for to be the first one
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_5 : Add to the instance 1, Container 1
     * - Contentlet_1 : Add to the instance 2, Container 1
     * - Contentlet_2 : Add to the instance 1, Container 2
     * - Contentlet_3 : Add to the instance 2, Container 2
     * - Contentlet_4 : Add to the instance 1, Container 3

     *
     * @throws DotDataException
     */
    @Test
    public void moveContainersUpdateMultiTrees() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().nextPersisted();
        final Container container_3 = new ContainerDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container_1, "1")
                .withContainer(container_2, "1")
                .withContainer(container_2, "2")
                .withContainer(container_3, "1")
                .withContainer(container_1, "2")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setInstanceID("1")
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setInstanceID("1")
                .setContentlet(contentlet_2)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setInstanceID("2")
                .setContentlet(contentlet_3)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_3)
                .setInstanceID("1")
                .setContentlet(contentlet_5)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setInstanceID("2")
                .setContentlet(contentlet_4)
                .nextPersisted();

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .change(container_1.getIdentifier(), "2", "1")
            .change(container_1.getIdentifier(), "1", "2")
             .build();

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, list(page.getIdentifier()), VariantAPI.DEFAULT_VARIANT.name());

        final List<MultiTree> multiTreesFromDB = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
        assertEquals(5, multiTreesFromDB.size());

        for (MultiTree multiTree : multiTreesFromDB) {
            if (multiTree.getContainer().equals(container_1.getIdentifier()) && multiTree.getRelationType().equals("1")) {
                assertEquals(contentlet_4.getIdentifier(), multiTree.getContentlet());
            } else if (multiTree.getContainer().equals(container_1.getIdentifier()) && multiTree.getRelationType().equals("2")) {
                assertEquals(contentlet_1.getIdentifier(), multiTree.getContentlet());
            } else if (multiTree.getContainer().equals(container_2.getIdentifier()) && multiTree.getRelationType().equals("1")) {
                assertEquals(contentlet_2.getIdentifier(), multiTree.getContentlet());
            }else if (multiTree.getContainer().equals(container_2.getIdentifier()) && multiTree.getRelationType().equals("2")) {
                assertEquals(contentlet_3.getIdentifier(), multiTree.getContentlet());
            } else if (multiTree.getContainer().equals(container_3.getIdentifier()) && multiTree.getRelationType().equals("1")) {
                assertEquals(contentlet_5.getIdentifier(), multiTree.getContentlet());
            }else {
                throw new AssertionError("UUID not expected: Container " + multiTree.getContainer() + " UUID " + multiTree.getRelationType());
            }
        }

    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have a Page with 5 containers instances from 3 different containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1, Container 1
     * - Contentlet_2 : Add to the instance 1, Container 2
     * - Contentlet_3 : Add to the instance 2, Container 2
     * - Contentlet_4 : Add to the instance 1, Container 3
     * - Contentlet_5 : Add to the instance 2, Container 1
     *
     * And you remove the instance 1 of COntainer 1
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : Removed
     * - Contentlet_2 : Add to the instance 1, Container 2
     * - Contentlet_3 : Add to the instance 2, Container 2
     * - Contentlet_4 : Add to the instance 1, Container 3
     * - Contentlet_5 : Add to the instance 2, Container 1
     *
     * @throws DotDataException
     */
    @Test
    public void removeContainersUpdateMultiTrees() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().nextPersisted();
        final Container container_3 = new ContainerDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container_1, "1")
                .withContainer(container_2, "1")
                .withContainer(container_2, "2")
                .withContainer(container_3, "1")
                .withContainer(container_1, "2")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setInstanceID("1")
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setInstanceID("1")
                .setContentlet(contentlet_2)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setInstanceID("2")
                .setContentlet(contentlet_3)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_3)
                .setInstanceID("1")
                .setContentlet(contentlet_4)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setInstanceID("2")
                .setContentlet(contentlet_5)
                .nextPersisted();

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .remove(container_1.getIdentifier(), "1")
            .change(container_1.getIdentifier(), "2", "1")
            .build();

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, list(page.getIdentifier()), VariantAPI.DEFAULT_VARIANT.name());

        final List<MultiTree> multiTreesFromDB = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
        assertEquals(4, multiTreesFromDB.size());

        for (MultiTree multiTree : multiTreesFromDB) {
            if (multiTree.getContainer().equals(container_1.getIdentifier()) && multiTree.getRelationType().equals("1")) {
                assertEquals(contentlet_5.getIdentifier(), multiTree.getContentlet());
            } else if (multiTree.getContainer().equals(container_2.getIdentifier()) && multiTree.getRelationType().equals("1")) {
                assertEquals(contentlet_2.getIdentifier(), multiTree.getContentlet());
            } else if (multiTree.getContainer().equals(container_2.getIdentifier()) && multiTree.getRelationType().equals("2")) {
                assertEquals(contentlet_3.getIdentifier(), multiTree.getContentlet());
            } else if (multiTree.getContainer().equals(container_3.getIdentifier()) && multiTree.getRelationType().equals("1")) {
                assertEquals(contentlet_4.getIdentifier(), multiTree.getContentlet());
            } else {
                throw new AssertionError("UUID not expected: Container " + multiTree.getContainer() + " UUID " + multiTree.getRelationType());
            }
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have 20 Page with 5 containers instances from 3 different containers,
     * and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1, Container 1
     * - Contentlet_2 : Add to the instance 1, Container 2
     * - Contentlet_3 : Add to the instance 2, Container 2
     * - Contentlet_4 : Add to the instance 1, Container 3
     * - Contentlet_5 : Add to the instance 2, Container 1
     *
     * And you remove the instance 1 of COntainer 1
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : Removed
     * - Contentlet_2 : Add to the instance 1, Container 2
     * - Contentlet_3 : Add to the instance 2, Container 2
     * - Contentlet_4 : Add to the instance 1, Container 3
     * - Contentlet_5 : Add to the instance 2, Container 1
     *
     * @throws DotDataException
     */
    @Test
    public void removeContainersUpdateMultiTreesMultiPages() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().nextPersisted();
        final Container container_3 = new ContainerDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container_1, "1")
                .withContainer(container_2, "1")
                .withContainer(container_2, "2")
                .withContainer(container_3, "1")
                .withContainer(container_1, "2")
                .next();

        final Template template = new TemplateDataGen()
                .drawed(true)
                .drawedBody(templateLayout)
                .nextPersisted();

        final int pagesNumber = 20;
        final HTMLPageAsset[] pages = new HTMLPageAsset[pagesNumber];

        for (int i = 0; i < pagesNumber; i++) {
            pages[i] = new HTMLPageDataGen(host, template).nextPersisted();

            new MultiTreeDataGen()
                    .setPage(pages[i])
                    .setContainer(container_1)
                    .setInstanceID("1")
                    .setContentlet(contentlet_1)
                    .nextPersisted();

            new MultiTreeDataGen()
                    .setPage(pages[i])
                    .setContainer(container_2)
                    .setInstanceID("1")
                    .setContentlet(contentlet_2)
                    .nextPersisted();

            new MultiTreeDataGen()
                    .setPage(pages[i])
                    .setContainer(container_2)
                    .setInstanceID("2")
                    .setContentlet(contentlet_3)
                    .nextPersisted();

            new MultiTreeDataGen()
                    .setPage(pages[i])
                    .setContainer(container_3)
                    .setInstanceID("1")
                    .setContentlet(contentlet_4)
                    .nextPersisted();

            new MultiTreeDataGen()
                    .setPage(pages[i])
                    .setContainer(container_1)
                    .setInstanceID("2")
                    .setContentlet(contentlet_5)
                    .nextPersisted();
        }

        final List<String> pageIds = Arrays.stream(pages)
                .map(page -> page.getIdentifier())
                .sorted(String::compareTo)
                .collect(Collectors.toList());

        final LayoutChanges layoutChanges = new LayoutChanges.Builder()
            .remove(container_1.getIdentifier(), "1")
            .change(container_1.getIdentifier(), "2", "1")
            .build();

        APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, pageIds, VariantAPI.DEFAULT_VARIANT.name());


        final String getMultiTreeQuery = "SELECT parent1,parent2,child,relation_type FROM multi_tree WHERE parent1 in (?" +
                new String(new char[pageIds.size() - 1]).replace("\0", ", ?") + ") order by parent1";

        final DotConnect dotConnect = new DotConnect().setSQL(getMultiTreeQuery);
        pageIds.stream().forEach(pageId -> dotConnect.addParam(pageId));

        final ArrayList<Map<String, Object>> multiTreeResult = dotConnect.loadResults();
        int multiTreeCurrentIndex = 0;

        for (int i = 0; i < pagesNumber; i++) {

            final List<Map<String, Object>> multiTress = getMultiTreeByPageOrder(pageIds.get(i), multiTreeResult,
                    multiTreeCurrentIndex);

            multiTreeCurrentIndex += multiTress.size();

            assertEquals(4, multiTress.size());

            for (final Map<String, Object> multiTree : multiTress) {

                if (multiTree.get("parent2").equals(container_1.getIdentifier()) && multiTree.get("relation_type").equals("1")) {
                    assertEquals(contentlet_5.getIdentifier(), multiTree.get("child"));
                } else if (multiTree.get("parent2").equals(container_2.getIdentifier()) && multiTree.get("relation_type").equals("1")) {
                    assertEquals(contentlet_2.getIdentifier(), multiTree.get("child"));
                } else if (multiTree.get("parent2").equals(container_2.getIdentifier()) && multiTree.get("relation_type").equals("2")) {
                    assertEquals(contentlet_3.getIdentifier(), multiTree.get("child"));
                } else if (multiTree.get("parent2").equals(container_3.getIdentifier()) && multiTree.get("relation_type").equals("1")) {
                    assertEquals(contentlet_4.getIdentifier(), multiTree.get("child"));
                } else {
                    throw new AssertionError("UUID not expected: Container " + multiTree.get("parent2") + " UUID " + multiTree.get("relation_type"));
                }
            }
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have disabled the DELETE_ORPHANED_CONTENTS_FROM_CONTAINER flag, and You have a Page with 4 containers
     * all of them are different instances of the same containers, and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * And you remove the instance 1
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : ORPHANED
     * - Contentlet_2 : Add to the instance 1
     * - Contentlet_3 : Add to the instance 2
     * - Contentlet_5 : Add to the instance 2
     * - Contentlet_4 : Add to the instance 3
     *
     * @throws DotDataException
     */
    @Test
    public void removeContainerUpdateMultiTreesWithOrphaned() throws DotDataException {

        boolean deleteOrphanedContentsFromContainer = Config.getBooleanProperty("DELETE_ORPHANED_CONTENTS_FROM_CONTAINER", true);
        MultiTreeAPIImpl.setDeleteOrphanedContentsFromContainer(false);

        try {
            final Host host = new SiteDataGen().nextPersisted();
            final Container container = new ContainerDataGen().nextPersisted();
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();

            final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                    .withContainer(container, "2")
                    .withContainer(container, "3")
                    .withContainer(container, "4")
                    .next();

            final Template template = new TemplateDataGen()
                    .drawed(true)
                    .drawedBody(templateLayout)
                    .nextPersisted();

            final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

            createMultiTrees(page, container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

            final LayoutChanges layoutChanges = new LayoutChanges.Builder()
                .remove(container.getIdentifier(), "1")
                .change(container.getIdentifier(), "2", "1")
                .change(container.getIdentifier(), "3", "2")
                .change(container.getIdentifier(), "4", "3")
                .build();

            APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, list(page.getIdentifier()), VariantAPI.DEFAULT_VARIANT.name());

            final List<MultiTree> multiTreesFromDB = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
            assertEquals(5, multiTreesFromDB.size());

            final Map<String, List<MultiTree>> groupedByInstanceId = multiTreesFromDB.stream()
                    .collect(Collectors.groupingBy(MultiTree::getRelationType));

            for (final String intanceId : groupedByInstanceId.keySet()) {
                final List<MultiTree> multiTrees = groupedByInstanceId.get(intanceId);

                switch (intanceId) {
                    case "-1":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_1.getIdentifier(), multiTrees.get(0).getContentlet());
                        break;
                    case "1":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_2.getIdentifier(), multiTrees.get(0).getContentlet());
                        break;
                    case "2":
                        assertEquals(2, multiTrees.size());

                        List<String> contentlets = multiTrees.stream().map(multiTree -> multiTree.getContentlet())
                                .collect(Collectors.toList());
                        assertTrue(contentlets.contains(contentlet_3.getIdentifier()));
                        assertTrue(contentlets.contains(contentlet_5.getIdentifier()));
                        break;
                    case "3":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_4.getIdentifier(), multiTrees.get(0).getContentlet());
                        break;
                    default:
                        throw new AssertionError("UUID not expected: " + intanceId);
                }
            }
        } finally {
            MultiTreeAPIImpl.setDeleteOrphanedContentsFromContainer(deleteOrphanedContentsFromContainer);
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#updateMultiTrees(String, LayoutChanges)}
     * When: You have enabled the DELETE_ORPHANED_CONTENTS_FROM_CONTAINER flag, and You have a Page with 4 containers
     * all of them are different instances of the same containers, and you have 5 Contentlets add as follows:
     * - Contentlet_1 : Add to the instance 1
     * - Contentlet_2 : Add to the instance 2
     * - Contentlet_3 : Add to the instance 3
     * - Contentlet_5 : Add to the instance 3
     * - Contentlet_4 : Add to the instance 4
     *
     * And you remove the instance 1
     *
     * Should: The Contentlets should finish as:
     *
     * - Contentlet_1 : ORPHANED
     * - Contentlet_2 : Add to the instance 1
     * - Contentlet_3 : Add to the instance 2
     * - Contentlet_5 : Add to the instance 2
     * - Contentlet_4 : Add to the instance 3
     *
     *  Also the page had a Contentlet_6 orphaned content
     *
     * @throws DotDataException
     */
    @Test
    public void removeContainerUpdateMultiTreesWhenAlreadyExistsOrphaned() throws DotDataException {

        boolean deleteOrphanedContentsFromContainer = Config.getBooleanProperty("DELETE_ORPHANED_CONTENTS_FROM_CONTAINER", true);
        MultiTreeAPIImpl.setDeleteOrphanedContentsFromContainer(false);

        try {
            final Host host = new SiteDataGen().nextPersisted();
            final Container container = new ContainerDataGen().nextPersisted();
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_5 = new ContentletDataGen(contentType).nextPersisted();
            final Contentlet contentlet_6 = new ContentletDataGen(contentType).nextPersisted();

            final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container, "1")
                    .withContainer(container, "2")
                    .withContainer(container, "3")
                    .withContainer(container, "4")
                    .next();

            final Template template = new TemplateDataGen()
                    .drawed(true)
                    .drawedBody(templateLayout)
                    .nextPersisted();

            final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

            createMultiTrees(page, container, contentlet_1, contentlet_2, contentlet_3, contentlet_4, contentlet_5);

            new MultiTreeDataGen()
                    .setPage(page)
                    .setContainer(container)
                    .setInstanceID("-1")
                    .setContentlet(contentlet_6)
                    .nextPersisted();


            final LayoutChanges layoutChanges = new LayoutChanges.Builder()
                .remove(container.getIdentifier(), "1")
                .change(container.getIdentifier(), "2", "1")
                .change(container.getIdentifier(), "3", "2")
                .change(container.getIdentifier(), "4", "3")
                .build();

            APILocator.getMultiTreeAPI().updateMultiTrees(layoutChanges, list(page.getIdentifier()), VariantAPI.DEFAULT_VARIANT.name());

            final List<MultiTree> multiTreesFromDB = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
            assertEquals(6, multiTreesFromDB.size());

            final Map<String, List<MultiTree>> groupedByInstanceId = multiTreesFromDB.stream()
                    .collect(Collectors.groupingBy(MultiTree::getRelationType));

            for (final String intanceId : groupedByInstanceId.keySet()) {
                final List<MultiTree> multiTrees = groupedByInstanceId.get(intanceId);

                switch (intanceId) {
                    case "-1":
                        assertEquals(2, multiTrees.size());

                        List<String> contentlets_1 = multiTrees.stream().map(multiTree -> multiTree.getContentlet())
                                .collect(Collectors.toList());

                        assertTrue(contentlets_1.contains(contentlet_1.getIdentifier()));
                        assertTrue(contentlets_1.contains(contentlet_6.getIdentifier()));
                        break;
                    case "1":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_2.getIdentifier(), multiTrees.get(0).getContentlet());
                        break;
                    case "2":
                        assertEquals(2, multiTrees.size());

                        List<String> contentlets_2 = multiTrees.stream().map(multiTree -> multiTree.getContentlet())
                                .collect(Collectors.toList());
                        assertTrue(contentlets_2.contains(contentlet_3.getIdentifier()));
                        assertTrue(contentlets_2.contains(contentlet_5.getIdentifier()));
                        break;
                    case "3":
                        assertEquals(1, multiTrees.size());
                        assertEquals(contentlet_4.getIdentifier(), multiTrees.get(0).getContentlet());
                        break;
                    default:
                        throw new AssertionError("UUID not expected: " + intanceId);
                }
            }
        } finally {
            MultiTreeAPIImpl.setDeleteOrphanedContentsFromContainer(deleteOrphanedContentsFromContainer);
        }
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#}
     *
     * When: A page is using an Advance Template with the follow code:
     *
     * #parseContainer('[Container_id]');
     *
     * Pay attention to the fact that the parseContainer function only takes one parameter.
     * A Contentlet is then added to this Container.
     *
     * Should: return one MultiTree and one Container Instance for this page
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getPageMultiTreesWithAdvanceTemplate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Container container = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .host(host)
                .body("#parseContainer('" + container.getIdentifier() + "')")
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet)
                .setInstanceID("dotParser_1")
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees = APILocator.getMultiTreeAPI()
                .getPageMultiTrees(htmlPageAsset, false);

        final Map<String, Set<PersonalizedContentlet>> contentletsOrderByUUID = pageMultiTrees.row(container.getIdentifier());

        assertEquals(1, contentletsOrderByUUID.size());

        Set<PersonalizedContentlet> contentlets = contentletsOrderByUUID.get("dotParser_1");

        assertEquals(1, contentlets.size());
        assertEquals(contentlet.getIdentifier(), contentlets.iterator().next().getContentletId());
    }

    /**
     * Method to test: {@link MultiTreeAPIImpl#}
     *
     * When: A page is using an Advance Template with the follow code:
     *
     * #parseContainer('[Container_id]');
     *
     * Pay attention to the fact that the parseContainer function only takes one parameter.
     * Container_id is a File Container
     * A Contentlet is then added to this Container.
     *
     * Should: return one MultiTree and one Container Instance for this page
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getPageMultiTreesWithAdvanceTemplateAndFileContainer() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        FileAssetContainer container = new ContainerAsFileDataGen()
                .contentType(contentType, "")
                .nextPersisted();


        container = (FileAssetContainer) APILocator.getContainerAPI()
                .find(container.getInode(), APILocator.systemUser(), true);


        final Template template = new TemplateDataGen()
                .host(host)
                .body("#parseContainer('" + container.getPath() + "')")
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet)
                .setInstanceID("dotParser_1")
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees = APILocator.getMultiTreeAPI()
                .getPageMultiTrees(htmlPageAsset, false);

        final Map<String, Set<PersonalizedContentlet>> contentletsOrderByUUID = pageMultiTrees.row(container.getIdentifier());

        assertEquals(1, contentletsOrderByUUID.size());

        Set<PersonalizedContentlet> contentlets = contentletsOrderByUUID.get("dotParser_1");

        assertEquals(1, contentlets.size());
        assertEquals(contentlet.getIdentifier(), contentlets.iterator().next().getContentletId());
    }

}
