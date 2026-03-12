package com.dotcms.rest.api.v1.site;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotcms.rest.WebResource;
import com.dotcms.util.PaginationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * Unit tests for the {@code parentHost} field added to {@link SiteForm} and the
 * {@code parentPath} field added to {@link SiteView} as part of the nestable-hosts feature.
 *
 * <p>These tests verify JSON serialisation / deserialisation behaviour and Host model behaviour.
 * They do not require a running dotCMS instance.</p>
 */
public class SiteFormParentHostTest {

    /**
     * Creates a {@link Host} instance suitable for unit tests without triggering
     * the cache lookup in the zero-arg {@link Host#Host()} constructor.
     * Using {@link Host#Host(Contentlet)} wraps a plain {@link Contentlet} without
     * any infrastructure calls.
     */
    private static Host testHost() {
        return new Host(new Contentlet());
    }

    /**
     * Creates a {@link SiteResource} suitable for unit tests using the
     * {@code @VisibleForTesting} constructor so that no real infrastructure
     * (APILocator, SiteHelper, etc.) is initialised.
     */
    private static SiteResource testSiteResource() {
        return new SiteResource(
                mock(WebResource.class),
                new SiteHelper(mock(HostAPI.class), mock(HostVariableAPI.class)),
                mock(PaginationUtil.class));
    }

    private final ObjectMapper mapper = new ObjectMapper();

    // ------------------------------------------------------------------
    // SiteForm — parentHost deserialization
    // ------------------------------------------------------------------

    /**
     * When the JSON payload includes {@code "parentHost"}, the field must be populated on the
     * deserialized {@link SiteForm}.
     */
    @Test
    public void testSiteForm_parentHostIsDeserialized() throws Exception {
        final String json = "{"
                + "\"siteName\":\"nested.example.com\","
                + "\"parentHost\":\"48190c8c-42c4-46af-8d1a-0cd5db894797\""
                + "}";

        final SiteForm form = mapper.readValue(json, SiteForm.class);

        assertEquals("48190c8c-42c4-46af-8d1a-0cd5db894797", form.getParentHost());
    }

    /**
     * When the JSON payload does NOT include {@code "parentHost"}, the getter must return
     * {@code null} (top-level host semantics – no reparenting).
     */
    @Test
    public void testSiteForm_parentHostDefaultsToNull() throws Exception {
        final String json = "{\"siteName\":\"toplevel.example.com\"}";

        final SiteForm form = mapper.readValue(json, SiteForm.class);

        assertNull("parentHost should be null when absent from JSON", form.getParentHost());
    }

    /**
     * When {@code "parentHost"} is explicitly set to {@code null} in JSON, the getter must
     * return {@code null}.
     */
    @Test
    public void testSiteForm_parentHostExplicitNull() throws Exception {
        final String json = "{\"siteName\":\"toplevel.example.com\",\"parentHost\":null}";

        final SiteForm form = mapper.readValue(json, SiteForm.class);

        assertNull("parentHost should be null when JSON null", form.getParentHost());
    }

    /**
     * {@link SiteForm#toString()} must include the {@code parentHost} value so it appears in
     * log output.
     */
    @Test
    public void testSiteForm_toStringIncludesParentHost() throws Exception {
        final String json = "{"
                + "\"siteName\":\"nested.example.com\","
                + "\"parentHost\":\"abc-123\""
                + "}";

        final SiteForm form = mapper.readValue(json, SiteForm.class);
        final String str = form.toString();

        org.junit.Assert.assertTrue(
                "toString() must include parentHost value",
                str.contains("abc-123"));
    }

    // ------------------------------------------------------------------
    // SiteView — parentPath default and builder
    // ------------------------------------------------------------------

    /**
     * When {@link SiteView.Builder#withParentPath(String)} is not called, the default value
     * must be {@code "/"} (top-level host).
     */
    @Test
    public void testSiteView_parentPathDefaultsToSlash() {
        final SiteView view = SiteView.Builder.builder()
                .withIdentifier("abc")
                .withSiteName("toplevel.example.com")
                .withVariables(java.util.List.of())
                .build();

        assertEquals("/", view.getParentPath());
    }

    /**
     * When {@link SiteView.Builder#withParentPath(String)} is called with a non-null value,
     * that value must be returned by {@link SiteView#getParentPath()}.
     */
    @Test
    public void testSiteView_parentPathSetViaBuilder() {
        final SiteView view = SiteView.Builder.builder()
                .withIdentifier("abc")
                .withSiteName("nested.example.com")
                .withParentPath("/en/")
                .withVariables(java.util.List.of())
                .build();

        assertEquals("/en/", view.getParentPath());
    }

    /**
     * When {@link SiteView.Builder#withParentPath(String)} is called with {@code null}, the
     * result must fall back to the default {@code "/"}.
     */
    @Test
    public void testSiteView_parentPathNullFallsBackToSlash() {
        final SiteView view = SiteView.Builder.builder()
                .withIdentifier("abc")
                .withSiteName("toplevel.example.com")
                .withParentPath(null)
                .withVariables(java.util.List.of())
                .build();

        assertEquals("/", view.getParentPath());
    }

