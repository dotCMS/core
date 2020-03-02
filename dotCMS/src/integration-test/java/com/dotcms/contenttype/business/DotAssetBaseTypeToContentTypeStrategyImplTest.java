package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@RunWith(DataProviderRunner.class)
public class DotAssetBaseTypeToContentTypeStrategyImplTest  extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * 1) creates a dotAsset content type for text plain
     * 2) creates a temporal file asset and gets the id
     * 3) creates a map with the contentlet properties (including as a file asset the temporal file id)
     * 4) runs the BaseTypeToContentTypeStrategy in order to get the new content type using the temporal file
     * @throws Exception
     */
    @Test
    public void test_apply_with_temporal_file() throws Exception {

        // creates a dotAsset for text files
        final String variable = "testDotAsset" + System.currentTimeMillis();
        final ContentType dotAssetContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).
                save(ContentTypeBuilder.builder(DotAssetContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(variable)
                .owner(APILocator.systemUser().getUserId()).build());

        final Map<String, Field> fieldMap = dotAssetContentType.fieldMap();
        com.dotcms.contenttype.model.field.Field binaryField           = fieldMap.get(DotAssetContentType.ASSET_FIELD_VAR);
        final FieldVariable allowFileTypes = ImmutableFieldVariable.builder().key(BinaryField.ALLOWED_FILE_TYPES)
                .value("text/plain").fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(allowFileTypes));

        APILocator.getContentTypeAPI(APILocator.systemUser()).save(dotAssetContentType);
        APILocator.getContentTypeFieldAPI().save(binaryField, APILocator.systemUser());
        APILocator.getContentTypeFieldAPI().save(allowFileTypes, APILocator.systemUser());

        final Optional<BaseTypeToContentTypeStrategy> baseTypeToContentTypeStrategy =
                BaseTypeToContentTypeStrategyResolver.getInstance().get(BaseContentType.DOTASSET);

        Assert.assertTrue(baseTypeToContentTypeStrategy.isPresent());
        MockHeaderRequest request = new MockHeaderRequest(
                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));
        request.setHeader("User-Agent", "Fake-Agent");
        request.setHeader("Host", "localhost");
        request.setHeader("Origin", "localhost");

        final File file = FileUtil.createTemporalFile("test", "txt");
        final String content = "This is a test temporal file";
        try (final FileWriter fileWriter = new FileWriter(file)) {

            fileWriter.write(content);
        }

        final DotTempFile dotTempFile = APILocator.getTempFileAPI().createTempFile(
                "test"+System.currentTimeMillis(), request, com.liferay.util.FileUtil.createInputStream(file));
        final String sessionId        = UUIDGenerator.generateUuid();
        /*
        {"contentlet":
                {
                    "baseType":"dotAsset",
                    "asset":"temp_28069eb1f1"
                }
            }
         */ // creates a contentlet map without content type but dotAsset baseType with temporal file
        final Map<String, Object> map =  CollectionsUtils.map("baseType", "dotAsset", "asset", dotTempFile.id);

        final Optional<ContentType> contentTypeOpt = baseTypeToContentTypeStrategy.get().apply(BaseContentType.DOTASSET,
                CollectionsUtils.map("user", APILocator.systemUser(), "host", APILocator.systemHost(),
                        "contentletMap", map, "accessingList", Arrays.asList(APILocator.systemUser().getUserId(),
                                APILocator.getTempFileAPI().getRequestFingerprint(request), sessionId)));

        Assert.assertTrue(contentTypeOpt.isPresent());
        Assert.assertEquals("", contentTypeOpt.get().variable(), variable);
    }
}
