package com.dotmarketing.business.portal;

import static org.junit.Assert.*;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.mockito.Mockito.*;

public class PortletFactoryTest {

    @Mock
    private PortletFactory portletFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPortlets() throws SystemException {
        Collection<Portlet> mockPortlets = Arrays.asList(mock(Portlet.class), mock(Portlet.class));
        when(portletFactory.getPortlets()).thenReturn(mockPortlets);

        Collection<Portlet> result = portletFactory.getPortlets();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testFindById() {
        String portletId = "test-portlet";
        Portlet mockPortlet = mock(Portlet.class);
        when(portletFactory.findById(portletId)).thenReturn(mockPortlet);

        Portlet result = portletFactory.findById(portletId);
        assertNotNull(result);
        assertEquals(mockPortlet, result);
    }

    @Test
    public void testUpdatePortlet() throws DotDataException {
        Portlet mockPortlet = mock(Portlet.class);
        when(portletFactory.updatePortlet(mockPortlet)).thenReturn(mockPortlet);

        Portlet result = portletFactory.updatePortlet(mockPortlet);
        assertNotNull(result);
        assertEquals(mockPortlet, result);
    }

    @Test
    public void testDeletePortlet() throws DotDataException {
        String portletId = "test-portlet";
        doNothing().when(portletFactory).deletePortlet(portletId);

        portletFactory.deletePortlet(portletId);
        verify(portletFactory, times(1)).deletePortlet(portletId);
    }

    @Test
    public void testInsertPortlet() throws DotDataException {
        Portlet mockPortlet = mock(Portlet.class);
        when(portletFactory.insertPortlet(mockPortlet)).thenReturn(mockPortlet);

        Portlet result = portletFactory.insertPortlet(mockPortlet);
        assertNotNull(result);
        assertEquals(mockPortlet, result);
    }

    @Test
    public void testPortletToXml() throws DotDataException {
        Portlet mockPortlet = mock(Portlet.class);
        String expectedXml = "<portlet><portlet-name>TestPortlet</portlet-name></portlet>";
        when(portletFactory.portletToXml(mockPortlet)).thenReturn(expectedXml);

        String result = portletFactory.portletToXml(mockPortlet);
        assertEquals(expectedXml, result);
    }

    @Test
    public void testXmlToPortlets() throws SystemException {
        String[] xmlFiles = {"file1.xml", "file2.xml"};
        Map<String, Portlet> mockPortletMap = new HashMap<>();
        mockPortletMap.put("portlet1", mock(Portlet.class));
        mockPortletMap.put("portlet2", mock(Portlet.class));
        when(portletFactory.xmlToPortlets(xmlFiles)).thenReturn(mockPortletMap);

        Map<String, Portlet> result = portletFactory.xmlToPortlets(xmlFiles);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("portlet1"));
        assertTrue(result.containsKey("portlet2"));
    }

    @Test
    public void testXmlToPortletsWithInputStream() throws SystemException {
        InputStream[] inputStreams = {mock(InputStream.class), mock(InputStream.class)};
        Map<String, Portlet> mockPortletMap = new HashMap<>();
        mockPortletMap.put("portlet1", mock(Portlet.class));
        mockPortletMap.put("portlet2", mock(Portlet.class));
        when(portletFactory.xmlToPortlets(inputStreams)).thenReturn(mockPortletMap);

        Map<String, Portlet> result = portletFactory.xmlToPortlets(inputStreams);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("portlet1"));
        assertTrue(result.containsKey("portlet2"));
    }

    @Test
    public void testXmlToPortlet() throws IOException, JAXBException {
        String xml = "<portlet><portlet-name>TestPortlet</portlet-name></portlet>";
        Portlet mockPortlet = mock(Portlet.class);
        when(portletFactory.xmlToPortlet(xml)).thenReturn(Optional.of(mockPortlet));

        Optional<Portlet> result = portletFactory.xmlToPortlet(xml);
        assertTrue(result.isPresent());
        assertEquals(mockPortlet, result.get());
    }
}