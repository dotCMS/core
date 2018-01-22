package com.dotcms.system.event.local;

import com.dotcms.system.event.local.model.Subscriber;

/**
 * Just for testing
 */
public class TestAnnotatedForDelete2Subscriber {

    private int called = 0;

    @Subscriber
    public void test(TestEventType1 testEventType1) {

        System.out.println("testEventType1: " + testEventType1.getMsg());
        called++;
    }

    @Subscriber
    public void test(TestEventType2 testEventType2) {

        System.out.println("testEventType2: " + testEventType2.getMsg());
        called++;
    }

    public int getCalled() {
        return called;
    }
}
