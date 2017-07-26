package com.dotcms.system.event.local;

import com.dotcms.UnitTestBase;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LocalSystemEventsAPITest
 * Test
 * @author jsanca
 */

public class LocalSystemEventsAPITest extends UnitTestBase {

    @Test
    public void annotationSubscribeTest() throws DotDataException {

        final TestAnnotatedSubscriber testAnnotatedSubscriber =
                new TestAnnotatedSubscriber();

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();

        localSystemEventsAPI.subscribe(testAnnotatedSubscriber);

        localSystemEventsAPI.notify(new TestEventType1("works??"));
        localSystemEventsAPI.asyncNotify(new TestEventType1("yeah works??"));

        Assert.assertTrue(testAnnotatedSubscriber.isCalled());


    } // annotationSubscribeTest.

    @Test
    public void delegateSubscribeTest() throws DotDataException {

        final TestDelegateSubscriber testDelegateSubscriber =
                new TestDelegateSubscriber();

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();

        localSystemEventsAPI.subscribe(TestEventType2.class, testDelegateSubscriber);

        localSystemEventsAPI.notify(new TestEventType2("it works??"));
        localSystemEventsAPI.asyncNotify(new TestEventType2("yeah it works??"));

        Assert.assertTrue(testDelegateSubscriber.isCalled());
    } // delegateSubscribeTest.

    @Test
    public void delegatePlusAnnotationSubscribeTest() throws DotDataException {

        final TestDelegatePlusAnnotationSubscriber testDelegateSubscriber =
                new TestDelegatePlusAnnotationSubscriber();

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();

        localSystemEventsAPI.subscribe(testDelegateSubscriber);
        localSystemEventsAPI.subscribe(TestEventType2.class, testDelegateSubscriber);

        localSystemEventsAPI.notify(new TestEventType1("works??"));
        localSystemEventsAPI.asyncNotify(new TestEventType1("yeah works??"));
        localSystemEventsAPI.notify(new TestEventType2("it works??"));
        localSystemEventsAPI.asyncNotify(new TestEventType2("yeah it works??"));

        Assert.assertTrue(testDelegateSubscriber.isCalled1());
        Assert.assertTrue(testDelegateSubscriber.isCalled2());
    } // delegatePlusAnnotationSubscribeTest.

    @Test
    public void twoAnnotationSubscribeTest() throws DotDataException {

        final TestDelegatePlusAnnotationSubscriber testDelegateSubscriber =
                new TestDelegatePlusAnnotationSubscriber();

        final TestAnnotatedSubscriber testAnnotatedSubscriber =
                new TestAnnotatedSubscriber();

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();

        localSystemEventsAPI.subscribe(testAnnotatedSubscriber);
        localSystemEventsAPI.subscribe(testDelegateSubscriber);
        localSystemEventsAPI.subscribe(TestEventType2.class, testDelegateSubscriber);

        localSystemEventsAPI.notify(new TestEventType1("works??"));
        localSystemEventsAPI.asyncNotify(new TestEventType1("yeah works??"));
        localSystemEventsAPI.notify(new TestEventType2("it works??"));
        localSystemEventsAPI.asyncNotify(new TestEventType2("yeah it works??"));

        Assert.assertTrue(testAnnotatedSubscriber.isCalled());
        Assert.assertTrue(testDelegateSubscriber.isCalled1());
        Assert.assertTrue(testDelegateSubscriber.isCalled2());
    } // twoAnnotationSubscribeTest.


    @Test
    public void deleteAnnotatedSubscriberTest() throws DotDataException {

        final TestAnnotatedForDeleteSubscriber testAnnotatedSubscriber =
                new TestAnnotatedForDeleteSubscriber();

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();
        localSystemEventsAPI.subscribe(testAnnotatedSubscriber);

        localSystemEventsAPI.notify(new TestEventType1("works??"));

        Assert.assertTrue(testAnnotatedSubscriber.isCalled());

        // check if the subscriber is there
        Assert.assertNotNull(localSystemEventsAPI.findSubscriber(TestEventType1.class,
                testAnnotatedSubscriber.getClass().getName() + "#test" ));

        // unsubscribe it
        Assert.assertTrue(localSystemEventsAPI.unsubscribe(TestEventType1.class,
                testAnnotatedSubscriber.getClass().getName() + "#test" ));

        // shouldn't be there.
        Assert.assertNull(localSystemEventsAPI.findSubscriber(TestEventType1.class,
                testAnnotatedSubscriber.getClass().getName() + "#test" ));

        // already removed, so should be false
        Assert.assertFalse(localSystemEventsAPI.unsubscribe(TestEventType1.class,
                testAnnotatedSubscriber.getClass().getName() + "#test" ));
    } // deleteAnnotatedSubscriberTest.

    @Test
    public void deleteAnnotated2SubscriberTest() throws DotDataException {

        final TestAnnotatedForDelete2Subscriber testAnnotatedSubscriber =
                new TestAnnotatedForDelete2Subscriber();

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();
        localSystemEventsAPI.subscribe(testAnnotatedSubscriber);

        localSystemEventsAPI.notify(new TestEventType1("works??"));
        localSystemEventsAPI.notify(new TestEventType2("really works??"));

        Assert.assertTrue(2 == testAnnotatedSubscriber.getCalled());

        localSystemEventsAPI.notify(new TestEventType1("works??"));
        localSystemEventsAPI.notify(new TestEventType2("really works??"));

        Assert.assertTrue(4 == testAnnotatedSubscriber.getCalled());

        Assert.assertTrue(localSystemEventsAPI.unsubscribe(testAnnotatedSubscriber));

        localSystemEventsAPI.notify(new TestEventType1("works??"));
        localSystemEventsAPI.notify(new TestEventType2("really works??"));

        // new message not received, so the counter still on 4
        Assert.assertTrue(4 == testAnnotatedSubscriber.getCalled());

    } // deleteAnnotatedSubscriberTest.

    @Test
    public void invalidAnnotatedSubscriberTest() throws DotDataException {

        final TestInvalidAnnotatedSubscriber testAnnotatedSubscriber =
                new TestInvalidAnnotatedSubscriber();

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();
        try {

            localSystemEventsAPI.subscribe(testAnnotatedSubscriber);
            Assert.fail("TestInvalidAnnotatedSubscriber has annotated an invalid subscriber method with 2 arguments");
        } catch (Exception e) {
            System.out.println("Worked");
        }
    } // invalidAnnotatedSubscriberTest.


    @Test
    public void orphanSubscriberTest() throws DotDataException {

        final LocalSystemEventsAPI localSystemEventsAPI =
                APILocator.getLocalSystemEventsAPI();
        final AtomicBoolean isCalled = new AtomicBoolean(false);

        localSystemEventsAPI.setOrphanEventSubscriber((orphanEvent) ->  {
            System.out.println("OrphanEvent: " + orphanEvent);
            isCalled.set(true);
        });

        localSystemEventsAPI.notify(new MyOrphanEvent());

        Assert.assertTrue(isCalled.get());
    } // orphanSubscriberTest.



} // E:O:F:LocalSystemEventsAPITest.
