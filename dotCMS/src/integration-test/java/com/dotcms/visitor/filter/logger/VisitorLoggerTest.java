package com.dotcms.visitor.filter.logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.visitor.domain.Visitor;
import com.dotcms.visitor.filter.characteristics.AbstractCharacter;
import com.dotcms.visitor.filter.servlet.VisitorFilter;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import java.lang.reflect.Constructor;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class VisitorLoggerTest {

    private static List<String> whiteListedHeader;
    private static List<String> whiteListedParams;

    private static Logger logger;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        whiteListedHeader = Arrays.asList(
                Config.getStringProperty("WHITELISTED_HEADERS", "").toLowerCase().split(","));

        whiteListedParams = Arrays.asList(
                Config.getStringProperty("WHITELISTED_PARAMS", "").toLowerCase().split(","));
    }

    @Ignore("Failures are inconsistent")
    @Test
    public void testLog() throws Exception {

        MockedAppender mockedAppender = new MockedAppender();

        try{

            logger = (Logger) LogManager.getLogger(VisitorLogger.class);
            logger.addAppender(mockedAppender);
            logger.setLevel(Level.INFO);
            HttpServletRequest mockRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);

            mockObjects(mockRequest);
            VisitorLogger.log(mockRequest, mockResponse);
            Thread.sleep(3000);
            Assert.assertTrue(!mockedAppender.message.isEmpty());
        } finally{
            if (logger !=null && mockedAppender!=null){
                logger.removeAppender(mockedAppender);
            }
        }


    }

    @Test
    public void testAddConstructor() throws NoSuchMethodException {
        List<Constructor<AbstractCharacter>> result = VisitorLogger.addConstructor(CustomCharacterTest.class);

        Assert.assertTrue(UtilMethods.isSet(result));
        Assert.assertTrue(result.stream()
                .anyMatch(constructor -> constructor.getDeclaringClass().getName()
                        .equals(CustomCharacterTest.class.getName())));
        VisitorLogger.removeConstructor(CustomCharacterTest.class);
    }

    @Test
    public void testRemoveConstructor() throws NoSuchMethodException {

        VisitorLogger.addConstructor(CustomCharacterTest.class);
        List<Constructor<AbstractCharacter>> result = VisitorLogger.removeConstructor(CustomCharacterTest.class);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    private void mockObjects(HttpServletRequest mockRequest) throws UnknownHostException {

        HttpSession mockSession = mock(HttpSession.class);

        when(mockRequest.getSession()).thenReturn(mockSession);
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getSession()).thenReturn(mockSession);

        when(mockRequest.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE))
                .thenReturn("http://localhost:8080/about-us/");
        when(mockRequest.getAttribute(VisitorFilter.DOTPAGE_PROCESSING_TIME))
                .thenReturn(Long.valueOf(1000));
        when(mockRequest.getAttribute(VisitorFilter.VANITY_URL_ATTRIBUTE)).thenReturn("/about-us/");
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");

        Visitor visitor = new Visitor();
        visitor.setIpAddress(HttpRequestDataUtil.getIpAddress(mockRequest));

        when(mockSession.getAttribute(WebKeys.VISITOR)).thenReturn(visitor);

        when(mockRequest.getHeaderNames()).thenReturn(new Vector(whiteListedHeader).elements());
        when(mockRequest.getParameterNames()).thenReturn(new Vector(whiteListedParams).elements());
    }


    public static class CustomCharacterTest extends AbstractCharacter {

        public CustomCharacterTest(AbstractCharacter incomingCharacter) {
            super(incomingCharacter);

            getMap().put("custom_key_test", "Custom character added");
        }
    }

    private static class MockedAppender extends AbstractAppender {

        List<String> message = new ArrayList<>();

        protected MockedAppender() {
            super("MockedAppender", null, null);
        }

        @Override
        public void append(LogEvent event) {
            message.add(event.getMessage().getFormattedMessage());
        }
    }

}
