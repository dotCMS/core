package com.dotmarketing.portlets.contentlet.util;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.rest.ContentHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContentletUtil {

	private static final ImmutableSet<String> fieldTypesToExcludeFromImportExport = ImmutableSet.of(
			FieldType.LINE_DIVIDER.toString(),
			FieldType.FILE.toString(),
			FieldType.IMAGE.toString(),
			FieldType.TAB_DIVIDER.toString(),
			FieldType.COLUMN.toString(),
			FieldType.ROW.toString(),
			FieldType.BUTTON.toString()
	);


	public static boolean isFieldTypeAllowedOnImportExport(final Field field){
		return !fieldTypesToExcludeFromImportExport.contains(field.getFieldType());
	}

	public static boolean isHost(final Contentlet contentlet){
		final Structure hostStrucuture = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(Host.HOST_VELOCITY_VAR_NAME);
		return contentlet.getStructureInode().equals(hostStrucuture.getInode());
	}

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
	 * @param contentlet the contentlet to generate the printable map from
	 *
	 * @return Contentlet with the values in place.
	 *
	 * @throws DotDataException
	 * @throws IOException 
     */
	public static Map<String, Object> getContentPrintableMap(
			final User user, final Contentlet contentlet)
			throws DotDataException, IOException {
		return getContentPrintableMap(user, contentlet, false);
	}

	/**
	 * Returns a {@link Map} that includes original content's map entries and also special entries for string
	 * representation of the values of binary and category field. It also set the tags to the contentlet's map for them
	 * to be included in the resulting map. This map can be used to return a string representation of the given content
	 * (e.g. REST, ES portlet)
	 *
	 * @param user User from Front End with permission to read Special Fields.
	 * @param sourceContentlet the contentlet to generate the printable map from
	 * @param allCategoriesInfo {@code "true"} to return all fields for
	 * the categories associated to the content (key, name, description),{@code "false"}
	 * to return only categories names.
	 *
	 * @return Contentlet with the values in place.
	 *
	 * @throws DotDataException
	 * @throws IOException
	 */
	public static Map<String, Object> getContentPrintableMap(
			final User user, final Contentlet sourceContentlet, final boolean allCategoriesInfo)
			throws DotDataException, IOException {

		Map<String, Object> m = new HashMap<>();

		sourceContentlet.setTags();
		final Contentlet contentlet = ContentHelper.getInstance().hydrateContentlet(sourceContentlet);
		m.putAll(contentlet.getMap());

		ContentType type=contentlet.getContentType();
		m.put("contentType", type.variable());
        m.put("baseType", type.baseType().name());
		for(com.dotcms.contenttype.model.field.Field f : type.fields()){
			if(f instanceof BinaryField){
			  File fsFile = contentlet.getBinary(f.variable());
			  if(fsFile !=null && fsFile.exists()) {
			    m.put(f.variable() + "Version", "/dA/" +  contentlet.getInode() + "/" + f.variable() + "/" + fsFile.getName()  );
				m.put(f.variable(), "/dA/" +  contentlet.getIdentifier() + "/" + f.variable() + "/" + fsFile.getName()	);
				m.put(f.variable() + "ContentAsset", contentlet.getIdentifier() + "/" +f.variable()	);
			  }
			} else if(f instanceof CategoryField) {

				List<Category> cats = null;
				try {
					cats = APILocator.getCategoryAPI().getParents(contentlet, user, true);
				} catch (Exception e) {
					Logger.error(ContentletUtil.class, String.format("Unable to get the Categories for given contentlet with inode= %s", contentlet.getInode()));
				}

				if(cats!=null && !cats.isEmpty()) {
					try {
						final Category parentCategory        = APILocator.getCategoryAPI().find(f.values(), user, true);
						final List<Category> childCategories = new ArrayList<>();

						if(parentCategory != null) {
							for (Category category : cats) {
								if (APILocator.getCategoryAPI().isParent(category, parentCategory, user,true)) {
									childCategories.add(category);
								}
							}
						}

						if (!childCategories.isEmpty()){
							Object categoriesValue;
							if (allCategoriesInfo) {
								categoriesValue = childCategories.stream()
										.map(Category::getMap)
										.collect(Collectors.toList());
							} else {
								categoriesValue = childCategories.stream()
										.map(Category::getCategoryName)
										.collect(Collectors.joining(", "));
							}
							m.put(f.variable(), categoriesValue);
						}
					} catch (DotSecurityException e) {
						Logger.error(ContentletUtil.class, String.format("Unable to get the Categories for given contentlet with inode= %s", contentlet.getInode()));
					}
				}
			}
		}

		if (type .baseType() == BaseContentType.HTMLPAGE ||type .baseType() == BaseContentType.FILEASSET){
			m.put("path", APILocator.getIdentifierAPI().find(contentlet.getIdentifier()).getPath());
		}
		try {
            m.put("hostName", APILocator.getHostAPI().find(contentlet.getHost(), user, true).getHostname());
        } catch (Exception e) {
            Logger.warn(ContentletUtil.class, "unable to get host:" + contentlet.getHost() + " : " + e.getMessage());
        }

		return m;
	}

	/**
	 * Utility method to improve the info in our logs
	 * @param contentlet
	 * @return
	 */
	public static String toShortString(final Contentlet contentlet) {
		if (null == contentlet) {
			return null;
		}

		final String contentType = contentlet.getContentType() != null ? contentlet.getContentType().name() : "Unknown";

		return String.format("Contentlet[name: %s, type: %s, lang: %s ,identifier: %s, inode: %s]",
				contentlet.getName(),
				contentType,
				contentlet.getLanguageId(),
				contentlet.getIdentifier(),
				contentlet.getInode()
		);
	}

}


