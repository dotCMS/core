package com.dotcms.util.network;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class IPUtilsTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}
    final static String[][] trueCases = {
            {"0.0.0.0/0", "127.0.0.1"},
            {"0.0.0.0/0", "244.244.244.1"},
            {"0.0.0.0/0", "0:0:0:0:0:0:0:1"},
            {"192.168.1.0/24", "192.168.1.1"},
            {"192.168.1.0/24", "192.168.1.0"},
            {"192.168.1.0/24", "192.168.1.255"},
            {"192.168.0.0/16", "192.168.1.255"},
            {"192.0.0.0/8", "192.168.1.255"},

    };


    
    final static String[][] falseCases = {
            {"255.0.0.1/24", "255.0.1.2"},
            {"244.244.244.2/32", "244.244.244.1"},
            {"192.168.2.0/24", "192.168.1.1"},
            {"192.168.2.0/24", "192.168.1.255"},
            {"192.169.1.0/24", "192.168.1.255"},
            {"192.169.0.0/16", "192.168.1.255"},
            {"192.0.0.0/8", "193.168.1.255"},

    };



    
    
    @Test
    public void test_true_cases() {
        for(String[] testCase : trueCases) {
            assertTrue( testCase[1] + " is in " + testCase[0], IPUtils.isIpInCIDR(testCase[1], testCase[0]));
            
            
        }

    }
    
    @Test
    public void test_false_cases() {
        for(String[] testCase : falseCases) {
            assertTrue( testCase[1] + " is NOT in " + testCase[0], !IPUtils.isIpInCIDR(testCase[1], testCase[0]));
            
            
        }

    }
}
