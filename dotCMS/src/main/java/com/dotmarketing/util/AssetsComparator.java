package com.dotmarketing.util;

import java.util.Comparator;

import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;



/**
 * @author will
 */
public class AssetsComparator implements Comparator {

	private final int direction;
	
	public AssetsComparator(int direction){
		this.direction = direction;
	}

	public AssetsComparator(){
	    this.direction = 1;
	}
	
	protected long getSortOrder(Object obj) {
	    if(obj instanceof WebAsset) return ((WebAsset)obj).getSortOrder();
	    if(obj instanceof Folder) return ((Folder)obj).getSortOrder();
	    if(obj instanceof FileAsset) return ((FileAsset)obj).getSortOrder();
	    if(obj instanceof HTMLPageAsset) return ((Contentlet)obj).getSortOrder();
	    return 0;
	}

	public int compare(Object o1, Object o2) {

	    long so1=getSortOrder(o1);
	    long so2=getSortOrder(o2);
	    
		if (so1 < so2) return -1*direction;
		else if (so1 == so2) return 0;
		else return 1*direction;

	}



}
