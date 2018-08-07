package com.dotmarketing.portlets.contentlet.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

public class ResourceLink {

    private String resourceLinkAsString;

    private String resourceLinkUriAsString;

    private String mimeType;

    private FileAsset fileAsset;

    private boolean editableAsText;

    private boolean downloadRestricted;

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

        public static ResourceLink build(final HttpServletRequest request, final User user, final Contentlet contentlet) throws DotDataException, DotSecurityException {

            final StringBuilder resourceLink = new StringBuilder();
            String resourceLinkUri = "";

            final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
            final Host host = APILocator.getHostAPI().find((String)request.getAttribute("host") , user, false);
            if (identifier != null && InodeUtils.isSet(identifier.getInode())){
                if(request.isSecure()){
                    resourceLink.append("https://");
                }else{
                    resourceLink.append("http://");
                }
                resourceLink.append(host.getHostname());
                if(request.getServerPort() != 80 && request.getServerPort() != 443){
                    resourceLink.append(":").append(request.getServerPort());
                }
                resourceLinkUri = identifier.getParentPath() + contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);
                resourceLink.append(UtilMethods.encodeURIComponent(resourceLinkUri));
                resourceLinkUri+="?language_id="+contentlet.getLanguageId();
                resourceLink.append("?language_id=").append(contentlet.getLanguageId());
            }

            final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
            final String mimeType = fileAsset.getMimeType();
            final String fileAssetName = fileAsset.getFileName();

            return new ResourceLink(resourceLink.toString(), resourceLinkUri, mimeType, fileAsset, isEdiatbleAsText(mimeType, fileAssetName), isDownloadRestricted(fileAssetName));
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

    }
}
