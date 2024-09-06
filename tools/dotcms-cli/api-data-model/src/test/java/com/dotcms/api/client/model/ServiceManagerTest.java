package com.dotcms.api.client.model;

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

@QuarkusTest
class ServiceManagerTest {

    public static final String FAKE_TOKEN_1 = "7WK5T79u5mIzjIXXi2oI9Fglmgivv7RAJ7izyj9tUyQ";
    public static final String FAKE_TOKEN_2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    public static final String FAKE_TOKEN_3 = "OpOSSw7e485LOP5PrzScxHb7SR6sAOMRckfFwi4rp7o";
    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").url(new URL("http://localhost:8080")).active(true).build());
    }

    @Test
    void Test_Persist_Then_Recover() throws IOException {

        final ServiceBean serviceBeanDefault = ServiceBean.builder().name("default")
                .active(false)
                .url(new URL("http://localhost:8080"))
                .credentials(
                        CredentialsBean.builder().user("admin")
                                .tokenSupplier(FAKE_TOKEN_1::toCharArray).build())
                .build();
        serviceManager.persist(serviceBeanDefault);

        final ServiceBean serviceBeanDemo1 = ServiceBean.builder().name("demo1")
                .active(false)
                .url(new URL("https://demo.dotcms.com"))
                .credentials(
                        CredentialsBean.builder().user("admin")
                                .tokenSupplier(FAKE_TOKEN_2::toCharArray).build())
                .build();
        serviceManager.persist(serviceBeanDemo1);

        final ServiceBean serviceBeanDemo2 = ServiceBean.builder().name("demo2")
                .active(false)
                .url(new URL("https://demo.dotcms.com"))
                .credentials(
                        CredentialsBean.builder().user("admin")
                                .tokenSupplier(FAKE_TOKEN_3::toCharArray).build())
                .build();
        serviceManager.persist(serviceBeanDemo2);

        final List<ServiceBean> serviceBeans = serviceManager.services();

        final ServiceBean beanDefault = serviceBeans.get(0);
        final ServiceBean beanDemo1 = serviceBeans.get(1);
        final ServiceBean beanDemo2 = serviceBeans.get(2);

        Assertions.assertEquals(serviceBeanDefault.name(),beanDefault.name());
        Assertions.assertEquals(serviceBeanDemo1.name(),beanDemo1.name());
        Assertions.assertEquals(serviceBeanDemo2.name(),beanDemo2.name());

        Assertions.assertEquals(1, serviceBeans.stream().filter(ServiceBean::active).count());

        //Now test adding a dupe

        final ServiceBean serviceBeanDemoDupe = ServiceBean.builder().name("demo2")
                .active(false)
                .url(new URL("https://demo.dotcms.com"))
                .credentials(
                        CredentialsBean.builder().user("admin")
                                .tokenSupplier(FAKE_TOKEN_3::toCharArray).build())
                .build();
        serviceManager.persist(serviceBeanDemoDupe);
        final List<ServiceBean> serviceBeansAfterDupeWasAdded = serviceManager.services();
        Assertions.assertEquals(3, serviceBeansAfterDupeWasAdded.size());

        //Test there should be only one active bean
        Assertions.assertEquals(1, serviceBeansAfterDupeWasAdded.stream().filter(ServiceBean::active).count());

        final ServiceBean activeBean = ServiceBean.builder().name("demo2")
                .active(true)
                .url(new URL("https://demo.dotcms.com"))
                .credentials(
                        CredentialsBean.builder().user("admin")
                                .tokenSupplier(FAKE_TOKEN_3::toCharArray).build())
                .build();

        serviceManager.persist(activeBean);
        final List<ServiceBean> withNewActiveBean = serviceManager.services();
        Assertions.assertEquals(withNewActiveBean.stream().filter(ServiceBean::active).count(),1);
        final Optional<ServiceBean> newActiveBean = withNewActiveBean.stream()
                .filter(ServiceBean::active).findFirst();
        Assertions.assertTrue(newActiveBean.isPresent());
        Assertions.assertEquals(newActiveBean.get(),activeBean);
    }

}
