package com.dotmarketing.osgi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.portal.PortletFactory;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.model.Portlet;
import com.liferay.util.Http;
import java.util.List;
import org.apache.felix.framework.OSGIUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import com.dotmarketing.business.portal.PortletAPI;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


class GenericBundleActivatorTest {

    private static final String COM_LIFERAY_PORTLET_JSPPORTLET = "com.liferay.portlet.JSPPortlet";
    private static final String COM_LIFERAY_PORTLET_VELOCITY_PORTLET = "com.liferay.portlet.VelocityPortlet";
    private static final String COM_LIFERAY_PORTLET_STRUTS_PORTLET = "com.liferay.portlet.StrutsPortlet";
    private static final String INIT_PARAM_VIEW_JSP = "view-jsp";
    private static final String INIT_PARAM_VIEW_TEMPLATE = "view-template";

    private GenericBundleActivator activator;
    private BundleContext bundleContext;
    private Bundle bundle;

    @BeforeEach
    public void setup() {
        activator = new GenericBundleActivator() {
            @Override
            public void start(BundleContext bundleContext) throws Exception {

            }

            @Override
            public void stop(BundleContext bundleContext) throws Exception {

            }
        };
        bundleContext = mock(BundleContext.class);
        bundle = mock(Bundle.class);
    }

