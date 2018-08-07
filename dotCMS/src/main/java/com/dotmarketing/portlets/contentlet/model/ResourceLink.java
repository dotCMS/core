package com.dotmarketing.portlets.contentlet.model;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

public class ResourceLink {

    private static final String HTTP_PREFIX = "http://";

    private static final String HTTPS_PREFIX = "https://";

    private static final String LANG_ID_PARAM = "?language_id=";

    private final String resourceLinkAsString;

    private final String resourceLinkUriAsString;

    private final String mimeType;

    private final FileAsset fileAsset;

    private final boolean editableAsText;

    private final boolean downloadRestricted;

    private ResourceLink(final String resourceLinkAsString, final String resourceLinkUriAsString,
            final String mimeType,
            final FileAsset fileAsset,
            final boolean editableAsText,
            final boolean downloadRestricted) {
        this.resourceLinkAsString = resourceLinkAsString;
        this.resourceLinkUriAsString = resourceLinkUriAsString;
        this.mimeType = mimeType;
        this.fileAsset = fileAsset;
        this.editableAsText = editableAsText;
        this.downloadRestricted = downloadRestricted;
    }

    public String getResourceLinkAsString() {
        return resourceLinkAsString;
    }

    public String getResourceLinkUriAsString() {
        return resourceLinkUriAsString;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileAsset getFileAsset() {
        return fileAsset;
    }

    public boolean isEditableAsText() {
        return editableAsText;
    }

    public boolean isDownloadRestricted() {
        return downloadRestricted;
    }

    @Override
    public String toString() {
        return resourceLinkAsString;
    }

    public static class ResourceLinkBuilder {

        public ResourceLink build(final HttpServletRequest request, final User user, final Contentlet contentlet) throws DotDataException, DotSecurityException {

            final StringBuilder resourceLink = new StringBuilder();
            final StringBuilder resourceLinkUri = new StringBuilder();

            final Identifier identifier = getIdentifier(contentlet);
            final Host host = getHost((String)request.getAttribute("host") , user);
            if (identifier != null && InodeUtils.isSet(identifier.getInode())){
                if(request.isSecure()){
                    resourceLink.append(HTTPS_PREFIX);
                }else{
                    resourceLink.append(HTTP_PREFIX);
                }
                resourceLink.append(host.getHostname());
                if(request.getServerPort() != 80 && request.getServerPort() != 443){
                    resourceLink.append(":").append(request.getServerPort());
                }
                resourceLinkUri.append(identifier.getParentPath()).append(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
                resourceLink.append(UtilMethods.encodeURIComponent(resourceLinkUri.toString()));
                resourceLinkUri.append(LANG_ID_PARAM).append(contentlet.getLanguageId());
                resourceLink.append(LANG_ID_PARAM).append(contentlet.getLanguageId());
            }

            final FileAsset fileAsset = getFileAsset(contentlet);
            final String mimeType = fileAsset.getMimeType();
            final String fileAssetName = fileAsset.getFileName();

            return new ResourceLink(resourceLink.toString(), resourceLinkUri.toString(), mimeType, fileAsset, isEdiatbleAsText(mimeType, fileAssetName), isDownloadRestricted(fileAssetName));
        }

        private static boolean isEdiatbleAsText(final String mimeType, final String fileAssetName ){
             return  (
                 !isRestrictedMimeType(mimeType) && (isEditableMimeType(mimeType) || fileAssetName.endsWith(".vm"))
             );
        }

        private static boolean isRestrictedMimeType(final String mimeType){
           return (mimeType.contains("officedocument") || mimeType.contains("svg"));
        }

        private static boolean isEditableMimeType(final String mimeType) {
            return (
                    mimeType.contains("text") ||
                    mimeType.contains("javascript") ||
                    mimeType.contains("json") ||
                    mimeType.contains("xml") ||
                    mimeType.contains("php")
            );
        }

        private static boolean isDownloadRestricted(final String fileAssetName ){
          return fileAssetName.endsWith(".vm") || fileAssetName.endsWith(".vtl");
        }

        Host getHost(final String host, final User user) throws DotDataException, DotSecurityException{
            return APILocator.getHostAPI().find(host , user, false);
        }

        Identifier getIdentifier(final Contentlet contentlet) throws DotDataException {
            return APILocator.getIdentifierAPI().find(contentlet);
        }

        FileAsset getFileAsset(final Contentlet contentlet){
           return APILocator.getFileAssetAPI().fromContentlet(contentlet);
        }
    }
}
