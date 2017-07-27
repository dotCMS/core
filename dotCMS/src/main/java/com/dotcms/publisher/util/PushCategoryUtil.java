package com.dotcms.publisher.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;
import static com.dotcms.enterprise.publishing.remote.bundler.CategoryBundler.CATEGORY_EXTENSION;

public class PushCategoryUtil {

	private XStream xstream;

	private Map<String, File> categoriesByInode;
	private Set<String> categoriesTopLevel;

	private int categoriesCount;
	
	public PushCategoryUtil(Collection<File> categories) throws DotPublishingException {
		xstream=new XStream(new DomDriver());

		categoriesByInode = new HashMap<String, File>();
		categoriesTopLevel = new HashSet<String>();
		categoriesCount = 0;

		for(File categoryFile : categories){

			if(categoryFile.getName().endsWith(CATEGORY_EXTENSION))
				categoriesCount++;

			if(categoryFile.isDirectory())
				continue;

			try {
				CategoryWrapper wrapper = getCategoryWrapperFromFile(categoryFile);

				categoriesByInode.put(wrapper.getCategory().getInode(), categoryFile);

				if(wrapper.isTopLevel())
					categoriesTopLevel.add(wrapper.getCategory().getInode());

			} catch (FileNotFoundException fnfe) {
				throw new DotPublishingException(fnfe.getMessage(),fnfe);
			}
		}
	}
	
	/**
	 * Find all top level categories. Returns all the Wrapper.
	 * 
	 * Mar 6, 2013 - 9:53:45 AM
	 */
	public List<CategoryWrapper> findTopLevelWrappers() throws FileNotFoundException {
		List<CategoryWrapper> topLevels = new ArrayList<CategoryWrapper>();
		for(String categoryInode : categoriesTopLevel){
			File categoryFile = categoriesByInode.get(categoryInode);
			topLevels.add( getCategoryWrapperFromFile(categoryFile) );
		}
		return topLevels;
	}

	public CategoryWrapper getCategoryWrapperFromInode(String inode) throws FileNotFoundException {
		File categoryFile = categoriesByInode.get(inode);
		return (categoryFile != null) ? getCategoryWrapperFromFile(categoryFile) : null;
	}
	
	private CategoryWrapper getCategoryWrapperFromFile(File category) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(category);
		try {
			return (CategoryWrapper)xstream.fromXML(fis);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}
	
	public int getCategoryXMLCount(){
		return categoriesCount;
	}
}
