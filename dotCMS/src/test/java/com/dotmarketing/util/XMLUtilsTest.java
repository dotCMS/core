package com.dotmarketing.util;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XMLUtilsTest {

    @Test
    public void isValidXML_null_Test() throws IOException {

        assertFalse(XMLUtils.isValidXML(null));
    }

    @Test
    public void isValidXML_empty_Test() throws IOException {

        assertFalse(XMLUtils.isValidXML(""));
    }

    @Test
    public void isValidXML_invalid_Test() throws IOException {

        assertFalse(XMLUtils.isValidXML("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"https://accounts.google.com/o/saml2?idpid=C00zafdhs\" validUntil=\"2020-10-17T13:33:53.000Z\">\n"));
    }

    @Test
    public void isValidXML_valid_Test() throws IOException {

        assertTrue(XMLUtils.isValidXML("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<test><test1/></test>"));
    }
}
