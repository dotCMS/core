package com.dotcms.rendering.velocity.viewtools.content;

import static com.dotcms.uuid.shorty.ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BinaryMap#getResizeUri(Integer, Integer)}</li>
     *     <li><b>Given Scenario:</b> Verify that the {@link BinaryMap#getResizeUri(Integer, Integer)} method returns the URI in the
     *     expected format.</li>
     *     <li><b>Expected Result:</b> The URI must not contain double slash.</li>
     * </ul>
     */
    @Test
    public void test_getResizeUri() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final BinaryMap binaryMap = new BinaryMap(banner, new LegacyFieldTransformer(binaryField).asOldField());

        String resizeUri = binaryMap.getResizeUri(200,200);

        Assert.assertFalse("Contains double slash sending both params", resizeUri.contains("//"));

        resizeUri = binaryMap.getResizeUri(0,200);

        Assert.assertFalse("Contains double slash sending only height", resizeUri.contains("//"));

        resizeUri = binaryMap.getResizeUri(200,0);

        Assert.assertFalse("Contains double slash sending only width", resizeUri.contains("//"));

        resizeUri = binaryMap.getResizeUri(0,0);

        Assert.assertFalse("Contains double slash sending no params", resizeUri.contains("//"));
    }
}
