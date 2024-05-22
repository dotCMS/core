package com.dotcms.api.client;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
class HybridServiceManagerTest {

    public static final char[] FAKE_TOKEN = "7WK5T79u5mIzjIXXi2oI9Fglmgivv7RAJ7izyj9tUyQ".toCharArray();

    @SecuredPassword
    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        Assertions.assertTrue(serviceManager instanceof HybridServiceManagerImpl);
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").url(new URL("http://localhost:8080")).active(true).build());
    }

    /**
     * This test is here to demonstrate that even if the keytar library is not working on the CI server, we still can rely on the yml file for storage.
     * This service manager is injected with a Mock that simulates the keytar library not working.
     * @throws IOException
     */
    @Test
    void Test_Service_Manager_Resilience() throws IOException {
        serviceManager.removeAll();
        Assert.assertTrue(serviceManager.services().isEmpty());
        final String key = "resilece-test";
        final ServiceBean serviceBean = ServiceBean.builder().name(key)
                .active(true)
                .url(new URL("http://localhost:8080"))
                .credentials(
                        CredentialsBean.builder().user("admin")
                                .tokenSupplier(() -> FAKE_TOKEN).token(FAKE_TOKEN).build())
                .build();

        serviceManager.persist(serviceBean);
        final List<ServiceBean> services = serviceManager.services();
        final Optional<ServiceBean> optional = serviceManager.services().stream().filter(bean -> key.equals(bean.name())).findFirst();
        Assertions.assertTrue(optional.isPresent());
        final ServiceBean bean = optional.get();
        Assertions.assertEquals(key,bean.name());
        Assertions.assertNotNull(bean.credentials());
        Assertions.assertEquals("admin", bean.credentials().user());
        final Optional<char[]> token = bean.credentials().loadToken();
        Assertions.assertNotNull(token);
        Assertions.assertTrue(token.isPresent());
        Assertions.assertEquals(new String(FAKE_TOKEN), new String(token.get()));
    }
}
