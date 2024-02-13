package com.dotmarketing.portlets.contentlet.util;

import com.dotcms.business.Unexportable;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This Utility Class provides useful methods to retrieve and transform specific Contentlet data.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
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
		return !(field.getClass().isAnnotationPresent(Unexportable.class));
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
		Map<String, Object> hostOrFolderMap = new HashMap<>();
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
				path = APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath();
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
		return new DotTransformerBuilder().contentResourceOptions(allCategoriesInfo)
				.siteToMapTransformer(false)
				.content(sourceContentlet).build().toMaps().get(0);
	}

	/**
	 * Generates a String with the most important or basic information in a Contentlet for it to be printed in our
	 * logs.
	 *
	 * @param contentlet The {@link Contentlet} that will be logged.
	 *
	 * @return The String containing the Contentlet's most important information.
	 */
	public static String toShortString(final Contentlet contentlet) {
		if (null == contentlet) {
			return null;
		}
		final String contentType = contentlet.getContentType() != null ? contentlet.getContentType().name() : "Unknown";
		return String.format("Contentlet [name: %s, type: %s, lang: %s, identifier: %s, inode: %s]",
				contentlet.getName(),
				contentType,
				contentlet.getLanguageId(),
				contentlet.getIdentifier(),
				contentlet.getInode()
		);
	}

	/**
	 * This method generates the copy suffix when there is a contentlet that already has the same
	 * URL.
	 * <ul>
	 * <li>if the new contentlet URL is NOT used then returns an empty suffix.</li>
	 * <li>if the new contentlet URL without "_copy" is used then returns a
	 * "_copy" suffix.</li>
	 * <li>if the new contentlet URL with or without "_copy" is used then
	 * returns a "_copy" plus timestamp in millis (example: "_copy_2122313123")
	 * suffix.</li>
	 * </ul>
	 *
	 * @param contentlet the contentlet that we are going to copy or move
	 * @param host
	 * @param folder     the destination folder
	 * @return the generated contentlet asset name suffix
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	public static String generateCopySuffix(final Contentlet contentlet, final Host host, final Folder folder)
			throws DotDataException, DotStateException, DotSecurityException {

		String assetNameSuffix = StringPool.BLANK;

		final boolean diffHost = ((host != null && contentlet.getHost() != null)
				&& !contentlet.getHost()
				.equalsIgnoreCase(host.getIdentifier()));

		// if different host we really don't need to
		if ((!contentlet.isFileAsset() && !contentlet.isHTMLPage()) && (
				diffHost || (folder != null && contentlet.getHost() != null) && !folder.getHostId()
						.equalsIgnoreCase(contentlet.getHost()))) {
			return assetNameSuffix;
		}

		final String sourcef = (UtilMethods.isSet(contentlet.getFolder())) ? contentlet.getFolder()
				: APILocator.getFolderAPI().findSystemFolder().getInode();
		final String destf = (UtilMethods.isSet(folder)) ? folder.getInode()
				: APILocator.getFolderAPI().findSystemFolder().getInode();

		if (!diffHost && sourcef.equals(destf)) { // is copying in the same folder and samehost?
			assetNameSuffix = "_copy";

			// We need to verify if already exist a content with suffix "_copy",
			// if already exists we need to append a timestamp
			if (isContentletUrlAlreadyUsed(contentlet, host, folder, assetNameSuffix)) {
				assetNameSuffix += "_" + System.currentTimeMillis();
			}
		} else {
			if (isContentletUrlAlreadyUsed(contentlet, host, folder, assetNameSuffix)) {
				throw new DotDataException(
						Sneaky.sneak(() -> LanguageUtil.get("error.copy.url.conflict")));
			}
		}

		return assetNameSuffix;
	}

	/**
	 * This method verifies if the contentlet that we are going to copy or cut into a folder doesn't
	 * have conflict with other contentlet that has the same URL.
	 *
	 * @param contentlet        the contentlet that we are going to copy or move
	 * @param destinationHost   the destination host
	 * @param destinationFolder the destination folder
	 * @param assetNameSuffix   the suffix string that we will append in the asset name. Sometimes
	 *                          you need to know if a asset name with a suffix is used or not
	 * @return true if the contentlet URL is already used otherwise returns false
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static boolean isContentletUrlAlreadyUsed(final Contentlet contentlet, final Host destinationHost,
											   final Folder destinationFolder, final String assetNameSuffix)
			throws DotStateException, DotDataException, DotSecurityException {
		Identifier contentletId = APILocator.getIdentifierAPI().find(contentlet);

		// Create new asset name
		final String contentletIdAssetName = contentletId.getAssetName();
		String fileExtension = StringPool.BLANK;
		if (contentlet.hasAssetNameExtension()) {
			final String ext = UtilMethods.getFileExtension(contentletIdAssetName);
			if (UtilMethods.isSet(ext)) {
				fileExtension = '.' + ext;
			}
		}
		final String futureAssetNameWithSuffix =
				UtilMethods.getFileName(contentletIdAssetName) + assetNameSuffix + fileExtension;

		// Check if page url already exist
		Identifier identifierWithSameUrl = null;
		if (UtilMethods.isSet(destinationFolder) && InodeUtils.isSet(
				destinationFolder.getInode())) { // Folders
			// Create new path
			Identifier folderId = APILocator.getIdentifierAPI()
					.find(destinationFolder.getIdentifier());
			final String path = (destinationFolder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ? "/"
					: folderId.getPath()) + futureAssetNameWithSuffix;

			final Host host =
					destinationFolder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ? destinationHost
							: APILocator.getHostAPI()
							.find(destinationFolder.getHostId(),
									APILocator.getUserAPI().getSystemUser(), false);
			identifierWithSameUrl = APILocator.getIdentifierAPI().find(host, path);
		} else if (UtilMethods.isSet(destinationHost) && InodeUtils.isSet(
				destinationHost.getInode())) { // Hosts
			identifierWithSameUrl = APILocator.getIdentifierAPI()
					.find(destinationHost, "/" + futureAssetNameWithSuffix);
		} else {
			// Host or folder object MUST be define
			Logger.error(ContentletUtil.class,
					"Host or folder destination are invalid, please check that one of those values are set propertly.");
			throw new DotDataException(
					"Host or folder destination are invalid, please check that one of those values are set propertly.");
		}

		return InodeUtils.isSet(identifierWithSameUrl.getId());
	}

}


