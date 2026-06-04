package com.dotcms.rest.api.v1.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import java.util.Optional;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link WorkflowResource#resolveContentletByVariant}, the guard that prevents
 * the {@code /api/v1/workflow/actions/default/fire/EDIT} endpoint from overwriting the DEFAULT
 * contentlet when an editor saves inside a UVE experiment variant.
 *
 * <p>Each test maps to one of the five resolution scenarios:
 * <ol>
 *   <li>DEFAULT requested, contentlet is DEFAULT → no copy needed</li>
 *   <li>variant-X requested, contentlet is already variant-X → no copy needed</li>
 *   <li>variant-X requested, contentlet is DEFAULT → sibling copy created with empty inode</li>
 *   <li>Non-existent variant requested → falls back to DEFAULT → no copy needed</li>
 *   <li>Null/empty variantName → defaults to DEFAULT without querying VariantAPI</li>
 * </ol>
 */
public class WorkflowResourceResolveVariantTest extends UnitTestBase {

    // -----------------------------------------------------------------------
    // Infrastructure helpers
    // -----------------------------------------------------------------------

    private WorkflowResource buildResource() {
        return new WorkflowResource(
                mock(WorkflowHelper.class),
                mock(ContentHelper.class),
                mock(WorkflowAPI.class),
                mock(ContentletAPI.class),
                mock(ResponseUtil.class),
                mock(PermissionAPI.class),
                mock(WorkflowImportExportUtil.class),
                new MultiPartUtils(mock(FileAssetAPI.class)),
                mock(WebResource.class),
                mock(SystemActionApiFireCommandFactory.class));
    }

    private Contentlet contentletWithVariant(final String variantId) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setVariantId(variantId);
        contentlet.setIdentifier("test-identifier");
        contentlet.setInode("test-inode");
        return contentlet;
    }

    private Variant variantOf(final String name) {
        return Variant.builder()
                .name(name)
                .description(Optional.empty())
                .archived(false)
                .build();
    }

    // -----------------------------------------------------------------------
    // Scenario 1: DEFAULT requested, contentlet is DEFAULT → no mismatch
    // -----------------------------------------------------------------------

    /**
     * When both the request and the found contentlet are on the DEFAULT variant, the original
     * contentlet is returned unchanged — no variant copy is necessary.
     */
    @Test
    public void testDefaultVariant_contentletIsDefault_returnsUnchanged() throws Exception {
        final VariantAPI variantAPI = mock(VariantAPI.class);
        when(variantAPI.get("DEFAULT")).thenReturn(Optional.of(VariantAPI.DEFAULT_VARIANT));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getVariantAPI).thenReturn(variantAPI);

            final WorkflowResource resource = buildResource();
            final Contentlet defaultContentlet = contentletWithVariant(VariantAPI.DEFAULT_VARIANT.name());

            final Contentlet result = resource.resolveContentletByVariant(defaultContentlet, "DEFAULT");

            assertSame("Should return the same contentlet instance — no copy needed", defaultContentlet, result);
            assertEquals("DEFAULT", result.getVariantId());
            assertEquals("test-inode", result.getInode());
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 2: variant-X requested, contentlet is already variant-X → no mismatch
    // -----------------------------------------------------------------------

    /**
     * When a variant-specific copy of the contentlet already exists and the request targets that
     * same variant, the existing copy is returned unchanged — no duplicate is created.
     */
    @Test
    public void testVariantX_contentletIsAlreadyVariantX_returnsUnchanged() throws Exception {
        final String variantX = "experiment-variant-1";
        final VariantAPI variantAPI = mock(VariantAPI.class);
        when(variantAPI.get(variantX)).thenReturn(Optional.of(variantOf(variantX)));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getVariantAPI).thenReturn(variantAPI);

            final WorkflowResource resource = buildResource();
            final Contentlet variantContentlet = contentletWithVariant(variantX);

            final Contentlet result = resource.resolveContentletByVariant(variantContentlet, variantX);

            assertSame("Should return the same contentlet instance — no copy needed", variantContentlet, result);
            assertEquals(variantX, result.getVariantId());
            assertEquals("test-inode", result.getInode());
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 3: variant-X requested, contentlet is DEFAULT → mismatch → sibling copy
    // -----------------------------------------------------------------------

    /**
     * Core fix scenario: the frontend always sends the DEFAULT inode because no variant copy exists
     * yet. The method must return a sibling with an empty inode (so the DB creates a new row) and
     * the requested variant, while preserving the original identifier so both the DEFAULT and the
     * new variant copy share the same content identity.
     */
    @Test
    public void testVariantX_contentletIsDefault_returnsSiblingWithEmptyInode() throws Exception {
        final String variantX = "experiment-variant-1";
        final VariantAPI variantAPI = mock(VariantAPI.class);
        when(variantAPI.get(variantX)).thenReturn(Optional.of(variantOf(variantX)));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getVariantAPI).thenReturn(variantAPI);

            final WorkflowResource resource = buildResource();
            final Contentlet defaultContentlet = contentletWithVariant(VariantAPI.DEFAULT_VARIANT.name());

            final Contentlet result = resource.resolveContentletByVariant(defaultContentlet, variantX);

            assertNotSame("A new sibling must be returned, not the original DEFAULT contentlet", defaultContentlet, result);
            assertTrue("Empty inode signals the persistence layer to create a new contentlet row",
                    result.getInode().isEmpty());
            assertEquals("Sibling must carry the requested variant", variantX, result.getVariantId());
            assertEquals("Sibling must inherit the DEFAULT contentlet's identifier",
                    "test-identifier", result.getIdentifier());
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 4: non-existent variantName → falls back to DEFAULT → no mismatch
    // -----------------------------------------------------------------------

    /**
     * When the requested variant no longer exists in the database (e.g., was deleted), the method
     * falls back to DEFAULT. If the contentlet is already on DEFAULT, no copy is created and the
     * original is returned safely.
     */
    @Test
    public void testUnknownVariant_fallsBackToDefault_returnsUnchanged() throws Exception {
        final VariantAPI variantAPI = mock(VariantAPI.class);
        when(variantAPI.get("does-not-exist")).thenReturn(Optional.empty());

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getVariantAPI).thenReturn(variantAPI);

            final WorkflowResource resource = buildResource();
            final Contentlet defaultContentlet = contentletWithVariant(VariantAPI.DEFAULT_VARIANT.name());

            final Contentlet result = resource.resolveContentletByVariant(defaultContentlet, "does-not-exist");

            assertSame("Unknown variant must fall back to DEFAULT; original contentlet returned unchanged",
                    defaultContentlet, result);
            assertEquals("test-inode", result.getInode());
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 5: null variantName → defaults to DEFAULT without querying VariantAPI
    // -----------------------------------------------------------------------

    /**
     * When no variantName is provided (null or empty string), the method defaults to DEFAULT
     * without making a database call to validate the variant. If the contentlet is on DEFAULT, it
     * is returned unchanged.
     */
    @Test
    public void testNullVariantName_defaultsToDefault_doesNotQueryVariantAPI() throws Exception {
        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getVariantAPI)
                    .thenThrow(new AssertionError("VariantAPI must not be queried when variantName is null"));

            final WorkflowResource resource = buildResource();
            final Contentlet defaultContentlet = contentletWithVariant(VariantAPI.DEFAULT_VARIANT.name());

            final Contentlet result = resource.resolveContentletByVariant(defaultContentlet, null);

            assertSame("Null variantName must default to DEFAULT without a DB lookup",
                    defaultContentlet, result);
            assertEquals("test-inode", result.getInode());
        }
    }
}
