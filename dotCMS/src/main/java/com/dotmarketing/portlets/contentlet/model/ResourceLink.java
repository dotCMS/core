package com.dotmarketing.portlets.contentlet.model;

import static com.dotcms.exception.ExceptionUtil.getLocalizedMessageOrDefault;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/***
 * This class is the result of a refactoring from an old JSP Snippet that was originally located in:
 * `html/portlet/ext/contentlet/field/edit_field.jsp`
 */
public class ResourceLink {

    private static final Set<String> RESTRICTED_FILE_EXTENSIONS = ImmutableSet.of("vm","vtl");

    static final String HOST_REQUEST_ATTRIBUTE = "host";

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

        public final ResourceLink build(final HttpServletRequest request, final User user, final Contentlet contentlet) throws DotDataException, DotSecurityException {

            if(!(contentlet.getContentType() instanceof FileAssetContentType) &&
                    (contentlet.getBaseType().isPresent() && contentlet.getBaseType().get() != BaseContentType.DOTASSET)) {

                throw new DotStateException(getLocalizedMessageOrDefault(user,"File-asset-contentlet-type-expected",
                        "Can only build Resource Links out of content with type `File Asset` or `DotAsset`.",getClass())
                );
            }

            final Identifier identifier = getIdentifier(contentlet);
            if (identifier != null && InodeUtils.isSet(identifier.getInode())){

                final boolean downloadRestricted = isDownloadPermissionBasedRestricted(contentlet, user);

                final StringBuilder resourceLink = new StringBuilder();
                final StringBuilder resourceLinkUri = new StringBuilder();

                Host host = getHost((String)request.getAttribute(HOST_REQUEST_ATTRIBUTE) , user);
                if(null == host){
                   host = getHost(contentlet.getHost(), user);
                }

                if(request.isSecure()){
                    resourceLink.append(HTTPS_PREFIX);
                }else{
                    resourceLink.append(HTTP_PREFIX);
                }
                resourceLink.append(host.getHostname());
                if(request.getServerPort() != 80 && request.getServerPort() != 443){
                    resourceLink.append(StringPool.COLON).append(request.getServerPort());
                }

                resourceLinkUri.append(identifier.getParentPath()).append(contentlet.getContentType() instanceof FileAssetContentType?
                        contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD): contentlet.getTitle());
                resourceLink.append(UtilMethods.encodeURIComponent(resourceLinkUri.toString()));
                resourceLinkUri.append(LANG_ID_PARAM).append(contentlet.getLanguageId());
                resourceLink.append(LANG_ID_PARAM).append(contentlet.getLanguageId());

                if (contentlet.getContentType() instanceof FileAssetContentType) {

                    final FileAsset fileAsset = getFileAsset(contentlet);
                    final String mimeType = fileAsset.getMimeType();
                    final String fileAssetName = fileAsset.getFileName();
                    return new ResourceLink(resourceLink.toString(), resourceLinkUri.toString(), mimeType, fileAsset, isEditableAsText(mimeType, fileAssetName), downloadRestricted);
                } else {

                    final File file = Try.of(()->contentlet.getBinary(DotAssetContentType.ASSET_FIELD_VAR)).getOrNull();
                    final String mimeType      = null != file? MimeTypeUtils.getMimeType(file):FileAsset.UNKNOWN_MIME_TYPE; // todo: this metadata should be taken from the metadata cache instead of the file.
                    final String fileAssetName = null != file?file.getName():FileAsset.UNKNOWN_MIME_TYPE;
                    return new ResourceLink(resourceLink.toString(), resourceLinkUri.toString(), mimeType, null, isEditableAsText(mimeType, fileAssetName), downloadRestricted);
                }
            }

            return new ResourceLink(StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, null, false, true);
        }

        private static boolean isEditableAsText(final String mimeType, final String fileAssetName ){
             return  mimeType != null && fileAssetName != null && (
                 !isRestrictedMimeType(mimeType) && (isEditableMimeType(mimeType) || fileAssetName.endsWith(".vm"))
             );
        }

        private static boolean isRestrictedMimeType(final String mimeType){
           return (mimeType.contains("officedocument") || mimeType.contains("svg"));
        }

        private static boolean isEditableMimeType(final String mimeType) {
            return mimeType != null && (
                    mimeType.contains("text") ||
                            mimeType.contains("javascript") ||
                            mimeType.contains("json") ||
                            mimeType.contains("xml") ||
                            mimeType.contains("php")
            );
        }

        Host getHost(final String hostId, final User user) throws DotDataException, DotSecurityException{
            return APILocator.getHostAPI().find(hostId , user, false);
        }

        Identifier getIdentifier(final Contentlet contentlet) throws DotDataException {
            Identifier identifier = null;
            if(!contentlet.isNew()){
                try {
                    identifier = APILocator.getIdentifierAPI().find(contentlet);
                }catch(Exception e){
                    Logger.warn(getClass(),"Unable to get identifier from contentlet", e);
                }
            }
            return identifier;
        }

        FileAsset getFileAsset(final Contentlet contentlet){
           return APILocator.getFileAssetAPI().fromContentlet(contentlet);
        }

        boolean isDownloadPermissionBasedRestricted(final Contentlet contentlet, final User user) throws DotDataException {
            return ResourceLink.isDownloadPermissionBasedRestricted(contentlet, user);
        }

    }

    /**
     *
     * @param contentlet
     * @param user
     * @return
     * @throws DotDataException
     */
    private static boolean isDownloadPermissionBasedRestricted(final Contentlet contentlet, final User user) throws DotDataException {
        return !APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false);
    }

    /**
     *  This method is used to determined if a front-end user based on the file the extension should be allowed for download or not.
     * @param fileAssetName
     * @param contentlet
     * @param user
     * @param request
     * @return
     * @throws DotDataException
     */
    public static boolean isDownloadRestricted(final String fileAssetName, final Contentlet contentlet, final User user, final HttpServletRequest request) throws DotDataException{
        final String extension = UtilMethods.getFileExtension(fileAssetName);
        if(RESTRICTED_FILE_EXTENSIONS.contains(extension)){
            //if we're not on admin mode or we just happen to be an anonymous user (null) we must restrict access right away.
            if(user == null || !PageMode.get(request).isAdmin){
               return true;
            }
            //if we're navigating on the backend then we should still restrict access based on permissions.
            return isDownloadPermissionBasedRestricted(contentlet, user);
        }
        return false;
    }

}
