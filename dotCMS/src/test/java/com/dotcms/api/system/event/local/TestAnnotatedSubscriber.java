package com.dotcms.api.system.event.local;

/**
 * Just for testing
 */
public class TestAnnotatedSubscriber {

    private boolean called = false;

    @Subscriber
    public void test(TestEventType1 testEventType1) {

        System.out.println(testEventType1.getMsg());
        called = true;
    }

    public boolean isCalled() {
        return called;
    }
}
