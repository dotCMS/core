package com.dotcms.contenttype.transform.contenttype;

import static org.junit.Assert.assertEquals;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

/**
 * Unit tests for the {@code clazz} discriminator resolution performed by
 * {@link ContentType.ClassNameAliasResolver}. In addition to the fully-qualified {@code Immutable*}
 * class name, the resolver accepts ergonomic short forms so callers (e.g. AI agents) can pass a
 * base-type name/alias ({@code "WIDGET"}, {@code "Form"}, {@code "File"}, ...) or the concrete
 * simple class name ({@code "SimpleContentType"}). Every accepted form must deserialize to the same
 * generated {@code Immutable*} class.
 *
 * <p>These deserialize straight into {@link ContentType} (the polymorphic type carrying the
 * {@code @JsonTypeIdResolver}), which is the exact chokepoint every {@code clazz} value flows
 * through and needs no database.</p>
 */
public class ContentTypeClazzAliasTest extends UnitTestBase {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private String resolvedClassName(final String clazz) throws Exception {
        final String json = "{\"clazz\":\"" + clazz + "\",\"name\":\"Test\",\"variable\":\"testVar\"}";
        return MAPPER.readValue(json, ContentType.class).getClass().getSimpleName();
    }

    private void assertResolves(final String clazz, final String expectedImmutableClass)
            throws Exception {
        assertEquals("clazz '" + clazz + "' should deserialize to " + expectedImmutableClass,
                expectedImmutableClass, resolvedClassName(clazz));
    }

    @Test
    public void test_baseType_enum_names_resolve() throws Exception {
        assertResolves("CONTENT", "ImmutableSimpleContentType");
        assertResolves("WIDGET", "ImmutableWidgetContentType");
        assertResolves("FORM", "ImmutableFormContentType");
        assertResolves("FILEASSET", "ImmutableFileAssetContentType");
        assertResolves("HTMLPAGE", "ImmutablePageContentType");
        assertResolves("PERSONA", "ImmutablePersonaContentType");
        assertResolves("VANITY_URL", "ImmutableVanityUrlContentType");
        assertResolves("KEY_VALUE", "ImmutableKeyValueContentType");
        assertResolves("DOTASSET", "ImmutableDotAssetContentType");
    }

    @Test
    public void test_baseType_names_are_case_insensitive() throws Exception {
        assertResolves("content", "ImmutableSimpleContentType");
        assertResolves("Widget", "ImmutableWidgetContentType");
        assertResolves("dotasset", "ImmutableDotAssetContentType");
    }

    @Test
    public void test_baseType_alternate_names_resolve() throws Exception {
        assertResolves("Form", "ImmutableFormContentType");
        assertResolves("File", "ImmutableFileAssetContentType");
        assertResolves("Page", "ImmutablePageContentType");
        assertResolves("VanityURL", "ImmutableVanityUrlContentType");
        assertResolves("KeyValue", "ImmutableKeyValueContentType");
        assertResolves("DotAsset", "ImmutableDotAssetContentType");
    }

    @Test
    public void test_concrete_simple_class_name_still_resolves() throws Exception {
        // Pre-existing leniency must be preserved.
        assertResolves("SimpleContentType", "ImmutableSimpleContentType");
        assertResolves("WidgetContentType", "ImmutableWidgetContentType");
    }

    @Test
    public void test_fully_qualified_immutable_class_name_still_resolves() throws Exception {
        // The documented canonical form must keep working unchanged.
        assertResolves("com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
                "ImmutableSimpleContentType");
        assertResolves("com.dotcms.contenttype.model.type.ImmutableWidgetContentType",
                "ImmutableWidgetContentType");
    }
}
