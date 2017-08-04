package com.dotcms.system.event.local;

import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.Subscriber;

/**
 * Just for testing
 */
public class TestDelegatePlusAnnotationSubscriber implements EventSubscriber<TestEventType2> {

    private boolean called1 = false;
    private boolean called2 = false;



    public boolean isCalled1() {
        return called1;
    }
    public boolean isCalled2() {
        return called2;
    }

    @Subscriber
    public void test(TestEventType1 testEventType1) {

        System.out.println(testEventType1.getMsg());
        called1 = true;
    }

    @Override
    public void notify(TestEventType2 event) {

        System.out.println(event.getMsg());
        called2 = true;
    }
}
