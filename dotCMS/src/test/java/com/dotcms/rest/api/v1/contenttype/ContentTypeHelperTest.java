package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.BaseContentType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
     * When: The Base Type name is passed by param does not exist.
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
}
