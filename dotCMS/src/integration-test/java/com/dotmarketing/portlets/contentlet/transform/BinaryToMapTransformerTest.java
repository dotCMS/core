package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.io.Files;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BinaryToMapTransformerTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Tests https://github.com/dotCMS/core/issues/16993
     */

    @Test
    public void testNullBinaryField_ConstructionShouldSucceed()
            throws IOException, URISyntaxException {
        // create content type with 1 text field and 2 binary fields
        final long time = System.currentTimeMillis();
        ContentType typeWithBinaries = null;

        try {
            typeWithBinaries = getContentTypeWithBinaries("testBinType" + time);

            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testImage = new File(Files.createTempDir(),
                    "test_image1" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testImage);

            final Contentlet contentWithBinaries = new ContentletDataGen(typeWithBinaries.id())
                    .setProperty("bin1", testImage)
                    .setProperty("bin2", null)
                    .setProperty("bin3", testImage)
                    .nextPersisted();

            final BinaryToMapTransformer transformer =
                    new BinaryToMapTransformer(contentWithBinaries);

            Assert.assertEquals(testImage.getName(), transformer.asMap().get("bin1"));
            Assert.assertNull(transformer.asMap().get("bin2"));
            Assert.assertEquals(testImage.getName(), transformer.asMap().get("bin3"));

        } finally {
            ContentTypeDataGen.remove(typeWithBinaries);
        }
    }


    private ContentType getContentTypeWithBinaries(final String contentTypeName) {

        ContentType commentsType = null;
        try {
            try {
                commentsType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (commentsType == null) {

                List<Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .type(BinaryField.class)
                                .name("bin1")
                                .velocityVarName("bin1")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .type(BinaryField.class)
                                .name("bin2")
                                .velocityVarName("bin2")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .type(BinaryField.class)
                                .name("bin3")
                                .velocityVarName("bin3")
                                .next()
                );



                commentsType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return commentsType;
    }

}
