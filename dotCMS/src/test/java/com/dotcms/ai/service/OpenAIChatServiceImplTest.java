package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotmarketing.util.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenAIChatServiceImplTest {

    private static final String RESPONSE_JSON =
            "{\"data\":[{\"url\":\"http://localhost:8080\",\"value\":\"this is a response\"}]}";

    private AppConfig config;
    private OpenAIChatService service;

    @Before
    public void setUp() {
        config = mock(AppConfig.class);
        service = prepareService(RESPONSE_JSON);
    }

    @Test
    public void test_sendRawRequest() {
        final JSONObject jsonObject = prepareJsonObject("Hello World!");

        final JSONObject result = service.sendRawRequest(jsonObject);

        assertFalse(jsonObject.containsKey("prompt"));
        assertTrue(jsonObject.containsKey("model"));
        assertTrue(jsonObject.containsKey("temperature"));
        assertEquals(2, jsonObject.getJSONArray("messages").size());
        assertNotNull(result);
    }

    @Test
    public void test_sendTextPrompt() {
        final JSONObject jsonObject = prepareJsonObject("Hello World!");

        final JSONObject result = service.sendTextPrompt(jsonObject.toString());

        assertNotNull(result);
    }

    private OpenAIChatService prepareService(final String response) {
        return new OpenAIChatServiceImpl(config) {
            @Override
            String doRequest(final String urlIn, final String openAiAPIKey, final JSONObject json) {
                return response;
            }
        };
    }

    private JSONObject prepareJsonObject(final String prompt) {
        when(config.getModel()).thenReturn("some-model");
        when(config.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE)).thenReturn(123.321F);
        when(config.getRolePrompt()).thenReturn("some-role-prompt");

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("prompt", prompt);

        return jsonObject;
    }

}
