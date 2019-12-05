package com.dotcms.security.secret;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServiceIntegrationAPIImplTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    @Test
    public void Test_Store_Json_Then_Recover_Decrypted_Then_Delete_Then_Verify_Its_Gone()
            throws DotDataException, DotSecurityException {
        final User admin = TestUserUtils.getAdminUser();
        ServiceIntegrationBean.Builder builder = new ServiceIntegrationBean.Builder();
        final String serviceKey = "anyServiceKey";
        final ServiceIntegrationBean bean = builder.withServiceKey(serviceKey)
                .withHiddenSecret("mySecret1", "Once I saw a UFO")
                .withHiddenSecret("mySecret2", "Football soccer sucks!")
                .withSecret("boolSecret1", true)
                .withSecret("boolSecret2", false)
                .build();
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        api.registerServiceIntegration(bean, admin);
        final Optional<ServiceIntegrationBean> optionalBean = api
                .getIntegrationForService(serviceKey, admin);

        assertTrue(optionalBean.isPresent());

        final ServiceIntegrationBean recoveredBean = optionalBean.get();
        assertEquals(serviceKey, recoveredBean.getServiceKey());
        assertTrue(recoveredBean.getSecrets().containsKey("mySecret1"));
        assertTrue(recoveredBean.getSecrets().containsKey("mySecret2"));
        assertTrue(recoveredBean.getSecrets().containsKey("boolSecret1"));
        assertTrue(recoveredBean.getSecrets().containsKey("boolSecret2"));

        final Secret secret1 = recoveredBean.getSecrets().get("mySecret1");
        assertEquals("Once I saw a UFO",secret1.getString());
        assertTrue(secret1.isHidden());

        final Secret secret2 = recoveredBean.getSecrets().get("mySecret2");
        assertEquals("Football soccer sucks!",secret2.getString());
        assertTrue(secret2.isHidden());

        final Secret boolSecret1 = recoveredBean.getSecrets().get("boolSecret1");
        assertTrue(boolSecret1.getBoolean());
        assertFalse(boolSecret1.isHidden());

        final Secret boolSecret2 = recoveredBean.getSecrets().get("boolSecret2");
        assertFalse(boolSecret2.getBoolean());
        assertFalse(boolSecret2.isHidden());
    }

    @Test(expected = DotSecurityException.class)
    public void Test_Non_Admin_User_Read_Attempt() throws DotDataException, DotSecurityException {
        final User nonAdmin = TestUserUtils.getChrisPublisherUser();
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        ServiceIntegrationBean.Builder builder = new ServiceIntegrationBean.Builder();
        api.registerServiceIntegration(builder.build(), nonAdmin);
    }


}
