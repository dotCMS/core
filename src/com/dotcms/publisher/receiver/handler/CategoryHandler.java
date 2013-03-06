package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.pusher.bundler.CategoryBundler;
import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publisher.util.PushCategoryUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.CategoryCache;
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
		if(pushCategoryUtil.getCategoryXMLCount()>0){
			// first step: delete ALL categories
			catAPI.deleteAll(userAPI.getSystemUser(), true);
			pushCategoryUtil = new PushCategoryUtil(categories);
			handleCategories(categories);
			CategoryCache cache = CacheLocator.getCategoryCache();
			cache.clearCache();
		}
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
				catAPI.removeChildren(topLevel.getCategory(), userAPI.getSystemUser(), true);
				// if  the category already exists
				if(null!=catAPI.find(topLevel.getCategory().getInode(), userAPI.getSystemUser(), true)){
					// save the category with the same Inode and without parents because it is a top level
					catAPI.save(null, topLevel.getCategory(), userAPI.getSystemUser(), true);
				}else{
					// save the category with the same Inode and without parents because it is a top level: it's new
					catAPI.publishRemote(null, topLevel.getCategory(), userAPI.getSystemUser(), true);
				}
				// try with children
				if(null!=topLevel.getChildren() && topLevel.getChildren().size()>0) {
					for(String inode : topLevel.getChildren())
						handleChildrenCategories(topLevel,pushCategoryUtil.getCategoryWrapperFromInode(inode));
				}
			}
			
		}catch(Exception e){
			throw new DotPublishingException(e.getMessage(),e);
		}
	}

	private void handleChildrenCategories(CategoryWrapper parent, CategoryWrapper wrapper) throws DotDataException, DotSecurityException, FileNotFoundException {
		// the category has children
		if(null!=wrapper.getChildren() && wrapper.getChildren().size()>0){
			if(null!=catAPI.find(wrapper.getCategory().getInode(), userAPI.getSystemUser(), true))
				catAPI.save(parent.getCategory(), wrapper.getCategory(), userAPI.getSystemUser(), true);
			else
				// save the category with the same Inode
				catAPI.publishRemote(parent.getCategory(), wrapper.getCategory(), userAPI.getSystemUser(), true);
			
			catAPI.removeChildren(wrapper.getCategory(), userAPI.getSystemUser(), true);
			for(String inode:wrapper.getChildren())
				handleChildrenCategories(wrapper, pushCategoryUtil.getCategoryWrapperFromInode(inode));
			
		}else{
			if(null!=catAPI.find(wrapper.getCategory().getInode(), userAPI.getSystemUser(), true))
				catAPI.save(parent.getCategory(),wrapper.getCategory(), userAPI.getSystemUser(), true);
			else
				// save the category with the same Inode and without parents because it is a top level
				catAPI.publishRemote(parent.getCategory(), wrapper.getCategory(), userAPI.getSystemUser(), true);
		}
	}
}
