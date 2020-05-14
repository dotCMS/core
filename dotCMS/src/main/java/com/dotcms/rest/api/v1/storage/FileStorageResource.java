package com.dotcms.rest.api.v1.storage;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.GenerateMetaDataConfiguration;
import com.dotcms.storage.RequestMetaData;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import io.vavr.Tuple2;
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

@Path("/v1/storage")
public class FileStorageResource {

    private final WebResource    webResource;
    private final FileStorageAPI fileStorageAPI;
    private final MultiPartUtils multiPartUtils;

    @VisibleForTesting
    protected FileStorageResource(final FileStorageAPI fileStorageAPI,
                               final WebResource webResource,
                               final MultiPartUtils multiPartUtils) {

        this.fileStorageAPI = fileStorageAPI;
        this.webResource    = webResource;
        this.multiPartUtils = multiPartUtils;
    }

    public FileStorageResource() {
        this(APILocator.getFileStorageAPI(), new WebResource(), new MultiPartUtils());
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

            builder.maxLength(ConversionUtils.toInt(bodyMap.get("maxLength"), FileStorageAPI.configuredMaxLength()));
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

            final JSONObject fileMap = (JSONObject) bodyMap.get("file");
            final String inode       = fileMap.getString("id");
            final String fileName    = fileMap.getString("name");
            final File contentMetaFile = new File(StringUtils.builder(
                    APILocator.getFileAssetAPI().getRealAssetsRootPath(), File.separator,
                    inode.charAt(0), File.separator, inode.charAt(1), File.separator, inode, File.separator,
                    fileName).toString());
            builder.metaDataFileSupplier(() -> Optional.of(contentMetaFile));
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

    /////////
    @PUT
    @Path("/metadata/get")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getMetadata(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      final Map<String, Object> bodyMap) throws JSONException {

        if (UtilMethods.isSet(bodyMap)) {

            final RequestMetaData.Builder builder = new RequestMetaData.Builder();
            if (bodyMap.containsKey("cacheKey")) {

                builder.cache(() -> (String) bodyMap.get("cacheKey"));
            }

            if (bodyMap.containsKey("file")) {

                final Map fileMap = (Map) bodyMap.get("file");
                final String inode       = (String)fileMap.get("id");
                final String fileName    = (String)fileMap.get("name");
                final File contentMetaFile = new File(StringUtils.builder(
                        APILocator.getFileAssetAPI().getRealAssetsRootPath(), File.separator,
                        inode.charAt(0), File.separator, inode.charAt(1), File.separator, inode, File.separator,
                        fileName).toString());
                builder.metaDataFileSupplier(() -> Optional.of(contentMetaFile));
            }

            return Response.ok(new ResponseEntityView(this.fileStorageAPI.retrieveMetaData(builder.build()))).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).build();
    }

}
