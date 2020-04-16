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

    private final Contentlet fileAsset;

    private final String fieldVar;

    private final boolean editableAsText;

    private final boolean downloadRestricted;

    private ResourceLink(final String resourceLinkAsString, final String resourceLinkUriAsString,
            final String mimeType,
            final Contentlet fileAsset,
            final String fieldVariable,
            final boolean editableAsText,
            final boolean downloadRestricted) {

        this.resourceLinkAsString = resourceLinkAsString;
        this.resourceLinkUriAsString = resourceLinkUriAsString;
        this.mimeType = mimeType;
        this.fileAsset = fileAsset;
        this.fieldVar = fieldVariable;
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

    public Contentlet getFileAsset() {
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

            if(!(contentlet.isFileAsset() || contentlet.isDotAsset())) {

                throw new DotStateException(getLocalizedMessageOrDefault(user,"File-asset-contentlet-type-expected",
                        "Can only build Resource Links out of content with type `File Asset` or `DotAsset`.",getClass())
                );
            }

            return build(request, user, contentlet, FileAssetAPI.BINARY_FIELD);
        }

        public final ResourceLink build(final HttpServletRequest request, final User user, final Contentlet contentlet, final String velocityVarName) throws DotDataException, DotSecurityException {

            final File binary           = Try.of(()->contentlet.getBinary(velocityVarName)).getOrNull();
            final Identifier identifier = getIdentifier(contentlet);
            if (binary==null || identifier == null && UtilMethods.isEmpty(identifier.getInode())){

                return new ResourceLink(StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, null, StringPool.BLANK, false, true);
            }

            final boolean downloadRestricted    = isDownloadPermissionBasedRestricted(contentlet, user);
            final StringBuilder resourceLink    = new StringBuilder();
            final StringBuilder resourceLinkUri = new StringBuilder();

            Host host = getHost((String)request.getAttribute(HOST_REQUEST_ATTRIBUTE) , user);
            if(null == host){
                host = getHost(contentlet.getHost(), user);
            }

            resourceLink.append(request.isSecure()? HTTPS_PREFIX:HTTP_PREFIX);
            resourceLink.append(host.getHostname());

            if(request.getServerPort() != 80 && request.getServerPort() != 443) {

                resourceLink.append(StringPool.COLON).append(request.getServerPort());
            }

            resourceLinkUri.append(identifier.getParentPath()).append(binary.getName());
            resourceLink.append(UtilMethods.encodeURIComponent(resourceLinkUri.toString()));
            resourceLinkUri.append(LANG_ID_PARAM).append(contentlet.getLanguageId());
            resourceLink.append(LANG_ID_PARAM).append(contentlet.getLanguageId());

            final String mimeType      = this.getMimiType(binary);
            final String fileAssetName = binary.getName();

            return new ResourceLink(resourceLink.toString(), resourceLinkUri.toString(), mimeType, contentlet,
                    velocityVarName, isEditableAsText(mimeType, fileAssetName), downloadRestricted);
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

        String getMimiType (final File binary) {

            return MimeTypeUtils.getMimeType(binary);
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
