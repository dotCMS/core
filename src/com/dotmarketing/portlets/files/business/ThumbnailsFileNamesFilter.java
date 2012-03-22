package com.dotmarketing.portlets.files.business;

import java.io.FilenameFilter;
import java.util.List;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

public class ThumbnailsFileNamesFilter implements FilenameFilter {
	List<Versionable> versions;
	
	@SuppressWarnings("unchecked")
	public ThumbnailsFileNamesFilter (Identifier fileIden) {
		try {
			versions = APILocator.getVersionableAPI().findAllVersions(fileIden);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(), e);
		} 
		
	}
	
	public boolean accept(java.io.File dir, String name) {
		for (Versionable version : versions) {
			File file = (File)version;
			if (name.startsWith(String.valueOf(file.getInode()) + "_thumb") 
					|| name.startsWith(String.valueOf(file.getInode()) + "_resized")
					|| name.contains(WebKeys.GENERATED_FILE)
			)
				return true;
		}
		return false;
	}
}