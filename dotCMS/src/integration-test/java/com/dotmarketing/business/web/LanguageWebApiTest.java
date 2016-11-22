package com.dotmarketing.business.web;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        // test passing in a parameter
        ImmutableMap<String, String> map =
                ImmutableMap.<String, String>builder().put("language_id", "2").build();

        pageRequest = new MockParameterRequest(pageRequest,map);
        Language lang2 = lapi.getLanguage(pageRequest);
        assertEquals(lang2.getId(), 2);
    }
    
    @Test
    public void testLangWebKeyParameter() {

        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        // test passing in a parameter
        ImmutableMap<String, String> map =
                ImmutableMap.<String, String>builder().put(WebKeys.HTMLPAGE_LANGUAGE, "2").build();

        pageRequest = new MockParameterRequest(pageRequest,map);
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 2);

    }
    
    @Test
    public void testLangAttribute() {
        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();

        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 2);

    }
    
    @Test
    public void testLangInSession() {
        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        assertEquals(pageRequest.getSession(false), null);
        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 2);
        assert(pageRequest.getSession(false) !=null);
        
        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, null);
        // this should still be 2 because of the session
        lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 2);

    }
    
    @Test
    public void testLangInTimeMachine() {
        HttpServletRequest pageRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                        .request();
        
        
        Language lang1 = lapi.getLanguage(pageRequest);
        assertEquals(lang1.getId(), 1);
        assertEquals(pageRequest.getSession(true), pageRequest.getSession(false));
        pageRequest.getSession().setAttribute("tm_lang", "2");
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 2);
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

        pageRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        
        
        Language lang = lapi.getLanguage(pageRequest);
        assertEquals(lang.getId(), 2);

    }
    
    
    
    
    
    
}
