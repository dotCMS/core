package com.dotcms.api.system.event.local;

import com.dotcms.UnitTestBase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import org.junit.Assert;
import org.junit.Test;

/**
 * MultipleRolesVerifier
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

    // todo: more cases

} // E:O:F:MultipleRolesVerifierTest.
