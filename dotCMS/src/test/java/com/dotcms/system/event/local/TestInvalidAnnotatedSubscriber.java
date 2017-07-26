package com.dotcms.system.event.local;

import com.dotcms.system.event.local.model.Subscriber;

/**
 * Just for testing
 */
public class TestInvalidAnnotatedSubscriber {

    private boolean called = false;

    // invalid has to arguments
    @Subscriber
    public void test(TestEventType1 testEventType1, TestEventType2 testEventType2) {

        System.out.println(testEventType1.getMsg());
        called = true;
    }

    public boolean isCalled() {
        return called;
    }
}
