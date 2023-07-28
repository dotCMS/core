package com.dotcms.api.client;

import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import com.starxg.keytar.Keytar;
import com.starxg.keytar.KeytarException;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HybridServiceManagerTest {

    public static final char[] FAKE_TOKEN = "7WK5T79u5mIzjIXXi2oI9Fglmgivv7RAJ7izyj9tUyQ".toCharArray();

    @SecuredPassword
    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        Assertions.assertTrue(serviceManager instanceof HybridServiceManagerImpl);
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    @Test
    void Test_Persist_Then_Recover() throws IOException, KeytarException {

        Keytar instance = Keytar.getInstance();
        final ServiceBean serviceBeanDefault = ServiceBean.builder().name("default")
                .active(false)
                .credentials(
                        CredentialsBean.builder().user("admin")
                                .token(FAKE_TOKEN).build())
                .build();

        serviceManager.persist(serviceBeanDefault);
        String pass = instance.getPassword("default","admin");
        String fakeToken = new String(FAKE_TOKEN);
        Assertions.assertEquals(fakeToken,pass);
        Optional<ServiceBean> optional = serviceManager.services().stream().filter(serviceBean -> "default".equals(serviceBean.name())).findFirst();
        Assertions.assertTrue(optional.isPresent());
        ServiceBean bean = optional.get();
        Assertions.assertEquals("default",bean.name());
        Assertions.assertNotNull(bean.credentials());
        Assertions.assertEquals("admin", bean.credentials().user());
        Assertions.assertEquals(fakeToken, new String(bean.credentials().token()));

        serviceManager.removeAll();

        optional = serviceManager.services().stream().filter(serviceBean -> "default".equals(serviceBean.name())).findFirst();
        Assertions.assertFalse(optional.isPresent(),"service instance should have been removed.");
        Assertions.assertNull(instance.getPassword("default","admin"));
        Assertions.assertFalse(serviceManager.selected().isPresent());

    }
}
