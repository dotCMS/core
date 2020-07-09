package com.dotmarketing.portlets.contentlet.util;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

	public static boolean isNewFieldTypeAllowedOnImportExport(final com.dotcms.contenttype.model.field.Field field){
		return field instanceof LineDividerField ||
				field instanceof FileField ||
				field instanceof ImageField ||
				field instanceof TabDividerField ||
				field instanceof ColumnField ||
				field instanceof RowField;
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
		sourceContentlet.setTags();
		return new DotTransformerBuilder().contentResourceOptions(allCategoriesInfo).content(sourceContentlet).build().toMaps().get(0);
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


