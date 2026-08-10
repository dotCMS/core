package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.rendering.velocity.viewtools.navigation.NavResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.Test;

/**
 * Pins the behaviour of {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)} — the
 * permission-inheritance key an asset is stored and looked up under.
 *
 * <p>The method was an {@code if / else if} chain of thirteen {@code instanceof} tests with repeated
 * casts; it is now a pattern {@code switch}. It had no direct coverage before, which is why this suite
 * exists: it is the evidence the conversion preserved every mapping, and it pins the one behaviour that
 * deliberately changed.</p>
 *
 * <p>Assets are mocked rather than constructed: instantiating a real {@link Contentlet} runs a static
 * initialiser that reaches CDI (`OSIndexAPIImpl`), which no plain unit test has. Mocking costs nothing
 * here — every assertion is about which branch the resolver takes, not about how the asset was built.</p>
 *
 * <p>Where the expected answer is "the asset's own declared type", the mock declares
 * {@link #DECLARED_TYPE} instead of the real value. That keeps the test from asserting a string it
 * itself stubbed: seeing the sentinel come back out proves the resolver fell through to
 * {@code default} rather than matching a branch that happened to produce the same value.</p>
 *
 * @author Fabrizio Araya
 */
public class PermissionBitFactoryImplResolvePermissionTypeTest {

    private static final String HOST_TYPE = Host.class.getCanonicalName();
    private static final String HTML_PAGE_TYPE = IHTMLPage.class.getCanonicalName();

    /** Stands in for whatever the asset declares, so a fall-through is distinguishable from a match. */
    private static final String DECLARED_TYPE = "com.example.DeclaredByTheAssetItself";

    private final PermissionBitFactoryImpl factory =
            new PermissionBitFactoryImpl(mock(PermissionCache.class));

