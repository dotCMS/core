package com.dotcms.rendering.velocity.viewtools.navigation;

import static com.dotcms.rendering.velocity.viewtools.navigation.NavResultHydrated.isActive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link NavResultHydrated#isActive(String, String, boolean, boolean)}, the
 * active-state decision behind {@code $nav.active} in Velocity templates.
 *
 * <p>Each test maps to a numbered row of the truth table in the feature's
 * {@code contracts/nav-active-resolution.md}. The rows fall into two groups, and the distinction
 * matters:
 *
 * <ul>
 *   <li><b>Baseline rows</b> (FE-*, API-*) pin behavior that already exists. They pass before the
 *       fix for issue #37105 and must still pass after it — that is what proves the fix does not
 *       regress front-end rendering, which runs this method for every navigation item of every
 *       page render.
 *   <li><b>Defect rows</b> (FIX-*, ROOT-*, XH-*, CFG-*, PRE-*) assert behavior that does not exist
 *       yet. They fail before the fix and pass after it.
 * </ul>
 *
 * @see <a href="https://github.com/dotCMS/core/issues/37105">#37105</a>
 */
public class NavResultHydratedTest extends UnitTestBase {

    private static final boolean FOLDER = true;
    private static final boolean PAGE = false;
    private static final boolean CODE_LINK = true;
    private static final boolean NOT_CODE_LINK = false;
    private static final boolean SAME_SITE = true;
    private static final boolean OTHER_SITE = false;

    private static final String API_PREFIX = "/api/v1/page/render";
    private static final String API_HTML_PREFIX = "/api/v1/page/renderHTML";

    /** Undo any index-page-name override so test order cannot leak a value between cases. */
    @After
    public void restoreIndexPageName() {
        NavResultHydrated.indexPageName = HTMLPageAssetAPIImpl.CMS_INDEX_PAGE::get;
    }

    // ------------------------------------------------------------------
    // Baseline — front end (CMSFilter has already normalized the URI).
    // Every row here must behave identically before and after the fix.
    // ------------------------------------------------------------------

