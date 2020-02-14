package com.dotcms.cache.lettuce;

import org.junit.Test;

public class LettuceClientTest {

    @Test
    public void test_client_is_singleton() throws Exception {

        LettuceClient client1 = LettuceClient.getInstance();
        
        LettuceClient client2 = LettuceClient.getInstance();
        
        assert(client1 == client2);

    }

}
