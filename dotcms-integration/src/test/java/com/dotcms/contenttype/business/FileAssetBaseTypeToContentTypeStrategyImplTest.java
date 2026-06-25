package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.util.WebKeys;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Integration test for {@link FileAssetBaseTypeToContentTypeStrategyImpl}, the strategy that
 * resolves a {@link BaseContentType#FILEASSET} content type from an uploaded binary. Mirrors
 * {@link DotAssetBaseTypeToContentTypeStrategyImplTest}.
 */
public class FileAssetBaseTypeToContentTypeStrategyImplTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private MockHeaderRequest mockRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request());
        request.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        request.setHeader("User-Agent", "Fake-Agent");
        request.setHeader("Host", "localhost");
        request.setHeader("Origin", "localhost");
        request.setAttribute(WebKeys.USER, APILocator.systemUser());
        return request;
    }

    private DotTempFile createTextTempFile(final MockHeaderRequest request) throws Exception {
        final File file = FileUtil.createTemporaryFile("test", "txt");
        try (final FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write("This is a test temporal file");
        }
        return APILocator.getTempFileAPI().createTempFile(
                "test" + System.currentTimeMillis(), request, com.liferay.util.FileUtil.createInputStream(file));
    }

    /**
     * Method to test: {@link FileAssetBaseTypeToContentTypeStrategyImpl#apply(BaseContentType, Map)}
     * Given Scenario: A File Asset content type whose "accept" field variable is {@code text/plain}
     *                 exists, and a temporal text file is uploaded with only {@code baseType: FILEASSET}.
     * Expected Result: The strategy resolves to that specific File Asset content type by mime type.
     */
    @Test
    public void test_apply_matches_specific_fileasset_by_mimetype() throws Exception {

        final String variable = "testFileAsset" + System.currentTimeMillis();
        final ContentType fileAssetContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(
                ContentTypeBuilder.builder(FileAssetContentType.class).folder(FolderAPI.SYSTEM_FOLDER)
                        .host(Host.SYSTEM_HOST).name(variable)
                        .owner(APILocator.systemUser().getUserId()).build());

        final Map<String, Field> fieldMap = fileAssetContentType.fieldMap();
        Field binaryField = fieldMap.get(FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR);
        final FieldVariable allowFileTypes = ImmutableFieldVariable.builder().key(BinaryField.ALLOWED_FILE_TYPES)
                .value("text/plain").fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(allowFileTypes));

        APILocator.getContentTypeAPI(APILocator.systemUser()).save(fileAssetContentType);
        APILocator.getContentTypeFieldAPI().save(binaryField, APILocator.systemUser());
        APILocator.getContentTypeFieldAPI().save(allowFileTypes, APILocator.systemUser());

        final Optional<BaseTypeToContentTypeStrategy> strategy =
                BaseTypeToContentTypeStrategyResolver.getInstance().get(BaseContentType.FILEASSET);
        Assert.assertTrue("A FILEASSET strategy must be registered", strategy.isPresent());

        final MockHeaderRequest request = this.mockRequest();
        final DotTempFile dotTempFile = this.createTextTempFile(request);
        final String sessionId = UUIDGenerator.generateUuid();

        // contentlet map without content type, only FILEASSET baseType + the temporal file
        final Map<String, Object> map = Map.of("baseType", "FILEASSET", "fileAsset", dotTempFile.id);
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        final Optional<ContentType> contentTypeOpt = strategy.get().apply(BaseContentType.FILEASSET,
                Map.of("user", APILocator.systemUser(), "host", defaultHost,
                        "contentletMap", map, "accessingList", Arrays.asList(APILocator.systemUser().getUserId(),
                                APILocator.getTempFileAPI().getRequestFingerprint(request), sessionId)));

        Assert.assertTrue(contentTypeOpt.isPresent());
        Assert.assertEquals(variable.toLowerCase(), contentTypeOpt.get().variable().toLowerCase());
        Assert.assertEquals(BaseContentType.FILEASSET, contentTypeOpt.get().baseType());
    }

    /**
     * Method to test: {@link FileAssetBaseTypeToContentTypeStrategyImpl#apply(BaseContentType, Map)}
     * Given Scenario: No File Asset content type targets the uploaded file's mime type specifically;
     *                 a binary is uploaded with only {@code baseType: FILEASSET}.
     * Expected Result: The strategy resolves to a File Asset content type — the system default
     *                  {@code FileAsset} (matched as the total-wildcard catch-all / safety fallback).
     */
    @Test
    public void test_apply_resolves_default_fileasset_when_no_specific_match() throws Exception {

        final Optional<BaseTypeToContentTypeStrategy> strategy =
                BaseTypeToContentTypeStrategyResolver.getInstance().get(BaseContentType.FILEASSET);
        Assert.assertTrue("A FILEASSET strategy must be registered", strategy.isPresent());

        final MockHeaderRequest request = this.mockRequest();
        final DotTempFile dotTempFile = this.createTextTempFile(request);
        final String sessionId = UUIDGenerator.generateUuid();

        final Map<String, Object> map = Map.of("baseType", "FILEASSET", "fileAsset", dotTempFile.id);
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        final Optional<ContentType> contentTypeOpt = strategy.get().apply(BaseContentType.FILEASSET,
                Map.of("user", APILocator.systemUser(), "host", defaultHost,
                        "contentletMap", map, "accessingList", Arrays.asList(APILocator.systemUser().getUserId(),
                                APILocator.getTempFileAPI().getRequestFingerprint(request), sessionId)));

        Assert.assertTrue("A FILEASSET content type must always be resolved", contentTypeOpt.isPresent());
        Assert.assertEquals(BaseContentType.FILEASSET, contentTypeOpt.get().baseType());
    }

    /**
     * Method to test: {@link DotAssetAPI#tryMatch(String, Host, com.liferay.portal.model.User)} via the
     * extracted {@link BaseTypeMimeTypeMatcher}.
     * Given Scenario: The dotAsset mime matching is now delegated to the shared matcher.
     * Expected Result: The default {@code FileAsset} content type is resolvable directly, confirming the
     *                  shared matcher works for the FILEASSET base type as it does for DOTASSET.
     */
    @Test
    public void test_matcher_resolves_default_fileasset_for_any_mimetype() throws DotDataException, DotSecurityException {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Optional<ContentType> matched = new BaseTypeMimeTypeMatcher().match(
                "application/octet-stream", defaultHost, APILocator.systemUser(),
                BaseContentType.FILEASSET, FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR);

        Assert.assertTrue(matched.isPresent());
        Assert.assertEquals(BaseContentType.FILEASSET, matched.get().baseType());
        Assert.assertEquals(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME.toLowerCase(),
                matched.get().variable().toLowerCase());
    }
}
