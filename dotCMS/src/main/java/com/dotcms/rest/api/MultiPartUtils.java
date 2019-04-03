package com.dotcms.rest.api;

import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.BodyPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.ContentDisposition;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataBodyPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataMultiPart;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi part utils
 * Can provide the files in the multifile in addition to the map values (string, object) from different parts
 * @author
 */
public class MultiPartUtils {


    private static final String NAME = "name";
    private static final String JSON = "json";
    private static final String FILE = "file";

    private final FileAssetAPI fileAssetAPI;

    public MultiPartUtils() {

        this(APILocator.getFileAssetAPI());
    }

    @VisibleForTesting
    public MultiPartUtils(final FileAssetAPI fileAssetAPI) {
        this.fileAssetAPI = fileAssetAPI;
    }

    /**
     * Gets the body map and binaries in a single call
     * @param multipart {@link FormDataMultiPart}
     * @return Tuple2 Map and list of files
     * @throws IOException
     * @throws JSONException
     */
    public Tuple2<Map<String,Object>, List<File>> getBodyMapAndBinariesFromMultipart(final FormDataMultiPart multipart) throws IOException, JSONException {

        return Tuple.of(this.getBodyMapFromMultipart(multipart), this.getBinariesFromMultipart(multipart));
    }

    /**
     * Get a summary of the json's in the multipart into the result
     * @param multipart {@link FormDataBodyPart}
     * @return Map (String, Object) result
     * @throws IOException
     * @throws JSONException
     */
    public Map<String, Object> getBodyMapFromMultipart(final FormDataMultiPart multipart) throws IOException, JSONException {

        final Map<String, Object> bodyMap = new HashMap<>();

        for (final BodyPart part : multipart.getBodyParts()) {

            final ContentDisposition contentDisposition = part.getContentDisposition();
            final String name =
                    contentDisposition != null && contentDisposition.getParameters().containsKey(NAME)?
                            contentDisposition.getParameters().get(NAME):
                            StringPool.BLANK;

            if (MediaType.APPLICATION_JSON_TYPE.equals(part.getMediaType()) || JSON.equalsIgnoreCase(name)) {

                bodyMap.putAll(WebResource.processJSON(part.getEntityAs(InputStream.class)));
            }
        }

        return bodyMap;
    } // getBodyMapFromMultipart.


    /**
     * Get the binaries, they will be already saved on the assets.
     * @param multipart {@link FormDataBodyPart}
     * @return List of Files
     * @throws IOException
     */
    public List<File> getBinariesFromMultipart(final FormDataMultiPart multipart) throws IOException {

        final List<File> binaries = new ArrayList<>();

        for (final FormDataBodyPart part : multipart.getFields(FILE)) {

            final File tmpFolder = new File(
                    this.fileAssetAPI.getRealAssetPathTmpBinary() + UUIDUtil.uuid());

            if(!tmpFolder.mkdirs()) {

                throw new IOException("Unable to create temp folder to save binaries");
            }

            final String filename = part.getContentDisposition().getFileName();
            final File tempFile   = new File(
                    tmpFolder.getAbsolutePath() + File.separator + filename);

            Files.deleteIfExists(tempFile.toPath());

            FileUtils.copyInputStreamToFile(part.getEntityAs(InputStream.class), tempFile);
            binaries.add(tempFile);
        }

        return binaries;
    } // getBinariesFromMultipart.


}
