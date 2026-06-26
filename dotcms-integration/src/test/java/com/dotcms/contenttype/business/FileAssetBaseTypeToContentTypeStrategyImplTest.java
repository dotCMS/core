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
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.SiteDataGen;
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
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.util.WebKeys;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integration test for {@link FileAssetBaseTypeToContentTypeStrategyImpl}, the strategy that
 * resolves a {@link BaseContentType#FILEASSET} content type from an uploaded binary. Mirrors
 * {@link DotAssetBaseTypeToContentTypeStrategyImplTest}.
 *
 * <p>Each test runs on its own dedicated site so the File Asset content types it creates have
 * priority over (and don't collide with) the system default {@code FileAsset} or any content type
 * left over from other tests; everything created is removed in {@link #tearDown()}.</p>
 */
public class FileAssetBaseTypeToContentTypeStrategyImplTest extends IntegrationTestBase {

    private final List<ContentType> contentTypesToCleanUp = new ArrayList<>();
    private Host testSite;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

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

    /**
     * Creates a File Asset content type on the given host whose binary field accepts the given mime
     * type(s), and registers it for cleanup.
     */
    private ContentType createFileAssetContentType(final Host host, final String accept, final String variable)
            throws DotSecurityException, DotDataException {

        ContentType fileAssetContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(
                ContentTypeBuilder.builder(FileAssetContentType.class).folder(FolderAPI.SYSTEM_FOLDER)
                        .host(host.getIdentifier()).name(variable)
                        .owner(APILocator.systemUser().getUserId()).build());

        final Map<String, Field> fieldMap = fileAssetContentType.fieldMap();
        Field binaryField = fieldMap.get(FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR);
        final FieldVariable allowFileTypes = ImmutableFieldVariable.builder().key(BinaryField.ALLOWED_FILE_TYPES)
                .value(accept).fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(allowFileTypes));

        APILocator.getContentTypeAPI(APILocator.systemUser()).save(fileAssetContentType);
        APILocator.getContentTypeFieldAPI().save(binaryField, APILocator.systemUser());
        APILocator.getContentTypeFieldAPI().save(allowFileTypes, APILocator.systemUser());

        contentTypesToCleanUp.add(fileAssetContentType);
        return fileAssetContentType;
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

    private Optional<ContentType> applyStrategy(final Map<String, Object> contentletMap, final Host host,
                                                final MockHeaderRequest request) {
        final Optional<BaseTypeToContentTypeStrategy> strategy =
                BaseTypeToContentTypeStrategyResolver.getInstance().get(BaseContentType.FILEASSET);
        Assert.assertTrue("A FILEASSET strategy must be registered", strategy.isPresent());

        final String sessionId = UUIDGenerator.generateUuid();
        return strategy.get().apply(BaseContentType.FILEASSET,
                Map.of("user", APILocator.systemUser(), "host", host,
                        "contentletMap", contentletMap, "accessingList", Arrays.asList(APILocator.systemUser().getUserId(),
                                APILocator.getTempFileAPI().getRequestFingerprint(request), sessionId)));
    }

    /**
     * Method to test: {@link FileAssetBaseTypeToContentTypeStrategyImpl#apply(BaseContentType, Map)}
     * Given Scenario: A File Asset content type whose "accept" is {@code text/plain} exists on the
     *                 current site, and a temporal text file is uploaded with only {@code baseType: FILEASSET}.
     * Expected Result: The strategy resolves to that specific File Asset content type by mime type
     *                  (an exact match on the current site has the highest precedence).
     */
    @Test
    public void test_apply_matches_specific_fileasset_by_mimetype() throws Exception {

        testSite = new SiteDataGen().nextPersisted();
        final String variable = "textFileAsset" + System.currentTimeMillis();
        this.createFileAssetContentType(testSite, "text/plain", variable);

        final MockHeaderRequest request = this.mockRequest();
        final DotTempFile dotTempFile = this.createTextTempFile(request);
        final Map<String, Object> map = Map.of("baseType", "FILEASSET", "fileAsset", dotTempFile.id);

        final Optional<ContentType> contentTypeOpt = this.applyStrategy(map, testSite, request);

        Assert.assertTrue(contentTypeOpt.isPresent());
        Assert.assertEquals(variable.toLowerCase(), contentTypeOpt.get().variable().toLowerCase());
        Assert.assertEquals(BaseContentType.FILEASSET, contentTypeOpt.get().baseType());
    }

    /**
     * Method to test: {@link FileAssetBaseTypeToContentTypeStrategyImpl#apply(BaseContentType, Map)}
     * Given Scenario: The only File Asset content type on the current site accepts {@code video/*},
     *                 and a text file (which it does not accept) is uploaded with {@code baseType: FILEASSET}.
     * Expected Result: The strategy still resolves a FILEASSET content type — falling back to the
     *                  system default {@code FileAsset} (the total-wildcard catch-all). We assert the
     *                  resolved base type rather than a specific variable, since multiple default
     *                  File Asset types may exist in a shared environment.
     */
    @Test
    public void test_apply_resolves_a_fileasset_type_when_no_specific_match() throws Exception {

        testSite = new SiteDataGen().nextPersisted();
        this.createFileAssetContentType(testSite, "video/*", "videoFileAsset" + System.currentTimeMillis());

        final MockHeaderRequest request = this.mockRequest();
        final DotTempFile dotTempFile = this.createTextTempFile(request);
        final Map<String, Object> map = Map.of("baseType", "FILEASSET", "fileAsset", dotTempFile.id);

        final Optional<ContentType> contentTypeOpt = this.applyStrategy(map, testSite, request);

        Assert.assertTrue("A FILEASSET content type must always be resolved", contentTypeOpt.isPresent());
        Assert.assertEquals(BaseContentType.FILEASSET, contentTypeOpt.get().baseType());
    }

    /**
     * Method to test: {@link BaseTypeMimeTypeMatcher#match(String, Host, com.liferay.portal.model.User, BaseContentType, String)}
     * Given Scenario: A File Asset content type whose "accept" is {@code image/*} exists on the current site.
     * Expected Result: The matcher resolves {@code image/png} to that content type (a partial-wildcard
     *                  match on the current site beats the system default wildcard).
     */
    @Test
    public void test_matcher_resolves_fileasset_by_mimetype() throws Exception {

        testSite = new SiteDataGen().nextPersisted();
        final String variable = "imageFileAsset" + System.currentTimeMillis();
        this.createFileAssetContentType(testSite, "image/*", variable);

        final Optional<ContentType> matched = new BaseTypeMimeTypeMatcher().match(
                "image/png", testSite, APILocator.systemUser(),
                BaseContentType.FILEASSET, FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR);

        Assert.assertTrue(matched.isPresent());
        Assert.assertEquals(BaseContentType.FILEASSET, matched.get().baseType());
        Assert.assertEquals(variable.toLowerCase(), matched.get().variable().toLowerCase());
    }
}
