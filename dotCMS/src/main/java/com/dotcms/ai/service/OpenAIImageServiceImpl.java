package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.ai.util.StopwordsUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OpenAIImageServiceImpl implements OpenAIImageService {


    private final AppConfig config;
    private final User user;
    public OpenAIImageServiceImpl(AppConfig appConfig, User user) {
        config = appConfig;
        this.user=user;
    }

    @Override
    public JSONObject sendRawRequest(String prompt) {
        return sendRequest(new JSONObject(prompt));
    }

    @Override
    public JSONObject sendTextPrompt(String textPrompt) {
        AIImageRequestDTO dto = new AIImageRequestDTO.Builder().prompt(textPrompt).build();
        return sendRequest(dto);
    }

    @Override
    public JSONObject sendRequest(JSONObject jsonObject) {
        if(!jsonObject.containsKey("prompt")){
            throw new DotRuntimeException("Image request missing `prompt` key:" + jsonObject);
        }

        if(jsonObject.getString("prompt").length()>400){
            StringBuilder builder=new StringBuilder();
            for(String token : jsonObject.getString("prompt").split("\\s+")){
                builder.append(token).append(" ");
                if(builder.length() + token.length() + 5>400){
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
            responseString = OpenAIRequest.doRequest(config.getApiImageUrl(), "POST", config.getApiKey(),
                    jsonObject);

            JSONObject returnObject = new JSONObject(responseString);

            if(returnObject.containsKey("error")) {
                throw new DotRuntimeException("Error generating image: " + returnObject.get("error"));
            } else {
                returnObject = returnObject.getJSONArray("data").getJSONObject(0);
                returnObject.put("originalPrompt", jsonObject.getString("prompt"));
            }
            return createTempFile(returnObject);

        } catch (Exception e) {
            Logger.warn(this.getClass(), "image request failed:" + e.getMessage(),e);
            Logger.warn(this.getClass(), "     --- response   :" +responseString);

            throw new DotRuntimeException("Error generating image:" + e, e);
        }
    }

    /**
     * @param dto
     * @return
     */
    @Override
    public JSONObject sendRequest(AIImageRequestDTO dto) {

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", config.getImageModel());
        jsonRequest.put("prompt", dto.getPrompt());
        jsonRequest.put("size", dto.getSize());
        jsonRequest.put("n", dto.getNumberOfImages());
        return sendRequest(jsonRequest);


    }

    private JSONObject createTempFile(JSONObject imageResponse) {

        final String url = imageResponse.optString("url");




        if (UtilMethods.isEmpty(() -> url)) {
            Logger.warn(this.getClass(), "imageResponse does not include URL:" + imageResponse.toString());
            throw new DotRuntimeException("Image Response does not include URL:" + imageResponse);
        }

        try {

            final String fileName = generateFileName(imageResponse.getString("originalPrompt"));
            imageResponse.put("tempFileName", fileName);
            final TempFileAPI tempApi = APILocator.getTempFileAPI();
            DotTempFile file = tempApi.createTempFileFromUrl(fileName, getRequest(),
                    new URL(url), 20, Integer.MAX_VALUE);
            imageResponse.put("response", file.id);
            return imageResponse;

        } catch (Exception e) {

            imageResponse.put("response", e.getMessage());
            imageResponse.put("error", e.getMessage());
            Logger.error(this.getClass(), "Error building tempfile:" + e.getMessage(), e);
            throw new DotRuntimeException("Error building tempfile from:" + imageResponse);
        }
    }

    HttpServletRequest getRequest() {
        if (null != HttpServletRequestThreadLocal.INSTANCE.getRequest()) {
            return HttpServletRequestThreadLocal.INSTANCE.getRequest();
        }

        String hostName = Try.of(
                        () -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname())
                .getOrElse("localhost");
        HttpServletRequest requestProxy = new MockSessionRequest(
                new MockHeaderRequest(
                        new FakeHttpRequest(hostName, "/").request(), "referer",
                        "https://" + hostName + "/fakeRefer"
                ).request());
        requestProxy.setAttribute(WebKeys.CMS_USER, user);
        requestProxy.getSession().setAttribute(WebKeys.CMS_USER, user);
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());

        return requestProxy;


    }

    private String generateFileName(String originalPrompt){
        final SimpleDateFormat dateToString = new SimpleDateFormat("yyyyMMdd_hhmmss");
        try {
            String newFileName = originalPrompt.toLowerCase();
            newFileName = newFileName.replaceAll("[^a-z0-9 -]", "");
            newFileName = new StopwordsUtil().removeStopwords(newFileName);

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






}