    /**
     * A contentlet whose content type resolves, with the given velocity variable name and base type.
     */
    private Contentlet contentletOf(final String velocityVarName, final BaseContentType baseType) {
        final Structure structure = mock(Structure.class);
        when(structure.getVelocityVarName()).thenReturn(velocityVarName);
        when(structure.getStructureType()).thenReturn(baseType.getType());

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getStructure()).thenReturn(structure);
        when(contentlet.getPermissionType()).thenReturn(DECLARED_TYPE);
        return contentlet;
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a contentlet with no resolvable content type, so {@code getStructure()} is null.
     * Expected result: it falls through to the declared type instead of throwing.
     *
     * <p>This is the behaviour that deliberately changed. The old chain read
     * {@code getStructure().getStructureType()} in the FILEASSET branch without the null check its
     * neighbouring branch performed, and {@link Contentlet#getStructure()} returns {@code null} when the
     * contentlet has no resolvable content type — so this input threw a {@link NullPointerException}.</p>
     */
    @Test
    public void test_contentletWithNoContentType_doesNotThrow() {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getStructure()).thenReturn(null);
        when(contentlet.getPermissionType()).thenReturn(DECLARED_TYPE);

        assertEquals(DECLARED_TYPE, factory.resolvePermissionType(contentlet));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a {@link Host}, which is itself a {@link Contentlet} subclass.
     * Expected result: resolves as a Host. Order-sensitive — Host has to be matched before Contentlet,
     * or the guarded Contentlet cases would claim it first.
     */
    @Test
    public void test_host_resolvesAsHost() {
        assertEquals(HOST_TYPE, factory.resolvePermissionType(mock(Host.class)));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a plain contentlet whose content type is the Host content type.
     * Expected result: resolves as a Host, even though the object is not a {@link Host} instance.
     */
    @Test
    public void test_contentletOfHostContentType_resolvesAsHost() {
        final Contentlet contentlet =
                contentletOf(Host.HOST_VELOCITY_VAR_NAME, BaseContentType.CONTENT);

        assertEquals(HOST_TYPE, factory.resolvePermissionType(contentlet));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: an {@link IHTMLPage}.
     * Expected result: resolves as an HTML page.
     */
    @Test
    public void test_htmlPage_resolvesAsHtmlPage() {
        assertEquals(HTML_PAGE_TYPE, factory.resolvePermissionType(mock(IHTMLPage.class)));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a contentlet whose base type is HTMLPAGE but which is not an {@link IHTMLPage}.
     * Expected result: resolves as an HTML page, through the guarded case rather than the type case.
     */
    @Test
    public void test_contentletOfHtmlPageBaseType_resolvesAsHtmlPage() {
        final Contentlet contentlet = contentletOf("myPage", BaseContentType.HTMLPAGE);

        assertEquals(HTML_PAGE_TYPE, factory.resolvePermissionType(contentlet));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a contentlet whose base type is FILEASSET.
     * Expected result: the declared type, by falling through to {@code default}.
     *
     * <p>The original chain had an explicit branch mapping this to
     * {@code Contentlet.class.getCanonicalName()}, which is precisely what
     * {@link Contentlet#getPermissionType()} already returns — and no {@code Contentlet} subclass
     * overrides it. The branch was therefore assigning the value the variable already held, so it was
     * dropped. The sentinel is what makes this test meaningful: it shows the resolver now reaches
     * {@code default} rather than matching a branch.</p>
     */
    @Test
    public void test_contentletOfFileAssetBaseType_fallsThroughToDeclaredType() {
        final Contentlet contentlet = contentletOf("fileAsset", BaseContentType.FILEASSET);

        assertEquals(DECLARED_TYPE, factory.resolvePermissionType(contentlet));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: an {@link Event}, which extends {@link Contentlet}.
     * Expected result: the declared type. This is the second branch dropped for the same reason — it
     * mapped Events to {@code Contentlet.class.getCanonicalName()}, which an Event already declares
     * through the accessor it inherits from {@link Contentlet}.
     */
    @Test
    public void test_event_fallsThroughToDeclaredType() {
        final Event event = mock(Event.class);
        when(event.getPermissionType()).thenReturn(DECLARED_TYPE);

        assertEquals(DECLARED_TYPE, factory.resolvePermissionType(event));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: an ordinary contentlet of a custom content type.
     * Expected result: the declared type, via {@code default}.
     */
    @Test
    public void test_ordinaryContentlet_fallsThroughToDeclaredType() {
        final Contentlet contentlet = contentletOf("myBlogPost", BaseContentType.CONTENT);

        assertEquals(DECLARED_TYPE, factory.resolvePermissionType(contentlet));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a template that has not been drawn.
     * Expected result: the declared type — the {@link TemplateLayout} override must not fire.
     */
    @Test
    public void test_templateNotDrawn_fallsThroughToDeclaredType() {
        final Template template = mock(Template.class);
        when(template.isDrawed()).thenReturn(Boolean.FALSE);
        when(template.getPermissionType()).thenReturn(DECLARED_TYPE);

        assertEquals(DECLARED_TYPE, factory.resolvePermissionType(template));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a drawn template.
     * Expected result: resolves as a {@link TemplateLayout}. This override runs after the type-based
     * resolution and replaces its result.
     */
    @Test
    public void test_templateDrawn_resolvesAsTemplateLayout() {
        final Template template = mock(Template.class);
        when(template.isDrawed()).thenReturn(Boolean.TRUE);
        when(template.getPermissionType()).thenReturn(DECLARED_TYPE);

        assertEquals(TemplateLayout.class.getCanonicalName(),
                factory.resolvePermissionType(template));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#resolvePermissionType(Permissionable)}
     * Given scenario: a {@link NavResult}.
     * Expected result: it defers to whatever it encloses, overriding the type-based resolution.
     */
    @Test
    public void test_navResult_defersToEnclosingType() {
        final NavResult navResult = mock(NavResult.class);
        when(navResult.getPermissionType()).thenReturn(DECLARED_TYPE);
        when(navResult.getEnclosingPermissionClassName()).thenReturn("com.example.Enclosing");

        assertEquals("com.example.Enclosing", factory.resolvePermissionType(navResult));
    }
}
