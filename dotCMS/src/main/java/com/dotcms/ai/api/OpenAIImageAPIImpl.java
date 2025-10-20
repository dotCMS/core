package com.dotcms.ai.api;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.client.AIProxyClient;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.util.OpenAiRequestUtil;
import com.dotcms.ai.util.StopWordsUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OpenAIImageAPIImpl implements ImageAPI {

    private static StopWordsUtil stopWordsUtil = StopWordsUtil.get();

    private final AppConfig config;
    private final User user;
    private final HostAPI hostApi;
    private final TempFileAPI tempFileApi;

    public OpenAIImageAPIImpl(final AppConfig config,
                              final User user,
                              final HostAPI hostApi,
                              final TempFileAPI tempFileApi) {
        this.config = config;
        this.user = user;
        this.hostApi = hostApi;
        this.tempFileApi = tempFileApi;
    }

    @Override
    public JSONObject sendRequest(final JSONObject jsonObject) {
        if (!jsonObject.containsKey(AiKeys.PROMPT)){
            throw new DotRuntimeException("Image request missing `prompt` key:" + jsonObject);
        }

        OpenAiRequestUtil.get().handleLargePrompt(jsonObject);
        jsonObject.putIfAbsent(AiKeys.MODEL, config.getImageModel().getCurrentModel());
        jsonObject.putIfAbsent(AiKeys.SIZE, config.getImageSize());

        String responseString = "";
        try {
            responseString = doRequest(config.getApiImageUrl(), jsonObject);

            JSONObject returnObject = new JSONObject(responseString);
            if (returnObject.containsKey(AiKeys.ERROR)) {
                throw new DotRuntimeException("Error generating image: " + returnObject.get(AiKeys.ERROR));
            } else {
                returnObject = returnObject.getJSONArray("data").getJSONObject(0);
                returnObject.put(AiKeys.ORIGINAL_PROMPT, jsonObject.getString(AiKeys.PROMPT));
            }

            return createTempFile(returnObject);
        } catch (Exception e) {
            Logger.warn(this.getClass(), "image request failed:" + e.getMessage(),e);
            Logger.warn(this.getClass(), "     --- response   :" + responseString);

            throw new DotRuntimeException("Error generating image:" + e, e);
        }
    }

    @Override
    public JSONObject sendRawRequest(final String prompt) {
        return sendRequest(new JSONObject(prompt));
    }

    @Override
    public JSONObject sendRequest(final AIImageRequestDTO dto) {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put(AiKeys.MODEL, config.getImageModel().getCurrentModel());
        jsonRequest.put(AiKeys.PROMPT, dto.getPrompt());
        jsonRequest.put(AiKeys.SIZE, dto.getSize());
        return sendRequest(jsonRequest);
    }

    @Override
    public JSONObject sendTextPrompt(final String textPrompt) {
        return sendRequest(getDtoBuilder().prompt(textPrompt).build());
    }

    private JSONObject createTempFile(final JSONObject imageResponse) {
        final String url = imageResponse.optString(AiKeys.URL);
        if (UtilMethods.isEmpty(() -> url)) {
            Logger.warn(this.getClass(), "imageResponse does not include URL:" + imageResponse);
            throw new DotRuntimeException("Image Response does not include URL:" + imageResponse);
        }

        try {
            final String fileName = generateFileName(imageResponse.getString(AiKeys.ORIGINAL_PROMPT));
            imageResponse.put("tempFileName", fileName);

            final DotTempFile file = tempFileApi.createTempFileFromUrl(fileName, getRequest(), new URL(url), 20);
            imageResponse.put(AiKeys.RESPONSE, file.id);
            imageResponse.put("tempFile", file.file.getAbsolutePath());

            return imageResponse;
        } catch (Exception e) {
            imageResponse.put(AiKeys.RESPONSE, e.getMessage());
            imageResponse.put(AiKeys.ERROR, e.getMessage());
            Logger.error(this.getClass(), "Error building tempfile:" + e.getMessage(), e);
            throw new DotRuntimeException("Error building tempfile from:" + imageResponse);
        }
    }

    /**
     * Gets a request object.
     *  - If there is a request in the thread local, it will return that request.
     *  - If not, it will create a fake request with a fake session and user.
     *
     * @return a {@link HttpServletRequest} instance
     */
    private HttpServletRequest getRequest() {
        if (null != HttpServletRequestThreadLocal.INSTANCE.getRequest()) {
            return HttpServletRequestThreadLocal.INSTANCE.getRequest();
        }

        final String hostName = Try.of(
                () -> hostApi
                        .findDefaultHost(getUser(), false)
                        .getHostname())
                .getOrElse("localhost");
        final HttpServletRequest requestProxy = new MockSessionRequest(
                new MockHeaderRequest(
                        new FakeHttpRequest(hostName, "/").request(),
                        "referer",
                        "https://" + hostName + "/fakeRefer")
                        .request());
        requestProxy.setAttribute(WebKeys.CMS_USER, user);
        requestProxy.getSession().setAttribute(WebKeys.CMS_USER, user);
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, UtilMethods.extractUserIdOrNull(user));

        return requestProxy;
    }

    private String generateFileName(final String originalPrompt) {
        final SimpleDateFormat dateToString = new SimpleDateFormat("yyyyMMdd_hhmmss");
        try {
            String newFileName = originalPrompt.toLowerCase();
            newFileName = newFileName.replaceAll("[^a-z0-9 -]", "");
            newFileName = stopWordsUtil.removeStopWords(newFileName);

            newFileName = String.join("-", newFileName.split("\\s+"));
            newFileName = newFileName.substring(0, Math.min(100, newFileName.length()));
            return newFileName.substring(0, newFileName.lastIndexOf("-"))
                    + "_"
                    + dateToString.format(new Date())
                    + ".png";
        }
        catch (Exception e){
            Logger.warn(this.getClass(), "unable to generate filename: " + e.getMessage(), e);
            return "temp_" + System.currentTimeMillis() + ".png";
        }
    }

    @VisibleForTesting
    String doRequest(final String urlIn, final JSONObject json) {
        return AIProxyClient.get()
                .callToAI(JSONObjectAIRequest.quickImage(config, json, UtilMethods.extractUserIdOrNull(user)))
                .getResponse();
    }

    @VisibleForTesting
    public User getUser() {
        return APILocator.systemUser();
    }

    @VisibleForTesting
    public AIImageRequestDTO.Builder getDtoBuilder() {
        return new AIImageRequestDTO.Builder();
    }

    public static void setStopWordsUtil(final StopWordsUtil stopWordsUtil) {
        OpenAIImageAPIImpl.stopWordsUtil = stopWordsUtil;
    }

}
