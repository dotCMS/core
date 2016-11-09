package com.dotmarketing.portlets.contentlet.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContentletUtil {
	
	//http://jira.dotmarketing.net/browse/DOTCMS-3393
	public static Map<String, Object> getHostFolderInfo(String hostFolderId, User user) throws DotDataException, DotSecurityException {
		String hostName = "";
		String path = "";
		String hostFolderPath ="";
		Map<String, Object> hostOrFolderMap = new HashMap<String, Object>();
		Host systemHost = APILocator.getHostAPI().findSystemHost(APILocator.getUserAPI().getSystemUser(), false);
				
		if(!UtilMethods.isSet(hostFolderId) || hostFolderId.equals("allHosts")){
			hostFolderId="";
			hostFolderPath = "";
			hostOrFolderMap.put("host", "");
			hostOrFolderMap.put("folder", "");
			hostOrFolderMap.put("path", "");
		}
		if(hostFolderId.equalsIgnoreCase(systemHost.getIdentifier()) ||
				hostFolderId.equalsIgnoreCase(FolderAPI.SYSTEM_FOLDER)){
			hostFolderPath = "all";
			hostOrFolderMap.put("host", "");
			hostOrFolderMap.put("folder", "");
			hostOrFolderMap.put("path", hostFolderPath);
			return hostOrFolderMap;
		}
		if(InodeUtils.isSet(hostFolderId)){
			Host host = APILocator.getHostAPI().find(hostFolderId, user, false);
			if(host != null) {
				hostName = host.getHostname();	
				hostOrFolderMap.put("path", hostName);
				hostOrFolderMap.put("host", host.getIdentifier());
				hostOrFolderMap.put("folder", "");
			} else {
				Folder folder = APILocator.getFolderAPI().find(hostFolderId,user,false);
				path = APILocator.getIdentifierAPI().find(folder).getPath();
				host = APILocator.getHostAPI().find(folder.getHostId(), user, false);
				hostName = host.getHostname();
				path.substring(path.length()-1);
				hostFolderPath = hostName + path;
				hostOrFolderMap.put("path",hostFolderPath);
				hostOrFolderMap.put("host", host.getIdentifier());
				hostOrFolderMap.put("folder", folder.getInode());
			}
		}	

		return hostOrFolderMap;
	}
	
	public static String sanitizeFileName(String fileName){
		return FileUtil.sanitizeFileName(fileName);
	}

	/**
	 * Returns a {@link Map} that includes original content's map entries and also special entries for string
	 * representation of the values of binary and category field. It also set the tags to the contentlet's map for them
	 * to be included in the resulting map. This map can be used to return a string representation of the given content
	 * (e.g. REST, ES portlet)
	 *
	 * @param user User from Front End with permission to read Special Fields.
	 * @param c the contentlet to generate the printable map from
	 *
	 * @return Contentlet with the values in place.
	 *
	 * @throws DotDataException
     */
	public static Map<String, Object> getContentPrintableMap(User user, Contentlet c) throws DotDataException {
		Map<String, Object> m = new HashMap<>();

		c.setTags();

		m.putAll(c.getMap());

		Structure s = c.getStructure();

		for(Field f : FieldsCache.getFieldsByStructureInode(s.getInode())){
			if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){
				m.put(f.getVelocityVarName(), "/contentAsset/raw-data/" +  c.getIdentifier() + "/" + f.getVelocityVarName()	);
				m.put(f.getVelocityVarName() + "ContentAsset", c.getIdentifier() + "/" +f.getVelocityVarName()	);
			} else if(f.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
				List<Category> cats = null;
				try {
					cats = APILocator.getCategoryAPI().getParents(c, user, true);
				} catch (Exception e) {
					Logger.error(ContentletUtil.class, String.format("Unable to get the Categories for given contentlet with inode= %s", c.getInode()));
				}

				if(cats!=null && !cats.isEmpty()) {
					String catsStr = cats.stream().map(Category::getCategoryName).collect(Collectors.joining(", "));
					m.put(f.getVelocityVarName(), catsStr);
				}
			}
		}

		return m;
	}
	
}


