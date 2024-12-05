package com.dotcms.util;

import java.net.MalformedURLException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataSourceAttributesTest {



    String badPassword = "pass:/+%&word";
    String userName = "testing:/+%&user";
    String url = "jdbc:postgresql://localhost:5432/dotcms";


    @Test
    public void testing_url_encoding() throws MalformedURLException {
        DataSourceAttributes dataSourceAttributes = new DataSourceAttributes(userName, badPassword, url);
        assertEquals("jdbc:postgresql://localhost:5432/dotcms", dataSourceAttributes.url);
        assertEquals("testing%3A%2F%2B%25%26user", dataSourceAttributes.username);


        assertEquals(
                "postgresql://testing%3A%2F%2B%25%26user:pass%3A%2F%2B%25%26word@localhost:5432/dotcms",
                dataSourceAttributes.getDbUrl()
        );



    }
}
