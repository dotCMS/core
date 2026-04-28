package com.dotcms.ai.api;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integration tests for {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)} method.
 *
 * <p>This test class validates all scenarios for determining whether a contentlet should be
 * processed by the AI Vision API for tag generation, including:</p>
 * <ul>
 *   <li>Content type validation (presence of TagField)</li>
 *   <li>Already tagged detection</li>
 *   <li>File validation (existence, size, type)</li>
 *   <li>AI secrets configuration</li>
 * </ul>
 *
 * @author Erick Gonzalez
 */
public class OpenAIVisionAPIImplTest {

    private static Host host;
    private static OpenAIVisionAPIImpl visionAPI;
    private static File testImageFile;
    private static ContentType contentTypeWithTags;
    private static ContentType contentTypeWithoutTags;
    private static Field binaryField;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();

        visionAPI = new OpenAIVisionAPIImpl();
        host = new SiteDataGen().nextPersisted();

        // Setup AI secrets for the test host
        setupAISecrets();

        // Load test image
        final URL imageUrl = FocalPointAPITest.class.getResource("/images/test.jpg");
        if (imageUrl != null) {
            testImageFile = new File(imageUrl.getFile());
        } else {
            // Create a minimal test image if not found
            testImageFile = createTestImage();
        }

        // Create content type with tag field
        final List<Field> fieldsWithTags = new ArrayList<>();
        binaryField = new FieldDataGen()
                .type(BinaryField.class)
                .velocityVarName("binaryField")
                .next();
        fieldsWithTags.add(binaryField);
        fieldsWithTags.add(new FieldDataGen()
                .type(TagField.class)
                .velocityVarName(DotAssetContentType.TAGS_FIELD_VAR)
                .next());

        contentTypeWithTags = new ContentTypeDataGen()
                .fields(fieldsWithTags)
                .host(host)
                .nextPersisted();

        // Create content type without tag field
        final List<Field> fieldsWithoutTags = new ArrayList<>();
        fieldsWithoutTags.add(new FieldDataGen()
                .type(BinaryField.class)
                .velocityVarName("binaryField")
                .next());

