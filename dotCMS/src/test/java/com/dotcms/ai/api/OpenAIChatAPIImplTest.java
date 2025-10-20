package com.dotcms.ai.api;

import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenAIChatAPIImplTest {

    private static final String RESPONSE_JSON =
            "{\"data\":[{\"url\":\"http://localhost:8080\",\"value\":\"this is a response\"}]}";

    private AppConfig config;
    private ChatAPI service;
    private User user;

    @Before
    public void setUp() {
        config = mock(AppConfig.class);
        user = mock(User.class);
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

    private ChatAPI prepareService(final String response) {
        return new OpenAIChatAPIImpl(config, user) {
            @Override
            String doRequest(final JSONObject json, final String userId) {
                return response;
            }
        };
    }

    private JSONObject prepareJsonObject(final String prompt) {
        when(config.getModel())
                .thenReturn(AIModel.builder().withType(AIModelType.TEXT).withModelNames("some-model").build());
        when(config.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE)).thenReturn(123.321F);
        when(config.getRolePrompt()).thenReturn("some-role-prompt");

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("prompt", prompt);

        return jsonObject;
    }

}
