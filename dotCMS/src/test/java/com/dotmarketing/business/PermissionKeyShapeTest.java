package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.PermissionKey.DeclaredByAsset;
import com.dotmarketing.business.PermissionKey.KnownType;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.Test;

/**
 * The assertions that only exist because {@link PermissionKey} exists.
 *
 * <p>{@code PermissionBitFactoryImplResolvePermissionTypeTest} pins the strings, and it passes
 * unchanged on this branch — that is the evidence the sealed type changed nothing observable. What
 * it cannot ask is the question this suite asks: <em>did that value come out of the catalogue, or
 * did the content make it up?</em> Both used to be a {@code String} and were indistinguishable.</p>
 *
 * @author Fabrizio Araya
 */
public class PermissionKeyShapeTest {

    private final PermissionBitFactoryImpl factory =
            new PermissionBitFactoryImpl(mock(PermissionCache.class));

    /** Stands in for whatever the asset declares. */
    private static final String DECLARED_TYPE = "com.example.DeclaredByTheAssetItself";

    /**
     * A consumer written the way a consumer of a sealed type gets written: an exhaustive switch,
     * with no {@code default}. Adding a third shape to {@link PermissionKey} stops this method from
     * compiling — which is the whole point of the exercise.
     */
    private static boolean isCatalogued(final PermissionKey key) {
        return switch (key) {
            case KnownType _ -> true;
            case DeclaredByAsset _ -> false;
        };
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionKey(Permissionable)}
     * Given scenario: an {@link IHTMLPage}.
     * Expected result: a catalogued key, carrying the enum constant rather than a loose string.
     */
    @Test
    public void test_htmlPage_isCatalogued() {
        final PermissionKey key = factory.resolvePermissionKey(mock(IHTMLPage.class));

        assertTrue(isCatalogued(key));
        assertEquals(new KnownType(PermissionableType.HTMLPAGES), key);
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionKey(Permissionable)}
     * Given scenario: a drawn {@link Template}.
     * Expected result: catalogued as TEMPLATE_LAYOUTS.
     */
    @Test
    public void test_drawnTemplate_isCataloguedAsTemplateLayout() {
        final Template template = mock(Template.class);
        when(template.isDrawed()).thenReturn(Boolean.TRUE);
        when(template.getPermissionType()).thenReturn(DECLARED_TYPE);

        assertEquals(new KnownType(PermissionableType.TEMPLATE_LAYOUTS),
                factory.resolvePermissionKey(template));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionKey(Permissionable)}
     * Given scenario: an asset of no recognised kind, which falls through to {@code default}.
     * Expected result: NOT catalogued — the value is whatever the asset said.
     *
     * <p>This is the distinction the {@code String} return type erased. Both this and the test above
     * used to produce a fully qualified class name and nothing told them apart.</p>
     */
    @Test
    public void test_assetOfNoRecognisedKind_isNotCatalogued() {
        final Permissionable asset = mock(Permissionable.class);
        when(asset.getPermissionType()).thenReturn(DECLARED_TYPE);

        final PermissionKey key = factory.resolvePermissionKey(asset);

        assertEquals(new DeclaredByAsset(DECLARED_TYPE), key);
        assertTrue(!isCatalogued(key));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionKey(Permissionable)}
     * Given scenario: a Host.
     * Expected result: <strong>not</strong> catalogued — and that is the finding, not the design.
     *
     * <p>{@link PermissionableType} has no {@code HOSTS} constant, so a Host cannot be expressed as
     * a {@link KnownType} even though it plainly is one. The string is right and the shape is wrong,
     * which is exactly the kind of thing a {@code String} return type cannot report. This test is
     * written to fail the day the constant is added — deliberately, so the question does not get
     * forgotten.</p>
     */
    @Test
    public void test_host_isNotCatalogued_becauseTheCatalogueIsMissingIt() {
        final PermissionKey key = factory.resolvePermissionKey(mock(com.dotmarketing.beans.Host.class));

        assertEquals(new DeclaredByAsset(com.dotmarketing.beans.Host.class.getCanonicalName()), key);
        assertTrue("Add HOSTS to PermissionableType and this branch becomes a KnownType",
                !isCatalogued(key));
    }
}
