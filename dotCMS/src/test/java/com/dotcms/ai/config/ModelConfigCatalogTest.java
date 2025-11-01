package com.dotcms.ai.config;

import com.dotcms.ai.config.parser.AiModelConfigParser;
import com.dotcms.ai.config.parser.AiVendorCatalogData;
import com.dotcms.util.ClasspathResourceLoader;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link AiModelConfigCatalogImpl}
 * @author jsanca
 */
public class ModelConfigCatalogTest {

    /**
     * Test that reads the configuration
     */
    @Test
    public void test_model_config_parser() throws IOException {

        final String aiJsonConfigPath = "/dot-ai/dot-ai-vendors-models-default-template-config.json";
        final String aiJsonConfiguration = ClasspathResourceLoader.readTextOrThrow(aiJsonConfigPath);

        Assert.assertTrue("Does not have configVersion: " + aiJsonConfiguration, aiJsonConfiguration.contains("configVersion"));
        final AiModelConfigParser modelConfigParser  = new AiModelConfigParser();
        AiVendorCatalogData vendorCatalogData = null;
        try {
            vendorCatalogData = modelConfigParser.parse(aiJsonConfiguration,
                    Map.of("OPENAI_API_KEY", "OPENAI_K",
                            "OPENAI_ORG", "OPENAI_O",
                            "OPENAI_PROJECT", "OPENAI_Z",
                            "ANTHROPIC_API_KEY", "ANTHROPIC",
                            "AZURE_OPENAI_API_KEY", "AZURE_K",
                            "AWS_REGION", "OPENAI_K",
                            "AWS_ACCESS_KEY_ID", "OPENAI_O",
                            "AWS_SECRET_ACCESS_KEY", "OPENAI_Z",
                            "AWS_SESSION_TOKEN", "ANTHROPIC",
                            "GCP_PROJECT_ID", "AZURE_K"));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        final AiModelConfigCatalog modelConfigCatalog = AiModelConfigCatalogImpl.from(vendorCatalogData);
        final AiModelConfig modelConfig = modelConfigCatalog.getChatConfig(AiVendor.OPEN_AI);

        Assert.assertNotNull(modelConfig);
        Assert.assertEquals("The default open ai model should be: gpt-4.1", "openai.chat.gpt-4o-mini", modelConfig.getName());

        final List<String> chatModelNames = modelConfigCatalog.getChatModelNames(AiVendor.OPEN_AI.getVendorName());
        Assert.assertTrue("Open AI should be include gpt-4o-mini", chatModelNames.contains("gpt-4o-mini"));
        Assert.assertTrue("Open AI should be include gpt-4.1", chatModelNames.contains("gpt-4.1"));

        Assert.assertEquals("Open AI key should be OPENAI_K", "OPENAI_K",modelConfig.get("apiKey"));
        Assert.assertEquals("Open AI baseUrl should be https://api.openai.com/v1", "https://api.openai.com/v1",modelConfig.get("baseUrl"));

        final AiModelConfig modelConfigGptMini = modelConfigCatalog.getChatConfig(AiVendor.OPEN_AI.getVendorName(), AiModel.OPEN_AI_GPT_4O_MINI.getModel());

        Assert.assertEquals("Open AI timeoutMs should be 30000", "30000",modelConfig.get("timeoutMs"));
        Assert.assertEquals("Open AI rateLimitQps should be 5", "5",modelConfig.get("rateLimitQps"));
        Assert.assertEquals("Open AI temperature should be 0.3", "0.3",modelConfig.get("temperature"));


        final List<String> vendorNames = modelConfigCatalog.getVendorNames();
        Assert.assertTrue("Open AI should be include in : " + vendorNames, vendorNames.contains("openai"));
        Assert.assertTrue("Anthropic should be include in " + vendorNames, vendorNames.contains("anthropic"));



    }

}