    /**
     * A deeply-nested parentPath ({@code "/en/nestedHost1/"}) must be preserved verbatim.
     */
    @Test
    public void testSiteView_deeplyNestedParentPath() {
        final SiteView view = SiteView.Builder.builder()
                .withIdentifier("xyz")
                .withSiteName("nestedHost2")
                .withParentPath("/en/nestedHost1/")
                .withVariables(java.util.List.of())
                .build();

        assertEquals("/en/nestedHost1/", view.getParentPath());
    }

    // ------------------------------------------------------------------
    // Host model — parentHost field get/set
    // ------------------------------------------------------------------

    /**
     * After calling {@link Host#setParentHost(String)}, {@link Host#getParentHost()} must return
     * the same value and it must also be present in the underlying map under the
     * {@link Host#PARENT_HOST_KEY} key.
     */
    @Test
    public void testHost_setAndGetParentHost_roundTrip() {
        final Host host = testHost();
        final String parentId = "48190c8c-42c4-46af-8d1a-0cd5db894797";

        host.setParentHost(parentId);

        assertEquals("getParentHost() must return the value set via setParentHost()",
                parentId, host.getParentHost());
        assertEquals("The underlying map must contain the parentHost under PARENT_HOST_KEY",
                parentId, host.getMap().get(Host.PARENT_HOST_KEY));
    }

    /**
     * When {@link Host#setParentHost(String)} has never been called, {@link Host#getParentHost()}
     * must return {@code null} (the host is treated as top-level).
     */
    @Test
    public void testHost_getParentHost_defaultsToNull() {
        final Host host = testHost();

        assertNull("getParentHost() must return null for a freshly constructed Host",
                host.getParentHost());
    }

    /**
     * Calling {@link Host#setParentHost(String)} with {@code null} must make
     * {@link Host#getParentHost()} return {@code null}.
     */
    @Test
    public void testHost_setParentHostNull_returnsNull() {
        final Host host = testHost();
        host.setParentHost("some-value");  // set a value first
        host.setParentHost(null);           // now clear it

        assertNull("getParentHost() must return null after setParentHost(null)",
                host.getParentHost());
    }

    /**
     * Calling {@link Host#setParentHost(String)} with an empty string must make
     * {@link Host#getParentHost()} return an empty string, not null.
     */
    @Test
    public void testHost_setParentHostEmpty_returnsEmpty() {
        final Host host = testHost();
        host.setParentHost("");

        assertEquals("getParentHost() must return empty string after setParentHost(\"\")",
                "", host.getParentHost());
    }

    /**
     * Setting {@link Host#PARENT_HOST_KEY} = {@code "parentHost"} constant must equal the
     * velocity variable name used by the HostFolderField in the Host content type.
     */
    @Test
    public void testHost_parentHostKeyConstant() {
        assertEquals("PARENT_HOST_KEY must match the HostFolderField velocity variable name",
                "parentHost", Host.PARENT_HOST_KEY);
    }

    // ------------------------------------------------------------------
    // SiteResource.validateNoCircularReference — via reflection
    // ------------------------------------------------------------------

    /**
     * When the site being updated is brand-new (blank identifier),
     * {@code validateNoCircularReference} must return without throwing.
     *
     * <p>This exercises the early-return path that skips the cycle check for new hosts.</p>
     */
    @Test
    public void testValidateNoCircularReference_newSite_noException() throws Exception {
        final SiteResource resource = testSiteResource();

        final java.lang.reflect.Method method =
                SiteResource.class.getDeclaredMethod("validateNoCircularReference",
                        String.class, String.class, Host.class);
        method.setAccessible(true);

        final Host newSite = testHost();
        // Identifier is blank — brand-new site, no descendants possible.

        // Must not throw.
        method.invoke(resource, "some-parent-id", "parentLabel", newSite);
    }

    /**
     * When the proposed parent host identifier equals the site's own identifier,
     * {@code validateNoCircularReference} must throw {@link IllegalArgumentException} indicating
     * a circular reference.
     *
     * <p>This exercises the self-reference detection path (A → A).</p>
     */
    @Test(expected = java.lang.reflect.InvocationTargetException.class)
    public void testValidateNoCircularReference_selfReference_throwsIllegalArgument()
            throws Exception {
        final SiteResource resource = testSiteResource();

        final java.lang.reflect.Method method =
                SiteResource.class.getDeclaredMethod("validateNoCircularReference",
                        String.class, String.class, Host.class);
        method.setAccessible(true);

        final Host site = testHost();
        site.setIdentifier("site-uuid-abc");

        // The proposed parent ID equals the site's own ID → self-reference cycle.
        // InvocationTargetException wraps the IllegalArgumentException thrown by the method.
        method.invoke(resource, "site-uuid-abc", "siteLabel", site);
    }
}
