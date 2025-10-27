package com.dotcms.rest.api.v1.contenttype;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MockContentTypeTest {

    /**
     * Method to create a list of Content Types for sorting tests.
     * @return A list of Content Types.
     */
    public static List<ContentType> ContentTypesForSortingMock() {
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