        contentTypeWithoutTags = new ContentTypeDataGen()
                .fields(fieldsWithoutTags)
                .host(host)
                .nextPersisted();
    }

    @AfterClass
    public static void afterClass() throws DotDataException, DotSecurityException {
        // Clean up created content types
        if (contentTypeWithTags != null) {
            Try.run(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
                    .delete(contentTypeWithTags));
        }
        if (contentTypeWithoutTags != null) {
            Try.run(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
                    .delete(contentTypeWithoutTags));
        }
        if (host != null) {
            Try.run(() -> APILocator.getHostAPI().archive(host, APILocator.systemUser(), false));
            Try.run(() -> APILocator.getHostAPI().delete(host, APILocator.systemUser(), false));
        }
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: Content type does not have a TagField
     * ExpectedResult: Returns false because tags cannot be applied without a tag field
     */
    @Test
    public void test_shouldProcessTags_noTagField_returnsFalse() throws Exception {
        // Create contentlet without tag field
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithoutTags.id())
                .host(host)
                .setProperty("binaryField", testImageFile)
                .nextPersisted();

        final Optional<Field> binaryFieldOpt = contentTypeWithoutTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertFalse("Should not process tags when content type has no tag field", result);
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: Contentlet is already tagged by AI (contains TAGGED_BY_DOTAI)
     * ExpectedResult: Returns false to avoid reprocessing already tagged content
     */
    @Test
    public void test_shouldProcessTags_alreadyTagged_returnsFalse() throws Exception {
        // Create contentlet with tag field
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithTags.id())
                .host(host)
                .setProperty("binaryField", testImageFile)
                .setProperty(DotAssetContentType.TAGS_FIELD_VAR, OpenAIVisionAPIImpl.TAGGED_BY_DOTAI.toLowerCase())
                .nextPersisted();

        final Optional<Field> binaryFieldOpt = contentTypeWithTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertFalse("Should not process tags when already tagged by AI", result);
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: Binary field has no file
     * ExpectedResult: Returns false because there's no image to process
     */
    @Test
    public void test_shouldProcessTags_noFile_returnsFalse() throws Exception {
        // Create contentlet without setting binary field
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithTags.id())
                .host(host)
                .nextPersisted();

        final Optional<Field> binaryFieldOpt = contentTypeWithTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertFalse("Should not process tags when no file exists", result);
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: File is too small (less than 100 bytes)
     * ExpectedResult: Returns false because file is likely invalid or corrupted
     */
    @Test
    public void test_shouldProcessTags_fileTooSmall_returnsFalse() throws Exception {
        // Create a very small file (less than 100 bytes)
        final File tinyFile = File.createTempFile("tiny", ".jpg");
        FileUtil.write(tinyFile, "small");

        Assert.assertTrue("Tiny file should be less than 100 bytes", tinyFile.length() < 100);

        // Create contentlet with tiny file
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithTags.id())
                .host(host)
                .setProperty("binaryField", tinyFile)
                .nextPersisted();

        final Optional<Field> binaryFieldOpt = contentTypeWithTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertFalse("Should not process tags when file is too small", result);

        // Cleanup
        tinyFile.delete();
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: File is not an image (text file)
     * ExpectedResult: Returns false because AI vision only processes images
     */
    @Test
    public void test_shouldProcessTags_notAnImage_returnsFalse() throws Exception {
        // Create a text file
        final File textFile = File.createTempFile("document", ".txt");
        FileUtil.write(textFile, "This is a text file with enough content to be larger than 100 bytes. " +
                "Adding more text to ensure it passes the size check but fails the image type check.");

        Assert.assertTrue("Text file should be larger than 100 bytes", textFile.length() > 100);

        // Create contentlet with text file
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithTags.id())
                .host(host)
                .setProperty("binaryField", textFile)
                .nextPersisted();

        final Optional<Field> binaryFieldOpt = contentTypeWithTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertFalse("Should not process tags when file is not an image", result);

        // Cleanup
        textFile.delete();
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: No AI secrets configured for the host
     * ExpectedResult: Returns false because AI API cannot be called without credentials
     */
    @Test
    public void test_shouldProcessTags_noSecrets_returnsFalse() throws Exception {
        // Create a new host without AI secrets
        final Host hostWithoutSecrets = new SiteDataGen().nextPersisted();

        try {
            // Create content type for this host
            final List<Field> fields = new ArrayList<>();
            fields.add(new FieldDataGen()
                    .type(BinaryField.class)
                    .velocityVarName("binaryField")
                    .next());
            fields.add(new FieldDataGen()
                    .type(TagField.class)
                    .velocityVarName(DotAssetContentType.TAGS_FIELD_VAR)
                    .next());

            final ContentType contentType = new ContentTypeDataGen()
                    .fields(fields)
                    .host(hostWithoutSecrets)
                    .nextPersisted();

            // Create contentlet
            final Contentlet contentlet = new ContentletDataGen(contentType.id())
                    .host(hostWithoutSecrets)
                    .setProperty("binaryField", testImageFile)
                    .nextPersisted();

            final Optional<Field> binaryFieldOpt = contentType.fields()
                    .stream()
                    .filter(f -> f instanceof BinaryField)
                    .findFirst();

            Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

            // Execute
            final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

            // Assert
            Assert.assertFalse("Should not process tags when no AI secrets configured", result);

            // Cleanup
            Try.run(() -> APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType));
        } finally {
            Try.run(() -> APILocator.getHostAPI().archive(hostWithoutSecrets, APILocator.systemUser(), false));
            Try.run(() -> APILocator.getHostAPI().delete(hostWithoutSecrets, APILocator.systemUser(), false));
        }
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: All conditions are met (has tag field, not tagged, valid image, has secrets)
     * ExpectedResult: Returns true, indicating the content should be processed
     */
    @Test
    public void test_shouldProcessTags_allConditionsMet_returnsTrue() throws Exception {
        // Create contentlet with valid image and all required conditions
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithTags.id())
                .host(host)
                .setProperty("binaryField", testImageFile)
                .nextPersisted();

        final Optional<Field> binaryFieldOpt = contentTypeWithTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertTrue("Should process tags when all conditions are met", result);
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: Valid PNG image file
     * ExpectedResult: Returns true for PNG images
     */
    @Test
    public void test_shouldProcessTags_pngImage_returnsTrue() throws Exception {
        // Try to load PNG test image
        final URL pngUrl = FocalPointAPITest.class.getResource("/images/test.png");
        File pngFile = null;

        if (pngUrl != null) {
            pngFile = new File(pngUrl.getFile());
        } else {
            // Create a minimal PNG-like test file if not found
            pngFile = File.createTempFile("test", ".png");
            // Write enough data to pass size check
            FileUtil.write(pngFile, "PNG_TEST_DATA_".repeat(10));
        }

        Assert.assertTrue("PNG file should be larger than 100 bytes", pngFile.length() > 100);

        // Create contentlet with PNG file
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithTags.id())
                .host(host)
                .setProperty("binaryField", pngFile)
                .nextPersisted();

        final Optional<Field> binaryFieldOpt = contentTypeWithTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertTrue("Should process tags for PNG images", result);
    }

    /**
     * Method to test: {@link OpenAIVisionAPIImpl#shouldProcessTags(Contentlet, Field)}
     * Given Scenario: Contentlet has other tags but not the AI marker tag
     * ExpectedResult: Returns true because it hasn't been processed by AI yet
     */
    @Test
    public void test_shouldProcessTags_hasOtherTagsButNotAITag_returnsTrue() throws Exception {
        // Create contentlet with tag field
        final Contentlet contentlet = new ContentletDataGen(contentTypeWithTags.id())
                .host(host)
                .setProperty("binaryField", testImageFile)
                .nextPersisted();

        // Add non-AI tags
        APILocator.getTagAPI().addContentletTagInode(
                "manual-tag",
                contentlet.getInode(),
                host.getIdentifier(),
                DotAssetContentType.TAGS_FIELD_VAR
        );
        APILocator.getTagAPI().addContentletTagInode(
                "another-tag",
                contentlet.getInode(),
                host.getIdentifier(),
                DotAssetContentType.TAGS_FIELD_VAR
        );

        final Optional<Field> binaryFieldOpt = contentTypeWithTags.fields()
                .stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst();

        Assert.assertTrue("Binary field should exist", binaryFieldOpt.isPresent());

        // Execute
        final boolean result = visionAPI.shouldProcessTags(contentlet, binaryFieldOpt.get());

        // Assert
        Assert.assertTrue("Should process tags when other tags exist but not AI marker", result);
    }

    // Helper methods

    private static void setupAISecrets() throws Exception {
        final Map<String, Secret> secrets = Map.of(
                AppKeys.API_URL.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(String.format(AiTest.API_URL, 8080).toCharArray())
                        .build(),

                AppKeys.API_IMAGE_URL.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(String.format(AiTest.API_IMAGE_URL, 8080).toCharArray())
                        .build(),

                AppKeys.API_KEY.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(AiTest.API_KEY.toCharArray())
                        .build(),

                AppKeys.TEXT_MODEL_NAMES.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(AiTest.MODEL.toCharArray())
                        .build(),

                AppKeys.IMAGE_MODEL_NAMES.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(AiTest.IMAGE_MODEL.toCharArray())
                        .build()
        );

        APILocator.getAppsAPI().saveSecrets(
                AppSecrets.builder()
                        .withKey(AppKeys.APP_KEY)
                        .withSecrets(secrets)
                        .build(),
                host,
                APILocator.systemUser()
        );
    }

    private static File createTestImage() throws Exception {
        // Create a minimal test image file
        final File testImage = File.createTempFile("test-image", ".jpg");
        // Write enough data to pass the 100 byte check
        final StringBuilder content = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            content.append("TEST_IMAGE_DATA_");
        }
        FileUtil.write(testImage, content.toString());
        return testImage;
    }
}
