package com.dotcms.rest.api.v1.categories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;

/**
 * Unit tests for {@link CategoryForm} Jackson deserialization, focused on the
 * {@code active} field default behavior (issue #35501).
 */
public class CategoryFormTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * AC1: A create payload that omits {@code active} must default to {@code true},
     * matching the {@code Category} model default and avoiding silently inactive categories.
     */
    @Test
    public void deserialize_omittedActive_defaultsToTrue() throws IOException {
        final String json = "{\"categoryName\":\"adds\",\"categoryVelocityVarName\":\"adds\","
                + "\"key\":\"asd\",\"keywords\":\"asd\"}";

        final CategoryForm form = mapper.readValue(json, CategoryForm.class);

        assertTrue("active must default to true when omitted from JSON", form.isActive());
    }

    /**
     * AC2: An explicit {@code "active": false} in the payload must be honored.
     */
    @Test
    public void deserialize_explicitActiveFalse_isPreserved() throws IOException {
        final String json = "{\"categoryName\":\"x\",\"active\":false}";

        final CategoryForm form = mapper.readValue(json, CategoryForm.class);

        assertFalse("explicit active=false must be preserved", form.isActive());
    }

    /**
     * AC3: An explicit {@code "active": true} in the payload must be honored.
     */
    @Test
    public void deserialize_explicitActiveTrue_isPreserved() throws IOException {
        final String json = "{\"categoryName\":\"x\",\"active\":true}";

        final CategoryForm form = mapper.readValue(json, CategoryForm.class);

        assertTrue("explicit active=true must be preserved", form.isActive());
    }

    /**
     * Builder default mirrors the deserialization default — guards against the
     * field being reset to the primitive default in either side of the form.
     */
    @Test
    public void builder_defaultActive_isTrue() {
        final CategoryForm form = new CategoryForm.Builder()
                .setCategoryName("x")
                .build();

        assertTrue("Builder default for active must be true", form.isActive());
    }

    /**
     * The update path (PUT /api/v1/categories) must be able to tell an omitted {@code active}
     * from an explicit value so it does not silently flip the flag. When omitted,
     * {@link CategoryForm#activeProvided()} must be {@code null} (issue #35501).
     */
    @Test
    public void deserialize_omittedActive_activeProvidedIsNull() throws IOException {
        final String json = "{\"inode\":\"abc\",\"categoryName\":\"adds\"}";

        final CategoryForm form = mapper.readValue(json, CategoryForm.class);

        assertNull("activeProvided() must be null when active is omitted", form.activeProvided());
    }

    /**
     * An explicit {@code "active": false} on update must be reported as provided so the update
     * path applies it instead of keeping the previous value (issue #35501).
     */
    @Test
    public void deserialize_explicitActiveFalse_activeProvidedIsFalse() throws IOException {
        final String json = "{\"inode\":\"abc\",\"categoryName\":\"adds\",\"active\":false}";

        final CategoryForm form = mapper.readValue(json, CategoryForm.class);

        assertEquals("activeProvided() must reflect explicit false", Boolean.FALSE,
                form.activeProvided());
    }

    /**
     * An explicit {@code "active": true} on update must be reported as provided (issue #35501).
     */
    @Test
    public void deserialize_explicitActiveTrue_activeProvidedIsTrue() throws IOException {
        final String json = "{\"inode\":\"abc\",\"categoryName\":\"adds\",\"active\":true}";

        final CategoryForm form = mapper.readValue(json, CategoryForm.class);

        assertEquals("activeProvided() must reflect explicit true", Boolean.TRUE,
                form.activeProvided());
    }
}
