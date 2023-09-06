package com.dotmarketing.microprofile.config;

import static org.junit.jupiter.api.Assertions.fail;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;

class EnvPropertyTest {

    @BeforeAll
    public static void init() throws Exception {
        Logger.info(EnvPropertyTest.class,"startup");
        IntegrationTestInitService.getInstance().init();
    }

    @org.junit.jupiter.api.Test
    void test() {
        Config config = ConfigProvider.getConfig();
        config.getPropertyNames().forEach(name -> {
            System.out.println(name + " = " + config.getOptionalValue(name, String.class).orElse("[NullOrEmpty]"));
        });

        String envVal = config.getValue("DOT_MAIL_SMTP_USER", String.class);
        System.out.println("envVal = " + envVal);
        String envVal2 = config.getValue("dot-mail-smtp-user", String.class);

        System.out.println("envVal = " + envVal);
        String envVal3 = config.getValue("dot.mail.smtp.user", String.class);

        System.out.println("envVal3 = " + envVal3);

        String envVal4 = config.getValue("mail.smtp.user", String.class);
        System.out.println("envVal4 = " + envVal4);

        fail("Not yet implemented");
    }

}
