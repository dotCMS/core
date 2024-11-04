package com.dotmarketing.portlets.contentlet.model;

import static com.dotcms.exception.ExceptionUtil.getLocalizedMessageOrDefault;

import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.MimeTypeUtils;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.lang3.StringUtils;

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

    private final String idPath;

    private final String mimeType;

    private final Contentlet fileAsset;

    private final Identifier identifier;

    private final String fieldVar;

    private final boolean editableAsText;

    private final boolean downloadRestricted;

    private final String configuredImageURL;

    private ResourceLink(final String resourceLinkAsString, final String resourceLinkUriAsString,
            final String mimeType,
            final Contentlet fileAsset,
            final Identifier identifier,
            final String fieldVariable,
            final boolean editableAsText,
            final boolean downloadRestricted,
            final String versionPath,
            final String idPath,
             final String configuredImageURL) {

        this.resourceLinkAsString    = resourceLinkAsString;
        this.resourceLinkUriAsString = resourceLinkUriAsString;
        this.mimeType                = mimeType;
        this.fileAsset               = fileAsset;
        this.identifier              = identifier;
        this.fieldVar                = fieldVariable;
        this.editableAsText          = editableAsText;
        this.downloadRestricted      = downloadRestricted;
        this.versionPath             = versionPath;
        this.idPath                  = idPath;
        this.configuredImageURL      = configuredImageURL;
    }

    public String getConfiguredImageURL() {
        return configuredImageURL;
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

    public String getAssetName() {
        return identifier == null ? StringPool.BLANK : identifier.getAssetName();
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

    public String getIdPath() {
        return idPath;
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

            return build(request, user, contentlet, contentlet.isFileAsset()?FileAssetAPI.BINARY_FIELD: DotAssetContentType.ASSET_FIELD_VAR);
        }

        public final ResourceLink build(final HttpServletRequest request, final User user, final Contentlet contentlet, final String fieldVelocityVarName) throws DotDataException, DotSecurityException {

            final Metadata metadata = contentlet.getBinaryMetadata(fieldVelocityVarName);

            final Identifier identifier = getIdentifier(contentlet);
            if (metadata==null || identifier == null || UtilMethods.isEmpty(identifier.getInode())){

                return new ResourceLink(StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, null, null, StringPool.BLANK, false, true,
                        StringPool.BLANK, StringPool.BLANK, StringPool.BLANK);
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
            final String mimeType               = metadata.getContentType();
            final Tuple2<String, String> resourceLink      = createResourceLink(contentlet, identifier, metadata, hostUrlBuilder.toString());
            final Tuple2<String, String> versionPathIdPath = createVersionPathIdPath(contentlet, fieldVelocityVarName, metadata);
            final String configuredImageURL                = getConfiguredImageURL (contentlet, identifier, metadata, host);

            return new ResourceLink(resourceLink._1(), resourceLink._2(), mimeType, contentlet, identifier,
                    fieldVelocityVarName, isEditableAsText(mimeType, metadata.getName()), downloadRestricted,
                    versionPathIdPath._1(), versionPathIdPath._2(), configuredImageURL);
        }

        private String getConfiguredImageURL(final Contentlet contentlet, final Identifier identifier, final Metadata metadata, final Host host) {

            final  String pattern = Config.getStringProperty("WYSIWYG_IMAGE_URL_PATTERN", "/dA/{shortyInode}/{name}");
            return replaceUrlPattern(pattern, contentlet, identifier, metadata, host);
        }

        /**
         * Generates the URL for a File Asset, based on a specific URL pattern.
         *
         * @param pattern    The pattern for the URL that will be generated.
         * @param contentlet The File Asset as Content whose URL will be generated.
         * @param identifier If the {@code contentlet} parameter IS a File Asset,  use this parameter to get the asset's
         *                   name.
         * @param metadata   If the {@code contentlet} parameter IS NOT a File Asset, use this parameter to get the
         *                   asset's name.
         * @param site       Tha Site that the File Asset belongs to.
         *
         * @return The generated URL for the File Asset.
         */
        String replaceUrlPattern(final String pattern, final Contentlet contentlet, final Identifier identifier, final Metadata metadata, final Host site) {
            final String fileName  = contentlet.isFileAsset() ? identifier.getAssetName() : metadata.getName();
            final String path      = pattern.equals("{path}{name}") ? getPath(identifier) : getPath(contentlet);
            final String extension = UtilMethods.getFileExtension(fileName);
            final ShortyIdAPI shortyAPI = APILocator.getShortyAPI();
            String shortyId = contentlet.getIdentifier();
            Optional<ShortyId> shortyValueOpt = shortyAPI.getShorty(contentlet.getIdentifier());
            if (shortyValueOpt.isPresent()) {
                final String shortyIdValue = shortyValueOpt.get().shortId;
                shortyId = Try.of(() -> shortyAPI.shortify(shortyIdValue)).getOrElse(shortyId);
            }
            String shortyInode = contentlet.getInode();
            shortyValueOpt = APILocator.getShortyAPI().getShorty(contentlet.getInode());
            if (shortyValueOpt.isPresent()) {
                final String shortyInodeValue = shortyValueOpt.get().shortId;
                shortyInode = Try.of(() -> shortyAPI.shortify(shortyInodeValue)).getOrElse(shortyInode);
            }

            final StrBuilder patternBuilder = new StrBuilder(pattern);

            return patternBuilder
                    .replaceAll("{name}",        fileName)
                    .replaceAll("{fileName}",    fileName)
                    .replaceAll("{path}",        path)
                    .replaceAll("{extension}",   extension)
                    .replaceAll("{languageId}",  String.valueOf(contentlet.getLanguageId()))
                    .replaceAll("{hostname}",    site.getHostname())
                    .replaceAll("{hostName}",    site.getHostname())
                    .replaceAll("{inode}",       contentlet.getInode())
                    .replaceAll("{hostId}",      site.getIdentifier())
                    .replaceAll("{identifier}",  contentlet.getIdentifier())
                    .replaceAll("{id}",          contentlet.getIdentifier())
                    .replaceAll("{shortyInode}", shortyInode)
                    .replaceAll("{shortyId}",    shortyId)
                    .replaceAll("{shortyIdentifier}", shortyId).toString();
        }

        Tuple2<String, String> createVersionPathIdPath (final Contentlet contentlet, final String velocityVarName,
                                                                                final Metadata binaryMeta) throws DotDataException {

            final Map<String, Object> properties = BinaryToMapTransformer.transform(binaryMeta, contentlet,
                    APILocator.getContentTypeFieldAPI().byContentTypeAndVar(contentlet.getContentType(), velocityVarName));

            final String versionPath = (String)properties.get("versionPath");
            final String idPath      = (String)properties.get("idPath");

            return Tuple.of(versionPath, idPath);
        }

        Tuple2<String, String> createResourceLink (final Contentlet contentlet, final Identifier identifier, Metadata metadata,
                                                   final String hostUrl)  {

            final StringBuilder resourceLink    = new StringBuilder(hostUrl);
            final StringBuilder resourceLinkUri = new StringBuilder();
            final String assetName = contentlet.isFileAsset() ? identifier.getAssetName() : metadata.getName();
            resourceLinkUri.append(identifier.getParentPath()).append(assetName);
            resourceLink.append(escapeUri(resourceLinkUri.toString()));
            resourceLinkUri.append(LANG_ID_PARAM).append(contentlet.getLanguageId());
            resourceLink.append(LANG_ID_PARAM).append(contentlet.getLanguageId());

            return Tuple.of(resourceLink.toString(), resourceLinkUri.toString());
        }

        String escapeUri (final String uri) {

            if (uri.indexOf('+') != -1) {

                final String [] uriArray = uri.split("\\+");
                for (int i = 0; i < uriArray.length; ++i) {

                    uriArray[i] = UtilMethods.encodeURIComponent(uriArray[i]).replaceAll("\\+", "%20");
                }

                return StringUtils.joinWith("+", (Object[]) uriArray);
            }

            return UtilMethods.encodeURIComponent(uri).replaceAll("\\+", "%20");
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


        /**
         * Generates the url for a file asset based on specific structure FileLink, which is the host with its prefix + the path with the id + language id
         * This method is aiming to be used when exporting, so the binary field could have a valid url when imported.
         *
         * @param request
         * @param user
         * @param contentlet
         * @param field
         *
         * @return the url (File Link) for the file asset
         */
        public String getFileLink(final HttpServletRequest request, final User user, final Contentlet contentlet, final String field) throws DotDataException, DotSecurityException {

            ResourceLink link = this.build(request, user, contentlet, field);
            Host host = getHost((String)request.getAttribute(HOST_REQUEST_ATTRIBUTE) , user);
            if(null == host){
                host = getHost(contentlet.getHost(), user);
            }

            final StringBuilder hostUrlBuilder = new StringBuilder(request.isSecure()? HTTPS_PREFIX:HTTP_PREFIX);

            hostUrlBuilder.append(host.getHostname());
            hostUrlBuilder.append(link.getConfiguredImageURL());
            return hostUrlBuilder.toString();
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

    /**
     * Centralized code to get path
     * @param contentlet
     * @return
     */
    public static String getPath(final Contentlet contentlet){
        return  "/dA/" + contentlet.getIdentifier() + StringPool.SLASH;
    }

    /**
     * Centralized code to get classical file path
     * this path not include the dA prefix
     * @param identifier
     * @return
     */
    public static String getPath(final Identifier identifier){
        return identifier.getParentPath();
    }

}
