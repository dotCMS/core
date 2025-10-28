package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ContentTypeHelper}
 */
public class ContentTypeHelperTest {
    /**
     * Method to test:  {@link ContentTypeHelper#getBaseTypeIndex(String)}
     * <p>
     * When: A valid Base Type name is passed by param. (e.g: Content).
     * <p>
     * Should: return the corresponding Base Type index.
     */
    @Test
    public void testGetBaseTypeIndex() {
        assertEquals(1, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.CONTENT.name()));
        assertEquals(2, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.WIDGET.name()));
        assertEquals(3, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.FORM.name()));
        assertEquals(4, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.FILEASSET.name()));
        assertEquals(5, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.HTMLPAGE.name()));
        assertEquals(6, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.PERSONA.name()));
        assertEquals(7, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.VANITY_URL.name()));
        assertEquals(8, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.KEY_VALUE.name()));
        assertEquals(9, ContentTypeHelper.getInstance().getBaseTypeIndex(BaseContentType.DOTASSET.name()));
    }

    /**
     * Method to test:  {@link ContentTypeHelper#getBaseTypeIndex(String)}
     * <p>
     * When: The Base Type name passed by param does not exist.
     * <p>
     * Should: throw an IllegalArgumentException exception.
     */
    @Test
    public void testThrowErrorOnGetBaseTypeIndex() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ContentTypeHelper.getInstance().getBaseTypeIndex("Fake_BaseType");
        });
        assertEquals("No enum BaseContentType with name [Fake_BaseType] was found", exception.getMessage());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#getEnsuredContentTypes(String)}
     * <p>
     * When: The Content Type list passed by param is null.
     * <p>
     * Should: return an empty list.
     */
    @Test
    public void testGetEnsuredContentTypes_withNull_returnsEmptyList() {
        String contentTypes = null;

        List<String> result = ContentTypeHelper.getInstance().getEnsuredContentTypes(contentTypes);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(Collections.emptyList(), result);
    }

    /**
     * Method to test:  {@link ContentTypeHelper#getEnsuredContentTypes(String)}
     * <p>
     * When: The Content Type list passed by param is empty.
     * <p>
     * Should: return an empty list.
     */
    @Test
    public void testGetEnsuredContentTypes_withBlankString_returnsEmptyList() {
        String contentTypes = "    ";

        List<String> result = ContentTypeHelper.getInstance().getEnsuredContentTypes(contentTypes);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#getEnsuredContentTypes(String)}
     * <p>
     * When: A single Content Type is passed by param.
     * <p>
     * Should: return a list with a just one item.
     */
    @Test
    public void testGetEnsuredContentTypes_withSingleContentType_returnsSingleElementList() {
        String contentTypes = "video";

        List<String> result = ContentTypeHelper.getInstance().getEnsuredContentTypes(contentTypes);

        assertEquals(1, result.size());
        assertEquals("video", result.get(0));
    }

    /**
     * Method to test:  {@link ContentTypeHelper#getEnsuredContentTypes(String)}
     * <p>
     * When: The Content Type list passed by param is a comma-separated list.
     * <p>
     * Should: return a list of content types.
     */
    @Test
    public void testGetEnsuredContentTypes_withMultipleContentTypes_returnsCorrectList() {
        String contentTypes = "video , content , blog";

        List<String> result = ContentTypeHelper.getInstance().getEnsuredContentTypes(contentTypes);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("video", "content", "blog"), result);
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed a Content Type list and the orderBy parameter is "name:asc".
     * <p>
     * Should: return a list of content types sorted by name in ascending order.
     */
    @Test
    public void testSorByName_Ascending() {
        List<ContentType> contentTypes = contentTypesForSortingMock();
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(contentTypes, "name:asc");

        assertEquals("Activity", sorted.get(0).name());
        assertEquals("Banner", sorted.get(1).name());
        assertEquals("Banner Carousel", sorted.get(2).name());
        assertEquals("Blog", sorted.get(3).name());
        assertEquals("dotAsset", sorted.get(4).name());
        assertEquals("Video", sorted.get(5).name());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed a Content Type list and the orderBy parameter is "name:desc".
     * <p>
     * Should: return a list of content types sorted by name in descending order.
     */
    @Test
    public void testSortByName_Descending() {
        List<ContentType> contentTypes = contentTypesForSortingMock();
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(contentTypes, "name:desc");

        assertEquals("Video", sorted.get(0).name());
        assertEquals("dotAsset", sorted.get(1).name());
        assertEquals("Blog", sorted.get(2).name());
        assertEquals("Banner Carousel", sorted.get(3).name());
        assertEquals("Banner", sorted.get(4).name());
        assertEquals("Activity", sorted.get(5).name());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed a Content Type list and the orderBy parameter is "name". (default direction is
     * ascending)
     * <p>
     * Should: return a list of content types sorted by name in ascending order.
     */
    @Test
    public void testSortByName_DefaultIsAscending() {
        List<ContentType> contentTypes = contentTypesForSortingMock();
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(contentTypes, "name");

        assertEquals("Activity", sorted.get(0).name());
        assertEquals("Banner", sorted.get(1).name());
        assertEquals("Banner Carousel", sorted.get(2).name());
        assertEquals("Blog", sorted.get(3).name());
        assertEquals("dotAsset", sorted.get(4).name());
        assertEquals("Video", sorted.get(5).name());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed a Content Type list and the orderBy parameter is "variable asc". (using space
     * separator)
     * <p>
     * Should: return a list of content types sorted by variable in ascending order.
     */
    @Test
    public void testSortByVelocityVarName_Ascending() {
        List<ContentType> contentTypes = contentTypesForSortingMock();
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(contentTypes, "velocity_var_name asc");

        assertEquals("Activity", sorted.get(0).variable());
        assertEquals("Banner", sorted.get(1).variable());
        assertEquals("BannerCarousel", sorted.get(2).variable());
        assertEquals("Blog", sorted.get(3).variable());
        assertEquals("dotAsset", sorted.get(4).variable());
        assertEquals("Video", sorted.get(5).variable());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed a Content Type Collection and the orderBy parameter is "velocity_var_name DESC". (using space
     * separator)
     * <p>
     * Should: return a list of content types sorted by variable in descending order.
     */
    @Test
    public void testSortByVelocityVarName_Descending_UsingSetTypeParam() {
        List<ContentType> contentTypes = contentTypesForSortingMock();
        Set<ContentType> contentTypesSet = new LinkedHashSet<>(contentTypes);
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(contentTypesSet, "velocity_var_name DESC");

        assertEquals("Video", sorted.get(0).name());
        assertEquals("dotAsset", sorted.get(1).name());
        assertEquals("Blog", sorted.get(2).name());
        assertEquals("Banner Carousel", sorted.get(3).name());
        assertEquals("Banner", sorted.get(4).name());
        assertEquals("Activity", sorted.get(5).name());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed an empty list.
     * <p>
     * Should: return an empty list.
     */
    @Test
    public void testSortWithEmptyList() {
        List<ContentType> emptyList = new ArrayList<>();
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(emptyList, "name");
        assertTrue(sorted.isEmpty());
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed a Content Type list and the orderBy parameter is null.
     * <p>
     * Should: return the original list.
     */
    @Test
    public void testSortWithNullOrderBy() {
        List<ContentType> contentTypes = contentTypesForSortingMock();
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(contentTypes, null);
        assertEquals(contentTypes, sorted);
    }

    /**
     * Method to test:  {@link ContentTypeHelper#sortContentTypes(Collection, String)}
     * <p>
     * When: Passed a Content Type list and the orderBy parameter is a blank string.
     * <p>
     * Should: return the original list.
     */
    @Test
    public void testSortWithBlankOrderBy() {
        List<ContentType> contentTypes = contentTypesForSortingMock();
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        List<ContentType> sorted = contentTypeHelper.sortContentTypes(contentTypes, "   ");
        assertEquals(contentTypes, sorted);
    }

    /**
     * Method to create a list of Content Types for sorting tests.
     * @return A list of Content Types.
     */
    public static List<ContentType> contentTypesForSortingMock() {
        List<ContentType> contentTypes = new ArrayList<>();

        // Mock ContentType 1: dotAsset
        ContentType ct1 = mock(ContentType.class);
        when(ct1.name()).thenReturn("dotAsset");
        when(ct1.variable()).thenReturn("dotAsset");
        when(ct1.description()).thenReturn("Default Content Type for DotAsset");
        when(ct1.id()).thenReturn("f2d8a1c7-2b77-2081-bcf1-b5348988c08d");
        when(ct1.modDate()).thenReturn(new Date(1761160685000L));
        when(ct1.iDate()).thenReturn(new Date(1587064483255L));
        when(ct1.sortOrder()).thenReturn(0);
        when(ct1.system()).thenReturn(false);
        when(ct1.versionable()).thenReturn(true);
        when(ct1.multilingualable()).thenReturn(false);
        when(ct1.defaultType()).thenReturn(false);
        when(ct1.fixed()).thenReturn(false);
        when(ct1.host()).thenReturn("SYSTEM_HOST");
        when(ct1.siteName()).thenReturn("systemHost");
        when(ct1.icon()).thenReturn("assessment");
        when(ct1.folder()).thenReturn("SYSTEM_FOLDER");

        // Mock ContentType 2: Video
        ContentType ct2 = mock(ContentType.class);
        when(ct2.name()).thenReturn("Video");
        when(ct2.variable()).thenReturn("Video");
        when(ct2.description()).thenReturn(null);
        when(ct2.id()).thenReturn("c77450b834901a20c8193ef9d561ee5b");
        when(ct2.modDate()).thenReturn(new Date(1761160686000L));
        when(ct2.iDate()).thenReturn(new Date(1717527600000L));
        when(ct2.sortOrder()).thenReturn(0);
        when(ct2.system()).thenReturn(false);
        when(ct2.versionable()).thenReturn(true);
        when(ct2.multilingualable()).thenReturn(false);
        when(ct2.defaultType()).thenReturn(false);
        when(ct2.fixed()).thenReturn(false);
        when(ct2.host()).thenReturn("SYSTEM_HOST");
        when(ct2.siteName()).thenReturn("systemHost");
        when(ct2.icon()).thenReturn("movie");
        when(ct2.folder()).thenReturn("SYSTEM_FOLDER");

        // Mock ContentType 3: Activity
        ContentType ct3 = mock(ContentType.class);
        when(ct3.name()).thenReturn("Activity");
        when(ct3.variable()).thenReturn("Activity");
        when(ct3.description()).thenReturn("Activities available at desitnations");
        when(ct3.id()).thenReturn("778f3246-9b11-4a2a-a101-e7fdf111bdad");
        when(ct3.modDate()).thenReturn(new Date(1761160688000L));
        when(ct3.iDate()).thenReturn(new Date(1567778770000L));
        when(ct3.sortOrder()).thenReturn(0);
        when(ct3.system()).thenReturn(false);
        when(ct3.versionable()).thenReturn(true);
        when(ct3.multilingualable()).thenReturn(false);
        when(ct3.defaultType()).thenReturn(false);
        when(ct3.fixed()).thenReturn(false);
        when(ct3.host()).thenReturn("48190c8c-42c4-46af-8d1a-0cd5db894797");
        when(ct3.siteName()).thenReturn("demo.dotcms.com");
        when(ct3.icon()).thenReturn("paragliding");
        when(ct3.folder()).thenReturn("SYSTEM_FOLDER");

        // Mock ContentType 4: Banner
        ContentType ct4 = mock(ContentType.class);
        when(ct4.name()).thenReturn("Banner");
        when(ct4.variable()).thenReturn("Banner");
        when(ct4.description()).thenReturn("Hero image used on homepage and landing pages");
        when(ct4.id()).thenReturn("4c441ada-944a-43af-a653-9bb4f3f0cb2b");
        when(ct4.modDate()).thenReturn(new Date(1761160686000L));
        when(ct4.iDate()).thenReturn(new Date(1489086945734L));
        when(ct4.sortOrder()).thenReturn(0);
        when(ct4.system()).thenReturn(false);
        when(ct4.versionable()).thenReturn(true);
        when(ct4.multilingualable()).thenReturn(false);
        when(ct4.defaultType()).thenReturn(false);
        when(ct4.fixed()).thenReturn(false);
        when(ct4.host()).thenReturn("SYSTEM_HOST");
        when(ct4.siteName()).thenReturn("systemHost");
        when(ct4.icon()).thenReturn("image_aspect_ratio");
        when(ct4.folder()).thenReturn("SYSTEM_FOLDER");

        // Mock ContentType 5: Blog
        ContentType ct5 = mock(ContentType.class);
        when(ct5.name()).thenReturn("Blog");
        when(ct5.variable()).thenReturn("Blog");
        when(ct5.description()).thenReturn("Travel Blog");
        when(ct5.id()).thenReturn("799f176a-d32e-4844-a07c-1b5fcd107578");
        when(ct5.modDate()).thenReturn(new Date(1761160688000L));
        when(ct5.iDate()).thenReturn(new Date(1543419364000L));
        when(ct5.sortOrder()).thenReturn(0);
        when(ct5.system()).thenReturn(false);
        when(ct5.versionable()).thenReturn(true);
        when(ct5.multilingualable()).thenReturn(false);
        when(ct5.defaultType()).thenReturn(false);
        when(ct5.fixed()).thenReturn(false);
        when(ct5.host()).thenReturn("48190c8c-42c4-46af-8d1a-0cd5db894797");
        when(ct5.siteName()).thenReturn("demo.dotcms.com");
        when(ct5.icon()).thenReturn("file_copy");
        when(ct5.folder()).thenReturn("SYSTEM_FOLDER");

        // Mock ContentType 6: BannerCarousel
        ContentType ct6 = mock(ContentType.class);
        when(ct6.name()).thenReturn("Banner Carousel");
        when(ct6.variable()).thenReturn("BannerCarousel");
        when(ct6.description()).thenReturn(null);
        when(ct6.id()).thenReturn("73061f34-7fa0-4f77-9724-5ca0013a0214");
        when(ct6.modDate()).thenReturn(new Date(1761160686000L));
        when(ct6.iDate()).thenReturn(new Date(1566406304000L));
        when(ct6.sortOrder()).thenReturn(0);
        when(ct6.system()).thenReturn(false);
        when(ct6.versionable()).thenReturn(true);
        when(ct6.multilingualable()).thenReturn(false);
        when(ct6.defaultType()).thenReturn(false);
        when(ct6.fixed()).thenReturn(false);
        when(ct6.host()).thenReturn("48190c8c-42c4-46af-8d1a-0cd5db894797");
        when(ct6.siteName()).thenReturn("demo.dotcms.com");
        when(ct6.icon()).thenReturn("360");
        when(ct6.folder()).thenReturn("SYSTEM_FOLDER");

        contentTypes.add(ct1);
        contentTypes.add(ct2);
        contentTypes.add(ct3);
        contentTypes.add(ct4);
        contentTypes.add(ct5);
        contentTypes.add(ct6);

        return contentTypes;
    }
}
