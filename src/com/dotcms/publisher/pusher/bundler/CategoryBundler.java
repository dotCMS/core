package com.dotcms.publisher.pusher.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * The Category Bundler: in this case we don't need any kind of categories Set because we push every time all the system categories.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Mar 6, 2013 - 9:34:34 AM
 */
public class CategoryBundler implements IBundler {

	private PushPublisherConfig config;
	private CategoryAPI catAPI = null;
	private UserAPI userAPI = null;
	public final static String CATEGORY_EXTENSION = ".category.xml" ;
	
	@Override
	public String getName() {
		return "Category Bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig)pc;
		catAPI = APILocator.getCategoryAPI();
		userAPI = APILocator.getUserAPI();
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");
		try{
			// retrieve all top level categories
			List<Category> topLevelCategories = catAPI.findTopLevelCategories(userAPI.getSystemUser(), true);
			for(Category topLevel : topLevelCategories){
				CategoryWrapper wrapper = new CategoryWrapper();
				wrapper.setTopLevel(true);
				wrapper.setCategory(topLevel);
				wrapper.setOperation(config.getOperation());
				// get all children
				Set<String> childrenInodes = getChildrenInodes(catAPI.findChildren(userAPI.getSystemUser(), topLevel.getInode(), true, null));
				wrapper.setChildren(childrenInodes);
				writeCategory(bundleRoot,wrapper);
				writeChildren(bundleRoot,childrenInodes);
			}
			
		}catch(Exception e){
			Logger.error(this, e.getMessage(), e);
		}
		
	}
	
	private void writeCategory(File bundleRoot, CategoryWrapper categoryWrapper) 
			throws IOException, DotBundleException, DotDataException, DotSecurityException, DotPublisherException {
		
		String liveworking = categoryWrapper.getCategory().isLive() ? "live" :  "working";

		String uri = categoryWrapper.getCategory().getInode();
		if(!uri.endsWith(CATEGORY_EXTENSION)){
			uri.replace(CATEGORY_EXTENSION, "");
			uri.trim();
			uri += CATEGORY_EXTENSION;
		}
				
		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ APILocator.getHostAPI().findSystemHost().getHostname() +File.separator + uri;

		File strFile = new File(myFileUrl);
		if(!strFile.exists()){
			strFile.mkdirs();
	
			BundlerUtil.objectToXML(categoryWrapper, strFile, true);
			strFile.setLastModified(Calendar.getInstance().getTimeInMillis());
		}
	}
	
	@Override
	public FileFilter getFileFilter() {		
		return new CategoryBundlerFilter();
	}

	private Set<String> getChildrenInodes(List<Category> children){
		Set<String> inodes = new HashSet<String>();
		for(Category child:children)
			inodes.add(child.getInode());
		return inodes;
	}
	
	/**
	 * For each top level category creates the children's xml recursively.  
	 * 
	 * Mar 6, 2013 - 9:33:00 AM
	 */
	private void writeChildren(File bundleRoot, Set<String> inodes) throws DotDataException, DotSecurityException, IOException, DotBundleException, DotPublisherException{
		for(String inode: inodes){
			Category cat = catAPI.find(inode, userAPI.getSystemUser(), true);
			if(null!=cat && UtilMethods.isSet(cat.getInode())){
				List<Category> children = catAPI.findChildren(userAPI.getSystemUser(), cat.getInode(), true, null);
				if(children.size()>0){
					CategoryWrapper wrapper = new CategoryWrapper();
					wrapper.setTopLevel(false);
					wrapper.setCategory(cat);
					wrapper.setOperation(config.getOperation());
					wrapper.setChildren(getChildrenInodes(children));
					writeCategory(bundleRoot,wrapper);					
					Set<String> childrenInodes = getChildrenInodes(children);
					writeChildren(bundleRoot, childrenInodes);
				}else{ // write the category
					CategoryWrapper wrapper = new CategoryWrapper();
					wrapper.setTopLevel(false);
					wrapper.setCategory(cat);
					wrapper.setOperation(config.getOperation());
					wrapper.setChildren(null);
					writeCategory(bundleRoot,wrapper);
				}
			}
		}
		
	}
	
	public class CategoryBundlerFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return (pathname.isDirectory() || pathname.getName().endsWith(CATEGORY_EXTENSION));
		}

	}
}
