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
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple4;
import io.vavr.control.Try;

import java.io.File;
import java.io.IOException;
import java.util.Map;
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

    private final String versionPath;

    private final String versionPathUri;

    private final String idPath;

    private final String idPathUri;

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
            final boolean downloadRestricted,
            final String versionPath,
            final String versionPathUri,
            final String idPath,
            final String idPathUri) {

        this.resourceLinkAsString    = resourceLinkAsString;
        this.resourceLinkUriAsString = resourceLinkUriAsString;
        this.mimeType                = mimeType;
        this.fileAsset               = fileAsset;
        this.fieldVar                = fieldVariable;
        this.editableAsText          = editableAsText;
        this.downloadRestricted      = downloadRestricted;
        this.versionPath             = versionPath;
        this.versionPathUri          = versionPathUri;
        this.idPath                  = idPath;
        this.idPathUri               = idPathUri;
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

    public String getVersionPath() {
        return versionPath;
    }

    public String getVersionPathUri() {
        return versionPathUri;
    }

    public String getIdPath() {
        return idPath;
    }

    public String getIdPathUri() {
        return idPathUri;
    }

    public String getFieldVar() {
        return fieldVar;
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

                return new ResourceLink(StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, null, StringPool.BLANK, false, true,
                        StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK);
            }

            Host host = getHost((String)request.getAttribute(HOST_REQUEST_ATTRIBUTE) , user);
            if(null == host){
                host = getHost(contentlet.getHost(), user);
            }

            final StringBuilder hostUrlBuilder = new StringBuilder(request.isSecure()? HTTPS_PREFIX:HTTP_PREFIX);
            hostUrlBuilder.append(host.getHostname());

            if(request.getServerPort() != 80 && request.getServerPort() != 443) {

                hostUrlBuilder.append(StringPool.COLON).append(request.getServerPort());
            }

            final boolean downloadRestricted    = isDownloadPermissionBasedRestricted(contentlet, user);
            final String mimeType               = this.getMimiType(binary);
            final String fileAssetName          = binary.getName();
            final Tuple2<String, String> resourceLink = this.createResourceLink(request, user, contentlet, identifier, binary, hostUrlBuilder.toString());
            final Tuple4<String, String, String, String> versionPathIdPath = this.createVersionPathIdPath(contentlet,
                    velocityVarName, binary, hostUrlBuilder.toString());

            return new ResourceLink(resourceLink._1(), resourceLink._2(), mimeType, contentlet,
                    velocityVarName, isEditableAsText(mimeType, fileAssetName), downloadRestricted,
                    versionPathIdPath._1(), versionPathIdPath._2(), versionPathIdPath._3(), versionPathIdPath._4());
        }

        private Tuple4<String, String, String, String> createVersionPathIdPath (final Contentlet contentlet, final String velocityVarName,
                                                                                final File binary, final String hostUrl) throws DotDataException {

            final BinaryToMapTransformer transformer = new BinaryToMapTransformer(contentlet);
            final Map<String, Object> properties = transformer.transform(binary, contentlet,
                    APILocator.getContentTypeFieldAPI().byContentTypeAndVar(contentlet.getContentType(), velocityVarName));

            final String versionPath = (String)properties.get("versionPath");
            final String idPath      = (String)properties.get("idPath");

            return Tuple.of(hostUrl + versionPath, versionPath, hostUrl + idPath, idPath);
        }

        private Tuple2<String, String> createResourceLink (final HttpServletRequest request, final User user,
                                                           final Contentlet contentlet, final Identifier identifier,
                                                           final File binary, final String hostUrl) throws DotSecurityException, DotDataException {

            final StringBuilder resourceLink    = new StringBuilder(hostUrl);
            final StringBuilder resourceLinkUri = new StringBuilder();

            resourceLinkUri.append(identifier.getParentPath()).append(binary.getName());
            resourceLink.append(UtilMethods.encodeURIComponent(resourceLinkUri.toString()));
            resourceLinkUri.append(LANG_ID_PARAM).append(contentlet.getLanguageId());
            resourceLink.append(LANG_ID_PARAM).append(contentlet.getLanguageId());

            return Tuple.of(resourceLink.toString(), resourceLinkUri.toString());
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
