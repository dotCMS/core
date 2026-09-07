package com.dotcms.rest.api.v1.drive;

import com.dotcms.browser.FieldSearchCriteria;
import com.dotcms.browser.FieldSearchCriteria.FilterKind;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.rest.exception.BadRequestException;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ContentDriveFieldFilterResolver} — how a raw {@code userSearchable} value
 * shape is validated against the field it names.
 *
 * <p>The value's JSON shape, not the field's data type, is what picks the operator: a boolean means
 * {@link FilterKind#BOOLEAN}, and that kind is only accepted for a Checkbox. So a True/False
 * <em>Radio</em> has to be filtered with the scalar string the UI sends, and the data-type-first
 * handler routing is what turns it into an exact boolean term downstream. Sending a JSON boolean for
 * one is rejected with HTTP 400 before any query is built — which is easy to get wrong, because the
 * field really is a boolean.</p>
 *
 * <p>Needs no database: a {@link ContentType} and its fields are immutables.</p>
 */
public class ContentDriveFieldFilterResolverTest {

    private static final String RADIO_VAR = "runReport";
    private static final String CHECKBOX_VAR = "active";

    private final ContentDriveFieldFilterResolver resolver = new ContentDriveFieldFilterResolver();

    /**
     * A content type carrying the two shapes that matter here: a True/False Radio (BOOL data type,
     * authored with db-style option values) and a single-option Checkbox.
     */
    private ContentType contentType() {
        final Field radio = FieldBuilder.builder(RadioField.class)
                .name(RADIO_VAR)
                .variable(RADIO_VAR)
                .dataType(DataTypes.BOOL)
                .values("Yes|1\r\nNo|0")
                .searchable(true)
                .indexed(true)
                .build();

        final Field checkbox = FieldBuilder.builder(CheckboxField.class)
                .name(CHECKBOX_VAR)
                .variable(CHECKBOX_VAR)
                .values("yes")
                .searchable(true)
                .indexed(true)
                .build();

        final ContentType type = ImmutableSimpleContentType.builder()
                .name("Filter Fixture")
                .variable("filterFixture")
                .id("filter-fixture-id")
                .build();

        // Seeds the lazy field list directly. `fields()` otherwise resolves through
        // APILocator.getContentTypeFieldAPI(), which would need a database.
        type.constructWithFields(List.of(radio, checkbox));

        return type;
    }

    /**
     * The scalar string form the UI sends for a True/False Radio is accepted. This is the case that
     * failed with HTTP 400 when the filter was expressed as a JSON boolean.
     */
    @Test
    public void radioAcceptsAScalarStringValue() {
        for (final String value : List.of("true", "false", "1", "0")) {
            final List<FieldSearchCriteria> criteria =
                    resolver.parse(Map.of(RADIO_VAR, value), contentType());

            assertEquals(1, criteria.size());
            assertEquals("scalar operator for " + value,
                    FilterKind.SCALAR, criteria.get(0).getKind());
            assertTrue("value preserved for " + value,
                    criteria.get(0).getValues().contains(value));
        }
    }

    /**
     * A JSON boolean is read as a BOOLEAN filter, and that kind is Checkbox-only — so it is rejected
     * for a Radio even though the field's data type IS boolean. Pinning this is the point: the
     * integration test asked for exactly this and got a 400 rather than a wrong result set.
     */
    @Test
    public void radioRejectsAJsonBooleanValue() {
        final ContentType type = contentType();

        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of(RADIO_VAR, true), type));
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of(RADIO_VAR, false), type));
    }

    /** A Checkbox is the one field a JSON boolean is valid for — ticked and unticked both. */
    @Test
    public void checkboxAcceptsAJsonBooleanValue() {
        for (final Boolean value : List.of(Boolean.TRUE, Boolean.FALSE)) {
            final List<FieldSearchCriteria> criteria =
                    resolver.parse(Map.of(CHECKBOX_VAR, value), contentType());

            assertEquals(1, criteria.size());
            assertEquals(FilterKind.BOOLEAN, criteria.get(0).getKind());
            assertEquals(value, criteria.get(0).getBooleanValue());
        }
    }

    /** No filters is not an error, and needs no content type. */
    @Test
    public void emptyFiltersResolveToNothing() {
        assertTrue(resolver.parse(Map.of(), contentType()).isEmpty());
        assertTrue(resolver.parse(null, null).isEmpty());
    }

    /** An unknown field name is a bad request rather than a silently ignored filter. */
    @Test
    public void unknownFieldIsRejected() {
        final ContentType type = contentType();

        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("noSuchField", "x"), type));
    }
}
