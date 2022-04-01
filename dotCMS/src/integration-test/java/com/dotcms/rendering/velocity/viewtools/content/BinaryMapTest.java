package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.dotcms.uuid.shorty.ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinaryMapTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test getSize method return expected result
     */
    @Test
    public void test_getSize() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertEquals("154.8 K", binaryMap.getSize());
    }

    /**
     * Test getWidth method return expected result
     */
    @Test
    public void test_getWidth() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertEquals(800, binaryMap.getWidth());
    }

    /**
     * Test getHeight method return expected result
     */
    @Test
    public void test_getHeight() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertEquals(500, binaryMap.getHeight());
    }

    /**
     * Test getFile method return expected result
     */
    @Test
    public void test_getFile() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertTrue(binaryMap.getFile().getName().startsWith("image"));
        assertTrue(binaryMap.getFile().getName().endsWith("jpg"));
    }

    /**
     * Test getThumbnailUri method return expected result
     */
    @Test
    public void test_getThumbnailUri() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertTrue(binaryMap.getThumbnailUri().startsWith("/contentAsset/image/"));
        assertTrue(binaryMap.getThumbnailUri().endsWith("/image/filter/Thumbnail"));
    }

    /**
     * Test getShortyUrlInode method return expected result
     */
    @Test
    public void test_getShortyUrlInode() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertTrue(binaryMap.getShortyUrlInode().startsWith("/dA/"));
        assertTrue(binaryMap.getShortyUrlInode().endsWith(".jpg"));
    }

    /**
     * Test getShorty method return expected result
     */
    @Test
    public void test_getShorty() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertEquals(MINIMUM_SHORTY_ID_LENGTH, binaryMap.getShorty().length());
    }

    /**
     * Test getShortyUrl method return expected result
     */
    @Test
    public void test_getShortyUrl() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertTrue(binaryMap.getShortyUrl().startsWith("/dA/"));
        assertTrue(binaryMap.getShortyUrl().endsWith(".jpg"));
    }

    /**
     * Test getRawUri method return expected result
     */
    @Test
    public void test_getRawUri() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertTrue(binaryMap.getRawUri().startsWith("/contentAsset/raw-data/"));
        assertTrue(binaryMap.getRawUri().endsWith("/image"));
    }

    /**
     * Test getName method return expected result
     */
    @Test
    public void test_getName() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        assertTrue(binaryMap.getName().startsWith("image"));
        assertTrue(binaryMap.getName().endsWith(".jpg"));
    }
}