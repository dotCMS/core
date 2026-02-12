package com.dotcms.contenttype.test;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.SiteDataGen;
import org.apache.commons.io.FileUtils;
import com.dotcms.util.TestMediaCreator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DotAssetAPITest extends ContentTypeBaseTest  {

    private final List<ContentType> contentTypesToCleanUp = new ArrayList<>();
    private Host testSite;

    @After
    public void tearDown() {
        for (final ContentType contentType : contentTypesToCleanUp) {
            try {
                ContentTypeDataGen.remove(contentType);
            } catch (Exception e) {
                Logger.warn(this, "Failed to clean up content type: " + contentType.variable(), e);
            }
        }
        contentTypesToCleanUp.clear();

        if (testSite != null) {
            try {
                APILocator.getHostAPI().archive(testSite, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(testSite, APILocator.systemUser(), false);
            } catch (Exception e) {
                Logger.warn(this, "Failed to clean up test site: " + testSite.getHostname(), e);
            }
            testSite = null;
        }
    }

    @Test
    public void test_try_match () throws Exception {

        // Use a dedicated test site instead of system host to ensure test content types
        // have priority over any pre-existing system host content types in tryMatch()
        // (site-specific types are matched before system host types)
        testSite = new SiteDataGen().nextPersisted();

        final Tuple2<Field, ContentType> fieldDotVideoAssetContentType = this.createDotAssetContentType(testSite,
                "video/*", "videoDotAsset" + System.currentTimeMillis());

        final Tuple2<Field, ContentType> fieldDotTextAssetContentType = this.createDotAssetContentType(testSite,
                "text/*", "textDotAsset" + System.currentTimeMillis());

        final Tuple2<Field, ContentType> fieldDotimageAssetContentType = this.createDotAssetContentType(testSite,
                "image/*", "imageDotAsset" + System.currentTimeMillis());

        final File tempTestFile = File
                .createTempFile("fileTest_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, "Test hi this a test longer than ten characters");

        Optional<ContentType> contentTypeOpt = Optional.empty();

        final File tempMovieTestFile = TestMediaCreator.lookupMOV();
        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempMovieTestFile, testSite, APILocator.systemUser());

        assertTrue(contentTypeOpt.isPresent());
        assertEquals(fieldDotVideoAssetContentType._2().variable(), contentTypeOpt.get().variable());

        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempTestFile, testSite, APILocator.systemUser());

        assertTrue(contentTypeOpt.isPresent());
        assertEquals(fieldDotTextAssetContentType._2().variable(), contentTypeOpt.get().variable());

        final File tempImageTestFile = TestMediaCreator.lookupPNG();
        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempImageTestFile, testSite, APILocator.systemUser());

        assertTrue(contentTypeOpt.isPresent());
        assertEquals(fieldDotimageAssetContentType._2().variable(), contentTypeOpt.get().variable());

        final File tempImageTestFile2 = TestMediaCreator.lookupJPG();
        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempImageTestFile2, testSite, APILocator.systemUser());

        assertTrue(contentTypeOpt.isPresent());
        assertEquals(fieldDotimageAssetContentType._2().variable(), contentTypeOpt.get().variable());

    }

    private Tuple2<Field, ContentType> createDotAssetContentType (final Host host, final String accept, final String variable) throws DotSecurityException, DotDataException {

        final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        ContentType dotAssetContentType     = contentTypeAPI
                .save(ContentTypeBuilder.builder(DotAssetContentType.class).folder(FolderAPI.SYSTEM_FOLDER)
                        .host(host.getIdentifier()).name(variable)
                        .owner(user.getUserId()).build());
        final Map<String, Field> fieldMap = dotAssetContentType.fieldMap();
        com.dotcms.contenttype.model.field.Field binaryField           = fieldMap.get(DotAssetContentType.ASSET_FIELD_VAR);
        final FieldVariable allowFileTypes = ImmutableFieldVariable.builder().key(BinaryField.ALLOWED_FILE_TYPES)
                .value(accept).fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(allowFileTypes));

        dotAssetContentType = contentTypeAPI.save(dotAssetContentType);
        binaryField = fieldAPI.save(binaryField, user);
        fieldAPI.save(allowFileTypes, user);

        // Register for cleanup to prevent test pollution
        contentTypesToCleanUp.add(dotAssetContentType);

        return Tuple.of(binaryField, dotAssetContentType);
    }



}
