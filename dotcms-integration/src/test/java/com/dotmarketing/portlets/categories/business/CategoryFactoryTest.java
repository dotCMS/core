package com.dotmarketing.portlets.categories.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.APIProvider;
import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCacheAddTestCase;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.dotmarketing.portlets.categories.model.HierarchedCategory;
import com.dotmarketing.portlets.categories.model.HierarchyShortCategory;
import com.dotmarketing.portlets.categories.model.ShortCategory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import graphql.AssertException;
import net.bytebuddy.utility.RandomString;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/***
 * Category Factory Test
 */
public class CategoryFactoryTest extends IntegrationTestBase {

    private static CategoryFactory categoryFactory;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        categoryFactory = FactoryLocator.getCategoryFactory();
    }

    /**
     * Method to test: {@link CategoryFactory#save(Category)}
     * Given Scenario: attempt to save an empty category object
     * ExpectedResult: Should throw a DotDataException
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void Test_Save_Empty_Category_Expect_Exception() throws DotDataException {
        Category category = new Category();
        categoryFactory.save(category);
    }

    /**
     * Method to test: {@link CategoryFactory#save(Category)}
     * Given Scenario: attempt to save an empty category object
     * ExpectedResult: We should be able to find the Category created using an existing inode
     * @throws DotDataException
     */
    @Test
    public void Test_Update_Non_Existing_Category_Expect_Exception() throws DotDataException {
        Category category = newCategory();
        category.setInode("lol-"+System.currentTimeMillis());
        categoryFactory.save(category);
        Category savedCategory = categoryFactory.find(category.getInode());
        categoryFactory.delete(savedCategory);
        assertNull(categoryFactory.find(category.getInode()));
    }

    /**
     * Method to test: {@link CategoryFactory#find(String)}
     * Given Scenario: attempt to find a non existing category
     * ExpectedResult: Should return null
     * @throws DotDataException
     */
    @Test
    public void Test_Find_Non_Existing_Category_By_Id() throws DotDataException {
        Category category = categoryFactory.find("lol-"+System.currentTimeMillis());
        assertNull(category);
    }

    /**
     * Method to test: {@link CategoryFactory#findByKey(String)}
     * Given Scenario: attempt to find a non existing category
     * ExpectedResult: Should return null
     * @throws DotDataException
     */
    @Test
    public void Test_Find_Non_Existing_Category_By_Key() throws DotDataException {
        final Category category = categoryFactory.findByKey("lol-"+System.currentTimeMillis());
        assertNull(category);
    }

    private Category newCategory() throws DotDataException {
        String categoryName = "cat_" + System.currentTimeMillis();
        final Category category = new Category();
        category.setCategoryName(categoryName);
        category.setCategoryVelocityVarName(categoryName);
        category.setSortOrder(1);
        category.setKey("key_"+System.currentTimeMillis());
        category.setDescription("");
        category.setActive(true);
        category.setKeywords("k1,k2,k3");
        categoryFactory.save(category);
        return category;
    }

    /**
     * Method to test: {@link CategoryFactory#save(Category)}
     * Given Scenario: attempt to save a category object then find it in different ways
     * ExpectedResult: Should be found everytime
     * @throws DotDataException
     */
    @Test
    public void Test_Save_Brand_New_Category_Then_Find_It() throws DotDataException {
        final Category category = newCategory();
        assertNotNull(category);
        assertNotNull(category.getInode());

        assertNotNull(categoryFactory.find(category.getInode()));
        assertNotNull(categoryFactory.findByKey(category.getKey()));
        assertNotNull(categoryFactory.findByName(category.getCategoryName()));
        assertNotNull(categoryFactory.findByVar(category.getCategoryVelocityVarName()));
    }

    /**
     * Method to test: {@link CategoryFactory#findAll()}
     * Given Scenario: get all categories then remove them then use find all again
     * ExpectedResult: After having removed all categories whe should expect none
     * @throws DotDataException
     */
    @Test
    public void Test_Find_All_Then_Delete_All() throws DotDataException {
        final Category category = newCategory();
        assertNotNull(category);
        final List<Category> all = categoryFactory.findAll();
        assertFalse(all.isEmpty());

        for(final Category cat:all){
          categoryFactory.delete(cat);
        }

        final List<Category> allAfterDelete = categoryFactory.findAll();
        assertTrue(allAfterDelete.isEmpty());
    }

    /**
     * Method to test: {@link CategoryFactory#getChildren(Categorizable)} {@link CategoryFactory#findChildrenByFilter(String, String, String)}
     * Given Scenario: create a parent-child category hierarchy the test the  child finders
     * ExpectedResult: Once removed the finders should return empty and vice-versa
     * @throws DotDataException
     */
    @Test
    public void Test_Find_Children_Then_Remove_Em() throws DotDataException {
        final Category parent = newCategory();
        assertNotNull(parent);
        final Category child = newCategory();
        assertNotNull(child);
        categoryFactory.addChild(parent, child, null);
        assertFalse(categoryFactory.getChildren(parent).isEmpty());
        assertFalse(categoryFactory.findChildrenByFilter(parent.getInode(), child.getCategoryName(),null).isEmpty());
        categoryFactory.removeChildren(parent);
        assertTrue(categoryFactory.getChildren(parent).isEmpty());
        assertTrue(categoryFactory.findChildrenByFilter(parent.getInode(), child.getCategoryName(),null).isEmpty());
    }

    /**
     * Method to test: {@link CategoryFactory#getParents(Categorizable, String)}
     * Given Scenario: create a parent-child category hierarchy the test the  child finders
     * ExpectedResult: Once removed the finders should return empty and vice-versa
     * @throws DotDataException
     */
    @Test
    public void Test_Find_Parent_Then_Remove_Em() throws DotDataException {
        final Category parent = newCategory();
        assertNotNull(parent);
        final Category child = newCategory();
        assertNotNull(child);
        categoryFactory.addChild(parent, child, null);
        assertFalse(categoryFactory.getParents(child).isEmpty());
        categoryFactory.delete(parent);
        assertTrue(categoryFactory.getParents(child).isEmpty());
    }

    /**
     * Method to test: {@link CategoryFactory#findTopLevelCategories()}
     * Given Scenario: create parent children items call top level finder
     * ExpectedResult: verify top level is found then delete parent call finder again. expect empty collection
     * @throws DotDataException
     */
    @Test
    public void Test_Find_Top_Level_Categories() throws DotDataException {
        final Category parent = newCategory();
        final Category child = newCategory();
        categoryFactory.addChild(parent, child, null);
        assertTrue(categoryFactory.findTopLevelCategories().stream().anyMatch(category -> parent.getInode().equals(category.getInode())));
        assertTrue(categoryFactory.findTopLevelCategories().stream().noneMatch(category -> child.getInode().equals(category.getInode())));
    }

    /**
     * Method to test: {@link CategoryFactory#sortChildren(String)}
     * Given Scenario: create parent children items alter order, save then find
     * ExpectedResult: items should be in natural ascending order
     * @throws DotDataException
     */
    @Test
    public void Test_ReOrder_Categories() throws DotDataException {
        final Category parent = newCategory();
        assertNotNull(parent);
        final Category child1 = newCategory();
        final Category child2 = newCategory();
        final Category child3 = newCategory();

        child1.setSortOrder(3);
        categoryFactory.save(child1);
        child2.setSortOrder(2);
        categoryFactory.save(child2);
        child3.setSortOrder(1);
        categoryFactory.save(child3);

        categoryFactory.sortChildren(parent.getInode());

        final List<Category> children = categoryFactory.getChildren(parent);
        int orderCount = 1;
        for(Category category:children){
            assertEquals(orderCount,category.getSortOrder().intValue());
            switch (orderCount){
                case 1:{
                    assertEquals(child3.getInode(),category.getInode());
                    break;
                }
                case 2:{
                    assertEquals(child2.getInode(),category.getInode());
                    break;
                }
                case 3:{
                    assertEquals(child1.getInode(),category.getInode());
                    break;
                }
            }
            orderCount++;
        }
    }

    /**
     * Method to test: {@link CategoryFactory#getParents(Categorizable, String)}
     * @throws DotDataException
     */ 
    @Test
    public void Test_Get_Parent_Categories() throws DotDataException {

        final Category root = newCategory();
        final Category root2 = newCategory();
        final Category leaf1 = newCategory();
        final Category leaf2 = newCategory();
        final Category leaf3 = newCategory();

        categoryFactory.save(root);

        categoryFactory.addChild(root, leaf1, null);
        categoryFactory.addChild(root2, leaf1, null);

        List<Category> parents = categoryFactory.getParents(leaf1);
        assertEquals(2,parents.size());

        categoryFactory.addChild(leaf1, leaf2, null);
        categoryFactory.addChild(leaf2, leaf3, null);

        parents = categoryFactory.getParents(leaf3);
        assertEquals(1,parents.size());

    }

    /**
     * Method to test: {@link CategoryFactory#hasDependencies(Category)}
     * Given Scenario: create parent test if has dependencies then add a child and test again
     * ExpectedResult: initially the root should not show any dependencies until we add a child
     * @throws DotDataException
     */
    @Test
    public void Test_Has_Dependencies() throws DotDataException {
        final Category root = newCategory();
        assertFalse(categoryFactory.hasDependencies(root));
        final Category leaf1 = newCategory();
        categoryFactory.addChild(root, leaf1, null);
        assertTrue(categoryFactory.hasDependencies(root));
    }


    private static Map<String, Object> createCategories(final String stringToFilterBy) {

        long now = System.currentTimeMillis();

        final Category topLevelCategory_1 = new CategoryDataGen().setCategoryName(now + "Top Level Category " + stringToFilterBy)
                .setKey(now + "top_level_categoria")
                .setCategoryVelocityVarName(now + "top_level_categoria")
                .nextPersisted();

        final Category childCategory_1 = new CategoryDataGen().setCategoryName(now + "Child Category 1")
                .setKey(now + "child_category_1 " + stringToFilterBy)
                .setCategoryVelocityVarName(now + "child_category_1")
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category childCategory_2 = new CategoryDataGen().setCategoryName(now + "Child Category 2")
                .setKey(now + "child_category_2")
                .setCategoryVelocityVarName(now + "child_category_2 " + stringToFilterBy)
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category childCategory_3 = new CategoryDataGen().setCategoryName(now + "Child Category 3 "  + stringToFilterBy)
                .setKey(now + "child_category_3")
                .setCategoryVelocityVarName(now + "child_category_3")
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category childCategory_4 = new CategoryDataGen().setCategoryName(now + "Child Category 4")
                .setKey(now + "child_category_4")
                .setCategoryVelocityVarName(now + "child_category_4")
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category childCategory_6 = new CategoryDataGen().setCategoryName(now + stringToFilterBy + "Child Category 6")
                .setKey(now +"child_category_6")
                .setCategoryVelocityVarName(now +"child_category_6")
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category childCategory_7 = new CategoryDataGen().setCategoryName(now +"Child " + stringToFilterBy + "Category 7")
                .setKey(now +"child_category_7")
                .setCategoryVelocityVarName(now +"child_category_7")
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category grandchildCategory_1 = new CategoryDataGen().setCategoryName(now +"Grand Child Category 1 " + stringToFilterBy)
                .setKey(now +"grand_child_category_1")
                .setCategoryVelocityVarName(now +"grand_child_category_1")
                .parent(childCategory_4)
                .nextPersisted();

        final Category grandchildCategory_2 = new CategoryDataGen().setCategoryName(now +"Grand Child Category 2 " + stringToFilterBy)
                .setKey(now +"grand_child_category_2")
                .setCategoryVelocityVarName(now +"grand_child_category_2")
                .parent(grandchildCategory_1)
                .nextPersisted();

        final Category topLevelCategory_2 = new CategoryDataGen().setCategoryName(now +"Top Level Category "  + stringToFilterBy)
                .setKey(now +"top_level_category_2")
                .setCategoryVelocityVarName(now +"top_level_category_2")
                .nextPersisted();

        final Category childCategory_5 = new CategoryDataGen().setCategoryName(now +"Child Category 5"  + stringToFilterBy)
                .setKey(now +"child_category_5 " + stringToFilterBy)
                .setCategoryVelocityVarName(now +"child_category_5 " + stringToFilterBy)
                .parent(topLevelCategory_2)
                .nextPersisted();

        final List<Category> topLevel1_Offspring = list(childCategory_1, childCategory_2, childCategory_3,
                childCategory_4, childCategory_6, childCategory_7, grandchildCategory_1, grandchildCategory_2);

        final List<Category> all = list(topLevelCategory_1, childCategory_1, childCategory_2, childCategory_3,
                childCategory_4, childCategory_6, childCategory_7, grandchildCategory_1, grandchildCategory_2,
                topLevelCategory_2, childCategory_5);

        final Map<String, List<Category>> parentList = new HashMap<>();
        parentList.put(topLevelCategory_1.getInode(), Collections.emptyList());
        parentList.put(childCategory_1.getInode(), list(topLevelCategory_1));
        parentList.put(childCategory_2.getInode(), list(topLevelCategory_1));
        parentList.put(childCategory_3.getInode(), list(topLevelCategory_1));
        parentList.put(childCategory_4.getInode(), list(topLevelCategory_1));
        parentList.put(childCategory_6.getInode(), list(topLevelCategory_1));
        parentList.put(childCategory_7.getInode(), list(topLevelCategory_1));
        parentList.put(grandchildCategory_1.getInode(), list(topLevelCategory_1, childCategory_4));
        parentList.put(grandchildCategory_2.getInode(), list(topLevelCategory_1, childCategory_4, grandchildCategory_1));
        parentList.put(topLevelCategory_2.getInode(), Collections.emptyList());
        parentList.put(childCategory_5.getInode(), list(topLevelCategory_2));

        final Map<String, Integer> childrenCount = new HashMap<>();
        childrenCount.put(topLevelCategory_1.getInode(), 6);
        childrenCount.put(childCategory_1.getInode(), 0);
        childrenCount.put(childCategory_2.getInode(), 0);
        childrenCount.put(childCategory_3.getInode(), 0);
        childrenCount.put(childCategory_4.getInode(), 1);
        childrenCount.put(childCategory_6.getInode(), 0);
        childrenCount.put(childCategory_7.getInode(), 0);
        childrenCount.put(grandchildCategory_1.getInode(), 1);
        childrenCount.put(grandchildCategory_2.getInode(), 0);
        childrenCount.put(topLevelCategory_2.getInode(), 1);
        childrenCount.put(childCategory_5.getInode(), 0);

        return Map.of("ALL", all,
                "TO_LEVEL_1_offspring", topLevel1_Offspring,
                "TO_LEVEL_1", topLevelCategory_1,
                "parentList", parentList,
                "childrenCount", childrenCount,
                "TOP_LEVELS", list(topLevelCategory_1, topLevelCategory_2),
                "TOP_LEVELS_1_CHILDREN", list(childCategory_1, childCategory_2, childCategory_3, childCategory_4, childCategory_6, childCategory_7),
                "CHILD_4", childCategory_4,
                "CHILD_4_CHILDREN", list(grandchildCategory_1)
        );
    }


    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is true
     * Should: must search on all the levels, so it must return the same that te method {@link CategoryFactoryImpl#findAll()}
     */
    @Test
    public void getAllCategories() throws DotDataException, DotSecurityException {
        new CategoryDataGen().nextPersisted();
        new CategoryDataGen().nextPersisted();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).build());

        final Collection<Category> expected = APILocator.getCategoryAPI().findAll(APILocator.systemUser(),
                false);

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is true and set a filter
     * Should: must search in all the levels but filtering by the string set,
     * so it must return the categories that contains the filter string in the key, name or variable name
     */
    @Test
    public void getAllCategoriesByFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<Category> expected = ((List<Category>) categoriesCreated.get("ALL")).stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).filter(filter).build());

        assertTrue(deepEquals(expected, resultCategories));
        checkNullParentList(resultCategories);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is true and set a rootInode
     * Should: must search in all the levels but since the root level,
     * it means return All the categories that are below of the root inode (Children, Grand children, etc)
     */
    @Test
    public void getAllCategoriesAndRootInode() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = (Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring");
        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).rootInode(topLevel1Id).build());

        assertTrue(deepEquals(expected, resultCategories));
        checkNullParentList(resultCategories);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is true, set a rootInode and a filtering
     * Should: must search in all the levels but since the root level,
     * it means return All the categories that are below of the root inode (Children, Grand children, etc) and
     * that contains the filter string in the key, name or variable name
     */
    @Test
    public void getAllCategoriesAndRootInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring"))
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).rootInode(topLevel1Id)
                        .filter(filter).build());

        assertTrue(deepEquals(expected, resultCategories));
        checkNullParentList(resultCategories);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is true and parentList is tru too
     * Should: must search in all the levels, Also calculate the parentList it means that all the path on the tree from
     * each Category to the Top level is calculated
     */
    @Test
    public void getAllCategoriesWithParentList() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> allCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).parentList(true).build());

        final Collection<Category> categories = APILocator.getCategoryAPI().findAll(APILocator.systemUser(),
                false);

        assertTrue(deepEquals(allCategories, categories));
        checkParentList(allCategories, categoriesCreated);
        checkNoChildrenCount(allCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels and parentList are set to true, also a filter is set
     * Should: must search in all the levels, Also calculated the parentList and filter the Category that contains
     * the filter string in the key, name or variable name
     */
    @Test
    public void getAllCategoriesWithParentListAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).parentList(true).filter(filter).build());

        Collection<Category> expected = APILocator.getCategoryAPI().findAll(APILocator.systemUser(), false)
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        assertTrue(deepEquals(expected, resultCategories));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels and parebtList are set to  true  also a root inode is set
     * Should: must search in all the levels, Also calculated the parentList, also
     * it means return All the categories that are below of the root inode (Children, Grand children, etc) because
     * a root inode is set
     */
    @Test
    public void getAllCategoriesWithParentListAndInode() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring"));

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).parentList(true).rootInode(topLevel1Id)
                        .build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels and parentList are set also a filter is set too
     * Should: must search in all the levels, Also calculated the parentList, and filter the Category that contains
     * the filter string in the key, name or variable name
     */
    @Test
    public void getAllCategoriesWithParentListAndInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring"))
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).parentList(true).filter(filter)
                        .rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels and countChildren are is true
     * Should: must search in all the levels, so it must return the same that te method {@link CategoryFactoryImpl#findAll()}
     * also calculating the count of children
     */
    @Test
    public void getAllCategoriesAndCountingChildren() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).build());

        final Collection<Category> categories = APILocator.getCategoryAPI().findAll(APILocator.systemUser(),
                false);

        assertTrue(deepEquals(resultCategories, categories));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels and countChildren are is true also a filter is set
     * Should: must search in all the levels and filter the Category that contains
     * the filter string in the key, name or variable name also calculating the count of children
     */
    @Test
    public void getAllCategoriesAndCountingChildrenAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<Category> expected = ((List<Category>) categoriesCreated.get("ALL")).stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).filter(filter).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels and countChildren are set to true also root inode and filter are set too,
     * Should: must search in all the levels but since the root level,
     * it means return All the categories that are below of the root inode (Children, Grand children, etc)
     * filtering the Category that contains the filter string in the key name or variable name also calculating the count of children
     */
    @Test
    public void getAllCategoriesAndCountingChildrenAndRootInode() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring"))
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true)
                        .rootInode(topLevel1Id).filter(filter).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels and countChildren are set to true also root inode is set too
     * Should: must search in all the levels but since the root level,
     *  it means return All the categories that are below of the root inode (Children, Grand children, etc) also
     *  count the children
     */
    @Test
    public void getAllCategoriesAndCountingChildrenAndRootInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring"));

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels, parentList and countChildren are set to true
     * Should: must search on all the levels, so it must return the same that te method {@link CategoryFactoryImpl#findAll()}
     * also count the children and the parent list
     */
    @Test
    public void getAllCategoriesAndCountingChildrenAndListParent() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).parentList(true).build());

        final Collection<Category> allCategories = APILocator.getCategoryAPI().findAll(APILocator.systemUser(),
                false);

        assertTrue(deepEquals(resultCategories, allCategories));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels, parentList and countChildren are set to true also set the filter
     * Should: must search on all the levels but filtering the Category that contains the filter string in the key name or variable name
     * also count the children
     */
    @Test
    public void getAllCategoriesAndCountingChildrenAndListParentAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<Category> expected = ((List<Category>) categoriesCreated.get("ALL")).stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).parentList(true)
                        .filter(filter).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels, parentList and countChildren are set to true also set the root inode
     * Should: must search in all the levels but since the root level,
     * it means return All the categories that are below of the root inode (Children, Grand children, etc)
     * also count of children
     */
    @Test
    public void getAllCategoriesAndCountingChildrenAndListParentAndRootInode() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring"));

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).parentList(true)
                        .rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels, parentList and countChildren are set to true also set the root inode and filter too
     * Should: must search in all the levels but since the root level,
     * it means return All the categories that are below of the root inode (Children, Grand children, etc)
     * filtering them by the filter string also count of children
     */
    @Test
    public void getAllCategoriesAndCountingChildrenAndListParentAndRootInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TO_LEVEL_1_offspring"))
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).parentList(true)
                        .rootInode(topLevel1Id).filter(filter).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false
     * Should: must return all the top level categories
     */
    @Test
    public void getNotAllLevels() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> allCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).build());

        final Collection<Category> allTopLevelsCategories = APILocator.getCategoryAPI().findTopLevelCategories(
                APILocator.systemUser(), false);

        assertTrue(deepEquals(allCategories, allTopLevelsCategories));
        checkNullParentList(allCategories);
        checkNoChildrenCount(allCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and filter is set too
     * Should: must return the top levels filtering them
     */
    @Test
    public void getNotAllLevelsAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> allCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).filter(filter).build());

        final Collection<Category> expected = APILocator.getCategoryAPI().findTopLevelCategories(
                        APILocator.systemUser(), false)
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        assertTrue(deepEquals(allCategories, expected));
        checkNullParentList(allCategories);
        checkNoChildrenCount(allCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and root inode is set too
     * Should: must return all the children of the Category with the root inode
     */
    @Test
    public void getNotAllLevelsAndRootInode() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN"));

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and root inode and filter are set too
     * Should: must return all the children of the Category with the root inode
     * that contains the filter string in the key, name or variable name
     */
    @Test
    public void getNotAllLevelsAndRootInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN")).stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).rootInode(topLevel1Id).filter(filter).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is set to false and parentList is set to true
     * Should: Return all the top levels
     */
    @Test
    public void getNotAllLevelsAndParentList() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).parentList(true).build());

        final Collection<Category> allTopLevelsCategories = APILocator.getCategoryAPI().findTopLevelCategories(
                APILocator.systemUser(), false);

        assertTrue(deepEquals(resultCategories, allTopLevelsCategories));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is set to false and parentList is set to true also filter is set too
     * Should: must return all the top levels Category that contains the filter string in the key, name or variable name
     */
    @Test
    public void getNotAllLevelsAndParentListAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).parentList(true).filter(filter).build());

        final Collection<Category> expected = APILocator.getCategoryAPI().findTopLevelCategories(
                APILocator.systemUser(), false)
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is set to false and parentList set to true also the root inode is set too
     * Should: Return all the Children of the root Inode category also must calculate the parentList
     */
    @Test
    public void getNotAllLevelsAndParentListRootAndInode() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN"));

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).parentList(true).rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels is set to false and parentList is set to true also rootInode and filter are set too
     * Should: Return all the Children of the root Inode category filtering by the filter String also must calculate the parentList
     */
    @Test
    public void getNotAllLevelsAndParentListAndInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN"))
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).parentList(true).rootInode(topLevel1Id)
                        .filter(filter).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and countChildren is set to true
     * Should: Must return all the top level categories counting the children
     */
    @Test
    public void getNotAllLevelsAndCountChildren() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).build());

        final Collection<Category> expected = APILocator.getCategoryAPI().findTopLevelCategories(
                        APILocator.systemUser(), false);

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and countChildren is set to true also the filter is set
     * Should: Must return the top level categories filtering them and counting the children
     */
    @Test
    public void getNotAllLevelsAndCountChildrenAndFilter() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).filter(filter).build());

        final Collection<Category> expected = APILocator.getCategoryAPI().findTopLevelCategories(
                APILocator.systemUser(), false)
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and countChildren is set to true also the root inode is set
     * Should: Must return all the children of the root Category and count the children
     */
    @Test
    public void getNotAllLevelsAndCountChildrenAndRootInode() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN"));
        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and countChildren is set to true also the root inode and filter are set
     * Should: Must filter the top levels categories and count the children
     */
    @Test
    public void getNotAllLevelsAndCountChildrenAndRootInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN"))
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).filter(filter)
                        .rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkNullParentList(resultCategories);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and countChildren and parentList are set to true
     * Should: Must return all the top levels categories calculating the parentList and counting the children
     */
    @Test
    public void getNotAllLevelsAndCountChildrenAndListParent() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).parentList(true).build());

        final Collection<Category> expected = APILocator.getCategoryAPI().findTopLevelCategories(
                APILocator.systemUser(), false);

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: AllLevels set to false and countChildren and parentList are set to true also filter is set too
     * Should: Return the top levels categories and filter them also calculate the parentList and counting the children
     */
    @Test
    public void getNotAllLevelsAndCountChildrenAndListParentAndFilter() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).parentList(true)
                        .filter(filter).build());

        final Collection<Category> expected = APILocator.getCategoryAPI().findTopLevelCategories(
                        APILocator.systemUser(), false)
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When:  AllLevels set to false and countChildren and parentList are set to true also root inode is set too
     * Should: Return the children of the root Category also calculated parentList and count the children
     */
    @Test
    public void getNotAllLevelsAndCountChildrenAndListParentAndRootInode() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN"));
        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();


        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).parentList(true)
                        .rootInode(topLevel1Id).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When:  AllLevels set to false and countChildren and parentList are set to true also root inode and filter are set too
     * Should: Return the children of the root Category and filter them also calculated parentList and count the children
     */
    @Test
    public void getNotAllLevelsAndCountChildrenAndListParentAndRootInodeAndFilter() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("TOP_LEVELS_1_CHILDREN"))
                .stream()
                .filter(category -> containsFilter(category, filter))
                .collect(Collectors.toList());

        final String topLevel1Id = ((Category) categoriesCreated.get("TO_LEVEL_1")).getInode();


        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).setCountChildren(true).parentList(true)
                        .rootInode(topLevel1Id).filter(filter).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkChildrenCount(resultCategories, categoriesCreated);
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When:  AllLevels set to false and parentList is set to true also root inode are set too
     * Should: Return the children of the root Category also calculated parentList
     */
    @Test
    public void getParentListButNotFromTopLevel() throws DotDataException, DotSecurityException {

        final  String  filter = new RandomString().nextString();
        final  Map<String, Object>  categoriesCreated = createCategories(filter);

        Collection<Category> expected = ((Collection<Category>) categoriesCreated.get("CHILD_4_CHILDREN"));

        final String child4Inode = ((Category) categoriesCreated.get("CHILD_4")).getInode();

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(false).parentList(true)
                        .rootInode(child4Inode).build());

        assertTrue(deepEquals(resultCategories, expected));
        checkParentList(resultCategories, categoriesCreated);
        checkNoChildrenCount(resultCategories);
    }

    private void checkChildrenCount(final List<HierarchedCategory> categories, final  Map<String, Object>  categoriesCreated) {

        final Map<String, Integer> childrenCountMap = (Map<String, Integer>) categoriesCreated.get("childrenCount");

        for (final HierarchedCategory category : categories) {
            final Integer childrenCount = childrenCountMap.get(category.getInode());

            if (Objects.isNull(childrenCount)) {
                continue;
            }

            assertEquals(childrenCount.intValue(), category.getChildrenCount());
        }
    }

    private void checkNoChildrenCount(final List<HierarchedCategory> categories) {
        boolean anyMatch = categories.stream()
                .anyMatch(category -> category.getChildrenCount() != 0);

        if (anyMatch) {
            throw new AssertException("All the ChildrenCount must be 0");
        }
    }

    private void checkParentList(final List<HierarchedCategory> categories, final  Map<String, Object>  categoriesCreated) {

        boolean anyMatch = categories.stream()
                .anyMatch(category -> Objects.isNull(category.getParentList()));

        if (anyMatch) {
            throw new AssertException("All the parentList must be different of null");
        }

        final Map<String, List<Category>> parentList = (Map<String, List<Category>>) categoriesCreated.get("parentList");

        for (final HierarchedCategory category : categories) {
            final List<Category> parentListExpected = parentList.get(category.getInode());

            if (Objects.isNull(parentListExpected)) {
                continue;
            }

            assertEquals(parentListExpected.size(), category.getParentList().size());

            for (int i = 0; i < parentListExpected.size(); i++) {
                final Category expectedParent = parentListExpected.get(i);
                final ShortCategory currentParent = category.getParentList().get(i);

                assertEquals(expectedParent.getInode(), currentParent.getInode());
                assertEquals(expectedParent.getKey(), currentParent.getKey());
                assertEquals(expectedParent.getCategoryName(), currentParent.getName());
            }
        }
    }

    private void checkNullParentList(final List<HierarchedCategory> categories) {

        boolean anyMatch = categories.stream()
                .anyMatch(category -> !Objects.isNull(category.getParentList()));

        if (anyMatch) {
            throw new AssertException("All the parentList must be null");
        }
    }

    private boolean containsFilter(final Category category, final String filter) {
        return category.getCategoryName().contains(filter) || category.getCategoryVelocityVarName().contains(filter) ||
                category.getKey().contains(filter);
    }

    private boolean deepEquals(final Collection<? extends Category> categories1,
                               final Collection<? extends Category> categories2) {

        if (categories1.size() != categories2.size()) {
            return false;
        }

        return categories1.stream().allMatch(category -> contains(category, categories2));
    }

    private boolean contains(final Category category, final Collection<? extends Category> categories) {
        return categories.stream().anyMatch(item -> equals(item, category));
    }

    private boolean equals(final Category category1, final Category category2) {
        return category1.getCategoryId().equals(category2.getCategoryId()) &&
                category1.getCategoryName().equals(category2.getCategoryName()) &&
                category1.getKey().equals(category2.getKey()) &&
                category1.getCategoryVelocityVarName().equals(category2.getCategoryVelocityVarName());
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When: Create a set of {@link Category} and called the method ordering by key
     * Should: return all children  Categories ordered
     *
     * @throws DotDataException
     */
    @Test
    public void getAllCategoriesFilteredOrdered() throws DotDataException, DotSecurityException {
        final Category topLevelCategory_1 = new CategoryDataGen().setCategoryName("Top Level Category 1")
                .setKey("top_level")
                .setCategoryVelocityVarName("top_level_categoria_1")
                .nextPersisted();

        final Category childCategory_1 = new CategoryDataGen().setCategoryName("Child Category 1")
                .setKey("A")
                .setCategoryVelocityVarName("child_category_1")
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category childCategory_2 = new CategoryDataGen().setCategoryName("Child Category 2")
                .setKey("C")
                .setCategoryVelocityVarName("child_category_2")
                .parent(topLevelCategory_1)
                .nextPersisted();

        final Category grandchildCategory_1 = new CategoryDataGen().setCategoryName("Grand Child Category 1")
                .setKey("B")
                .setCategoryVelocityVarName("grand_child_category_1")
                .parent(childCategory_2)
                .nextPersisted();

        final CategorySearchCriteria categorySearchCriteria = new CategorySearchCriteria.Builder()
                .orderBy("category_key")
                .direction(OrderDirection.ASC)
                .rootInode(topLevelCategory_1.getInode())
                .searchAllLevels(true)
                .build();

        final List<String> categoriesInode = FactoryLocator.getCategoryFactory().findAll(categorySearchCriteria)
            .stream().map(Category::getInode).collect(Collectors.toList());

        List<String> categoriesExpected = list(childCategory_1, grandchildCategory_1, childCategory_2).stream()
                .map(Category::getInode).collect(Collectors.toList());

        assertEquals(categoriesExpected.size(), categoriesInode.size());

        for (int i =0; i < categoriesExpected.size(); i++){
            assertEquals(categoriesExpected.get(i), categoriesInode.get(i));
        }

    }

    @Test
    public void hierarchyCategory() throws DotDataException, DotSecurityException {
        final Category topLevelCategory = new CategoryDataGen().setCategoryName("Top Level Category")
                .setKey("top_level")
                .setCategoryVelocityVarName("top_level_categoria")
                .nextPersisted();

        final Category childCategory = new CategoryDataGen().setCategoryName("Child Category")
                .setKey("child")
                .setCategoryVelocityVarName("child_category")
                .parent(topLevelCategory)
                .nextPersisted();

        final Category grandChildCategory = new CategoryDataGen().setCategoryName("Grand Child Category")
                .setKey("grand_child")
                .setCategoryVelocityVarName("grand_child_category")
                .parent(childCategory)
                .nextPersisted();

        final Category greatGrandchildCategory = new CategoryDataGen().setCategoryName("Great Grand Child Category")
                .setKey("great_grand_child")
                .setCategoryVelocityVarName("great_grand_child_category")
                .parent(grandChildCategory)
                .nextPersisted();

        final CategorySearchCriteria categorySearchCriteria = new CategorySearchCriteria.Builder()
                .rootInode(topLevelCategory.getInode())
                .filter("Great Grand Child")
                .searchAllLevels(true)
                .parentList(true)
                .build();

        final List<HierarchedCategory> categories = FactoryLocator.getCategoryFactory().findAll(categorySearchCriteria);

        assertEquals(1, categories.size());
        assertEquals(greatGrandchildCategory.getKey(), categories.get(0).getKey());

        final List<ShortCategory> hierarchy = categories.get(0).getParentList();
        assertEquals(3, hierarchy.size());

        assertEquals(hierarchy.get(0).getName(), topLevelCategory.getCategoryName());
        assertEquals(hierarchy.get(0).getKey(), topLevelCategory.getKey());
        assertEquals(hierarchy.get(0).getInode(), topLevelCategory.getInode());

        assertEquals(hierarchy.get(1).getName(), childCategory.getCategoryName());
        assertEquals(hierarchy.get(1).getKey(), childCategory.getKey());
        assertEquals(hierarchy.get(1).getInode(), childCategory.getInode());

        assertEquals(hierarchy.get(2).getName(), grandChildCategory.getCategoryName());
        assertEquals(hierarchy.get(2).getKey(), grandChildCategory.getKey());
        assertEquals(hierarchy.get(2).getInode(), grandChildCategory.getInode());
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.categories.model.HierarchyShortCategory}
     * when: Create 4 Category each is a child of the previous one and find for the great grand child and the grand child
     * should: Return the great grand child we all the parent list
     * @throws DotDataException
     */
    @Test
    public void findHierarchy() throws DotDataException {
        final Category topLevelCategory = new CategoryDataGen().setCategoryName("Top Level Category")
                .setKey("top_level")
                .setCategoryVelocityVarName("top_level_categoria")
                .nextPersisted();

        final Category childCategory = new CategoryDataGen().setCategoryName("Child Category")
                .setKey("child")
                .setCategoryVelocityVarName("child_category")
                .parent(topLevelCategory)
                .nextPersisted();

        final Category grandChildCategory = new CategoryDataGen().setCategoryName("Grand Child Category")
                .setKey("grand_child")
                .setCategoryVelocityVarName("grand_child_category")
                .parent(childCategory)
                .nextPersisted();

        final Category greatGrandchildCategory = new CategoryDataGen().setCategoryName("Great Grand Child Category")
                .setKey("great_grand_child")
                .setCategoryVelocityVarName("great_grand_child_category")
                .parent(grandChildCategory)
                .nextPersisted();

        final List<HierarchyShortCategory> categories = FactoryLocator.getCategoryFactory()
                .findHierarchy(list(grandChildCategory.getKey(), greatGrandchildCategory.getKey()));

        assertEquals(2, categories.size());

        for (final HierarchyShortCategory category : categories) {
            if (grandChildCategory.getInode().equals(category.getInode())) {
                assertEquals(2, category.getParentList().size());
                assertEquals("Top Level Category", category.getParentList().get(0).getName());
                assertEquals("Child Category", category.getParentList().get(1).getName());
            } else if (greatGrandchildCategory.getInode().equals(category.getInode())) {
                assertEquals(3, category.getParentList().size());
                assertEquals("Top Level Category", category.getParentList().get(0).getName());
                assertEquals("Child Category", category.getParentList().get(1).getName());
                assertEquals("Grand Child Category", category.getParentList().get(2).getName());
            } else {
                throw new AssertionError("Unexpected Category");
            }
        }
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When:
     * - Create a Top level Category and one child.
     * - Crate a Content Type with a category Field.
     * - Crate a Contentlet and select the Child Category as value of the Category Field.
     * - Find these 2 categories and count the children
     *
     * Should:
     * - For the Top level Category the counting should be 1
     * - For the Child the counting should be 0.
     */
    @Test
    public void getCategoriesLinkWithContentlet() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();

        final Category topLevelCategory = new CategoryDataGen().setCategoryName("Top Level Category - " + filter)
                .setKey("top_level_getCategoriesLinkWithContentlet")
                .setCategoryVelocityVarName("top_level_categoria_getCategoriesLinkWithContentlet")
                .nextPersisted();

        final Category childCategory = new CategoryDataGen().setCategoryName("Child Category - " + filter)
                .setKey("child_getCategoriesLinkWithContentlet")
                .setCategoryVelocityVarName("child_category_getCategoriesLinkWithContentlet")
                .parent(topLevelCategory)
                .nextPersisted();

        final Field catField = new FieldDataGen()
                .type(CategoryField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen().field(catField).nextPersisted();

        Contentlet content = new ContentletDataGen(contentType.id()).next();

        content = APILocator.getContentletAPI().checkin(content, APILocator.systemUser(), false,
                list(childCategory));

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).setCountChildren(true).filter(filter).build());

        System.out.println("resultCategories = " + resultCategories);

        assertEquals(2, resultCategories.size());

        for (HierarchedCategory resultCategory : resultCategories) {
            if (resultCategory.getCategoryName().equals(topLevelCategory.getCategoryName())) {
                assertEquals(1, resultCategory.getChildrenCount());
            } else if (resultCategory.getCategoryName().equals(childCategory.getCategoryName())) {
                assertEquals(0, resultCategory.getChildrenCount());
            } else {
                throw new AssertionError("Category is not expected");
            }
        }
    }

    /**
     * Method to test: {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)}
     * When:
     * - Create a Top level Category and one child.
     * - Crate a Content Type with a category Field.
     * - Crate a Contentlet and select the Child Category as value of the Category Field.
     * - Find these 2 categories and count the children
     * -  and set parentList to true
     *
     * Should:
     * - For the Top level Category the counting should be 1
     * - For the Child the counting should be 0.
     */
    @Test
    public void getCategoriesLinkWithContentletWithListparent() throws DotDataException, DotSecurityException {
        final  String  filter = new RandomString().nextString();

        final Category topLevelCategory = new CategoryDataGen().setCategoryName("Top Level Category - " + filter)
                .setKey("top_level_getCategoriesLinkWithContentlet")
                .setCategoryVelocityVarName("top_level_categoria_getCategoriesLinkWithContentlet")
                .nextPersisted();

        final Category childCategory = new CategoryDataGen().setCategoryName("Child Category - " + filter)
                .setKey("child_getCategoriesLinkWithContentlet")
                .setCategoryVelocityVarName("child_category_getCategoriesLinkWithContentlet")
                .parent(topLevelCategory)
                .nextPersisted();

        final Field catField = new FieldDataGen()
                .type(CategoryField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen().field(catField).nextPersisted();

        Contentlet content = new ContentletDataGen(contentType.id()).next();

        content = APILocator.getContentletAPI().checkin(content, APILocator.systemUser(), false,
                list(childCategory));

        final List<HierarchedCategory> resultCategories = FactoryLocator.getCategoryFactory().findAll(
                new CategorySearchCriteria.Builder().searchAllLevels(true).parentList(true).setCountChildren(true).filter(filter).build());

        System.out.println("resultCategories = " + resultCategories);

        assertEquals(2, resultCategories.size());

        for (HierarchedCategory resultCategory : resultCategories) {
            if (resultCategory.getCategoryName().equals(topLevelCategory.getCategoryName())) {
                assertEquals(1, resultCategory.getChildrenCount());
            } else if (resultCategory.getCategoryName().equals(childCategory.getCategoryName())) {
                assertEquals(0, resultCategory.getChildrenCount());
            } else {
                throw new AssertionError("Category is not expected");
            }
        }
    }
}
