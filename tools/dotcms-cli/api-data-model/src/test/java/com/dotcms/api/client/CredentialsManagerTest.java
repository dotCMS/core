package com.dotcms.api.client;

import com.dotcms.model.config.CredentialsBean;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CredentialsManagerTest {

    @Inject
    CredentialsManager credentialsManager;

    @Test
    public void Test_Persist_Then_Recover() throws IOException {
        credentialsManager.persist("service-3",
                CredentialsBean.builder().token("Token").user("User").build());
        credentialsManager.persist("service-4",
                CredentialsBean.builder().token("Token").user("User").build());
    }

}
