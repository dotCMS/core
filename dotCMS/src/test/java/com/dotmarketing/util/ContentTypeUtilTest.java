package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;

/**
 * Ensures the correct behavior of the {@link ContentTypeUtil} class.
 * 
 * @author Freddy Rodriguez
 */
public class ContentTypeUtilTest extends UnitTestBase {
    private static final String CONTENT_TYPE_INODE = "38a3f133-85e1-4b07-b55e-179f38303b90";
    private static final String LAYOUT_ID = "71b8a1ca-37b6-4b6e-a43b-c7482f28db6c";
    private static final String CTX_PATH = "/ctx";
    private static final String ACTION_PARAM = "&p_p_action=1&p_p_state=maximized";

    private ContentTypeUtil contentTypeUtil;
    private HttpServletRequest request;
    private LayoutAPI layoutAPI;
    private LanguageAPI languageAPI;
    private User user;
    private Language language;

    @Before
    public void setUp() throws DotDataException {
        request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        layoutAPI = mock(LayoutAPI.class);
        LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        languageAPI = mock(LanguageAPI.class);
        user = mock(User.class);
        language = mock(Language.class);
        HttpServletRequestThreadLocal httpServletRequestThreadLocal = mock(HttpServletRequestThreadLocal.class);
        contentTypeUtil = new ContentTypeUtil(layoutAPI, languageAPI, httpServletRequestThreadLocal, loginService);

        Layout layout = new Layout();
        layout.setPortletIds(List.of(PortletID.CONTENT.toString()));
        layout.setId(LAYOUT_ID);

        when(layoutAPI.loadLayoutsForUser(user)).thenReturn(List.of(layout));
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(WebKeys.CTX_PATH)).thenReturn(CTX_PATH);
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(user);
        when(httpServletRequestThreadLocal.getRequest()).thenReturn(request);
        when(loginService.getLoggedInUser(request)).thenReturn(user);
    }

    /**
     * Tests the behavior of {@link ContentTypeUtil#getActionUrl(com.dotcms.contenttype.model.type.ContentType)} method.<br>
     * <p>
     * Given: A structure with a specified language ID.<br>
     * Expected result: The generated action URL matches the expected URL format based on the provided language ID.
     * </p>
     */
    @Test
    public void testGetActionUrl() {
        final long providedLanguageId = 2L;

        when(languageAPI.getLanguage(any(), any())).thenReturn(language);
        when(language.getId()).thenReturn(providedLanguageId);

        Structure structure = createMockStructure();
        String expectedUrl = generateExpectedUrl(LAYOUT_ID, CONTENT_TYPE_INODE, String.valueOf(providedLanguageId));

        String actionUrl = contentTypeUtil.getActionUrl(new StructureTransformer(structure).from());

        assertUrlMatchesExpected(actionUrl, expectedUrl);
    }

    /**
     * Tests the behavior of {@link ContentTypeUtil#getActionUrl(HttpServletRequest, String, User, String, String)} method
     * when a language ID is provided.<br>
     * <p>
     * Given: A request, user, content type inode, and an action path with a specified language ID.<br>
     * Expected result: The generated action URL matches the expected URL format based on the provided language ID.
     * </p>
     */
    @Test
    public void testGetActionUrl_withLanguageId() {
        final long providedLanguageId = 2L;

        String expectedUrl = generateExpectedUrl(LAYOUT_ID, CONTENT_TYPE_INODE, String.valueOf(providedLanguageId));
        String actionUrl = contentTypeUtil.getActionUrl(request, CONTENT_TYPE_INODE, user, "/ext/contentlet/edit_contentlet", String.valueOf(providedLanguageId));

        assertUrlMatchesExpected(actionUrl, expectedUrl);
    }

    /**
     * Tests the behavior of {@link ContentTypeUtil#getActionUrl(HttpServletRequest, String, User, String, String)} method
     * when no language ID is provided, falling back to the default language.<br>
     * <p>
     * Given: A request, user, content type inode, and an action path with no specified language ID.<br>
     * Expected result: The generated action URL matches the expected URL format based on the default language ID.
     * </p>
     */
    @Test
    public void testGetActionUrl_withoutLanguageId() {
        final long fallbackLanguageId = 1L;
        when(languageAPI.getDefaultLanguage()).thenReturn(language);
        when(language.getId()).thenReturn(fallbackLanguageId);

        String expectedUrl = generateExpectedUrl(LAYOUT_ID, CONTENT_TYPE_INODE, String.valueOf(fallbackLanguageId));
        String actionUrl = contentTypeUtil.getActionUrl(request, CONTENT_TYPE_INODE, user, "/ext/contentlet/edit_contentlet", null);

        assertUrlMatchesExpected(actionUrl, expectedUrl);
    }

    private String generateExpectedUrl(String layoutId, String contentTypeInode, String languageId) {
        return "/ctx/portal_public/layout" +
                "?p_l_id=" + layoutId +
                "&p_p_id=" + PortletID.CONTENT +
                "&p_p_action=1" +
                "&p_p_state=maximized" +
                "&_content_inode=" +
                "&_content_referer=%2Fctx%2Fportal_public%2Flayout%3Fp_l_id%3D" + layoutId +
                "%26p_p_id%3D" + PortletID.CONTENT +
                "%26p_p_action%3D1" +
                "%26p_p_state%3Dmaximized" +
                "%26_content_inode%3D" +
                "%26_content_structure_id%3D" + contentTypeInode +
                "%26_content_cmd%3Dnew" +
                "%26_content_lang%3D" + languageId +
                "%26_content_struts_action%3D%252Fext%252Fcontentlet%252Fview_contentlets" +
                "&_content_selectedStructure=" + contentTypeInode +
                "&_content_cmd=new" +
                "&_content_lang=" + languageId +
                "&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet";
    }


    private Structure createMockStructure() {
        Structure structure = mock(Structure.class);
        when(structure.getStructureType()).thenReturn(1);
        when(structure.getInode()).thenReturn(CONTENT_TYPE_INODE);
        when(structure.getModDate()).thenReturn(new Date());
        when(structure.getIDate()).thenReturn(new Date());
        when(structure.getName()).thenReturn("testSt");
        when(structure.getVelocityVarName()).thenReturn("testSt");
        return structure;
    }

    private void assertUrlMatchesExpected(String actualUrl, String expectedUrl) {
        Map<String, String> expectedParams = parseQueryParams(expectedUrl);
        Map<String, String> actualParams = parseQueryParams(actualUrl);
        assertEquals("The expected actionUrl parameters are not the same as the ones generated by the Util.", expectedParams, actualParams);
    }

    private Map<String, String> parseQueryParams(String url) {
        return Stream.of(url.substring(url.indexOf('?') + 1).split("&"))
                .map(param -> param.split("="))
                .collect(Collectors.toMap(param -> param[0], param -> param.length > 1 ? param[1] : ""));
    }
}