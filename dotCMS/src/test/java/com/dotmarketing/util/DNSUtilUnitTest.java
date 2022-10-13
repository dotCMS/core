package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import org.junit.Test;

public class DNSUtilUnitTest {

    /**
     * Method to test: {@link DNSUtil#reverseDns(String)}
     * When: test wit the follow Ips: 205.251.198.30 and 8.8.8.8
     * Should: return ns-1566.awsdns-03.co.uk. and dns.google.
     *
     */
    @Test
    public void reverseDns() throws IOException {
        assertEquals("ns-1566.awsdns-03.co.uk.", DNSUtil.reverseDns("205.251.198.30"));
        assertEquals("dns.google.", DNSUtil.reverseDns("8.8.8.8"));
    }

    /**
     * Method to test: {@link DNSUtil#reverseDns(String)}
     * When: Use Invalid Ip address
     * Should: throw {@link java.net.UnknownHostException}
     *
     */
    @Test(expected = UnknownHostException.class)
    public void notExistsIP() throws IOException {
        DNSUtil.reverseDns("not_ip");
    }

    /**
     * Method to test: {@link DNSUtil#reverseDns(String)}
     * When: Use with any IP
     * Should: return the same IP
     *
     */
    @Test()
    public void anyIP() throws IOException {
        String name = DNSUtil.reverseDns("192.168.0.100");
        // Need to handle amazon resolution e.g. ip-192-168-0-100.us-east-2.compute.internal.
        //(192/.168/./0./100)|
        assertTrue(name.matches("(192\\.168\\.0\\.100)|(ip-192-168-0-100\\..*\\.compute\\.internal\\.)"));
    }
}
