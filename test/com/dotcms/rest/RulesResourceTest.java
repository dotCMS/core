//package com.dotcms.rest;
//
//import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpStatus;
//import com.dotcms.repackage.javax.ws.rs.core.Response;
//import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
//import com.dotcms.repackage.org.hamcrest.CoreMatchers;
//import com.dotcms.repackage.org.mockito.Mockito;
//import com.dotcms.rest.api.v1.ruleengine.RestRule;
//import com.dotcms.rest.config.AuthenticationProvider;
//import com.dotmarketing.beans.Host;
//import com.dotmarketing.business.ApiProvider;
//import com.dotmarketing.portlets.contentlet.business.HostAPI;
//import com.dotmarketing.portlets.rules.business.RulesAPI;
//import com.dotmarketing.portlets.rules.model.Rule;
//import com.liferay.portal.model.User;
//import javax.servlet.http.HttpServletRequest;
//import org.junit.Before;
//import org.junit.Test;
//
//import static com.dotcms.repackage.org.hamcrest.CoreMatchers.is;
//import static com.dotcms.repackage.org.hamcrest.CoreMatchers.notNullValue;
//import static com.dotcms.repackage.org.junit.Assert.assertThat;
//import static com.dotcms.repackage.org.mockito.Mockito.mock;
//import static com.dotcms.repackage.org.mockito.Mockito.when;
//
//public class RulesResourceTest {
//
//    private ApiProvider apiProvider;
//    private AuthenticationProvider auth;
//    private HttpServletRequest request;
//    private HostAPI hostApi;
//    private RulesAPI rulesApi;
//    private Host host;
//
//    @Before
//    public void setUp() throws Exception {
//        this.host = mock(Host.class);
//        request = mockTheRequest();
//        hostApi = mockTheHostApi();
//        rulesApi = mockTheRuleApi();
//        apiProvider = mockTheApi(hostApi, rulesApi);
//        auth = mockTheAuth();
//    }
//
//    private static AuthenticationProvider mockTheAuth() {
//        AuthenticationProvider auth = mock(AuthenticationProvider.class);
//        when(auth.authenticate(Mockito.any(HttpServletRequest.class))).thenReturn(mock(User.class));
//        return auth;
//    }
//
//    private static HttpServletRequest mockTheRequest() {
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        return request;
//    }
//
//    private static HostAPI mockTheHostApi() {
//        HostAPI hostApi = mock(HostAPI.class);
//        return hostApi;
//    }
//
//    private static RulesAPI mockTheRuleApi() {
//        RulesAPI mock = mock(RulesAPI.class);
//        return mock;
//    }
//
//    private static ApiProvider mockTheApi(HostAPI hostAPI, RulesAPI rulesAPI) {
//        ApiProvider mock = mock(ApiProvider.class);
//        when(mock.hostAPI()).thenReturn(hostAPI);
//        when(mock.rulesAPI()).thenReturn(rulesAPI);
//        return mock;
//    }
//
//    @Test
//    public void testGetRules() throws Exception {
//    }
//
//    @Test
//    public void testGetRule() throws Exception {
//        RulesResource resource = new RulesResource(apiProvider, auth);
//        String siteId = "123";
//        String ruleId = "abc";
//        Host mockHost = mock(Host.class);
//        Rule mockRule = mock(Rule.class);
//
//        User user = auth.authenticate(request);
//        when(hostApi.find(siteId, user, false)).thenReturn(mockHost);
//        when(rulesApi.getRuleById(ruleId, user, false)).thenReturn(mockRule);
//        Response ruleResponse = resource.getRule(request, siteId, ruleId);
//
//        System.out.println(ruleResponse.getEntity());
//        System.out.println(ruleResponse.getMetadata());
//        assertThat(ruleResponse, notNullValue());
//        assertThat(ruleResponse.getStatus(), is(HttpStatus.SC_OK));
//    }
//
//    @Test
//    public void testMapJsonObjectHappyPath() throws Exception {
//        JSONObject json = new JSONObject();
//        String name = "Fake Name";
//        String folder = "SYSTEM";
//        Rule.FireOn fireOn = Rule.FireOn.EVERY_PAGE;
//        String siteId = "abcd-1234";
//        long modDate = System.currentTimeMillis();
//        json.put("name", name);
//        json.put("folder", folder);
//        json.put("fireOn", fireOn);
//        json.put("shortCircuit", false);
//        json.put("site", siteId);
//        json.put("modDate", modDate);
//
//
//        RulesResource resource = new RulesResource(apiProvider, auth);
//        RestRule restRule = resource.mapJsonToRestRule(host, json);
//        assertThat(restRule.name, is(name));
//        assertThat(restRule.folder, is(folder));
//        assertThat(restRule.fireOn, is(fireOn.toString()));
//        assertThat(restRule.site, is(siteId));
//        assertThat(restRule.modDate.getTime(), is(modDate));
//
//    }
//
//    @Test
//    public void testRuleNameMustBeValid() throws Exception {
//        JSONObject json = new JSONObject();
//        String folder = "SYSTEM";
//        Rule.FireOn fireOn = Rule.FireOn.EVERY_PAGE;
//        String siteId = "abcd-1234";
//        long modDate = System.currentTimeMillis();
//        json.put("folder", folder);
//        json.put("fireOn", fireOn);
//        json.put("shortCircuit", false);
//        json.put("site", siteId);
//        json.put("modDate", modDate);
//
//        RestRule restRule = null;
//
//        try {
//            RulesResource resource = new RulesResource(apiProvider, auth);
//            restRule = resource.mapJsonToRestRule(host, json);
//        } catch (Exception e) {
//            assertThat("'" + e.getMessage() + "' should contain 'name' ", e.getMessage().contains("name"), CoreMatchers.is(true));
//        }
//
//        assertThat("Exception should have been thrown: ", restRule, CoreMatchers.nullValue());
//    }
//
//    @Test
//    public void testGetConditionGroups() throws Exception {
//
//    }
//}