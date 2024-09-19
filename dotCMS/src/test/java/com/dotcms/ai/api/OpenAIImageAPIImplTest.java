package com.dotcms.ai.api;

import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.util.StopWordsUtil;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenAIImageAPIImplTest {

    private static final String RESPONSE_JSON =
            "{\"data\":[{\"url\":\"http://localhost:8080\",\"value\":\"this is a response\"}]}";
    private static final String INVALID_RESPONSE_JSON = "{\"data\":[{\"value\":\"this is a response\"}]}";
    private static final String ERROR_RESPONSE_JSON = "{\"error\":\"this is an error\"}";

    private AppConfig config;
    private User user;
    private HostAPI hostApi;
    private TempFileAPI tempFileApi;
    private AIImageRequestDTO.Builder dtoBuilder;
    private ImageAPI service;

    @Before
    public void setUp() {
        config = mock(AppConfig.class);
        user = mock(User.class);
        hostApi = mock(HostAPI.class);
        tempFileApi = mock(TempFileAPI.class);
        dtoBuilder = mock(AIImageRequestDTO.Builder.class);
        service = prepareService(RESPONSE_JSON, user);
    }

    /**
     * @Test
     * Scenario: Send a request with a valid JSON object
     * Given a valid JSON object
     * When the sendRequest method is called
     * Then the JSON object should contain the keys "originalPrompt", "value", "url", "tempFileName", and "response"
     */
    @Test
    public void test_sendRequest() throws Exception {
        final JSONObject jsonObject = prepareJsonObject("Hello World!");

        final JSONObject result = service.sendRequest(jsonObject);

        assertImageResponse(result, result.containsKey("tempFileName"));
    }

    /**
     * @Test
     * Scenario: Send a request with a prompt over 400 characters long
     * Given a JSON object with a prompt over 400 characters long
     * When the sendRequest method is called
     * Then the result should contain the keys "originalPrompt", "value", "url", "tempFileName", and "response"
     */
    @Test
    public void test_sendRequest_promptOver400CharsLong() throws Exception {
        final JSONObject jsonObject = prepareJsonObject(
                RandomStringUtils.randomAlphanumeric(200)
                        + " "
                        + RandomStringUtils.randomAlphanumeric(200));

        final JSONObject result = service.sendRequest(jsonObject);

        assertImageResponse(result, result.containsKey("tempFileName"));
    }

    /**
     * @Test
     * Scenario: Send a request that results in an error when generating the file name
     * Given a JSON object
     * When the sendRequest method is called and an error occurs during file name generation
     * Then the result should contain the keys "originalPrompt", "value", "url", "tempFileName", and "response"
     */
    @Test(expected = DotRuntimeException.class)
    public void test_sendRequest_withError() throws Exception {
        service = prepareService(ERROR_RESPONSE_JSON, user);

        final JSONObject jsonObject = prepareJsonObject("Hello World!");

        service.sendRequest(jsonObject);
    }

    /**
     * @Test
     * Scenario: Send a request that results in an error
     * Given a JSON object
     * When the sendRequest method is called with an error response
     * Then a DotRuntimeException should be thrown
     */
    @Test
    public void test_sendRequest_withErrorWhenGeneratingFileName() throws Exception {
        final JSONObject jsonObject = prepareJsonObject("Hello World!");

        final StopWordsUtil stopWordsUtil = mock(StopWordsUtil.class);
        OpenAIImageAPIImpl.setStopWordsUtil(stopWordsUtil);
        when(stopWordsUtil.removeStopWords(anyString())).thenThrow(RuntimeException.class);

        final JSONObject result = service.sendRequest(jsonObject);

        assertImageResponse(result, result.getString("tempFileName").startsWith("temp_"));
    }

    /**
     * @Test
     * Scenario: Send a request that results in a temp file with no URL error
     * Given a JSON object
     * When the sendRequest method is called with an invalid response
     * Then a DotRuntimeException should be thrown
     */
    @Test(expected = DotRuntimeException.class)
    public void test_sendRequest_withTempFileNoUrlError() throws Exception {
        service = prepareService(INVALID_RESPONSE_JSON, user);

        final JSONObject jsonObject = prepareJsonObject("Hello World!");

        service.sendRequest(jsonObject);
    }

    /**
     * @Test
     * Scenario: Send a request that results in a temp file error
     * Given a JSON object
     * When the sendRequest method is called and an error occurs during temp file creation
     * Then a DotRuntimeException should be thrown
     */
    @Test(expected = DotRuntimeException.class)
    public void test_sendRequest_withTempFileError() throws Exception {
        final JSONObject jsonObject = prepareJsonObject("Hello World!", true);

        service.sendRequest(jsonObject);
    }

    /**
     * @Test
     * Scenario: Send a raw request with a valid JSON object
     * Given a valid JSON object
     * When the sendRawRequest method is called
     * Then the result should contain the keys "originalPrompt", "value", "url", "tempFileName", and "response"
     */
    @Test
    public void test_SendRawRequest() throws Exception {
        final JSONObject jsonObject = prepareJsonObject("Hello World!");

        final JSONObject result = service.sendRawRequest(jsonObject.toString());

        assertImageResponse(result, result.containsKey("tempFileName"));
    }

    /**
     * @Test
     * Scenario: Send a text prompt with a valid JSON object
     * Given a valid JSON object
     * When the sendTextPrompt method is called
     * Then the result should contain the keys "originalPrompt", "value", "url", "tempFileName", and "response"
     */
    @Test
    public void test_sendTextPrompt() throws Exception {
        final JSONObject jsonObject = prepareJsonObject("Hello World!");
        final AIImageRequestDTO dto = mock(AIImageRequestDTO.class);
        when(dto.getPrompt()).thenReturn(jsonObject.getString("prompt"));
        when(dto.getSize()).thenReturn("some-image-size");
        when(dto.getNumberOfImages()).thenReturn(1);
        when(dtoBuilder.prompt(anyString())).thenReturn(dtoBuilder);
        when(dtoBuilder.build()).thenReturn(dto);

        final JSONObject result = service.sendTextPrompt(jsonObject.toString());

        assertImageResponse(result, result.containsKey("tempFileName"));
    }

    private static void assertImageResponse(JSONObject result, boolean result1) {
        assertTrue(result.containsKey("originalPrompt"));
        assertTrue(result.containsKey("value"));
        assertTrue(result.containsKey("url"));
        assertTrue(result1);
        assertTrue(result.containsKey("response"));
        assertTrue(result.containsKey("tempFile"));
    }

    private ImageAPI prepareService(final String response,
                                    final User user) {
        return new OpenAIImageAPIImpl(config, user, hostApi, tempFileApi) {
            @Override
            public String doRequest(final String urlIn, final JSONObject json) {
                return response;
            }

            @Override
            public User getUser() {
                return user;
            }

            @Override
            public AIImageRequestDTO.Builder getDtoBuilder() {
                return dtoBuilder;
            }
        };
    }

    private JSONObject prepareJsonObject(final String prompt, final boolean tempFileError) throws Exception {
        when(config.getImageModel()).thenReturn(AIModel.builder().withType(AIModelType.IMAGE).withModelNames("some-image-model").build());
        when(config.getImageSize()).thenReturn("some-image-size");
        final File file = mock(File.class);
        when(file.getName()).thenReturn(UUIDGenerator.shorty());
        when(file.getPath()).thenReturn("/some/path/here");
        final DotTempFile tempFile = new DotTempFile("some-id", file);

        if (tempFileError) {
            when(tempFileApi.createTempFileFromUrl(anyString(), any(), any(), anyInt())).thenThrow(DotRuntimeException.class);
        } else {
            when(tempFileApi.createTempFileFromUrl(anyString(), any(), any(), anyInt())).thenReturn(tempFile);
        }

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("prompt", prompt);

        return jsonObject;
    }

    private JSONObject prepareJsonObject(final String prompt) throws Exception {
        return prepareJsonObject(prompt, false);
    }

}
