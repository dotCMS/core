package com.dotcms.publisher.endpoint.business;

import org.junit.Before;
import org.junit.Test;

import static org.jgroups.util.Util.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PublishingEndPointFactoryImplTest {

    private PublishingEndPointFactoryImpl publishingEndPointFactory;

    @Before
    public void setup() {
        publishingEndPointFactory = new PublishingEndPointFactoryImpl();
    }


    @Test
    public void shouldReturnTrueWhenIPAddressesAreTheSame() {
        assertTrue(publishingEndPointFactory.isMatchingEndpoint("127.0.0.1", "127.0.0.1"));
    }

    @Test
         public void shouldReturnTrueWhenIPAddressAndHostnameReferToSameMachine() {
        assertTrue(publishingEndPointFactory.isMatchingEndpoint("127.0.0.1", "localhost"));
    }

    @Test
    public void shouldReturnTrueWhenMachineNamesAreTheSame() {
        assertTrue(publishingEndPointFactory.isMatchingEndpoint("localhost", "localhost"));
    }

    @Test
    public void shouldReturnFalseWhenIPAddressesAreDifferent() {
        assertFalse(publishingEndPointFactory.isMatchingEndpoint("127.0.0.1", "127.0.0.2"));
    }

    @Test
    public void shouldReturnFalseWhenIPAddressAndHostnameReferToDifferentMachines() {
        assertFalse(publishingEndPointFactory.isMatchingEndpoint("127.0.0.1", "localhost2"));
    }

    @Test
    public void shouldReturnFalseWhenMachineNamesAreDifferent() {
        assertFalse(publishingEndPointFactory.isMatchingEndpoint("localhost2", "localhost"));
    }
}
