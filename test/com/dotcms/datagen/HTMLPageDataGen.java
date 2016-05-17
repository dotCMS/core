package com.dotcms.datagen;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;

/**
 * Class used to create {@link Contentlet} objects of type HTMLPageAsset for test purposes
 * @author Nollymar Longa
 *
 */
public class HTMLPageDataGen extends ContentletDataGen{
	
	/**
	 * Default constructor
	 */
	public HTMLPageDataGen(){
		structureInode = HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE;
	}
}
