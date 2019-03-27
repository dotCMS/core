package com.dotcms.publisher.endpoint.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

public class PublishingEndPointFactoryImplTest {

    private static PublishingEndPointFactoryImpl publishingEndPointFactory;

    @BeforeClass
    public static void setup() throws Exception {
       
    	//Setting web app environment
       IntegrationTestInitService.getInstance().init();
       
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
