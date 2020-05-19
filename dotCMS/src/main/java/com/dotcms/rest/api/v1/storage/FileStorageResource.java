package com.dotcms.rest.api.v1.storage;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.storage.ContentletMetadataAPI;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.GenerateMetaDataConfiguration;
import com.dotcms.storage.RequestMetaData;
import com.dotcms.storage.StorageKey;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.workflow.form.FireActionForm;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Path("/v1/storage")
public class FileStorageResource {

    private final WebResource    webResource;
    private final FileStorageAPI fileStorageAPI;
    private final MultiPartUtils multiPartUtils;
    private final ContentletAPI contentletAPI;
    private final ContentletMetadataAPI contentletMetadataAPI;

    @VisibleForTesting
    protected FileStorageResource(final FileStorageAPI fileStorageAPI,
                                  final WebResource webResource,
                                  final MultiPartUtils multiPartUtils,
                                  final ContentletAPI contentletAPI,
                                  final ContentletMetadataAPI contentletMetadataAPI) {

        this.fileStorageAPI = fileStorageAPI;
        this.webResource    = webResource;
        this.multiPartUtils = multiPartUtils;
        this.contentletAPI  = contentletAPI;
        this.contentletMetadataAPI = contentletMetadataAPI;
    }

    public FileStorageResource() {
        this(APILocator.getFileStorageAPI(), new WebResource(),
                new MultiPartUtils(), APILocator.getContentletAPI(), APILocator.getContentletMetadataAPI());
    }

    @PUT
    @Path("/metadata/basic")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response generateMetadata(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                               final FormDataMultiPart multipart) throws IOException, JSONException {

        final Tuple2<Map<String,Object>, List<File>> bodyAndBinaries =
                this.multiPartUtils.getBodyMapAndBinariesFromMultipart(multipart);
        final List<File> files            = bodyAndBinaries._2();
        final Map<String, Object> bodyMap = bodyAndBinaries._1();

        final ImmutableMap.Builder<String, Object> bodyResultBuilder = new ImmutableMap.Builder<>();
        if (!UtilMethods.isSet(files)) {

            throw new BadRequestException("Must send files to generate the metadata");
        }

        for (final File file : files) {

            if (bodyMap.isEmpty()) {

                bodyResultBuilder.put(file.getName(), this.fileStorageAPI.generateRawBasicMetaData(file));
            } else {

                generateMetadata(bodyMap, bodyResultBuilder, file, false);
            }
        }

        return Response.ok(new ResponseEntityView(bodyResultBuilder.build())).build();
    }

    private GenerateMetaDataConfiguration buildConfig (final Map<String, Object> bodyMap,
                                                        final boolean full) throws JSONException {

        final GenerateMetaDataConfiguration.Builder builder = new GenerateMetaDataConfiguration.Builder();
        builder.full(full);

        if (bodyMap.containsKey("cacheKey")) {

            builder.cache(() -> (String) bodyMap.get("cacheKey"));
        }

        if (bodyMap.containsKey("override")) {

            builder.override(ConversionUtils.toBoolean(bodyMap.get("override").toString(), false));
        }

        if (bodyMap.containsKey("maxLength")) {

            builder.maxLength(ConversionUtils.toLong(bodyMap.get("maxLength"), FileStorageAPI.configuredMaxLength()));
        }

        if (bodyMap.containsKey("fields")) {

            final JSONArray fieldJSONArray = (JSONArray)bodyMap.get("fields");
            final Set<String> fieldSet = new HashSet<>();
            for (int i = 0; i < fieldJSONArray.length(); ++i) {

                fieldSet.add(fieldJSONArray.getString(i));
            }
            builder.metaDataKeyFilter(fieldSet::contains);
        }

        if (bodyMap.containsKey("file")) {

            final String metadataBucketName = Config.getStringProperty("METADATA_BUCKET_NAME", "dotmetadata");
            final JSONObject fileMap = (JSONObject) bodyMap.get("file");
            final String inode       = fileMap.getString("id");
            final String fileName    = fileMap.getString("name");
            final String path        = (StringUtils.builder(File.separator,
                    inode.charAt(0), File.separator, inode.charAt(1), File.separator, inode, File.separator,
                    fileName).toString());
            builder.storageKey(new StorageKey.Builder().bucket(metadataBucketName).path(path).build());
        }

        return builder.build();
    }

    private void generateMetadata(final Map<String, Object> bodyMap,
                                  final ImmutableMap.Builder<String, Object> bodyResultBuilder,
                                  final File file, final boolean full) throws JSONException {

        bodyResultBuilder.put(file.getName(), this.fileStorageAPI.generateMetaData(file, buildConfig(bodyMap, full)));
    }

    @PUT
    @Path("/metadata/full")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response generateFullMetadata(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               final FormDataMultiPart multipart) throws IOException, JSONException {

        final Tuple2<Map<String,Object>, List<File>> bodyAndBinaries =
                this.multiPartUtils.getBodyMapAndBinariesFromMultipart(multipart);
        final List<File> files            = bodyAndBinaries._2();
        final Map<String, Object> bodyMap = bodyAndBinaries._1();

        final ImmutableMap.Builder<String, Object> bodyResultBuilder = new ImmutableMap.Builder<>();
        if (!UtilMethods.isSet(files)) {

            throw new BadRequestException("Must send files to generate the metadata");
        }

        for (final File file : files) {

            if (bodyMap.isEmpty()) {

                bodyResultBuilder.put(file.getName(), this.fileStorageAPI.generateRawFullMetaData(file, FileStorageAPI.configuredMaxLength()));
            } else {

                generateMetadata(bodyMap, bodyResultBuilder, file, true);
            }
        }

        return Response.ok(new ResponseEntityView(bodyResultBuilder.build())).build();
    }

