package com.dotcms.rest.api.v1.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.ThemeAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the labels {@link TemplateHelper#toTemplateView(Template, User)} builds for the
 * template picker: {@code htmlTitle} is an HTML fragment, so every value it embeds must be
 * HTML-encoded, while {@code title} and {@code fullTitle} stay plain text.
 */
class TemplateHelperTest {

    private static final String HOST_NAME = "demo.dotcms.com";
    private static final String CUSTOM_LAYOUT_LABEL = "Custom & Layout";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<LanguageUtil> languageUtil;
    private TemplateAPI templateAPI;
    private TemplateHelper templateHelper;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = mock(User.class);
        templateAPI = mock(TemplateAPI.class);
        final ThemeAPI themeAPI = mock(ThemeAPI.class);
        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final VersionableAPI versionableAPI = mock(VersionableAPI.class);
        when(versionableAPI.getLockedBy(any())).thenReturn(Optional.empty());
        when(themeAPI.findThemeById(any(), any(), anyBoolean())).thenReturn(null);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::systemUser).thenReturn(user);
        apiLocator.when(APILocator::getTemplateAPI).thenReturn(templateAPI);
        apiLocator.when(APILocator::getThemeAPI).thenReturn(themeAPI);
        apiLocator.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);
        apiLocator.when(APILocator::getVersionableAPI).thenReturn(versionableAPI);

        languageUtil = mockStatic(LanguageUtil.class);
        languageUtil.when(() -> LanguageUtil.get(anyString())).thenReturn(CUSTOM_LAYOUT_LABEL);

        templateHelper = new TemplateHelper(mock(ContainerAPI.class));
    }

    @AfterEach
    void tearDown() {
        languageUtil.close();
        apiLocator.close();
    }

    private Template templateWithTitle(final String title, final Host host) throws Exception {
        final Template template = mock(Template.class);
        when(template.getTitle()).thenReturn(title);
        when(template.getIdentifier()).thenReturn("template-id");
        when(templateAPI.getTemplateHost(template)).thenReturn(host);
        return template;
    }

    private static Host hostNamed(final String hostName) {
        final Host host = mock(Host.class);
        when(host.getHostname()).thenReturn(hostName);
        when(host.getIdentifier()).thenReturn("host-id");
        return host;
    }

    /**
     * Given a template whose title holds markup and special characters and a parent host,
     * when the view is built, then the HTML label carries the title encoded (no tag from the title
     * survives) while the plain-text fields keep the title verbatim.
     */
    @Test
    void testToTemplateView_WhenTitleHasMarkup_ShouldEncodeHtmlTitle() throws Exception {
        final String title = "Launch <b>page</b> & \"promo\"";
        final Template template = templateWithTitle(title, hostNamed(HOST_NAME));

        final TemplateView view = templateHelper.toTemplateView(template, user);

        assertFalse(view.getHtmlTitle().contains("<b>"),
                "markup from the title must not reach the HTML label");
        assertTrue(view.getHtmlTitle().contains("&lt;b&gt;page&lt;/b&gt;"),
                "the title must be HTML-encoded inside the label");
        assertTrue(view.getHtmlTitle().contains("&amp;"),
                "an ampersand in the title must be HTML-encoded inside the label");
        assertTrue(view.getHtmlTitle().startsWith("<div>"),
                "the label keeps its two-line markup wrapper");
        assertTrue(view.getHtmlTitle().contains("<small>" + HOST_NAME),
                "the label keeps the host name on its second line");
        assertEquals(title, view.getTitle(), "title stays plain text");
        assertEquals(title + " (" + HOST_NAME + ")", view.getFullTitle(),
                "fullTitle stays plain text");
    }

    /**
     * Given a parent host whose name holds markup, when the view is built, then the host name
     * is encoded inside the HTML label too.
     */
    @Test
    void testToTemplateView_WhenHostNameHasMarkup_ShouldEncodeHtmlTitle() throws Exception {
        final Template template = templateWithTitle("Landing Page", hostNamed("<i>demo</i>"));

        final TemplateView view = templateHelper.toTemplateView(template, user);

        assertFalse(view.getHtmlTitle().contains("<i>"),
                "markup from the host name must not reach the HTML label");
        assertTrue(view.getHtmlTitle().contains("&lt;i&gt;demo&lt;/i&gt;"),
                "the host name must be HTML-encoded inside the label");
        assertEquals("Landing Page (<i>demo</i>)", view.getFullTitle(),
                "fullTitle stays plain text");
    }

    /**
     * Given a template with no parent host, when the view is built, then the HTML label is
     * the encoded title alone and fullTitle is the raw title.
     */
    @Test
    void testToTemplateView_WhenNoHost_ShouldEncodeTitleAlone() throws Exception {
        final String title = "Launch <b>page</b>";
        final Template template = templateWithTitle(title, null);

        final TemplateView view = templateHelper.toTemplateView(template, user);

        assertFalse(view.getHtmlTitle().contains("<"),
                "a label without host must hold no tag at all");
        assertTrue(view.getHtmlTitle().contains("&lt;b&gt;page&lt;/b&gt;"),
                "the title must be HTML-encoded");
        assertEquals(title, view.getFullTitle(), "fullTitle stays the raw title");
    }

    /**
     * Given an anonymous (custom layout) template, when the view is built, then both labels
     * are the localized custom-layout label, HTML-encoded in the HTML label and plain in fullTitle.
     */
    @Test
    void testToTemplateView_WhenAnonymous_ShouldKeepCustomLayoutLabel() throws Exception {
        final Template template = templateWithTitle("anonymous_layout_x", hostNamed(HOST_NAME));
        when(template.isAnonymous()).thenReturn(true);

        final TemplateView view = templateHelper.toTemplateView(template, user);

        assertEquals("Custom &amp; Layout", view.getHtmlTitle(),
                "the localized label is HTML-encoded like any other label text");
        assertEquals(CUSTOM_LAYOUT_LABEL, view.getFullTitle(), "fullTitle stays plain text");
    }

    /**
     * Given a plain title and host, when the view is built, then the HTML label keeps the
     * shape the picker renders (title line, host line) with no spurious entities.
     */
    @Test
    void testToTemplateView_WhenPlainTitle_ShouldKeepTwoLineLabel() throws Exception {
        final Template template = templateWithTitle("Landing Page", hostNamed(HOST_NAME));

        final TemplateView view = templateHelper.toTemplateView(template, user);

        assertTrue(view.getHtmlTitle().startsWith("<div>Landing Page</div>"),
                "title line unchanged for a plain title");
        assertTrue(view.getHtmlTitle().contains("<small>" + HOST_NAME),
                "host line unchanged for a plain host name");
        assertFalse(view.getHtmlTitle().contains("&"), "no entity for plain text");
        assertEquals("Landing Page (" + HOST_NAME + ")", view.getFullTitle());
    }

    /**
     * Given a template with a null title, when the view is built, then the HTML
     * label does not print the word "null".
     */
    @Test
    void testToTemplateView_WhenNullTitle_ShouldNotPrintNull() throws Exception {
        final Template template = templateWithTitle(null, hostNamed(HOST_NAME));

        final TemplateView view = templateHelper.toTemplateView(template, user);

        assertFalse(view.getHtmlTitle().contains("null"),
                "a missing title must not be rendered as the text null");
        assertTrue(view.getHtmlTitle().contains("<small>" + HOST_NAME),
                "the host line is still rendered");
    }
}
