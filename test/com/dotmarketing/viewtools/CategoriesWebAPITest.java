package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CategoriesWebAPITest extends ServletTestCase {

	CategoryAPI catAPI;
	UserAPI userAPI;
	User systemUser;
	User defaultUser;
	ArrayList<Category> categories;
	int maxCategories = 10;
	Random random;

	@Override
	protected void setUp() throws Exception {

		catAPI = APILocator.getCategoryAPI();
		userAPI = APILocator.getUserAPI();
		categories = new ArrayList<Category>();
		random = new Random();


		Category category = new Category();
		category.setActive(true);
		category.setCategoryName("Test Root Category");
		category.setCategoryVelocityVarName("testRootCategory");
		category.setDescription("Test Root Category Description");
		category.setKey("testRootCategoryKey");
		category.setSortOrder(0);
		systemUser = userAPI.getSystemUser();
		defaultUser = userAPI.getDefaultUser();

		catAPI.save(null, category, systemUser, true);
		categories.add(category);

		for(int i = 0;i < maxCategories;i++)
		{
			int order = random.nextInt();
			Category localCategory =  new Category();
			localCategory.setActive(true);
			localCategory.setCategoryName("Test Inner Category " + i);
			localCategory.setCategoryVelocityVarName("testInnerCategory"+ i);
			localCategory.setDescription("Test Inner Category Description " + i);
			localCategory.setKey("testRootCategoryKey " + i);
			localCategory.setSortOrder(order);
			catAPI.save(category, localCategory, systemUser, true);
			categories.add(localCategory);
		}		
	}

	@Override
	protected void tearDown() throws Exception {		
		for(int i = categories.size() - 1;i >= 0;i--)
		{
			Category localCategory = categories.get(i);
			catAPI.delete(localCategory, systemUser, true);
		}
	}

	/**
	 * This method test the creation of the Root Category
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void testCouldCreateRoot() throws DotDataException, DotSecurityException
	{		
		boolean found = false;
		Category category = catAPI.findByKey("testRootCategoryKey", systemUser, true);
		if(UtilMethods.isSet(category))
		{
			found = true;
		}
		assertEquals(true,found);
	}

	/**
	 * This methods test the creation of the inner categories
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void testCouldCreateInnerCategories() throws DotDataException, DotSecurityException
	{		
		boolean found = false;
		Category category = catAPI.findByKey("testRootCategoryKey", systemUser, true);
		List<Category> categories = catAPI.getChildren(category, systemUser, true);
		if(categories.size() == maxCategories)
		{
			found = true;
		}
		assertEquals(true,found);
	}
	
	/**
	 * This methods test if an anonymous user could retrieve inner categories using the WebAPI and the category object
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void testCouldRetrieveInnerCategoriesWebAPIUsingObject() throws DotDataException
	{
		boolean found = false;
		try
		{
			Category category = catAPI.findByKey("testRootCategoryKey", systemUser, true);
			CategoriesWebAPI catWebAPI = new CategoriesWebAPI();			
			List<Category> categories = catWebAPI.getChildrenCategories(category); 
			if(categories.size() > 0)
			{
				found = true;
			}
		}
		catch(DotSecurityException ex)
		{
			found = false;
		}
		assertEquals(false,found);
	}
	
	/**
	 * This methods test if an anonymous user could retrieve inner categories using the WebAPI and the category's inode value
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void testCouldRetrieveInnerCategoriesWebAPIUsingInode() throws DotDataException
	{
		boolean found = false;
		try
		{
			Category category = catAPI.findByKey("testRootCategoryKey", systemUser, true);
			CategoriesWebAPI catWebAPI = new CategoriesWebAPI();			
			List<Category> categories = catWebAPI.getChildrenCategories((Inode) category); 
			if(categories.size() > 0)
			{
				found = true;
			}
		}
		catch(DotSecurityException ex)
		{
			found = false;
		}
		assertEquals(false,found);
	}
	
	/**
	 * This methods test if an anonymous user could retrieve inner categories using the WebAPI and the category's inode value
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void testCouldRetrieveInnerCategoriesWebAPIUsingInodeValue() throws DotDataException
	{
		boolean found = false;
		try
		{
			Category category = catAPI.findByKey("testRootCategoryKey", systemUser, true);
			CategoriesWebAPI catWebAPI = new CategoriesWebAPI();
			List<Category> categories = catWebAPI.getChildrenCategories(category.getInode());
			if(categories.size() > 0)
			{
				found = true;
			}
		}
		catch(DotSecurityException ex)
		{
			found = false;
		}
		assertEquals(false,found);
	}
}
