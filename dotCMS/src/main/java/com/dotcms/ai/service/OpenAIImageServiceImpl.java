package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.util.OpenAIRequest;
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

public class OpenAIImageServiceImpl implements OpenAIImageService {

    private static StopWordsUtil stopWordsUtil = StopWordsUtil.get();

    private final AppConfig config;
    private final User user;
    private final HostAPI hostApi;
    private final TempFileAPI tempFileApi;

    public OpenAIImageServiceImpl(final AppConfig config,
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
        if (!jsonObject.containsKey("prompt")){
            throw new DotRuntimeException("Image request missing `prompt` key:" + jsonObject);
        }

        final String prompt = jsonObject.getString("prompt");
        if (prompt.length() > 400) {
            final StringBuilder builder = new StringBuilder();
            for(final String token : prompt.split("\\s+")) {
                builder.append(token).append(" ");
                if (builder.length() + token.length() + 5 > 400){
                    break;
                }

            }
            jsonObject.put("prompt", builder.toString());
        }

        jsonObject.putIfAbsent("model", config.getImageModel());
        jsonObject.putIfAbsent("size", config.getImageSize());
        jsonObject.putIfAbsent("n", 1);

        String responseString = "";
        try {
            responseString = doRequest(config.getApiImageUrl(), config.getApiKey(), jsonObject);

            JSONObject returnObject = new JSONObject(responseString);
            if (returnObject.containsKey("error")) {
                throw new DotRuntimeException("Error generating image: " + returnObject.get("error"));
            } else {
                returnObject = returnObject.getJSONArray("data").getJSONObject(0);
                returnObject.put("originalPrompt", jsonObject.getString("prompt"));
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
        jsonRequest.put("model", config.getImageModel());
        jsonRequest.put("prompt", dto.getPrompt());
        jsonRequest.put("size", dto.getSize());
        jsonRequest.put("n", dto.getNumberOfImages());
        return sendRequest(jsonRequest);
    }

    @Override
    public JSONObject sendTextPrompt(final String textPrompt) {
        return sendRequest(getDtoBuilder().prompt(textPrompt).build());
    }

    private JSONObject createTempFile(final JSONObject imageResponse) {
        final String url = imageResponse.optString("url");
        if (UtilMethods.isEmpty(() -> url)) {
            Logger.warn(this.getClass(), "imageResponse does not include URL:" + imageResponse);
            throw new DotRuntimeException("Image Response does not include URL:" + imageResponse);
        }

        try {
            final String fileName = generateFileName(imageResponse.getString("originalPrompt"));
            imageResponse.put("tempFileName", fileName);

            final DotTempFile file = tempFileApi.createTempFileFromUrl(fileName, getRequest(), new URL(url), 20);
            imageResponse.put("response", file.id);
            imageResponse.put("tempFile", file.file.getAbsolutePath());

            return imageResponse;
        } catch (Exception e) {
            imageResponse.put("response", e.getMessage());
            imageResponse.put("error", e.getMessage());
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
    HttpServletRequest getRequest() {
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
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());

        return requestProxy;
    }

    private String generateFileName(final String originalPrompt) {
        final SimpleDateFormat dateToString = new SimpleDateFormat("yyyyMMdd_hhmmss");
        try {
            String newFileName = originalPrompt.toLowerCase();
            newFileName = newFileName.replaceAll("[^a-z0-9 -]", "");
            newFileName = stopWordsUtil.removeStopWords(newFileName);

            newFileName = String.join("-", newFileName.split("\\s+"));
            newFileName = newFileName.substring(0, Math.min(100,newFileName.length()));
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
    String doRequest(final String urlIn, final String openAiAPIKey, final JSONObject json) {
        return OpenAIRequest.doRequest(urlIn, "POST", openAiAPIKey, json);
    }

    @VisibleForTesting
    User getUser() {
        return APILocator.systemUser();
    }

    @VisibleForTesting
    AIImageRequestDTO.Builder getDtoBuilder() {
        return new AIImageRequestDTO.Builder();
    }

    public static void setStopWordsUtil(final StopWordsUtil stopWordsUtil) {
        OpenAIImageServiceImpl.stopWordsUtil = stopWordsUtil;
    }

}