    @PUT
    @Path("/metadata")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response generateContentletMetadata(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               final MetadataForm metadataForm) throws IOException, DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                                                        .requestAndResponse(request, response)
                                                        .rejectWhenNoUser(true)
                                                        .requiredBackendUser(true).init();

        if (!UtilMethods.isSet(metadataForm) ||
                (!UtilMethods.isSet(metadataForm.getIdentifier()) && !UtilMethods.isSet(metadataForm.getInode()))) {

            throw new BadRequestException("Must send identifier or inode");
        }

        final long languageId = UtilMethods.isSet(metadataForm.getLanguage())?
                LanguageUtil.getLanguageId(metadataForm.getLanguage()):-1;

        Logger.debug(this, ()-> "Generating the metadata for: " + metadataForm);

        final Contentlet contentlet = this.getContentlet(metadataForm.getInode(), metadataForm.getIdentifier(), languageId,
                ()-> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(), initDataObject, PageMode.get(request));

        return Response.ok(new ResponseEntityView(this.contentletMetadataAPI.generateContentletMetadata(contentlet))).build();
    }

    private Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        PageMode mode         = pageMode;

        if(UtilMethods.isSet(inode)) {

            contentlet = this.contentletAPI.find
                    (inode, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.notNull(contentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);
        } else if (UtilMethods.isSet(identifier)) {

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<ShortyId> shortyIdOptional = APILocator.getShortyAPI().getShorty(identifier);
            final String longIdentifier = shortyIdOptional.isPresent()? shortyIdOptional.get().longId:identifier;
            final Optional<Contentlet> currentContentlet =  language <= 0?
                    this.getContentletByIdentifier(longIdentifier, mode, initDataObject.getUser(), sessionLanguage):
                    this.contentletAPI.findContentletByIdentifierOrFallback
                            (longIdentifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.isTrue(currentContentlet.isPresent(), ()-> "contentlet-was-not-found", DoesNotExistException.class);

            contentlet = currentContentlet.get();
        }

        DotPreconditions.notNull(contentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);

        return contentlet;
    }

    @CloseDBIfOpened
    public Optional<Contentlet> getContentletByIdentifier(final String identifier,
                                                          final PageMode mode,
                                                          final User user,
                                                          final Supplier<Long> sessionLanguageSupplier) throws DotDataException {

        Contentlet contentlet = null;
        final long sessionLanguage  = sessionLanguageSupplier.get();

        if(sessionLanguage > 0) {

            contentlet = Try.of(()->this.contentletAPI.findContentletByIdentifier
                    (identifier, mode.showLive, sessionLanguage, user, mode.respectAnonPerms)).getOrNull();
        }

        if (null == contentlet) {

            final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            if (defaultLanguage != sessionLanguage) {

                contentlet =  Try.of(()->this.contentletAPI.findContentletByIdentifier
                        (identifier, mode.showLive, defaultLanguage, user, mode.respectAnonPerms)).getOrNull();
            }
        }

        return null == contentlet?
                Optional.ofNullable(this.contentletAPI.findContentletByIdentifierAnyLanguage(identifier)):
                Optional.ofNullable(contentlet);
    }
    /////////
    @PUT
    @Path("/metadata/get")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getMetadata(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      final Map<String, Object> bodyMap)  {

        if (UtilMethods.isSet(bodyMap)) {

            final RequestMetaData.Builder builder = new RequestMetaData.Builder();
            if (bodyMap.containsKey("cacheKey")) {

                builder.cache(() -> (String) bodyMap.get("cacheKey"));
            }

            if (bodyMap.containsKey("file")) {

                final String metadataBucketName = Config.getStringProperty("METADATA_BUCKET_NAME", "dotmetadata");
                final Map fileMap = (Map) bodyMap.get("file");
                final String inode       = (String)fileMap.get("id");
                final String fileName    = (String)fileMap.get("name");
                final String path        = (StringUtils.builder(File.separator,
                        inode.charAt(0), File.separator, inode.charAt(1), File.separator, inode, File.separator,
                        fileName).toString());
                builder.storageKey(new StorageKey.Builder().bucket(metadataBucketName).path(path).build());
            }

            return Response.ok(new ResponseEntityView(this.fileStorageAPI.retrieveMetaData(builder.build()))).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("/metadata/content/get")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getContentMetadata(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     final MetadataForm metadataForm) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true).init();

        if (!UtilMethods.isSet(metadataForm) ||
                (!UtilMethods.isSet(metadataForm.getIdentifier()) && !UtilMethods.isSet(metadataForm.getInode()))) {

            throw new BadRequestException("Must send identifier or inode");
        }

        if (!UtilMethods.isSet(metadataForm.getField())) {

            throw new BadRequestException("Must send field");
        }

        final long languageId = UtilMethods.isSet(metadataForm.getLanguage())?
                LanguageUtil.getLanguageId(metadataForm.getLanguage()):-1;

        Logger.debug(this, ()-> "Generating the metadata for: " + metadataForm);

        final Contentlet contentlet = this.getContentlet(metadataForm.getInode(), metadataForm.getIdentifier(), languageId,
                ()-> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(), initDataObject, PageMode.get(request));

        final Map<String, Field> fieldMap = contentlet.getContentType().fieldMap();

        if (!fieldMap.containsKey(metadataForm.getField())) {
            throw new BadRequestException("Field variable sent, is not valid for the contentlet: " + contentlet.getIdentifier());
        }

        return Response.ok(new ResponseEntityView(
                metadataForm.isNocache()?
                        this.contentletMetadataAPI.getMetadataNoCache(contentlet, metadataForm.getField()):
                        this.contentletMetadataAPI.getMetadata(contentlet, metadataForm.getField()))).build();
    }

}
