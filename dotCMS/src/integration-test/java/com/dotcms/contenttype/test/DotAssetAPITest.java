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
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.TestMediaCreator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DotAssetAPITest extends ContentTypeBaseTest  {

    @Test
    public void test_try_match () throws Exception {

        final Host host = APILocator.systemHost();
        final Tuple2<Field, ContentType> fieldDotVideoAssetContentType = this.createDotAssetContentType(host,
                "video/*", "videoDotAsset" + System.currentTimeMillis());

        final Tuple2<Field, ContentType> fieldDotTextAssetContentType = this.createDotAssetContentType(host,
                "text/*", "textDotAsset" + System.currentTimeMillis());

        final Tuple2<Field, ContentType> fieldDotimageAssetContentType = this.createDotAssetContentType(host,
                "image/*", "imageDotAsset" + System.currentTimeMillis());

        final File tempTestFile = File
                .createTempFile("fileTest_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, "Test hi this a test longer than ten characters");

        Optional<ContentType> contentTypeOpt = Optional.empty();

        final File tempMovieTestFile = new File(ConfigTestHelper.getPathToTestResource("images/testmovie.mov"));
        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempMovieTestFile, host, APILocator.systemUser());

        assertTrue(contentTypeOpt.isPresent());
        assertEquals(fieldDotVideoAssetContentType._2().variable(), contentTypeOpt.get().variable());

        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempTestFile, host, APILocator.systemUser());

        assertTrue(contentTypeOpt.isPresent());
        assertEquals(fieldDotTextAssetContentType._2().variable(), contentTypeOpt.get().variable());

        final File tempImageTestFile = TestMediaCreator.createPNG();
        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempImageTestFile, host, APILocator.systemUser());

        assertTrue(contentTypeOpt.isPresent());
        assertEquals(fieldDotimageAssetContentType._2().variable(), contentTypeOpt.get().variable());

        final File tempImageTestFile2 = TestMediaCreator.createJPEG();
        contentTypeOpt = APILocator.getDotAssetAPI().tryMatch(
                tempImageTestFile2, host, APILocator.systemUser());

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

        return Tuple.of(binaryField, dotAssetContentType);
    }



}
