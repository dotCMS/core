package com.dotmarketing.portlets.categories.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CategoryFactoryImplTest extends ServletTestCase {

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
			localCategory.setCategoryName("Test Inner Category" + i);
			localCategory.setCategoryVelocityVarName("testInnerCategory" + i);
			localCategory.setDescription("Test Root Category Description " + i);
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
		if(UtilMethods.isSet(category) && InodeUtils.isSet(category.getInode()))
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
	public void testCouldCreateInnerClasses() throws DotDataException, DotSecurityException
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
	 * This methods test that the inner categories are retrieved in order from the cache
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */	
	public void testOrderInnerClassesCache() throws DotDataException, DotSecurityException
	{
		boolean returnValue = testOrderInnerClaases();
		assertEquals(true,returnValue);
	}

	/**
	 * This methods test that the inner categories are retrieved in order from the DB
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void testOrderInnerClassesDB() throws DotDataException, DotSecurityException
	{
		catAPI.clearCache();
		boolean returnValue = testOrderInnerClaases();
		assertEquals(true,returnValue);
	}
	
	/**
	 * This methods test if an anonymous user could retrieve a root category
	 * @throws DotDataException
	 */
	public void testCouldAnonymousRetrieveRootCategory() throws DotDataException 
	{		
		boolean found = false;
		try
		{			
			Category category = catAPI.findByKey("testRootCategoryKey", defaultUser, true);			
			if(UtilMethods.isSet(category))
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
	 * This methods test if an anonymous user could retrieve inner categories
	 * @throws DotDataException
	 */
	public void testCouldAnonymousRetrieveInnerCategories() throws DotDataException
	{
		boolean found = false;
		try
		{
			Category category = catAPI.findByKey("testRootCategoryKey", systemUser, true);
			List<Category> categories = catAPI.getChildren(category, defaultUser, true);
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
	 * This methods test that the inner categories are retrieved in order
	 * @return if the categories are returned in order by sortOrder and name
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private boolean testOrderInnerClaases() throws DotDataException,
	DotSecurityException {
		boolean returnValue = true;
		int order = Integer.MIN_VALUE;
		String categoryName = "";
		Category category = catAPI.findByKey("testRootCategoryKey", systemUser, true);
		List<Category> categories = catAPI.getChildren(category, systemUser, true);
		for(Category localCategory : categories)
		{
			int localOrder = localCategory.getSortOrder();
			if(localOrder < order)
			{
				returnValue = false;
				break;
			}
			if(localCategory.getSortOrder() == order)
			{		
				String localCategoryName = localCategory.getCategoryName(); 
				if(localCategoryName.compareTo(categoryName) < 0)
				{
					returnValue = false;
					break;
				}				
			}
			order = localCategory.getSortOrder();
			categoryName = localCategory.getCategoryName();
		}
		return returnValue;
	}	
}
