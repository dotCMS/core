package com.dotmarketing.portlets.categories.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.HierarchedCategory;
import com.dotmarketing.portlets.categories.model.HierarchyShortCategory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.bytebuddy.utility.RandomString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by Jonathan Gamba
 * Date: 4/8/13
 */
public class CategoryAPITest extends IntegrationTestBase {

    private static User user;
    private static Host defaultHost;
    private static ContentTypeAPIImpl contentTypeApi;
    private static CategoryAPI categoryAPI;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        HostAPI hostAPI = APILocator.getHostAPI();

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        defaultHost = hostAPI.findDefaultHost( user, false );
        contentTypeApi  = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
        categoryAPI = APILocator.getCategoryAPI();
    }


    /**
     * Testing {@link CategoryAPI#findTopLevelCategories(User, boolean, int, int, String, String)},
     * {@link CategoryAPI#findTopLevelCategories(User, boolean)} and {@link CategoryAPI#findTopLevelCategories(User, boolean, String)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void findTopLevelCategories() throws DotSecurityException, DotDataException {

        //***************************************************************
        int start = 0;
        int count = 10;//TODO: A -1 or 0 wont work in order to request all que records
        String filter = null;
        String sort = null;
        
        //Test the category API
        PaginatedCategories categories = categoryAPI.findTopLevelCategories(user, false, start, count, filter, sort);

        //Apply some validations
        assertNotNull(categories.getCategories());
        assertFalse(categories.getCategories().isEmpty());
        assertTrue(categories.getTotalCount() > 0);

        //***************************************************************
        filter = "bike";
        sort = null;

        //Test the category API
        categories = categoryAPI.findTopLevelCategories(user, false, start, count, filter, sort);

        //Apply some validations
        assertNotNull(categories.getCategories());
        assertFalse(categories.getCategories().isEmpty());
        assertTrue(categories.getTotalCount() > 0);

        //***************************************************************
        filter = null;
        sort = "mod_date";

        //Test the category API
        categories = categoryAPI.findTopLevelCategories(user, false, start, count, filter, sort);

        //Apply some validations
        assertNotNull(categories.getCategories());
        assertFalse(categories.getCategories().isEmpty());
        assertTrue(categories.getTotalCount() > 0);

        //***************************************************************
        filter = null;

        //Test the category API
        List<Category> categoriesList = categoryAPI.findTopLevelCategories(user, false, filter);

        //Apply some validations
        assertNotNull(categoriesList);
        assertFalse(categoriesList.isEmpty());
        assertTrue(categoriesList.size() > 0);

        //***************************************************************
        //Test the category API
        categoriesList = categoryAPI.findTopLevelCategories(user, false);

        //Apply some validations
        assertNotNull(categoriesList);
        assertFalse(categoriesList.isEmpty());
        assertTrue(categoriesList.size() > 0);
    }

    /**
     * Testing {@link CategoryAPI#findChildren(User, String, boolean, int, int, String, String)} and
     * {@link CategoryAPI#findChildren(User, String, boolean, String)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void findChildren() throws DotSecurityException, DotDataException {

        final CategoryDataGen rootCategoryDataGen = new CategoryDataGen().setCategoryName("Bikes-"+System.currentTimeMillis()).setKey("Bikes").setKeywords("Bikes").setCategoryVelocityVarName("bikes");
        final Category child1 = new CategoryDataGen().setCategoryName("RoadBike-"+System.currentTimeMillis()).setKey("RoadBike").setKeywords("RoadBike").setCategoryVelocityVarName("roadBike").next();
        final Category child2 = new CategoryDataGen().setCategoryName("MTB-"+System.currentTimeMillis()).setKey("MTB").setKeywords("MTB").setCategoryVelocityVarName("mtb").next();

        final Category root = rootCategoryDataGen.children(child1, child2).nextPersisted();
        System.out.println(root);

        //Find a parent category
        PaginatedCategories categories = categoryAPI.findTopLevelCategories(user, false, 0, 10, "bike", null);

        //Apply some validations
        assertNotNull(categories.getCategories());
        assertFalse(categories.getCategories().isEmpty());
        assertTrue(categories.getTotalCount() > 0);

        String inode = categories.getCategories().get(0).getInode();

        //***************************************************************
        int start = 0;
        int count = 10;//TODO: A -1 or 0 wont work in order to request all que records
        String filter = null;
        String sort = null;

        //Test the category API
        categories = categoryAPI.findChildren(user, inode, false, start, count, filter, sort);

        //Apply some validations
        assertNotNull(categories.getCategories());
        assertFalse(categories.getCategories().isEmpty());
        assertTrue(categories.getTotalCount() > 0);

        //***************************************************************
        filter = "road";
        sort = null;

        //Test the category API
        categories = categoryAPI.findChildren(user, inode, false, start, count, filter, sort);

        //Apply some validations
        assertNotNull(categories.getCategories());
        assertFalse(categories.getCategories().isEmpty());
        assertTrue(categories.getTotalCount() > 0);

        //***************************************************************
        filter = null;
        sort = "mod_date";

        //Test the category API
        categories = categoryAPI.findChildren(user, inode, false, start, count, filter, sort);

        //Apply some validations
        assertNotNull(categories.getCategories());
        assertFalse(categories.getCategories().isEmpty());
        assertTrue(categories.getTotalCount() > 0);

        //***************************************************************
        filter = "road";

        //Test the category API
        List<Category> categoriesList = categoryAPI.findChildren(user, inode, false, filter);

        //Apply some validations
        assertNotNull(categoriesList);
        assertFalse(categoriesList.isEmpty());
        assertTrue(categoriesList.size() > 0);
    }

    /**
     * Testing {@link CategoryAPI#getParents(Categorizable, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see CategoryAPI
     * @see Category
     */
    @Test
    public void getParents () throws Exception {

        Long time = new Date().getTime();

        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        Category parentCategory = null;
        Contentlet contentlet   = null;
        Structure testStructure = null;

        List<Category> categories = new ArrayList<>();

        //***************************************************************
        //Creating new categories

        try {
            //Adding the parent category
            parentCategory = new Category();
            parentCategory.setCategoryName("Movies" + time);
            parentCategory.setKey("movies" + time);
            parentCategory.setCategoryVelocityVarName("movies" + time);
            parentCategory.setSortOrder((String) null);
            parentCategory.setKeywords(null);
            //Saving it
            categoryAPI.save(null, parentCategory, user, false);

            //Creating child categories
            //New Child category
            Category childCategory1 = new Category();
            childCategory1.setCategoryName("Action" + time);
            childCategory1.setKey("action" + time);
            childCategory1.setCategoryVelocityVarName("action" + time);
            childCategory1.setSortOrder((String) null);
            childCategory1.setKeywords(null);
            //Saving it
            categoryAPI.save(parentCategory, childCategory1, user, false);
            categories.add(childCategory1);
            //New Child category
            Category childCategory2 = new Category();
            childCategory2.setCategoryName("Drama" + time);
            childCategory2.setKey("drama" + time);
            childCategory2.setCategoryVelocityVarName("drama" + time);
            childCategory2.setSortOrder((String) null);
            childCategory2.setKeywords(null);
            //Saving it
            categoryAPI.save(parentCategory, childCategory2, user, false);
            categories.add(childCategory2);

            //***************************************************************
            //Verify If we find the parent for the categories we just added categories
            List<Category> parents = categoryAPI.getParents(childCategory1, user, false);
            assertNotNull(parents);
            assertTrue(parents.size() > 0);
            assertEquals(parents.get(0), parentCategory);

            parents = categoryAPI.getParents(childCategory2, user, false);
            assertNotNull(parents);
            assertTrue(parents.size() > 0);
            assertEquals(parents.get(0), parentCategory);

            //***************************************************************
            //Set up a new structure with categories

            //Create the new structure
            testStructure = createStructure(
                    "JUnit Test Categories Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_categories_structure_" + String.valueOf(new Date().getTime()));
            //Add a Text field
            Field textField = new Field("JUnit Test Text", Field.FieldType.TEXT,
                    Field.DataType.TEXT, testStructure, false, true, true, 1, false, false, false);
            FieldFactory.saveField(textField);
            //Add a Category field
            Field categoryField = new Field("JUnit Movies", Field.FieldType.CATEGORY,
                    Field.DataType.TEXT, testStructure, true, true, true, 2, false, false, true);
            categoryField.setValues(parentCategory.getInode());
            FieldFactory.saveField(categoryField);

            //***************************************************************
            //Set up a content for the categories structure
            contentlet = new Contentlet();
            contentlet.setStructureInode(testStructure.getInode());
            contentlet.setHost(defaultHost.getIdentifier());
            contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());

            //Validate if the contenlet is OK
            contentletAPI.validateContentlet(contentlet, categories);

            //Saving the contentlet
            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet = APILocator.getContentletAPI().checkin(contentlet, categories,
                    permissionAPI.getPermissions(contentlet, false, true), user, false);
            APILocator.getVersionableAPI().setLive(contentlet);

            //***************************************************************
            //Verify If we find the parent for these categories
            parents = categoryAPI.getParents(contentlet, user, false);
            assertNotNull(parents);
            assertTrue(parents.size() == 2);
        } finally{
            try {
                if (UtilMethods.isSet(contentlet) && UtilMethods.isSet(contentlet.getIdentifier())) {

                    contentletAPI.destroy(contentlet, user, false);
                }

                if (UtilMethods.isSet(testStructure) && UtilMethods.isSet(testStructure.id())) {
                    contentTypeApi.delete(new StructureTransformer(testStructure).from());
                }

                if (UtilMethods.isSet(categories)) {
                    for (Category category : categories) {
                        categoryAPI.delete(category, user, false);
                    }
                }

                if (UtilMethods.isSet(parentCategory)) {
                    categoryAPI.delete(parentCategory, user, false);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * This method will focus on testing the Category cache
     *
     * @throws Exception
     * @see CategoryAPI
     * @see Category
     * @see CategoryCache
     */
    @Test
    public void verifyCache () throws Exception {

        Long time = new Date().getTime();

        CategoryCache categoryCache = CacheLocator.getCategoryCache();

        List<Category> categories = new ArrayList<>();

        //***************************************************************
        //Creating new categories

        //---------------------------------------------------------------
        //Adding the parent category
        Category parentCategory = new Category();
        parentCategory.setCategoryName( "Movies" + time );
        parentCategory.setKey( "movies" + time );
        parentCategory.setCategoryVelocityVarName( "movies" + time );
        parentCategory.setSortOrder( (String) null );
        parentCategory.setKeywords( null );
        //Saving it
        categoryAPI.save( null, parentCategory, user, false );

        //Verify the cache -> THE SAVE SHOULD ADD NOTHING TO CACHE, JUST THE LOAD
        Category cachedCategory = categoryCache.get( parentCategory.getCategoryId() );
        assertNull( cachedCategory );
        //The find should add the category to the cache
        Category foundCategory = categoryAPI.find( parentCategory.getCategoryId(), user, false );
        assertNotNull( foundCategory );
        //Now it should be in cache
        cachedCategory = categoryCache.get( parentCategory.getCategoryId() );
        assertNotNull( cachedCategory );
        assertEquals( cachedCategory, parentCategory );

        //---------------------------------------------------------------
        //Creating child categories

        //New Child category
        Category childCategory1 = new Category();
        childCategory1.setCategoryName( "Action" + time );
        childCategory1.setKey( "action" + time );
        childCategory1.setCategoryVelocityVarName( "action" + time );
        childCategory1.setSortOrder( (String) null );
        childCategory1.setKeywords( null );
        //Saving it
        categoryAPI.save( parentCategory, childCategory1, user, false );
        categories.add( childCategory1 );

        //Verify the cache -> THE SAVE SHOULD ADD NOTHING TO CACHE, JUST THE LOAD
        cachedCategory = categoryCache.get( childCategory1.getCategoryId() );
        assertNull( cachedCategory );
        //The find should add the category to the cache
        foundCategory = categoryAPI.find( childCategory1.getCategoryId(), user, false );
        assertNotNull( foundCategory );
        //Now it should be in cache
        cachedCategory = categoryCache.get( childCategory1.getCategoryId() );
        assertNotNull( cachedCategory );
        assertEquals( cachedCategory, childCategory1 );

        //---------------------------------------------------------------
        //New Child category
        Category childCategory2 = new Category();
        childCategory2.setCategoryName( "Drama" + time );
        childCategory2.setKey( "drama" + time );
        childCategory2.setCategoryVelocityVarName( "drama" + time );
        childCategory2.setSortOrder( (String) null );
        childCategory2.setKeywords( null );
        //Saving it
        categoryAPI.save( parentCategory, childCategory2, user, false );
        categories.add( childCategory2 );

        //SUB-CATEGORY: Adding another level
        Category subCategory = new Category();
        subCategory.setCategoryName( "Drama_Sublevel1" + time );
        subCategory.setKey( "drama_Sublevel1" + time );
        subCategory.setCategoryVelocityVarName( "dramaSubLevel1" + time );
        subCategory.setSortOrder( (String) null );
        subCategory.setKeywords( null );
        //Saving it
        categoryAPI.save( childCategory2, subCategory, user, false );


        //***************************************************************

        //PARENT CATEGORY
        //Verify If we find the children for the parent category we just added categories
        List<Category> cachedCategories = categoryCache.getChildren( parentCategory );
        assertNull( cachedCategories );//Verify the cache -> We should have nothing on cache at this point
        List<Category> children = categoryAPI.getChildren( parentCategory, user, true );
        assertNotNull( children );
        assertTrue( children.size() > 0 );
        assertTrue( children.size() == 2 );
        //Now it should be something in cache
        cachedCategories = categoryCache.getChildren( parentCategory );
        assertNotNull( cachedCategories );
        assertTrue( cachedCategories.size() == 2 );

        //---------------------------------------------------------------
        //CATEGORY 1
        //Verify If we find the parent for the categories we just added categories
        List<String> cachedCategoriesStr = categoryCache.getParents( childCategory1 );
        assertNotNull( cachedCategories ); //Verify the cache -> when you create a child cat, the parent is load on cache.
        List<Category> parents = categoryAPI.getParents( childCategory1, user, false );
        assertNotNull( parents );
        assertTrue( parents.size() > 0 );
        assertEquals( parents.get( 0 ), parentCategory );
        //Now it should be something in cache
        cachedCategoriesStr = categoryCache.getParents( childCategory1 );
        assertNotNull( cachedCategoriesStr );
        assertTrue( cachedCategoriesStr.size() == 1 );

        //---------------------------------------------------------------
        //CATEGORY 2
        parents = categoryAPI.getParents( childCategory2, user, false );
        assertNotNull( parents );
        assertTrue( parents.size() > 0 );
        assertEquals( parents.get( 0 ), parentCategory );
        //Verify If we find the children for this child category
        cachedCategories = categoryCache.getChildren( childCategory2 );
        assertNull( cachedCategories );//Verify the cache -> We should have nothing on cache at this point
        children = categoryAPI.getChildren( childCategory2, user, true );
        assertNotNull( children );
        assertTrue( children.size() > 0 );
        assertTrue( children.size() == 1 );
        //Now it should be something in cache
        cachedCategories = categoryCache.getChildren( childCategory2 );
        assertNotNull( cachedCategories );
        assertTrue( cachedCategories.size() == 1 );

        //---------------------------------------------------------------
        //SUB-CATEGORY
        //Verify If we find the parent for the sub-category we just added
        cachedCategoriesStr = categoryCache.getParents( subCategory );
        assertNotNull( cachedCategories );//Verify the cache -> when you create a child cat, the parent is load on cache.
        parents = categoryAPI.getParents( subCategory, user, false );
        assertNotNull( parents );
        assertTrue( parents.size() > 0 );
        assertEquals( parents.get( 0 ), childCategory2 );
        //Now it should be something in cache
        cachedCategoriesStr = categoryCache.getParents( subCategory );
        assertNotNull( cachedCategoriesStr );
        assertTrue( cachedCategoriesStr.size() == 1 );

        //***************************************************************
        //Lets add another subcategory to verify we are cleaning the caches

        //SUB-CATEGORY: Adding another subcategory
        Category subCategory2 = new Category();
        subCategory2.setCategoryName( "Drama_Sublevel1_2" + time );
        subCategory2.setKey( "drama_Sublevel1_2" + time );
        subCategory2.setCategoryVelocityVarName( "dramaSubLevel1_2" + time );
        subCategory2.setSortOrder( (String) null );
        subCategory2.setKeywords( null );
        //Saving it
        categoryAPI.save( childCategory2, subCategory2, user, false );

        //Verify the parent of the one we just saved
        parents = categoryAPI.getParents( subCategory2, user, false );
        assertNotNull( parents );
        assertTrue( parents.size() > 0 );
        assertEquals( parents.get( 0 ), childCategory2 );

        //Verify If the children list was updated
        cachedCategories = categoryCache.getChildren( childCategory2 );
        assertNull( cachedCategories );//Verify the cache -> We should have nothing on cache at this point
        children = categoryAPI.getChildren( childCategory2, user, true );
        assertNotNull( children );
        assertTrue( children.size() > 0 );
        assertTrue( children.size() == 2 );
        //Now it should be something in cache
        cachedCategories = categoryCache.getChildren( childCategory2 );
        assertNotNull( cachedCategories );
        assertTrue( cachedCategories.size() == 2 );

        //************************DELETE*********************************
        //Delete the category
        categoryAPI.delete( childCategory2, user, false );
        //Verify the cache
        cachedCategories = categoryCache.getChildren( parentCategory );
        assertNull( cachedCategories );//Verify the cache -> The delete should clean the cache
        children = categoryAPI.getChildren( parentCategory, user, true );
        assertNotNull( children );
        assertTrue( children.size() > 0 );
        assertTrue( children.size() == 1 );

        Category category = categoryCache.get( childCategory2.getCategoryId() );
        assertNull( category );//Shouldn't exits
        cachedCategories = categoryCache.getChildren( childCategory2 );
        assertNull( cachedCategories );//Shouldn't exist
    }

    @Test
    public void testSortChildren() {

        final CategoryCache categoryCache = CacheLocator.getCategoryCache();

        final String categoryAKey = "categoryA";
        final String categoryBKey = "categoryB";
        final String categoryCKey = "categoryC";

        Category parentCategory = null;
        Category childCategoryA = null;
        Category childCategoryB = null;
        Category childCategoryC = null;

        try {
            //Create Parent Category.
            parentCategory = new Category();
            parentCategory.setCategoryName( "Parent Category" );
            parentCategory.setKey( "parent" );
            parentCategory.setCategoryVelocityVarName( "parent" );
            parentCategory.setSortOrder( (String) null );
            parentCategory.setKeywords( null );

            categoryAPI.save( null, parentCategory, user, false );

            Category foundCategory = categoryAPI.find( parentCategory.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create First Child Category.
            childCategoryA = new Category();
            childCategoryA.setCategoryName( "Category A" );
            childCategoryA.setKey( categoryAKey );
            childCategoryA.setCategoryVelocityVarName( "categoryA" );
            childCategoryA.setSortOrder( 1 );
            childCategoryA.setKeywords( null );

            categoryAPI.save( parentCategory, childCategoryA, user, false );

            foundCategory = categoryAPI.find( childCategoryA.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create Second Child Category.
            childCategoryB = new Category();
            childCategoryB.setCategoryName( "Category B" );
            childCategoryB.setKey( categoryBKey );
            childCategoryB.setCategoryVelocityVarName( "categoryB" );
            childCategoryB.setSortOrder( 2 );
            childCategoryB.setKeywords( null );

            categoryAPI.save( parentCategory, childCategoryB, user, false );

            foundCategory = categoryAPI.find( childCategoryB.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create Third Child Category.
            childCategoryC = new Category();
            childCategoryC.setCategoryName( "Category C" );
            childCategoryC.setKey( categoryCKey );
            childCategoryC.setCategoryVelocityVarName( "categoryC" );
            childCategoryC.setSortOrder( 3 );
            childCategoryC.setKeywords( null );

            categoryAPI.save( parentCategory, childCategoryC, user, false );

            foundCategory = categoryAPI.find( childCategoryC.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Check that the original order follows SortOrder value.
            List<Category> children = categoryAPI.findChildren( user, parentCategory.getInode(), false, null );
            assertEquals( 3, children.size() );
            assertEquals( categoryAKey, children.get( 0 ).getKey() );
            assertEquals( categoryBKey, children.get( 1 ).getKey() );
            assertEquals( categoryCKey, children.get( 2 ).getKey() );

            //Reorder.
            childCategoryA.setSortOrder( 3 );
            childCategoryB.setSortOrder( 2);
            childCategoryC.setSortOrder( 1 );

            //Saving all the children.
            categoryAPI.save( parentCategory, childCategoryA, user, false );
            categoryAPI.save( parentCategory, childCategoryB, user, false );
            categoryAPI.save( parentCategory, childCategoryC, user, false );

            assertNull( categoryCache.get( childCategoryA.getCategoryId() ) );
            assertNull( categoryCache.get( childCategoryB.getCategoryId() ) );
            assertNull( categoryCache.get( childCategoryC.getCategoryId() ) );

            //This call will put the children on the cache.
            categoryAPI.sortChildren( parentCategory.getInode() );

            //Check new order.
            assertEquals( Integer.valueOf(3), categoryCache.get( childCategoryA.getCategoryId() ).getSortOrder() );
            assertEquals( Integer.valueOf(2), categoryCache.get( childCategoryB.getCategoryId() ).getSortOrder() );
            assertEquals( Integer.valueOf(1), categoryCache.get( childCategoryC.getCategoryId() ).getSortOrder() );

        } catch ( Exception e ) {
            fail( e.getMessage() );
        } finally {
            try {
                //Deleting Child Categories.
                if ( childCategoryA != null ){
                    categoryAPI.delete( childCategoryA, user, false );
                }
                if ( childCategoryB != null ){
                    categoryAPI.delete( childCategoryB, user, false );
                }
                if ( childCategoryC != null ){
                    categoryAPI.delete( childCategoryC, user, false );
                }
                if ( parentCategory != null ){
                    //Delete Parent Category.
                    categoryAPI.delete( parentCategory, user, false );
                }
            } catch ( Exception e ){
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates an Structure object for a later use in the tests
     *
     * @param name
     * @param structureVelocityVarName
     * @throws com.dotmarketing.exception.DotHibernateException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    protected static Structure createStructure ( String name, String structureVelocityVarName ) throws DotDataException, DotSecurityException {

        PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        //Set up a test folder
        Folder testFolder = APILocator.getFolderAPI().createFolders( "/" + new Date().getTime() + "/", defaultHost, user, false );
        permissionAPI.permissionIndividually( permissionAPI.findParentPermissionable( testFolder ), testFolder, user);

        //Create the structure
        Structure testStructure = new Structure();

        testStructure.setDefaultStructure( false );
        testStructure.setDescription( "JUnit Test Structure Description." );
        testStructure.setHost( defaultHost.getIdentifier() );
        testStructure.setFolder( testFolder.getInode() );
        testStructure.setName( name );
        testStructure.setOwner( user.getUserId() );
        testStructure.setDetailPage( "" );
        testStructure.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
        testStructure.setVelocityVarName( structureVelocityVarName );

        //Saving the structure
        StructureFactory.saveStructure( testStructure );
        CacheLocator.getContentTypeCache().add( testStructure );

        return testStructure;
    }

    @Test
    public void testDuplicatedCategories() {

        Category category = null;

        try {
            //Test varName with proper camel case.
            final String categoryVarName = "categoryVarNameToTest";
            String suggestedCategoryVarName = categoryAPI
                    .suggestVelocityVarName(categoryVarName);

            assertEquals(categoryVarName, suggestedCategoryVarName);

            //Test varName with spaces and 1st letter uppercase.
            final String categoryVarNameWithSpaces = "Category Var Name To Test";
            suggestedCategoryVarName = categoryAPI
                    .suggestVelocityVarName(categoryVarNameWithSpaces);

            assertEquals(categoryVarName, suggestedCategoryVarName);

            //Test varName with spaces and no uppercase.
            final String categoryVarNameWithSpacesNouppercase = "category var name to test";
            suggestedCategoryVarName = categoryAPI
                    .suggestVelocityVarName(categoryVarNameWithSpacesNouppercase);

            assertEquals(categoryVarName, suggestedCategoryVarName);

            //Now lets create a Category to check how we handle duplicated varNames.
            category = new Category();
            category.setCategoryName("Category Var Name To Test");
            category.setKey("categoryNameWithSpaces-1");
            category.setCategoryVelocityVarName(categoryVarName);
            category.setSortOrder((String) null);
            category.setKeywords(null);

            categoryAPI.save(null, category, user, false);

            Category foundCategory = categoryAPI.find(category.getCategoryId(), user, false);
            assertNotNull(foundCategory);

            //suggestVelocityVarName should return {categoryVarName}-1 because {categoryVarName} already exists.
            suggestedCategoryVarName = categoryAPI
                    .suggestVelocityVarName(categoryVarName);

            assertEquals(categoryVarName + "1", suggestedCategoryVarName);

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                if (category != null) {
                    //Delete Parent Category.
                    categoryAPI.delete(category, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void getCategoryTreeUp_hierarchyLevelThree_Success(){

        List<Category> categoriesToDelete = Lists.newArrayList();

        try {
            //Create Parent Category.
            Category parentCategory = new Category();
            parentCategory.setCategoryName( "Parent Category" );
            parentCategory.setKey( "parent" );
            parentCategory.setCategoryVelocityVarName( "parent" );
            parentCategory.setSortOrder( (String) null );
            parentCategory.setKeywords( null );

            categoryAPI.save( null, parentCategory, user, false );
            categoriesToDelete.add(parentCategory);

            //Create First Child Category.
            Category childCategoryA = new Category();
            childCategoryA.setCategoryName( "Category A" );
            childCategoryA.setKey( "categoryA" );
            childCategoryA.setCategoryVelocityVarName( "categoryA" );
            childCategoryA.setSortOrder( 1 );
            childCategoryA.setKeywords( null );

            categoryAPI.save( parentCategory, childCategoryA, user, false );
            categoriesToDelete.add(childCategoryA);

            //Create Second Child Category.
            Category childCategoryB = new Category();
            childCategoryB.setCategoryName( "Category B" );
            childCategoryB.setKey( "categoryB" );
            childCategoryB.setCategoryVelocityVarName( "categoryB" );
            childCategoryB.setSortOrder( 2 );
            childCategoryB.setKeywords( null );

            categoryAPI.save( parentCategory, childCategoryB, user, false );
            categoriesToDelete.add(childCategoryB);

            //Create First Grand-Child Category.
            Category childCategoryA2 = new Category();
            childCategoryA2.setCategoryName( "Category A-2" );
            childCategoryA2.setKey( "categoryA2" );
            childCategoryA2.setCategoryVelocityVarName( "categoryA2" );
            childCategoryA2.setSortOrder( 1 );
            childCategoryA2.setKeywords( null );

            categoryAPI.save( childCategoryA, childCategoryA2, user, false );
            categoriesToDelete.add(childCategoryA2);

            final List<Category> categoryTreeUp = categoryAPI.getCategoryTreeUp
                    (childCategoryA2, user, false);

            // First element should be a Fake Category. We need to start checking on the second element.
            assertEquals("We should have 4 categories", 4, categoryTreeUp.size());
            assertEquals("Second element should be the Parent", parentCategory, categoryTreeUp.get(1));
            assertEquals("Third element should be Child", childCategoryA, categoryTreeUp.get(2));
            assertEquals("Last element should be current category", childCategoryA2, categoryTreeUp.get(3));

        } catch ( Exception e ) {
            fail( e.getMessage() );
        } finally {
            cleanCategories(categoriesToDelete);
        }
    }

    /**
     * Test cases:
     * 1. Creating a new category without a key (with valid varName)
     * 2. Creating a new category with key.
     * 3. Creating a new Category with repeated key.
     * 4. Updating an old category with no key.
     * 5. Updating an old category with new key.
     * 6. Updating an old category with repeated key.
     */
    @Test
    public void checkUniqueKey_severalCases_Success() {

        List<Category> categoriesToDelete = Lists.newArrayList();

        try {
            ///////////////////////////////////////////////////////////////
            //1. Creating a new category without a key (with valid varName)
            ///////////////////////////////////////////////////////////////
            Category newCategoryWithoutKey = new Category();
            newCategoryWithoutKey.setCategoryName("Category Without Key");
            newCategoryWithoutKey.setCategoryVelocityVarName("category-wo-key");

            categoryAPI.save(null, newCategoryWithoutKey, user, false);
            newCategoryWithoutKey = categoryAPI
                    .find(newCategoryWithoutKey.getCategoryId(), user, false);
            categoriesToDelete.add(newCategoryWithoutKey);

            assertNotNull("Category should have a key after API save method.",
                    newCategoryWithoutKey.getKey());

            ///////////////////////////////////////////////////////////////
            //2. Creating a new category with key.
            ///////////////////////////////////////////////////////////////
            final String keyFromCategoryWithKey = "category-w-key-diff-var";

            Category newCategoryWithKey = new Category();
            newCategoryWithKey.setCategoryName("Category With Key");
            newCategoryWithKey.setCategoryVelocityVarName("category-w-key");
            newCategoryWithKey.setKey(keyFromCategoryWithKey);

            categoryAPI.save(null, newCategoryWithKey, user, false);
            newCategoryWithKey = categoryAPI.find(newCategoryWithKey.getCategoryId(), user, false);
            categoriesToDelete.add(newCategoryWithKey);

            assertNotNull("Category should have a key after API save method.",
                    newCategoryWithKey.getKey());
            assertEquals("Category key should be the same because is unique.",
                    keyFromCategoryWithKey, newCategoryWithKey.getKey());

            ///////////////////////////////////////////////////////////////
            //3. Creating a new Category with repeated key.
            ///////////////////////////////////////////////////////////////
            Category newCategoryWithRepeaterKey = new Category();
            newCategoryWithRepeaterKey.setCategoryName("Category With R Key");
            newCategoryWithRepeaterKey.setCategoryVelocityVarName("category-w-r-key");
            newCategoryWithRepeaterKey.setKey(keyFromCategoryWithKey);

            categoryAPI.save(null, newCategoryWithRepeaterKey, user, false);
            newCategoryWithRepeaterKey = categoryAPI
                    .find(newCategoryWithRepeaterKey.getCategoryId(), user, false);
            categoriesToDelete.add(newCategoryWithRepeaterKey);

            assertNotNull("Category should have a key after API save method.",
                    newCategoryWithRepeaterKey.getKey());
            assertNotEquals("Category should have a diff key after API save method.",
                    keyFromCategoryWithKey, newCategoryWithRepeaterKey.getKey());

            ///////////////////////////////////////////////////////////////
            //4. Updating an old category with no key.
            ///////////////////////////////////////////////////////////////
            newCategoryWithoutKey.setKey("");

            categoryAPI.save(null, newCategoryWithoutKey, user, false);
            newCategoryWithoutKey = categoryAPI
                    .find(newCategoryWithoutKey.getCategoryId(), user, false);

            assertNotNull("Category should have a key after API save method.",
                    newCategoryWithoutKey.getKey());

            ///////////////////////////////////////////////////////////////
            //5. Updating an old category with new key.
            ///////////////////////////////////////////////////////////////
            final String newKeyForUpdate = "category-w-n-key";
            newCategoryWithoutKey.setKey(newKeyForUpdate);

            categoryAPI.save(null, newCategoryWithoutKey, user, false);
            newCategoryWithoutKey = categoryAPI
                    .find(newCategoryWithoutKey.getCategoryId(), user, false);

            assertNotNull("Category should have a key after API save method.",
                    newCategoryWithoutKey.getKey());
            assertEquals("Category should be new.", newKeyForUpdate,
                    newCategoryWithoutKey.getKey());

            ///////////////////////////////////////////////////////////////
            //6. Updating an old category with repeated key.
            ///////////////////////////////////////////////////////////////
            newCategoryWithoutKey.setKey(keyFromCategoryWithKey);

            categoryAPI.save(null, newCategoryWithoutKey, user, false);
            newCategoryWithoutKey = categoryAPI
                    .find(newCategoryWithoutKey.getCategoryId(), user, false);

            assertNotNull("Category should have a key after API save method.",
                    newCategoryWithoutKey.getKey());
            assertNotEquals("Category should be another cause key was already in use.",
                    keyFromCategoryWithKey, newCategoryWithoutKey.getKey());

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            cleanCategories(categoriesToDelete);
        }
    }


    @Test
    public void test_Find_Categories_Within_ContentType() throws Exception {
        ContentType contentType = null;
        List<Category> categoriesToDelete = Lists.newArrayList();
        try {
            //Create Parent Category.
            Category parentCategory = new Category();
            parentCategory.setCategoryName("CT-Category-Parent");
            parentCategory.setKey("parent");
            parentCategory.setCategoryVelocityVarName("parent");
            parentCategory.setSortOrder((String) null);
            parentCategory.setKeywords(null);

            categoryAPI.save(null, parentCategory, user, false);
            categoriesToDelete.add(parentCategory);

            //Create First Child Category.
            Category childCategoryA = new Category();
            childCategoryA.setCategoryName("CT-Category-A");
            childCategoryA.setKey("categoryA");
            childCategoryA.setCategoryVelocityVarName("categoryA");
            childCategoryA.setSortOrder(1);
            childCategoryA.setKeywords(null);

            categoryAPI.save(parentCategory, childCategoryA, user, false);
            categoriesToDelete.add(childCategoryA);

            //Second Level Category.
            Category childCategoryA_1 = new Category();
            childCategoryA_1.setCategoryName("CT-Category-A-1");
            childCategoryA_1.setKey("categoryA-1");
            childCategoryA_1.setCategoryVelocityVarName("categoryA-1");
            childCategoryA_1.setSortOrder(1);
            childCategoryA_1.setKeywords(null);

            categoryAPI.save(childCategoryA, childCategoryA_1, user, false);
            categoriesToDelete.add(childCategoryA_1);

            //Create Second Child Category.
            Category childCategoryB = new Category();
            childCategoryB.setCategoryName("CT-Category-B");
            childCategoryB.setKey("categoryB");
            childCategoryB.setCategoryVelocityVarName("categoryB");
            childCategoryB.setSortOrder(2);
            childCategoryB.setKeywords(null);

            categoryAPI.save(parentCategory, childCategoryB, user, false);
            categoriesToDelete.add(childCategoryB);

            //Second Level Category.
            Category childCategoryB_1 = new Category();
            childCategoryB_1.setCategoryName("CT-Category-B-1");
            childCategoryB_1.setKey("categoryB-1");
            childCategoryB_1.setCategoryVelocityVarName("categoryB-1");
            childCategoryB_1.setSortOrder(1);
            childCategoryB_1.setKeywords(null);

            categoryAPI.save(childCategoryB, childCategoryB_1, user, false);
            categoriesToDelete.add(childCategoryB_1);

            contentType = createContentTypeWithCatAndTextField(parentCategory);

            final List<Category> categories = categoryAPI.findCategories(contentType, user);
            assertEquals(5, categories.size());

            categories.forEach( category -> {
                assertTrue(category.getCategoryName().startsWith("CT-Category"));
            });

        } finally {

            try {
                for (final Category category : categoriesToDelete) {
                    categoryAPI.delete(category, user, false);
                }

                if (contentType != null) {
                    contentTypeApi.delete(contentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private ContentType createContentTypeWithCatAndTextField(final Category parentCategory)
            throws DotSecurityException, DotDataException {

        final long time = System.currentTimeMillis();
        final String textFieldVar = "title"+time;
        final String catFieldVar = "eventType"+time;

        final SiteDataGen siteDataGen = new SiteDataGen();
        final Host demoHost = siteDataGen.nextPersisted();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class)
                .name("TestCat" + time)
                .variable("TestCat" + time)
                .host(demoHost.getIdentifier())
                .build();

        type = APILocator.getContentTypeAPI(user).save(type);

        com.dotcms.contenttype.model.field.Field titleField = FieldBuilder.builder(TextField.class)
                .name(textFieldVar)
                .variable(textFieldVar)
                .contentTypeId(type.id())
                .build();

        APILocator.getContentTypeFieldAPI().save(titleField, user);

        com.dotcms.contenttype.model.field.Field catField = FieldBuilder.builder(CategoryField.class)
                .name(catFieldVar)
                .variable(catFieldVar)
                .values(parentCategory.getInode())
                .contentTypeId(type.id())
                .build();

        APILocator.getContentTypeFieldAPI().save(catField, user);

        return APILocator.getContentTypeAPI(user).find(type.inode());
    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a admin user create a top level category
     * ExpectedResult: Category created successfully
     */
    @Test
    public void test_save_createTopLevelCategory_asAdmin_success()
            throws DotSecurityException, DotDataException {
        //Create new Top Level Category
        final String categoryName = "newCategory" + System.currentTimeMillis();
        final Category newCategory = new CategoryDataGen().setCategoryName(categoryName)
                .setCategoryVelocityVarName(categoryName).setKey(categoryName).nextPersisted();
        //Find created Category
        final Category getCategory = categoryAPI.findByKey(categoryName, user,false);
        //Check that the category obtained is the same as the created
        assertNotNull(getCategory);
        assertEquals(categoryName,getCategory.getCategoryName());
    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a admin user create a top level category and a subcategory of it
     * ExpectedResult: Categories created successfully
     */
    @Test
    public void test_save_createSubCategory_asAdmin_success()
            throws DotSecurityException, DotDataException {
        final String parentCategoryName = "newParentCategory" + System.currentTimeMillis();
        final String childCategoryName = "newChildCategory" + System.currentTimeMillis();
        //Create Child Category
        final Category newChildCategory = new CategoryDataGen().setCategoryName(childCategoryName)
                .setCategoryVelocityVarName(childCategoryName).setKey(childCategoryName).next();
        //Create Parent Category
        final Category newParentCategory = new CategoryDataGen().setCategoryName(parentCategoryName)
                .setCategoryVelocityVarName(parentCategoryName).setKey(parentCategoryName).children(newChildCategory).nextPersisted();
        //Find Parent Category
        final Category getParentCategory = categoryAPI.findByKey(parentCategoryName,user,false);
        //Check that was created successfully
        assertNotNull(getParentCategory);
        assertEquals(parentCategoryName,getParentCategory.getCategoryName());
        //Find Child Category
        final Category getChildCategory = categoryAPI.findByKey(childCategoryName,user,false);
        //Check was created successfully
        assertNotNull(getChildCategory);
        assertEquals(childCategoryName,getChildCategory.getCategoryName());
        assertTrue(categoryAPI.isParent(getChildCategory,getParentCategory,user));
    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a limited user, with the required permissions (can add children, publish categories), create a top level category
     * ExpectedResult: Category created successfully
     */
    @Test
    public void test_save_createTopLevelCategory_asLimitedUser_success()
            throws DotSecurityException, DotDataException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();
        //Give Permissions Over the SystemHost Can Add children
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                APILocator.systemHost().getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, true);
        APILocator.getPermissionAPI().save(permissions, APILocator.systemHost(), user, false);
        //Give Permissions Over the Categories
        permissions = new Permission(PermissionableType.CATEGORY.getCanonicalName(),
                APILocator.systemHost().getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH, true);
        APILocator.getPermissionAPI().save(permissions, APILocator.systemHost(), user, false);
        //Create new top level Category as limited user
        final String categoryName = "newCategory" + System.currentTimeMillis();
        final Category newCategory = new Category();
        newCategory.setCategoryName(categoryName);
        newCategory.setCategoryVelocityVarName(categoryName);
        newCategory.setKey(categoryName);

        categoryAPI.save(null,newCategory,limitedUser,false);
        //Find the new Category using the limited user
        final Category getCategory = categoryAPI.findByKey(categoryName,limitedUser,false);
        assertNotNull(getCategory);
        assertEquals(categoryName,getCategory.getCategoryName());
    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a limited user, without the required permission, create a top level category
     * ExpectedResult: Fail to create a top level category
     */
    @Test(expected = DotSecurityException.class)
    public void test_save_createTopLevelCategory_asLimitedUser_withoutPermissions_throwDotSecurityException()
            throws DotSecurityException, DotDataException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();
        //Create new top level Category as limited user
        final String categoryName = "newCategory" + System.currentTimeMillis();
        final Category newCategory = new Category();
        newCategory.setCategoryName(categoryName);
        newCategory.setCategoryVelocityVarName(categoryName);
        newCategory.setKey(categoryName);

        categoryAPI.save(null,newCategory,limitedUser,false);
    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a limited user, with the required permissions, create a top level category
     * and a subcategory of the new top level category
     * ExpectedResult: Categories created successfully
     */
    @Test
    public void test_save_createSubCategory_asLimitedUser_success()
            throws DotSecurityException, DotDataException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //Create new top level Category as admin user
        final String parentCategoryName = "newParentCategory" + System.currentTimeMillis();
        final Category newParentCategory = new Category();
        newParentCategory.setCategoryName(parentCategoryName);
        newParentCategory.setCategoryVelocityVarName(parentCategoryName);
        newParentCategory.setKey(parentCategoryName);

        categoryAPI.save(null,newParentCategory,user,false);
        //Find the new Category using the admin user
        final Category getParentCategory = categoryAPI.findByKey(parentCategoryName,user,false);
        assertNotNull(getParentCategory);
        assertEquals(parentCategoryName,getParentCategory.getCategoryName());

        //Give Permissions Over the Category
        final Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                getParentCategory.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        APILocator.getPermissionAPI().save(permissions, getParentCategory, user, false);

        //Create new sub Category as limited user
        final String childCategoryName = "newChildCategory" + System.currentTimeMillis();
        final Category newChildCategory = new Category();
        newChildCategory.setCategoryName(childCategoryName);
        newChildCategory.setCategoryVelocityVarName(childCategoryName);
        newChildCategory.setKey(childCategoryName);

        categoryAPI.save(getParentCategory,newChildCategory,limitedUser,false);
        //Find the new Category using the limited user
        final Category getChildCategory = categoryAPI.findByKey(childCategoryName,limitedUser,false);
        assertNotNull(getChildCategory);
        assertEquals(childCategoryName,getChildCategory.getCategoryName());
        assertTrue(categoryAPI.isParent(getChildCategory,getParentCategory,limitedUser));
    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a limited user, with the required permissions, create a top level category
     * and a subcategory of the new top level category
     * ExpectedResult: Categories created successfully
     */
    @Test(expected = DotSecurityException.class)
    public void test_save_createSubCategory_asLimitedUser_withoutPermissions_throwDotSecurityException()
            throws DotSecurityException, DotDataException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //Create new top level Category as admin user
        final String parentCategoryName = "newParentCategory" + System.currentTimeMillis();
        final Category newParentCategory = new Category();
        newParentCategory.setCategoryName(parentCategoryName);
        newParentCategory.setCategoryVelocityVarName(parentCategoryName);
        newParentCategory.setKey(parentCategoryName);

        categoryAPI.save(null,newParentCategory,user,false);
        //Find the new Category using the admin user
        final Category getParentCategory = categoryAPI.findByKey(parentCategoryName,user,false);
        assertNotNull(getParentCategory);
        assertEquals(parentCategoryName,getParentCategory.getCategoryName());

        //Create new sub Category as limited user
        final String childCategoryName = "newChildCategory" + System.currentTimeMillis();
        final Category newChildCategory = new Category();
        newChildCategory.setCategoryName(childCategoryName);
        newChildCategory.setCategoryVelocityVarName(childCategoryName);
        newChildCategory.setKey(childCategoryName);

        categoryAPI.save(getParentCategory,newChildCategory,limitedUser,false);
    }

  /**
     * Method to test: {@link CategoryAPI#isParent(Category, Category, User, boolean)}
     * Given scenario: Create a Category with 3 levels of depth:
     *          Parent Category
     *                  Child Category
     *                          Grand Child Category
     *
     *                  And check if the grand child category belongs to the parent category
     * Expected result: true, since the grand child category is a sub sub category
     */
    @Test
    public void test_isParent_givenGrandChildCategoryBelongsToParentCategory_returnTrue()
            throws DotSecurityException, DotDataException {
        final List<Category> categoriesToDelete = Lists.newArrayList();
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        try {
            //Create Parent Category.
            final Category parentCategory = new CategoryDataGen()
                    .setCategoryName("CT-Category-Parent")
                    .setKey("parent")
                    .setCategoryVelocityVarName("parent")
                    .nextPersisted();

            categoriesToDelete.add(parentCategory);

            //Create First Child Category.
            final Category childCategoryA = new CategoryDataGen()
                    .setCategoryName("CT-Category-A")
                    .setKey("categoryA")
                    .setCategoryVelocityVarName("categoryA")
                    .next();

            categoriesToDelete.add(childCategoryA);

            //Second Level Category.
            Category childCategoryA_1 = new CategoryDataGen()
                    .setCategoryName("CT-Category-A-1")
                    .setKey("categoryA-1")
                    .setCategoryVelocityVarName("categoryA-1")
                    .next();

            categoriesToDelete.add(childCategoryA_1);

            categoryAPI.save(parentCategory, childCategoryA, user, false);
            categoryAPI.save(childCategoryA, childCategoryA_1, user, false);

            Assert.assertTrue(categoryAPI.isParent(childCategoryA_1,parentCategory,user,true));


        }finally {
            for (final Category category : categoriesToDelete) {
                categoryAPI.delete(category, user, false);
            }
        }
    }

    /**
     * Method to test: {@link CategoryAPI#isParent(Category, Category, User, boolean)}
     * Given scenario: Create a Category with 3 levels of depth:
     *          Parent Category
     *                  Child Category
     *                          Grand Child Category
     *
     *           Parent Category 2
     *      *                  Child Category 2
     *      *                          Grand Child Category 2
     *
     *                  And check if the grand child category 2 belongs to the parent category
     * Expected result: false
     */
    @Test
    public void test_isParent_givenGrandChildCategoryThatNoBelongsToParentCategory_returnFalse()
            throws DotSecurityException, DotDataException {
        List<Category> categoriesToDelete = Lists.newArrayList();
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        try {
            //Create Parent Category.
            final Category parentCategory = new CategoryDataGen()
                    .setCategoryName("CT-Category-Parent")
                    .setKey("parent")
                    .setCategoryVelocityVarName("parent")
                    .nextPersisted();

            categoriesToDelete.add(parentCategory);

            //Create First Child Category.
            final Category childCategoryA = new CategoryDataGen()
                    .setCategoryName("CT-Category-A")
                    .setKey("categoryA")
                    .setCategoryVelocityVarName("categoryA")
                    .next();

            categoriesToDelete.add(childCategoryA);

            //Second Level Category.
            Category childCategoryA_1 = new CategoryDataGen()
                    .setCategoryName("CT-Category-A-1")
                    .setKey("categoryA-1")
                    .setCategoryVelocityVarName("categoryA-1")
                    .next();

            categoriesToDelete.add(childCategoryA_1);

            categoryAPI.save(parentCategory, childCategoryA, user, false);
            categoryAPI.save(childCategoryA, childCategoryA_1, user, false);

            //Create Parent Category 2.
            final Category parentCategory2 = new CategoryDataGen()
                    .setCategoryName("CT-Category-Parent2")
                    .setKey("parent2")
                    .setCategoryVelocityVarName("parent2")
                    .nextPersisted();

            categoriesToDelete.add(parentCategory2);

            //Create First Child Category 2.
            final Category childCategoryA2 = new CategoryDataGen()
                    .setCategoryName("CT-Category-A2")
                    .setKey("categoryA2")
                    .setCategoryVelocityVarName("categoryA2")
                    .next();

            categoriesToDelete.add(childCategoryA2);

            //Second Level Category 2.
            Category childCategoryA_2 = new CategoryDataGen()
                    .setCategoryName("CT-Category-A-2")
                    .setKey("categoryA-2")
                    .setCategoryVelocityVarName("categoryA-2")
                    .next();

            categoriesToDelete.add(childCategoryA_2);

            categoryAPI.save(parentCategory2, childCategoryA2, user, false);
            categoryAPI.save(childCategoryA2, childCategoryA_2, user, false);

            Assert.assertFalse(categoryAPI.isParent(childCategoryA_2,parentCategory,user,true));


        }finally {
            for (final Category category : categoriesToDelete) {
                categoryAPI.delete(category, user, false);
            }
        }

    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a limited user, with the required permissions (edit over the category), edit a category
     * ExpectedResult: Categories edited successfully
     */
    @Test
    public void test_save_editCategory_asLimitedUser_success()
            throws DotSecurityException, DotDataException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //Create new top level Category as admin user
        final String parentCategoryName = "newParentCategory" + System.currentTimeMillis();
        final Category newParentCategory = new Category();
        newParentCategory.setCategoryName(parentCategoryName);
        newParentCategory.setCategoryVelocityVarName(parentCategoryName);
        newParentCategory.setKey(parentCategoryName);

        categoryAPI.save(null, newParentCategory, user, false);
        //Find the new Category using the admin user
        final Category getCategory = categoryAPI.findByKey(parentCategoryName, user, false);
        assertNotNull(getCategory);
        assertEquals(parentCategoryName, getCategory.getCategoryName());

        //Give Permissions Over the Category
        final Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                getCategory.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        APILocator.getPermissionAPI().save(permissions, getCategory, user, false);

        //Make some changes over the category
        final String editedCategoryName = "editedCategory" +  System.currentTimeMillis();
        getCategory.setCategoryName(editedCategoryName);
        getCategory.setKey(editedCategoryName);
        getCategory.setKeywords(editedCategoryName);
        categoryAPI.save(null,getCategory,limitedUser,false);

        final Category getEditedCategory = categoryAPI.findByKey(editedCategoryName, user, false);
        assertNotNull(getEditedCategory);
        assertEquals(editedCategoryName, getEditedCategory.getCategoryName());

    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As a limited user, with the required permissions (edit over the category), edit a category
     * ExpectedResult: Failed to edit Category, DotSecurityException
     */
    @Test(expected = DotSecurityException.class)
    public void test_save_editCategory_asLimitedUser_withoutPermissions_throwDotSecurityException()
            throws DotSecurityException, DotDataException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //Create new top level Category as admin user
        final String parentCategoryName = "newParentCategory" + System.currentTimeMillis();
        final Category newParentCategory = new Category();
        newParentCategory.setCategoryName(parentCategoryName);
        newParentCategory.setCategoryVelocityVarName(parentCategoryName);
        newParentCategory.setKey(parentCategoryName);

        categoryAPI.save(null, newParentCategory, user, false);
        //Find the new Category using the admin user
        final Category getCategory = categoryAPI.findByKey(parentCategoryName, user, false);
        assertNotNull(getCategory);
        assertEquals(parentCategoryName, getCategory.getCategoryName());

        //Make some changes over the category
        final String editedCategoryName = "editedCategory" +  System.currentTimeMillis();
        getCategory.setCategoryName(editedCategoryName);
        getCategory.setKey(editedCategoryName);
        getCategory.setKeywords(editedCategoryName);
        categoryAPI.save(null,getCategory,limitedUser,false);
    }

    /**
     * Method to test: {@link CategoryAPI#save(Category, Category, User, boolean)}
     * Given Scenario: As an admin create a top level category and give a limited user permissions
     * to edit it. Also give Add Children permissions over the System Host.
     * Try to add a new top level category using the limited user.
     * ExpectedResult: Category created failed because lack of permissions (no Edit over Category under System Host).
     */
    @Test(expected = DotSecurityException.class)
    public void test_save_createTopLevelCategory_asLimitedUser_fail()
            throws DotSecurityException, DotDataException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //Create new top level Category as admin user
        final String parentCategoryName = "newParentCategory" + System.currentTimeMillis();
        final Category newParentCategory = new Category();
        newParentCategory.setCategoryName(parentCategoryName);
        newParentCategory.setCategoryVelocityVarName(parentCategoryName);
        newParentCategory.setKey(parentCategoryName);

        categoryAPI.save(null, newParentCategory, user, false);
        //Find the new Category using the admin user
        final Category getCategory = categoryAPI.findByKey(parentCategoryName, user, false);
        assertNotNull(getCategory);
        assertEquals(parentCategoryName, getCategory.getCategoryName());

        //Give Permissions Over the Category
        Permission permissions = new Permission(PermissionableType.CATEGORY.getCanonicalName(),
                getCategory.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        APILocator.getPermissionAPI().save(permissions, getCategory, user, false);

        //Give Permissions Over the SystemHost Can Add children
        permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                APILocator.systemHost().getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, true);
        APILocator.getPermissionAPI().save(permissions, APILocator.systemHost(), user, false);

        //Create new top level Category as limited user
        final String categoryName = "newCategory" + System.currentTimeMillis();
        final Category newCategory = new Category();
        newCategory.setCategoryName(categoryName);
        newCategory.setCategoryVelocityVarName(categoryName);
        newCategory.setKey(categoryName);

        categoryAPI.save(null, newCategory, limitedUser, false);
    }

    /**
     * Method to test: {@link CategoryAPIImpl#findAll(CategorySearchCriteria, User, boolean)}
     * When: Call the API method
     * Should: it should
     * - Use the {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)} to search the {@link Category}
     * - Use the {@link PermissionAPI#filterCollection(List, int, boolean, User)} method to check permission
     */
    @Test
    public void getAllCategoriesFiltered() throws DotDataException, DotSecurityException {
        final String inode = new RandomString().nextString();
        final String filter = new RandomString().nextString();
        final String orderBy = "category_key";

        final CategorySearchCriteria searchingCriteria = new CategorySearchCriteria.Builder()
                        .rootInode(inode)
                        .direction(OrderDirection.DESC)
                        .orderBy(orderBy)
                        .filter(filter)
                        .limit(10)
                        .build();

        final User user = mock();

        final HierarchedCategory category1 = mock(HierarchedCategory.class);
        final HierarchedCategory category2 = mock(HierarchedCategory.class);
        final HierarchedCategory category3 = mock(HierarchedCategory.class);

        final List<HierarchedCategory> categoriesAfterSearch = list(category1, category2, category3);
        final List<HierarchedCategory> categoriesAfterPermission = list(category1, category2);

        final CategoryFactory categoryFactory = mock();
        when(categoryFactory.findAll(searchingCriteria)).thenReturn(categoriesAfterSearch);

        final PermissionAPI permissionAPI = mock();
        when(permissionAPI.filterCollection(categoriesAfterSearch, PermissionAPI.PERMISSION_READ, false, user))
                .thenReturn(categoriesAfterPermission);

        final CategoryAPI categoryAPI = new CategoryAPIImpl(categoryFactory, permissionAPI);
        PaginatedCategories paginatedCategories = categoryAPI.findAll(searchingCriteria, user, false);

        assertEquals(categoriesAfterPermission.size(), (int) paginatedCategories.getTotalCount());
        assertTrue(categoriesAfterPermission.containsAll(paginatedCategories.getCategories()));
    }

    /**
     * Method to test: {@link CategoryAPIImpl#findAll(CategorySearchCriteria, User, boolean)}
     * When: Create 9 Category, call the two times:
     * first: limit =5, offset =0
     * second: limit =5, offset =5
     *
     * Should: Return All the Categories with the 2 called
     */
    @Test
    public void getAllCategoriesFilteredWithPagination() throws DotDataException, DotSecurityException {
        final String inode = new RandomString().nextString();
        final String filter = new RandomString().nextString();
        final String orderBy = "category_key";

        final CategorySearchCriteria searchingCriteria_1 =
                new CategorySearchCriteria.Builder()
                        .rootInode(inode)
                        .direction(OrderDirection.DESC)
                        .orderBy(orderBy)
                        .filter(filter)
                        .limit(5)
                        .offset(0)
                        .build();

        final CategorySearchCriteria searchingCriteria_2 = new CategorySearchCriteria.Builder()
                        .rootInode(inode)
                        .direction(OrderDirection.DESC)
                        .orderBy(orderBy)
                        .filter(filter)
                        .limit(5)
                        .offset(5)
                        .build();

        final HierarchedCategory category1 = mock(HierarchedCategory.class);
        final HierarchedCategory category2 = mock(HierarchedCategory.class);
        final HierarchedCategory category3 = mock(HierarchedCategory.class);
        final HierarchedCategory category4 = mock(HierarchedCategory.class);
        final HierarchedCategory category5 = mock(HierarchedCategory.class);
        final HierarchedCategory category6 = mock(HierarchedCategory.class);
        final HierarchedCategory category7 = mock(HierarchedCategory.class);
        final HierarchedCategory category8 = mock(HierarchedCategory.class);
        final HierarchedCategory category9 = mock(HierarchedCategory.class);

        final User user = mock();

        final List<HierarchedCategory> categories = list(category1, category2, category3, category4, category5, category6,
                category7, category8, category9);

        final CategoryFactory categoryFactory = mock();
        when(categoryFactory.findAll(searchingCriteria_1)).thenReturn(categories);
        when(categoryFactory.findAll(searchingCriteria_2)).thenReturn(categories);

        final PermissionAPI permissionAPI = mock();
        when(permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, false, user))
                .thenReturn(categories);



        final CategoryAPI categoryAPI = new CategoryAPIImpl(categoryFactory, permissionAPI);
        PaginatedCategories firstPage = categoryAPI.findAll(searchingCriteria_1, user, false);

        assertEquals(9, (int) firstPage.getTotalCount());
        assertEquals(5, firstPage.getCategories().size());
        assertTrue(list(category1, category2, category3, category4, category5).containsAll(firstPage.getCategories()));


        PaginatedCategories secondPage = categoryAPI.findAll(searchingCriteria_2, user, false);

        assertEquals(9, (int) secondPage.getTotalCount());
        assertEquals(4, secondPage.getCategories().size());
        assertTrue(list(category6, category7, category8, category9).containsAll(secondPage.getCategories()));
    }

    /**
     * Method to test: {@link CategoryAPIImpl#findAll(CategorySearchCriteria, User, boolean)}
     * When: Call the API method
     * Should: it should
     * - Use the {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)} to search the {@link Category}
     * - Use the {@link PermissionAPI#filterCollection(List, int, boolean, User)} method to check permission
     */
    @Test
    public void findHierarchy() throws DotDataException, DotSecurityException {
        final String inode = new RandomString().nextString();

        final HierarchyShortCategory category1 = mock(HierarchyShortCategory.class);
        when(category1.getInode()).thenReturn(inode);

        final HierarchyShortCategory category2 = mock(HierarchyShortCategory.class);
        final HierarchyShortCategory category3 = mock(HierarchyShortCategory.class);

        final List<HierarchyShortCategory> categoriesAExpected = list(category2, category3);

        final List<String> inodes = list(category1.getInode());
        final CategoryFactory categoryFactory = mock();
        when(categoryFactory.findHierarchy(inodes)).thenReturn(categoriesAExpected);

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);

        final CategoryAPI categoryAPI = new CategoryAPIImpl(categoryFactory, permissionAPI);
        final List<HierarchyShortCategory> hierarchy = categoryAPI.findHierarchy(inodes);

        assertEquals(categoriesAExpected.size(), hierarchy.size());
        assertTrue(categoriesAExpected.containsAll(hierarchy));

        verify(categoryFactory).findHierarchy(inodes);
    }
}