    /**
     * FE-1: on /TravelHub/index, the folder /TravelHub is the current section.
     */
    @Test
    public void isActive_frontEndIndexPage_marksParentFolderActive() {
        assertTrue(isActive("/TravelHub/index", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-2: on /TravelHub/index, the index page itself is the current page.
     */
    @Test
    public void isActive_frontEndIndexPage_marksThePageActive() {
        assertTrue(isActive("/TravelHub/index", "/TravelHub/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-3: /Travel must not light up while the request is under /TravelHub. This is the
     * false positive the trailing-slash normalization exists to prevent.
     */
    @Test
    public void isActive_frontEndIndexPage_leavesSamePrefixSiblingInactive() {
        assertFalse(isActive("/TravelHub/index", "/Travel", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-4: an unrelated folder stays inactive.
     */
    @Test
    public void isActive_frontEndIndexPage_leavesUnrelatedFolderInactive() {
        assertFalse(isActive("/TravelHub/index", "/Partners", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-5: every ancestor folder of the current page is active. This is long-standing,
     * intended behavior — it is how a section tree stays expanded.
     */
    @Test
    public void isActive_frontEndNestedIndexPage_marksEveryAncestorActive() {
        assertTrue(isActive("/a/b/c/index", "/a", FOLDER, NOT_CODE_LINK, SAME_SITE));
        assertTrue(isActive("/a/b/c/index", "/a/b", FOLDER, NOT_CODE_LINK, SAME_SITE));
        assertTrue(isActive("/a/b/c/index", "/a/b/c", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-6: at the site root, the root index page is the current page.
     */
    @Test
    public void isActive_frontEndSiteRoot_marksRootIndexPageActive() {
        assertTrue(isActive("/index", "/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-7: the site root must not light up a top-level folder.
     */
    @Test
    public void isActive_frontEndSiteRoot_leavesTopLevelFolderInactive() {
        assertFalse(isActive("/index", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-8: on a non-index page, the containing folder is still the current section.
     */
    @Test
    public void isActive_frontEndNonIndexPage_marksParentFolderActive() {
        assertTrue(isActive("/TravelHub/contact", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-9: on a non-index page, that page is the current page.
     */
    @Test
    public void isActive_frontEndNonIndexPage_marksThePageActive() {
        assertTrue(isActive("/TravelHub/contact", "/TravelHub/contact", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-10: a URL-mapped detail URI resolves its containing folder, unchanged.
     */
    @Test
    public void isActive_frontEndUrlMappedDetailPage_marksContainingFolderActive() {
        assertTrue(isActive("/store/product/widget-123", "/store", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FE-11: a Velocity code link is never active, whatever the URI.
     */
    @Test
    public void isActive_codeLink_isNeverActive() {
        assertFalse(isActive("/TravelHub/index", "/TravelHub/index", PAGE, CODE_LINK, SAME_SITE));
        assertFalse(isActive("/index", "/index", PAGE, CODE_LINK, SAME_SITE));
    }

    // ------------------------------------------------------------------
    // Baseline — Page API with an explicit page URI.
    // These pin the prefix strip added for issue #17896 (AC-005).
    // ------------------------------------------------------------------

    /**
     * API-1: the /api/v1/page/render prefix is stripped, so the folder still resolves.
     */
    @Test
    public void isActive_apiIndexPage_marksParentFolderActive() {
        assertTrue(isActive(API_PREFIX + "/TravelHub/index", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * API-2: the index page itself resolves through the Page API.
     */
    @Test
    public void isActive_apiIndexPage_marksThePageActive() {
        assertTrue(
                isActive(API_PREFIX + "/TravelHub/index", "/TravelHub/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * API-3: a non-index page resolves through the Page API.
     */
    @Test
    public void isActive_apiNonIndexPage_marksParentFolderActive() {
        assertTrue(
                isActive(API_PREFIX + "/TravelHub/contact", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    // ------------------------------------------------------------------
    // The defect — Page API with a folder-style URI and no page segment (#37105).
    // These fail before the fix.
    // ------------------------------------------------------------------

    /**
     * FIX-1: the reported bug. UVE navigates in-editor to /api/v1/page/render/TravelHub, and the
     * folder that is plainly the current section reports inactive.
     */
    @Test
    public void isActive_apiFolderStyleUri_marksFolderActive() {
        assertTrue(isActive(API_PREFIX + "/TravelHub", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FIX-2: the page branch fails from the same cause — the nav href carries /index and the
     * request URI does not.
     */
    @Test
    public void isActive_apiFolderStyleUri_marksIndexPageActive() {
        assertTrue(isActive(API_PREFIX + "/TravelHub", "/TravelHub/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FIX-3: the trailing-slash form of the same folder URI.
     */
    @Test
    public void isActive_apiFolderStyleUriWithTrailingSlash_marksFolderActive() {
        assertTrue(isActive(API_PREFIX + "/TravelHub/", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FIX-4: nested folders — every ancestor is active, matching the front end (FE-5).
     */
    @Test
    public void isActive_apiNestedFolderStyleUri_marksEveryAncestorActive() {
        assertTrue(isActive(API_PREFIX + "/a/b/c", "/a", FOLDER, NOT_CODE_LINK, SAME_SITE));
        assertTrue(isActive(API_PREFIX + "/a/b/c", "/a/b", FOLDER, NOT_CODE_LINK, SAME_SITE));
        assertTrue(isActive(API_PREFIX + "/a/b/c", "/a/b/c", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FIX-5: the positive half of the same-prefix pair.
     */
    @Test
    public void isActive_apiFolderStyleUri_marksSamePrefixFolderActive() {
        assertTrue(isActive(API_PREFIX + "/Travel", "/Travel", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FIX-6: the negative half, and the guard that matters. Without it, FIX-5 would be satisfied
     * by an implementation that lights up every folder sharing a prefix.
     */
    @Test
    public void isActive_apiFolderStyleUri_leavesSamePrefixSiblingInactive() {
        assertFalse(isActive(API_PREFIX + "/Travel", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FIX-7: a URL-mapped detail URI under the API prefix resolves its containing folder, and is
     * not newly treated as a folder itself. Mirrors the front-end row FE-10.
     */
    @Test
    public void isActive_apiUrlMappedDetailPage_marksContainingFolderActive() {
        assertTrue(isActive(API_PREFIX + "/store/product/widget-123", "/store", FOLDER,
                NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * FIX-8: a vanity URL that matches no navigation item marks nothing active and does not throw.
     * Makes executable the spec's non-goal that vanity and URL-mapped resolution must not get worse.
     */
    @Test
    public void isActive_apiVanityUrlMatchingNoNavItem_marksNothingActive() {
        assertFalse(isActive(API_PREFIX + "/summer-sale", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
        assertFalse(isActive(API_PREFIX + "/summer-sale", "/TravelHub/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    // ------------------------------------------------------------------
    // Site root and degenerate URIs (AC-003).
    // ------------------------------------------------------------------

    /**
     * ROOT-1: /api/v1/page/render/ resolves the root index page, as the front end does for
     * /index (FE-6).
     */
    @Test
    public void isActive_apiSiteRoot_marksRootIndexPageActive() {
        assertTrue(isActive(API_PREFIX + "/", "/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * ROOT-2: the site root must not light up a top-level folder.
     */
    @Test
    public void isActive_apiSiteRoot_leavesTopLevelFolderInactive() {
        assertFalse(isActive(API_PREFIX + "/", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * ROOT-3: the explicit root index URI is unchanged.
     */
    @Test
    public void isActive_apiSiteRootIndexPage_marksRootIndexPageActive() {
        assertTrue(isActive(API_PREFIX + "/index", "/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * ROOT-4: no degenerate URI throws. The derived parent path is never computed from a negative
     * index, and is never the empty string.
     */
    @Test
    public void isActive_degenerateUris_doNotThrow() {
        isActive(API_PREFIX + "/", "/index", PAGE, NOT_CODE_LINK, SAME_SITE);
        isActive(API_PREFIX + "/", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE);
        isActive("/", "/index", PAGE, NOT_CODE_LINK, SAME_SITE);
        isActive("/index", "/", FOLDER, NOT_CODE_LINK, SAME_SITE);
        isActive("no-leading-slash", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE);
    }

    // ------------------------------------------------------------------
    // Cross-host residual (contracts section 4). XH-2 is the one case that can
    // invalidate the approach, so it is asserted rather than assumed.
    // ------------------------------------------------------------------

    /**
     * XH-1: a host-qualified href matches neither the request URI nor its index candidate.
     */
    @Test
    public void isActive_hostQualifiedHref_isNotActive() {
        assertFalse(isActive("/a/b", "//hostB/a/b", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * XH-2: a folder href equal to the current page's URI. On one site this state is
     * unrepresentable — the identifier table's uniqueness constraints forbid a folder and a page
     * sharing a path — so it can only arise from an item pulled from another site, whose hrefs are
     * unqualified. On a front-end URI no candidate is synthesized at all, so nothing can light up.
     */
    @Test
    public void isActive_folderHrefEqualToFrontEndPageUri_isNotActive() {
        assertFalse(isActive("/a/b", "/a/b", FOLDER, NOT_CODE_LINK, SAME_SITE));
        assertFalse(isActive("/a/b", "/a/b", FOLDER, NOT_CODE_LINK, OTHER_SITE));
    }

    /**
     * XH-3: the same collision under the Page API, where a candidate IS synthesized. A folder from
     * another site whose unqualified href matches the current page's path must not be activated by
     * that guess — the guess is only meaningful for the site being rendered.
     */
    @Test
    public void isActive_apiFolderHrefFromAnotherSite_isNotActive() {
        assertFalse(isActive(API_PREFIX + "/a/b", "/a/b", FOLDER, NOT_CODE_LINK, OTHER_SITE));
        assertFalse(isActive(API_HTML_PREFIX + "/a/b", "/a/b", FOLDER, NOT_CODE_LINK, OTHER_SITE));
    }

    /**
     * XH-4: the same shape on the site being rendered is the defect this fix exists for, and must
     * still resolve. Without this, the guard added for XH-3 would undo FIX-1.
     */
    @Test
    public void isActive_apiFolderHrefOnRequestedSite_isActive() {
        assertTrue(isActive(API_PREFIX + "/a/b", "/a/b", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * XH-5: an item from another site is still matched by the ordinary comparison — the guard only
     * withholds the synthesized candidate, it does not exclude the item altogether.
     */
    @Test
    public void isActive_pageHrefFromAnotherSite_stillMatchesDirectly() {
        assertTrue(isActive(API_PREFIX + "/a/b", "/a/b", PAGE, NOT_CODE_LINK, OTHER_SITE));
        assertTrue(isActive("/a/b/index", "/a/b", FOLDER, NOT_CODE_LINK, OTHER_SITE));
    }

    // ------------------------------------------------------------------
    // Configured index page name (AC-007). Driven through the indexPageName seam,
    // not Config.setProperty, which the memoized Lazy ignores.
    // ------------------------------------------------------------------

    /**
     * CFG-1: with the default name, the folder's index page resolves.
     */
    @Test
    public void isActive_defaultIndexPageName_resolvesIndexPage() {
        NavResultHydrated.indexPageName = () -> "index";
        assertTrue(isActive(API_PREFIX + "/TravelHub", "/TravelHub/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * CFG-2: with CMS_INDEX_PAGE set to "default", the page named "default" resolves.
     */
    @Test
    public void isActive_configuredIndexPageName_resolvesConfiguredPage() {
        NavResultHydrated.indexPageName = () -> "default";
        assertTrue(isActive(API_PREFIX + "/TravelHub", "/TravelHub/default", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * CFG-3: and the page named "index" then does not — otherwise the editor and the front end
     * would disagree on a site that configures the property.
     */
    @Test
    public void isActive_configuredIndexPageName_doesNotResolveDefaultNamedPage() {
        NavResultHydrated.indexPageName = () -> "default";
        assertFalse(isActive(API_PREFIX + "/TravelHub", "/TravelHub/index", PAGE, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * CFG-4: on the front end, a URI already ending in the configured name needs no candidate.
     */
    @Test
    public void isActive_configuredIndexPageName_frontEndUriNeedsNoCandidate() {
        NavResultHydrated.indexPageName = () -> "default";
        assertTrue(isActive("/TravelHub/default", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    // ------------------------------------------------------------------
    // Prefix strip (US2).
    // ------------------------------------------------------------------

    /**
     * PRE-2 / PRE-3: /api/v1/page/renderHTML renders Velocity too, but the current global
     * substring replace mangles its URI into "HTML/about-us".
     */
    @Test
    public void isActive_renderHtmlEndpoint_stripsItsOwnPrefix() {
        assertTrue(isActive(API_HTML_PREFIX + "/TravelHub/index", "/TravelHub", FOLDER,
                NOT_CODE_LINK, SAME_SITE));
        assertTrue(isActive(API_HTML_PREFIX + "/TravelHub", "/TravelHub", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * PRE-5: _render-sources returns references only and performs no Velocity render, so its
     * prefix is deliberately not stripped.
     */
    @Test
    public void isActive_renderSourcesEndpoint_prefixIsNotStripped() {
        assertFalse(isActive("/api/v1/page/_render-sources/TravelHub/index", "/TravelHub", FOLDER,
                NOT_CODE_LINK, SAME_SITE));
    }

    /**
     * A page path that merely begins with "render" must not be mangled: the strip requires the
     * full prefix followed by a slash.
     */
    @Test
    public void isActive_pathBeginningWithRender_isNotStripped() {
        assertTrue(isActive("/rendering/index", "/rendering", FOLDER, NOT_CODE_LINK, SAME_SITE));
    }

    // ------------------------------------------------------------------
    // The signature Velocity binds to.
    // ------------------------------------------------------------------

    /**
     * {@code $nav.active} in a template is resolved by Velocity's introspection, which looks for a
     * public no-argument {@code isActive()} returning {@code boolean}. This class now overloads the
     * name — the instance method plus the package-private static that holds the decision — and
     * Velocity matches by method name before arity, so the binding is worth pinning down. Were it
     * ever to break, every customer template would silently lose its active state while every
     * behavioral test in this class kept passing, since they all call the static directly.
     *
     * <p>The end-to-end counterpart, which actually renders {@code #if($n.active)} through the
     * Velocity engine, lives in {@code NavToolTest} where a real instance can be built.
     */
    @Test
    public void isActive_hasThePublicNoArgBooleanSignatureVelocityResolves() throws Exception {
        final Method property = NavResultHydrated.class.getMethod("isActive");

        assertEquals("Velocity binds $nav.active to a boolean-returning isActive()",
                boolean.class, property.getReturnType());
        assertTrue("Velocity can only reach a public method",
                Modifier.isPublic(property.getModifiers()));
        assertFalse("Velocity resolves the property against an instance, not the class",
                Modifier.isStatic(property.getModifiers()));
    }
}
