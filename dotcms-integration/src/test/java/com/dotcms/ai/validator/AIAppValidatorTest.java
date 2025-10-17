package com.dotcms.ai.validator;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AiAppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the AIAppValidator class.
 * This class tests the validation of AI configurations and model usage.
 * It ensures that the AI models specified in the application configuration are supported
 * and not exhausted.
 *
 * The tests cover scenarios for valid configurations, invalid configurations, and configurations
 * with missing fields.
 *
 * @author vico
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class AIAppValidatorTest {

    private static WireMockServer wireMockServer;
    private static SystemMessageEventUtil systemMessageEventUtil;
    private Host host;
    private AiAppConfig appConfig;
    private AIAppValidator validator = AIAppValidator.get();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        wireMockServer = AiTest.prepareWireMock();
        final Host systemHost = APILocator.systemHost();
        AiTest.aiAppSecrets(systemHost);
        ConfigService.INSTANCE.config(systemHost);
        systemMessageEventUtil = mock(SystemMessageEventUtil.class);
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() {
        IPUtils.disabledIpPrivateSubnet(true);
        host = new SiteDataGen().nextPersisted();
        validator.setSystemMessageEventUtil(systemMessageEventUtil);
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    @Test
    /**
     * Scenario: Validating AI configuration with unsupported models
     * Given an AI configuration with unsupported models
     * When the configuration is validated
     * Then a warning message should be pushed to the user
     */
    public void test_validateAIConfig() throws Exception {
        final String invalidModel = "some-made-up-model-10";
        AiTest.aiAppSecrets(host, invalidModel, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);

        verify(systemMessageEventUtil, atLeastOnce()).pushMessage(any(SystemMessage.class), anyList());
    }

    /**
     * Scenario: Validating AI models usage with exhausted models
     * Given an AI model with exhausted models
     * When the models usage is validated
     * Then a warning message should be pushed to the user for each exhausted model
     */
    @Test
    public void test_validateModelsUsage() throws Exception {
        final String invalidModels = "some-made-up-model-20,some-decommissioned-model-21";
        AiTest.aiAppSecrets(host, invalidModels, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);

        final JSONObjectAIRequest request = JSONObjectAIRequest.builder().withUserId("jon.snow").build();
        validator.validateModelsUsage(appConfig.getModel(), request);

        verify(systemMessageEventUtil, atLeastOnce()).pushMessage(any(SystemMessage.class), anyList());
    }
}
