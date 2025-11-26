package com.liferay.portal.ejb;

import static org.junit.Assert.*;
import com.dotmarketing.business.portal.PortletFactory;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import static org.mockito.Mockito.*;

public class PortletManagerUtilTest {

    @Mock
    private PortletFactory portletFactory;

    @Mock
    private Portlet mockPortlet;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddPortlets() throws SystemException, DotDataException {
        try (MockedStatic<PortletManagerFactory> factoryMock = mockStatic(PortletManagerFactory.class)) {
            factoryMock.when(PortletManagerFactory::getManager).thenReturn(portletFactory);

            InputStream[] xmlStreams = {mock(InputStream.class), mock(InputStream.class)};
            Map<String, Portlet> portletMap = new HashMap<>();
            portletMap.put("portlet1", mockPortlet);
            portletMap.put("portlet2", mockPortlet);

            when(portletFactory.xmlToPortlets(xmlStreams)).thenReturn(portletMap);
            when(portletFactory.insertPortlet(any(Portlet.class))).thenReturn(mockPortlet);

            Map<String, Portlet> result = PortletManagerUtil.addPortlets(xmlStreams);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(portletFactory, times(2)).insertPortlet(any(Portlet.class));
        }
    }

    @Test(expected = SystemException.class)
    public void testAddPortletsException() throws SystemException {
        try (MockedStatic<PortletManagerFactory> factoryMock = mockStatic(PortletManagerFactory.class)) {
            factoryMock.when(PortletManagerFactory::getManager).thenReturn(portletFactory);

            InputStream[] xmlStreams = {mock(InputStream.class)};
            when(portletFactory.xmlToPortlets(xmlStreams)).thenThrow(new RuntimeException("Test exception"));

            PortletManagerUtil.addPortlets(xmlStreams);
        }
    }

    @Test
    public void testGetPortletById() throws SystemException {
        try (MockedStatic<PortletManagerFactory> factoryMock = mockStatic(PortletManagerFactory.class)) {
            factoryMock.when(PortletManagerFactory::getManager).thenReturn(portletFactory);

            String companyId = "testCompany";
            String portletId = "testPortlet";

            when(portletFactory.findById(portletId)).thenReturn(mockPortlet);

            Portlet result = PortletManagerUtil.getPortletById(companyId, portletId);

            assertNotNull(result);
            assertEquals(mockPortlet, result);
        }
    }

    @Test(expected = SystemException.class)
    public void testGetPortletByIdException() throws SystemException {
        try (MockedStatic<PortletManagerFactory> factoryMock = mockStatic(PortletManagerFactory.class)) {
            factoryMock.when(PortletManagerFactory::getManager).thenReturn(portletFactory);

            String companyId = "testCompany";
            String portletId = "testPortlet";

            when(portletFactory.findById(portletId)).thenThrow(new RuntimeException("Test exception"));

            PortletManagerUtil.getPortletById(companyId, portletId);
        }
    }

    @Test
    public void testGetPortlets() throws SystemException {
        try (MockedStatic<PortletManagerFactory> factoryMock = mockStatic(PortletManagerFactory.class)) {
            factoryMock.when(PortletManagerFactory::getManager).thenReturn(portletFactory);

            String companyId = "testCompany";
            Collection<Portlet> portlets = Arrays.asList(mockPortlet, mockPortlet);

            when(portletFactory.getPortlets()).thenReturn(portlets);

            Collection<Portlet> result = PortletManagerUtil.getPortlets(companyId);

            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Test(expected = SystemException.class)
    public void testGetPortletsException() throws SystemException {
        try (MockedStatic<PortletManagerFactory> factoryMock = mockStatic(PortletManagerFactory.class)) {
            factoryMock.when(PortletManagerFactory::getManager).thenReturn(portletFactory);

            String companyId = "testCompany";

            when(portletFactory.getPortlets()).thenThrow(new RuntimeException("Test exception"));

            PortletManagerUtil.getPortlets(companyId);
        }
    }
}