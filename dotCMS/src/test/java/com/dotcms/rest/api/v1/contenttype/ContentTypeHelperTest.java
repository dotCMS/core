package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.BaseContentType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
}