    @Test
    void testRegisterPortlets() throws Exception {
        // Arrange
        String[] xmls = {"conf/portlet.xml", "conf/liferay-portlet.xml"};
        Portlet jspPortlet = mockPortlet(COM_LIFERAY_PORTLET_JSPPORTLET, "ext/hello.jsp");
        Portlet velocityPortlet = mockPortlet(COM_LIFERAY_PORTLET_VELOCITY_PORTLET, "ext/view.vtl");
        Portlet strutsPortlet = mockPortlet(COM_LIFERAY_PORTLET_STRUTS_PORTLET, "ext/strutshello/view_hello");

        Map<String, Portlet> portletMap = new ConcurrentHashMap<>();
        portletMap.put("EXT_HELLO_WORLD", jspPortlet);
        portletMap.put("EXT_VELOCITY_WORLD", velocityPortlet);
        portletMap.put("EXT_STRUTS_WORLD", strutsPortlet);

        // Mock bundle and context behavior
        when(bundleContext.getBundle()).thenReturn(bundle);
        when(bundle.getResource(anyString())).thenAnswer(invocation -> {
            String resourcePath = invocation.getArgument(0);
            // Return a dummy URL for simplicity
            return new URL("http", "localhost", "/test/" + resourcePath);
        });

        // Load XML content from test resource file
        String xmlContent = loadXmlFromResource("com/dotmarketing/osgi/bundle/conf/portlet.xml");

        // Mock Http.URLtoString to return the XML content
        try (MockedStatic<Http> httpMock = mockStatic(Http.class);
                MockedStatic<PortletManagerFactory> portletManagerFactoryMock = mockStatic(PortletManagerFactory.class);
                MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class);
                MockedStatic<ActivatorUtil> activatorUtilMock = mockStatic(ActivatorUtil.class);
                MockedStatic<OSGIUtil> osgiUtilStaticMock = mockStatic(OSGIUtil.class);

        ) {
            httpMock.when(() -> Http.URLtoString(any(URL.class))).thenReturn(xmlContent);

            // Mock PortletManagerFactory.getManager
            PortletFactory mockPortletFactory = mock(PortletFactory.class);
            portletManagerFactoryMock.when(PortletManagerFactory::getManager).thenReturn(mockPortletFactory);
            PortletAPI mockPortletAPI = mock(PortletAPI.class);
            apiLocatorMock.when(APILocator::getPortletAPI).thenReturn(mockPortletAPI);


            when(mockPortletFactory.xmlToPortlets(any(InputStream[].class))).thenAnswer(invocation -> {
                // Return mock portlet based on input
                InputStream[] inputStream = invocation.getArgument(0);
                String xml = new String(inputStream[0].readAllBytes(), StandardCharsets.UTF_8);
                // Mock logic to create Portlet
                return Map.of(jspPortlet.getPortletId(),jspPortlet, velocityPortlet.getPortletId(),velocityPortlet, strutsPortlet.getPortletId(),strutsPortlet);
            });

            OSGIUtil osgiUtilMock = mock(OSGIUtil.class);
            List<String> mockStoppedIds = mock(List.class);
            osgiUtilStaticMock.when(OSGIUtil::getInstance).thenReturn(osgiUtilMock);
            when(osgiUtilMock.getPortletIDsStopped()).thenReturn(mockStoppedIds);


            // Mock the getBundleFolder to return a valid folder path
            activatorUtilMock.when(() -> ActivatorUtil.getBundleFolder(any(BundleContext.class), anyString()))
                    .thenReturn("/mocked/bundle/folder");

            // Simulate the effect of moveResources
            activatorUtilMock.when(() -> ActivatorUtil.moveResources(any(BundleContext.class), anyString()))
                    .then(invocation -> {
                        BundleContext context = invocation.getArgument(0);
                        String path = invocation.getArgument(1);
                        System.out.println("Mocked moveResources called with path: " + path);
                        return null; // or any relevant simulated effect
                    });

            // Simulate the effect of moveVelocityResources
            activatorUtilMock.when(() -> ActivatorUtil.moveVelocityResources(any(BundleContext.class), anyString()))
                    .then(invocation -> {
                        BundleContext context = invocation.getArgument(0);
                        String path = invocation.getArgument(1);
                        System.out.println("Mocked moveVelocityResources called with path: " + path);
                        return null; // or any relevant simulated effect
                    });



            Collection<Portlet> result = activator.registerPortlets(bundleContext, xmls);


            assertEquals(3, result.size());

            result.forEach(portlet -> {
                assertNotNull(portlet.getPortletId());
                assertNotNull(portlet.getInitParams());
                Map<String, String> initParams = portlet.getInitParams();
                if (portlet.getPortletId().equals(jspPortlet.getPortletId())) {
                    assertNotNull(initParams.get(INIT_PARAM_VIEW_JSP));
                    assertEquals("/mocked/bundle/folder/ext/hello.jsp",portlet.getInitParams().get(INIT_PARAM_VIEW_JSP));
                } else if (portlet.getPortletId().equals(velocityPortlet.getPortletId())) {
                    assertNotNull(initParams.get(INIT_PARAM_VIEW_TEMPLATE));
                    assertEquals("/mocked/bundle/folder/ext/view.vtl",portlet.getInitParams().get(INIT_PARAM_VIEW_TEMPLATE));
                } else if (portlet.getPortletId().equals(strutsPortlet.getPortletId())) {
                    assertNotNull(initParams.get(INIT_PARAM_VIEW_TEMPLATE));
                    // Struts portlet should have a different view template and path is not modified
                    assertEquals("ext/strutshello/view_hello",portlet.getInitParams().get(INIT_PARAM_VIEW_TEMPLATE));
                }
            });

        }
    }

    // Helper method to create a mocked Portlet with specific class and init params
    private Portlet mockPortlet(String portletClass, String initParamValue) {
        Portlet portlet = mock(Portlet.class);
        when(portlet.getPortletClass()).thenReturn(portletClass);
        when(portlet.getPortletId()).thenReturn(UUID.randomUUID().toString());

        Map<String, String> initParams = new HashMap<>();
        String initParamName = portletClass.equals(COM_LIFERAY_PORTLET_JSPPORTLET) ? INIT_PARAM_VIEW_JSP : INIT_PARAM_VIEW_TEMPLATE;
        initParams.put(initParamName, initParamValue);
        when(portlet.getInitParams()).thenReturn(initParams);

        return portlet;
    }

    // Helper method to load XML content from a resource file
    private String loadXmlFromResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}