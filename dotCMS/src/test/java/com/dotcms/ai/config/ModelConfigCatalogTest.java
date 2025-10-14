package com.dotcms.ai.config;

import com.dotcms.ai.config.parser.AiModelConfigParser;
import com.dotcms.ai.config.parser.AiVendorCatalogData;
import com.dotcms.util.ClasspathResourceLoader;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
        Assert.assertEquals("The default open ai model should be: gpt-4.1", "gpt-4.1", modelConfig.getName());
    }

}

