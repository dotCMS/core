package com.dotcms.util;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by nollymar on 3/8/17.
 */
public class HttpRequestDataUtilTest {

    @Test
    public void testGetIpAddress() throws UnknownHostException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("www.dotcms.com,www.google.com,www.github.com");

        InetAddress ip = HttpRequestDataUtil.getIpAddress(request);
        Assert.assertNotNull(ip);
        Assert.assertEquals(ip.getHostName(), "www.dotcms.com");
        Assert.assertNotNull(ip.getHostAddress());

    }
}
