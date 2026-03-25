package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProviderConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void test_deserialize_fullConfig() throws Exception {
        final String json =
                "{\"provider\":\"openai\",\"model\":\"gpt-4o\",\"apiKey\":\"sk-test\"," +
                "\"maxTokens\":1024,\"temperature\":0.7,\"maxRetries\":3,\"timeout\":30," +
                "\"size\":\"1024x1024\",\"endpoint\":\"https://my.endpoint.com\"," +
                "\"deploymentName\":\"my-deploy\",\"apiVersion\":\"2024-02-01\"," +
                "\"region\":\"us-east-1\",\"accessKeyId\":\"AKID\",\"secretAccessKey\":\"secret\"," +
                "\"projectId\":\"my-project\",\"location\":\"us-central1\"}";

        final ProviderConfig config = MAPPER.readValue(json, ProviderConfig.class);

        assertEquals("openai", config.getProvider());
        assertEquals("gpt-4o", config.getModel());
        assertEquals("sk-test", config.getApiKey());
        assertEquals(Integer.valueOf(1024), config.getMaxTokens());
        assertEquals(Double.valueOf(0.7), config.getTemperature());
        assertEquals(Integer.valueOf(3), config.getMaxRetries());
        assertEquals(Integer.valueOf(30), config.getTimeout());
        assertEquals("1024x1024", config.getSize());
        assertEquals("https://my.endpoint.com", config.getEndpoint());
        assertEquals("my-deploy", config.getDeploymentName());
        assertEquals("2024-02-01", config.getApiVersion());
        assertEquals("us-east-1", config.getRegion());
        assertEquals("AKID", config.getAccessKeyId());
        assertEquals("secret", config.getSecretAccessKey());
        assertEquals("my-project", config.getProjectId());
        assertEquals("us-central1", config.getLocation());
    }

    @Test
    public void test_deserialize_minimalConfig() throws Exception {
        final String json = "{\"provider\":\"openai\",\"model\":\"gpt-4o-mini\",\"apiKey\":\"sk-test\"}";

        final ProviderConfig config = MAPPER.readValue(json, ProviderConfig.class);

        assertEquals("openai", config.getProvider());
        assertEquals("gpt-4o-mini", config.getModel());
        assertNull(config.getMaxTokens());
        assertNull(config.getTemperature());
        assertNull(config.getMaxRetries());
        assertNull(config.getEndpoint());
    }

    @Test
    public void test_deserialize_unknownFieldsIgnored() throws Exception {
        final String json = "{\"provider\":\"openai\",\"model\":\"gpt-4o\",\"apiKey\":\"sk-test\"," +
                "\"futureField\":\"someValue\",\"anotherUnknown\":42}";

        final ProviderConfig config = MAPPER.readValue(json, ProviderConfig.class);

        assertNotNull(config);
        assertEquals("openai", config.getProvider());
    }

}
