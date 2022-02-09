package com.dotcms.content.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ObjectMapperTest {

    @BeforeClass
    public static void prepare() throws Exception {

    }

    @Test
    public void TestJsonHtmlEscapingDisabledByDefault() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String inputHtml = "<p>Any Random text will do</p>";
        final Map<String,Object> in = ImmutableMap.of("html", inputHtml);
        final String out = objectMapper.writeValueAsString(in);
        //If the text would have come out escaped this could never be true
        Assert.assertTrue(out.contains(inputHtml));
    }


}
