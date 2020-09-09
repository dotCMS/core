package com.dotmarketing.factories;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Ruleable;
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
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.startup.runonce.Task04315UpdateMultiTreePK;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
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
                        .setInstanceId(RELATION_TYPE + i);

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
        multiTree.setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT);
        multiTree.setTreeOrder( 1 );
        
        //delete out any previous relation
        APILocator.getMultiTreeAPI().deleteMultiTree(multiTree);
        CacheLocator.getMultiTreeCache().clearCache();
        Table<String, String, Set<PersonalizedContentlet>> trees= APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);
        
        Table<String, String, Set<PersonalizedContentlet>> cachedTrees= APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

        Logger.info(this, "\n\n**** cachedTrees: " + cachedTrees);
        // should be the same object coming from in memory cache
        assert(trees==cachedTrees);

        CacheLocator.getMultiTreeCache().removePageMultiTrees(page.getIdentifier());

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
    multiTree.setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT);
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
        assertTrue("container should have contentlet for keyTag:" + MultiTree.DOT_PERSONALIZATION_DEFAULT, personalizedContentletSet
            .contains(new PersonalizedContentlet(content1.getIdentifier(), MultiTree.DOT_PERSONALIZATION_DEFAULT)));
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
        multiTree1.setInstanceId("1");
        multiTree1.setTreeOrder( 1 );

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list(multiTree1) );
        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list() );

        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(parent1);

        assertEquals(0, multiTrees.size());
    }

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

        List<MultiTree> multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, MultiTree.DOT_PERSONALIZATION_DEFAULT);

        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());

        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, personalization);

        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(1, multiTrees.size());

        multiTrees = multiTreeAPI.copyPersonalizationForPage(htmlPage, MultiTree.DOT_PERSONALIZATION_DEFAULT, newPersonalization);
        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());
        org.junit.Assert.assertEquals(newPersonalization, multiTrees.get(0).getPersonalization());
        org.junit.Assert.assertEquals(newPersonalization, multiTrees.get(1).getPersonalization());

        multiTreeAPI.deletePersonalizationForPage(htmlPage, newPersonalization);
        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, newPersonalization);

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
        org.junit.Assert.assertTrue(personalizationSet.contains(MultiTree.DOT_PERSONALIZATION_DEFAULT));
        org.junit.Assert.assertTrue(personalizationSet.contains(personalization));

        final Set<String> allPersonalizationSet = multiTreeAPI.getPersonalizations();

        org.junit.Assert.assertNotNull(allPersonalizationSet);
        org.junit.Assert.assertTrue(allPersonalizationSet.size() >= 2);
        org.junit.Assert.assertTrue(allPersonalizationSet.contains(MultiTree.DOT_PERSONALIZATION_DEFAULT));
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
        org.junit.Assert.assertTrue(allPersonalizationSet.contains(MultiTree.DOT_PERSONALIZATION_DEFAULT));
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

        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list(multiTree1, multiTree2, multiTree3) );
        APILocator.getMultiTreeAPI().saveMultiTrees( parent1, list(multiTree1) );

        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(parent1);

        assertEquals(2, multiTrees.size());
        assertTrue(multiTrees.contains(multiTree1));
        assertTrue(multiTrees.contains(multiTree3));
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
            .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
            .setTreeOrder(1)
            .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(enContentlet2)
                .setInstanceID(uniqueId)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(espContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
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
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                MultiTree.DOT_PERSONALIZATION_DEFAULT,
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
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(espContentlet)
                .setInstanceID(uniqueId)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
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
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page.getIdentifier(),
                MultiTree.DOT_PERSONALIZATION_DEFAULT,
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
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        final MultiTree newMultiTreeEnContent2 = new MultiTreeDataGen()
                .setPage(page1)
                .setContainer(container)
                .setContentlet(contentlet2)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(2)
                .nextPersisted();

        //
        new MultiTreeDataGen()
                .setPage(page2)
                .setContainer(container)
                .setContentlet(contentlet1)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setPage(page2)
                .setContainer(container)
                .setContentlet(contentlet2)
                .setInstanceID(UUIDGenerator.shorty())
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
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
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .next();

        APILocator.getMultiTreeAPI().overridesMultitreesByPersonalization(
                page1.getIdentifier(),
                MultiTree.DOT_PERSONALIZATION_DEFAULT,
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
    ///
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

        pageMultiTrees.rowKeySet().contains(container.getIdentifier());
        pageMultiTrees.rowKeySet().contains(emptyContainer.getIdentifier());
        pageMultiTrees.rowKeySet().contains(fileAssetContainer.getIdentifier());
        pageMultiTrees.rowKeySet().contains(emptyFileAssetContainer.getIdentifier());
    }

    /**
     * Method to Test: {@link MultiTreeAPI#getPageMultiTrees(IHTMLPage, boolean)}
     * When: Drawed Template with 2 {@link FileAssetContainer} (one empty) and 2 {@link Container} (one empty)
     * Should: Return the 4 containers
     * */
    @Test
    public void testEmptyContainersInDrawedTemplate() throws DotDataException, DotSecurityException {
        final Container container = new ContainerDataGen().nextPersisted();
        FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen().nextPersisted();
        fileAssetContainer = (FileAssetContainer) APILocator.getContainerAPI().find(fileAssetContainer.getInode(), APILocator.systemUser(), false);

        final Container emptyContainer = new ContainerDataGen().nextPersisted();
        FileAssetContainer emptyFileAssetContainer = new ContainerAsFileDataGen().nextPersisted();
        emptyFileAssetContainer = (FileAssetContainer) APILocator.getContainerAPI().find(emptyFileAssetContainer.getInode(), APILocator.systemUser(), false);

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container)
                .withContainer(fileAssetContainer)
                .withContainer(emptyContainer)
                .withContainer(emptyFileAssetContainer)
                .next();

        final Template template = new TemplateDataGen()
                .drawedBody(templateLayout)
                .nextPersisted();

        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        createContentAndMultiTree(container, fileAssetContainer, page);

        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees = APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

        System.out.println("multiTrees = " + pageMultiTrees);

        pageMultiTrees.rowKeySet().contains(container.getIdentifier());
        pageMultiTrees.rowKeySet().contains(emptyContainer.getIdentifier());
        pageMultiTrees.rowKeySet().contains(fileAssetContainer.getIdentifier());
        pageMultiTrees.rowKeySet().contains(emptyFileAssetContainer.getIdentifier());
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
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(fileAssetContainer)
                .setContentlet(contentlet_2)
                .setInstanceID(ContainerUUID.UUID_START_VALUE)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setTreeOrder(1)
                .nextPersisted();
    }
}
