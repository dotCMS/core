package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.pusher.bundler.CategoryBundler;
import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publisher.util.PushCategoryUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.liferay.util.FileUtil;

public class CategoryHandler implements IHandler {
	
	private CategoryAPI catAPI = APILocator.getCategoryAPI();
	private UserAPI userAPI = APILocator.getUserAPI();
	private PushCategoryUtil pushCategoryUtil;
	
	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
		Collection<File> categories = FileUtil.listFilesRecursively(bundleFolder, new CategoryBundler().getFileFilter());
		pushCategoryUtil = new PushCategoryUtil(categories);
		handleCategories(categories);
	}

	/**
	 * Handle categories: first we need all the top level categories and than for each one recursively publish the children.
	 * 
	 * Mar 6, 2013 - 9:41:42 AM
	 */
	private void handleCategories(Collection<File> categories) throws DotPublishingException {
		try{
			// get the top level categories
			List<CategoryWrapper> topLevels = pushCategoryUtil.findTopLevelWrappers();
			
			// first delete and recreate the top level
			for(CategoryWrapper topLevel : topLevels){
				// delete the category including the children
				catAPI.deleteChilren(topLevel.getCategory().getInode());
				catAPI.delete(topLevel.getCategory(), userAPI.getSystemUser(), true);
				
				// save the category with the same Inode and without parents because it is a top level
				catAPI.publishRemote(null, topLevel.getCategory(), userAPI.getSystemUser(), true);
				
				//TODO Recursively creates children...
			}
			
		}catch(Exception e){
			throw new DotPublishingException(e.getMessage(),e);
		}
	}

	
}
