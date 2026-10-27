package com.dotcms.saml.service.impl;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class TestRawTemplateProcessor {

    @Test
    public void testBuildSAMLObject() throws IOException {

        final RawTemplateProcessor rawTemplateProcessor = new RawTemplateProcessor();
        final StringWriter out = new StringWriter();
        final Map<String, String> contextMap = new HashMap<>();

        contextMap.put("action", "https://test.dotcms.com/o/saml2/idp?idpid=ccc");
        contextMap.put("RelayState", "");
        contextMap.put("SAMLRequest", "xxx");
        contextMap.put("SAMLResponse", "yyy");
        rawTemplateProcessor.renderTemplateFile(out, contextMap, "/templates/auth-post-raw.txt");

        Assert.assertNotNull(out.toString());
        Assert.assertTrue(out.toString().contains("https://test.dotcms.com/o/saml2/idp?idpid=ccc"));
        Assert.assertTrue(out.toString().contains("xxx"));
        Assert.assertTrue(out.toString().contains("yyy"));
    }
}
