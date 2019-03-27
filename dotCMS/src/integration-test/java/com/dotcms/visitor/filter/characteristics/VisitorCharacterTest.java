package com.dotcms.visitor.filter.characteristics;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.unittest.TestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.visitor.filter.servlet.VisitorFilter;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.rules.business.FiredRulesList;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class VisitorCharacterTest {

    public static List<String> whiteListedCookies;
    public static List<String> whiteListedHeader;
    public static List<String> whiteListedParams;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        whiteListedCookies = Arrays.asList(
                Config.getStringProperty("WHITELISTED_COOKIES", "").toLowerCase().split(","));

        whiteListedHeader = Arrays.asList(
                Config.getStringProperty("WHITELISTED_HEADERS", "").toLowerCase().split(","));

        whiteListedParams = Arrays.asList(
                Config.getStringProperty("WHITELISTED_PARAMS", "").toLowerCase().split(","));
    }

    @DataProvider
    public static Object[][] cases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();

            //Test case for VisitorCharacter
            data.add(new TestCase(VisitorCharacter.class, Stream
                    .of("ipHash", "dmid", "device", "weightedTags", "persona", "pagesViewed",
                            "agent").collect(
                            Collectors.toList())));

            //Test for GDPRCharacter
            data.add(new TestCase(GDPRCharacter.class,
                    Stream.of("ip", "userId").collect(Collectors.toList())));

            //Test for RulesEngineCharacter
            //request.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST) == null
            //request.getSession().getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST) == null
            data.add(new TestCase(RulesEngineCharacter.class, Collections.EMPTY_LIST));

            //Test for RulesEngineCharacter
            //request.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST) != null
            //request.getSession().getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST) != null
            data.add(new TestCase(RulesEngineCharacter.class,
                    Stream.of("rulesRequest", "rulesSession").collect(Collectors.toList())));

            //Test for HeaderCharacter
            data.add(new TestCase(HeaderCharacter.class, new ArrayList()));

            //Test for ParamsCharacter
            data.add(new TestCase(ParamsCharacter.class,
                    Stream.of("queryString").collect(Collectors.toList())));

            //Test for GeoCharacter
            data.add(new TestCase(GeoCharacter.class,
                    Stream.of("g.ip").collect(Collectors.toList())));

            return TestUtil.toCaseArray(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @UseDataProvider("cases")
    public void testConstructor(TestCase aCase) throws Exception {

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        mockObjects(mockRequest, aCase);
        assertEquals(aCase.keys.size(), aCase.keys.stream()
                .filter(aCase.getCharacterInstance(mockRequest, mockResponse).getMap()::containsKey)
                .count());
    }

    private void mockObjects(HttpServletRequest mockRequest, TestCase aCase) {

        HttpSession mockSession = mock(HttpSession.class);
        when(mockRequest.getSession()).thenReturn(mockSession);

        Cookie[] cookies = whiteListedCookies.stream().map((s -> new Cookie(s, s)))
                .toArray(Cookie[]::new);

        addWhiteListedKeys(aCase);

        when(mockRequest.getCookies()).thenReturn(cookies);
        when(mockRequest.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE))
                .thenReturn("http://localhost:8080/about-us/");
        when(mockRequest.getAttribute(VisitorFilter.DOTPAGE_PROCESSING_TIME))
                .thenReturn(Long.valueOf(1000));
        when(mockRequest.getAttribute(VisitorFilter.VANITY_URL_ATTRIBUTE)).thenReturn("/about-us/");
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(mockRequest.getHeaderNames()).thenReturn(new Vector(whiteListedHeader).elements());
        when(mockRequest.getParameterNames()).thenReturn(new Vector(whiteListedParams).elements());

        if (aCase.character == RulesEngineCharacter.class && UtilMethods.isSet(aCase.keys)) {
            when(mockRequest.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST))
                    .thenReturn(new FiredRulesList());
            when(mockRequest.getSession().getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST))
                    .thenReturn(new FiredRulesList());
        }
    }

    private void addWhiteListedKeys(TestCase aCase) {

        if (aCase.character == GDPRCharacter.class) {
            aCase.keys.addAll(whiteListedCookies.stream()
                    .map((s -> new StringBuilder("c.").append(s).toString()))
                    .collect(Collectors.toList()));
        } else if (aCase.character == HeaderCharacter.class) {
            aCase.keys.addAll(whiteListedHeader.stream()
                    .map((s -> new StringBuilder("h.").append(s).toString()))
                    .collect(Collectors.toList()));
        } else if (aCase.character == ParamsCharacter.class) {
            aCase.keys.addAll(whiteListedParams.stream()
                    .map((s -> new StringBuilder("p.").append(s).toString()))
                    .collect(Collectors.toList()));
        }
    }

    private static class TestCase {

        private Class character;
        private List keys;

        public TestCase(Class character, List keys) {
            this.keys = keys;
            this.character = character;
        }

        public AbstractCharacter getCharacterInstance(HttpServletRequest mockRequest,
                HttpServletResponse mockResponse) throws Exception {
            AbstractCharacter abstractVisitor = new BaseCharacter(mockRequest, mockResponse);
            return (AbstractCharacter) this.character.getConstructor(AbstractCharacter.class)
                    .newInstance(abstractVisitor);
        }

    }

}
