package com.dotcms.publisher.util;

import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

public class DependencyUtil {
	
	public static void setDependencies(PublisherConfig config) throws DotDataException {
		String id = config.getObjectId();
		Identifier iden = APILocator.getIdentifierAPI().find(id);
		
		if(iden.getAssetType().equals("htmlpage")) {
			setHTMLPageDependencies(iden);
		}
	}
	
	public static void setHTMLPageDependencies(Identifier iden) {
//		APILocator.getHTMLPageAPI().
	}

}
