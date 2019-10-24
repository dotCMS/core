package com.dotmarketing.business.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.TestDataUtils;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.apache.struts.Globals;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableMap;
import org.mockito.Mockito;
import java.util.Locale;

public class LanguageWebApiTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }

    LanguageWebAPIImpl lapi = new LanguageWebAPIImpl();


    @Test
    public void testDefaultLanguage() {


        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();

        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang, APILocator.getLanguageAPI().getDefaultLanguage());

    }

    @Test
    public void testLanguageIdParameter() {

        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        // test passing in a parameter
        ImmutableMap<String, String> map =
                ImmutableMap.<String, String>builder().put("language_id", String.valueOf(spanishLanguage.getId())).build();

        pageRequest = new MockParameterRequest(pageRequest,map);
        Language lang2 = lapi.getLanguage(pageRequest);
        assertEquals(lang2.getId(), spanishLanguage.getId());
    }
    
    @Test
    public void testLangWebKeyParameter() {

        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        // test passing in a parameter
        ImmutableMap<String, String> map =
                ImmutableMap.<String, String>builder().put(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId())).build();

        pageRequest = new MockParameterRequest(pageRequest,map);
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), spanishLanguage.getId());

    }
    
    @Test
    public void testLangAttribute() {

        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();

        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), spanishLanguage.getId());

    }
    
    @Test
    public void testLangInSession() {

        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        assertEquals(pageRequest.getSession(false), null);
        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), spanishLanguage.getId());
        assert(pageRequest.getSession(false) !=null);
        
        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, null);
        // this should still be 2 because of the session
        lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), spanishLanguage.getId());

    }
    
    @Test
    public void testLangInTimeMachine() {
        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();

        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();
        
        Language lang1 = lapi.getLanguage(pageRequest);
        assertEquals(lang1.getId(), 1);
        assertEquals(pageRequest.getSession(true), pageRequest.getSession(false));
        pageRequest.getSession().setAttribute("tm_lang", String.valueOf(spanishLanguage.getId()));
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), spanishLanguage.getId());
        assert(pageRequest.getSession(false) !=null);
        
        
        
        pageRequest.getSession().setAttribute("tm_lang", null);
        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, null);
        // this should still be 1 because we have removed the tm_lang
        lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 1);

    }
    
    @Test
    public void testBadLangWebKeyParameter() {

        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        Language lang1 = lapi.getLanguage(pageRequest);
        assertEquals(lang1.getId(), 1);
        
        
        
        pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        // test passing in a parameter
        ImmutableMap<String, String> map =
                ImmutableMap.<String, String>builder().put(WebKeys.HTMLPAGE_LANGUAGE, "NOTALANG").build();

        pageRequest = new MockParameterRequest(pageRequest,map);
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 1);
        
        
        
        pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        // test passing in a parameter
        map =
                ImmutableMap.<String, String>builder().put("language_id", "NOTALANG").build();

        pageRequest = new MockParameterRequest(pageRequest,map);
        lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 1);
        

    }
    
    @Test
    public void testSettingMultipleWays() {

        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();
        
        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        Map<String, String> map = new HashMap<>();
        //map.put("language_id", "2");

        pageRequest = new MockParameterRequest(pageRequest,map);
        
        
        Language lang1 = lapi.getLanguage(pageRequest);
        assertEquals(lang1.getId(), 1);
        
        
        
        pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();

        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), spanishLanguage.getId());

    }

    /**
     * Should return default lang when request is null
     */
    @Test
    public void testShouldReturnDefaultLangWhenReturnNull() {
        final Language backendLanguage = lapi.getBackendLanguage(null);
        assertEquals(backendLanguage, APILocator.getLanguageAPI().getDefaultLanguage());
    }

    /**
     * Should return default lang when request is null
     */
    @Test
    public void testShouldReturnParameterLanguage() {
        final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getParameter(WebKeys.BACKEND_LANGUAGE_PARAMETER_NAME)).thenReturn("en_US");

        Language backendLanguage = lapi.getBackendLanguage(null);
        assertEquals(backendLanguage, APILocator.getLanguageAPI().getLanguage(1));

        when(mockRequest.getParameter(WebKeys.BACKEND_LANGUAGE_PARAMETER_NAME)).thenReturn("en-US");

        backendLanguage = lapi.getBackendLanguage(null);
        assertEquals(backendLanguage, APILocator.getLanguageAPI().getLanguage(1));
    }

    /**
     * Should return default lang when request's parameter and session attribute is null
     */
    @Test
    public void testShouldReturnDefaultWhenParameterIsNoTRight() {
        final HttpSession session = Mockito.mock(HttpSession.class);

        final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getParameter(WebKeys.BACKEND_LANGUAGE_PARAMETER_NAME)).thenReturn("en");
        when(mockRequest.getSession(false)).thenReturn(session);

        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(null);

        final Language backendLanguage = lapi.getBackendLanguage(mockRequest);
        assertEquals(backendLanguage, APILocator.getLanguageAPI().getDefaultLanguage());
    }

    /**
     * Should return default lang when request's parameter is null and there is not session
     */
    @Test
    public void testShouldReturnDefaultWhenIsNotSession() {
        final HttpSession session = Mockito.mock(HttpSession.class);

        final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getParameter(WebKeys.BACKEND_LANGUAGE_PARAMETER_NAME)).thenReturn("en");
        when(mockRequest.getSession(false)).thenReturn(null);

        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(null);

        final Language backendLanguage = lapi.getBackendLanguage(mockRequest);
        assertEquals(backendLanguage, APILocator.getLanguageAPI().getDefaultLanguage());
    }

    /**
     * Should return lang from session when session attribute is not null
     */
    @Test
    public void testShouldReturnLangFromSession() {
        final HttpSession session = Mockito.mock(HttpSession.class);

        final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getParameter(WebKeys.BACKEND_LANGUAGE_PARAMETER_NAME)).thenReturn("en");
        when(mockRequest.getSession(false)).thenReturn(session);

        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale("en", "US"));

        final Language backendLanguage = lapi.getBackendLanguage(mockRequest);
        assertEquals(backendLanguage, APILocator.getLanguageAPI().getLanguage(1));
    }
}
