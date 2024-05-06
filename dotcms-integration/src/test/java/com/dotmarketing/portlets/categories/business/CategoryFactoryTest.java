package com.dotmarketing.portlets.categories.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

/***
 * Category Factory Test
 */
public class CategoryFactoryTest extends IntegrationTestBase {

    private static CategoryFactory categoryFactory;

    @BeforeClass
    public static void prepare() throws Exception {
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

    @NotNull
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

}
