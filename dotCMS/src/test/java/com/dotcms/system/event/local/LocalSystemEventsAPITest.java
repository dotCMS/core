package com.dotcms.system.event.local;

import com.dotcms.UnitTestBase;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.DateUtil;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

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

        localSystemEventsAPI.unsubscribe(testAnnotatedSubscriber);

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

        localSystemEventsAPI.unsubscribe(testDelegateSubscriber);
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

        localSystemEventsAPI.unsubscribe(testDelegateSubscriber);
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

        localSystemEventsAPI.unsubscribe(testDelegateSubscriber);
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

        localSystemEventsAPI.unsubscribe(testAnnotatedSubscriber);
    } // deleteAnnotatedSubscriberTest.

    @Ignore("Failures are inconsistent")
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
        localSystemEventsAPI.unsubscribe(TestEventType1.class);
        localSystemEventsAPI.unsubscribe(TestEventType2.class);

        localSystemEventsAPI.notify(new TestEventType1("works??"));
        localSystemEventsAPI.notify(new TestEventType2("really works??"));

        // new message not received, so the counter still on 4
        Assert.assertEquals(4, testAnnotatedSubscriber.getCalled());

        localSystemEventsAPI.unsubscribe(testAnnotatedSubscriber);

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

    /**
     * Given scenario: We're testing that even when no subscribers are added the asyncNotify callback gets called
     * Expected results: We expect that the callback gets called.
     */
    @Test
    public void Test_Non_Blocking_Notification_On_Event_Consumed_With_No_Subscribers() {
        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        final AtomicBoolean isOnCompleteCalled = new AtomicBoolean(false);
        final String message = "Async notify Event.";
        localSystemEventsAPI.asyncNotify(new TestEventType1(message), event -> {
            Assert.assertTrue(event instanceof TestEventType1);
            final TestEventType1 eventType1 = (TestEventType1)event;
            Assert.assertEquals(eventType1.getMsg(), message);
            isOnCompleteCalled.set(true);
        });
        DateUtil.sleep(2000);
        Assert.assertTrue(isOnCompleteCalled.get());
    }

    /**
     * Given scenario: We're testing that even when subscribers are added  the asyncNotify callback gets called just once.
     * Expected results: We expect that the callback gets called exactly once.
     */
    @Test
    public void Test_Non_Blocking_Notification_On_Event_Consumed_With_Subscribers() {
        final String message = "Async notify Event 2.";
        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        localSystemEventsAPI.subscribe(new TestDelegateSubscriber(){
            @Override
            public void notify(final TestEventType2 event) {
                Assert.assertEquals(event.getMsg(), message);
            }
        });
        final AtomicInteger callsCount = new AtomicInteger(0);

        localSystemEventsAPI.asyncNotify(new TestEventType2(message), event -> {
            Assert.assertTrue(event instanceof TestEventType2);
            final TestEventType2 eventType2 = (TestEventType2)event;
            Assert.assertEquals(eventType2.getMsg(), message);
            callsCount.incrementAndGet();
        });
        DateUtil.sleep(2000);
        Assert.assertEquals(callsCount.get(), 1);
    }

    /**
     * Given scenario: We're testing that an event that implements KeyFilterable and is broadcast only reaches certain audience.
     * We creating a few subscribers each one of the provides a key. The Event must reach only those which have the same id.
     * Expected results: Only the subscribers with a matching id will be able to consume the event;
     */
    @Test
    public void Test_Send_Event_For_Limited_Audience() {

        final Map<String, AtomicInteger> callsCounts = ImmutableMap
                .of("subscriber1", new AtomicInteger(0), "subscriber2", new AtomicInteger(0),
                        "subscriber3", new AtomicInteger(0));

        final KeyFilterableTestSubscriberTest subscriber1 = new KeyFilterableTestSubscriberTest("subscriber1"){
            @Override
            public void notify(final KeyFilterableEvent event) {
                Assert.assertEquals(event.getKey(),"subscriber1");
                callsCounts.get(event.getKey().toString()).incrementAndGet();
            }
        };
        final KeyFilterableTestSubscriberTest subscriber2 = new KeyFilterableTestSubscriberTest("subscriber2"){
            @Override
            public void notify(final KeyFilterableEvent event) {
                Assert.assertEquals(event.getKey(),"subscriber2");
                callsCounts.get(event.getKey().toString()).incrementAndGet();
            }
        };
        final KeyFilterableTestSubscriberTest subscriber3 = new KeyFilterableTestSubscriberTest("subscriber3"){
            @Override
            public void notify(final KeyFilterableEvent event) {
                Assert.assertEquals(event.getKey(),"subscriber3");
                callsCounts.get(event.getKey().toString()).incrementAndGet();
            }
        };

        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

        localSystemEventsAPI.subscribe(KeyFilterableEvent.class, subscriber1);
        localSystemEventsAPI.subscribe(KeyFilterableEvent.class, subscriber2);
        localSystemEventsAPI.subscribe(KeyFilterableEvent.class, subscriber3);

        localSystemEventsAPI.notify(new KeyFilterableEvent("subscriber1"));
        localSystemEventsAPI.notify(new KeyFilterableEvent("subscriber3"));

        Assert.assertEquals(callsCounts.get("subscriber1").get(),1);
        Assert.assertEquals(callsCounts.get("subscriber2").get(),0);
        Assert.assertEquals(callsCounts.get("subscriber3").get(),1);

        localSystemEventsAPI.asyncNotify(new KeyFilterableEvent("subscriber3"));

        DateUtil.sleep(2000);
        Assert.assertEquals(callsCounts.get("subscriber3").get(),2);
    }


    } // E:O:F:LocalSystemEventsAPITest.
