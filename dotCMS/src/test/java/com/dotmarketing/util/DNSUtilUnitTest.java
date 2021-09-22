package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.UnknownHostException;
import org.junit.Test;

public class DNSUtilUnitTest {

    /**
     * Method to test: {@link DNSUtil#reverseDns(String)}
     * When: test wit the follow Ips: 205.251.198.30 and 8.8.8.8
     * Should: return ns-1566.awsdns-03.co.uk. and dns.google.
     *
     * @throws IOException
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
     * @throws IOException
     */
    @Test(expected = UnknownHostException.class)
    public void notExistsIP() throws IOException {
        DNSUtil.reverseDns("not_ip");
    }

    /**
     * Method to test: {@link DNSUtil#reverseDns(String)}
     * When: Use local Ip address
     * Should: return 127.0.0.1
     *
     * @throws IOException
     */
    @Test()
    public void localIP() throws IOException {
        assertEquals("127.0.0.1",DNSUtil.reverseDns("127.0.0.1"));
    }

    /**
     * Method to test: {@link DNSUtil#reverseDns(String)}
     * When: Use with any IP
     * Should: return the same IP
     *
     * @throws IOException
     */
    @Test()
    public void anyIP() throws IOException {
        assertEquals("192.168.0.100",DNSUtil.reverseDns("192.168.0.100"));
    }
}
