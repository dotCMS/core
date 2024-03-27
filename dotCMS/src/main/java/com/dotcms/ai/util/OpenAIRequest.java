package com.dotcms.ai.util;


import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class OpenAIRequest {

    static final ConcurrentHashMap<OpenAIModel,Long> lastRestCall = new ConcurrentHashMap<>();




    private OpenAIRequest() {
    }

    public static String doRequest(String url, String method, String openAiAPIKey, JSONObject json)  {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doRequest(url, method, openAiAPIKey, json, out);
        return out.toString();


    }


    private static HttpUriRequest resolveMethod(String method, String urlIn) {
        if ("post" .equalsIgnoreCase(method)) {
            return new HttpPost(urlIn);
        }
        if ("put" .equalsIgnoreCase(method)) {
            return new HttpPut(urlIn);
        }
        if ("delete" .equalsIgnoreCase(method)) {
            return new HttpDelete(urlIn);
        }
        if ("patch" .equalsIgnoreCase(method)) {
            return new HttpPatch(urlIn);
        }

        return  new HttpGet(urlIn);

    }
    public static void doPost(String urlIn,  String openAiAPIKey, JSONObject json, OutputStream out) {
       doRequest(urlIn,"post",openAiAPIKey,json,out);
    }

    public static void doGet(String urlIn,  String openAiAPIKey, JSONObject json, OutputStream out) {
        doRequest(urlIn,"get",openAiAPIKey,json,out);
    }


    /**
     * this allows for a streaming response.  It also attempts to rate limit requests based on OpenAI limits
     * @param urlIn
     * @param method
     * @param openAiAPIKey
     * @param json
     * @param out
     */
    public static void doRequest(String urlIn, String method, String openAiAPIKey, JSONObject json, OutputStream out) {
        if(ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.info(OpenAIRequest.class, "posting:" + json);
        }
        final OpenAIModel model = OpenAIModel.resolveModel(json.optString("model"));


        long sleep = lastRestCall.computeIfAbsent(model, m -> 0L) + model.minIntervalBetweenCalls() - System.currentTimeMillis();
        if (sleep > 0) {
            Logger.info(OpenAIRequest.class, "Rate limit:" + model.apiPerMinute + "/minute, or 1 every " + (60000 / model.apiPerMinute) + "ms. Sleeping:" + sleep);
            Try.run(() -> Thread.sleep(sleep));
        }
        lastRestCall.put(model, System.currentTimeMillis());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {


            StringEntity jsonEntity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
            HttpUriRequest httpRequest = resolveMethod(method, urlIn);
            httpRequest.setHeader("Content-Type", "application/json");
            httpRequest.setHeader("Authorization", "Bearer " + openAiAPIKey);
            if (null !=json && !json.getAsMap().isEmpty()) {
                Try.run(() -> ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(jsonEntity));
            }
            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                BufferedInputStream in = new BufferedInputStream(response.getEntity().getContent());
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }

            }

        } catch (Exception e) {
            if(ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)){
                Logger.warn(OpenAIRequest.class, "INVALID REQUEST: " + e.getMessage(),e);
                Logger.warn(OpenAIRequest.class, " -  " + method + " : " +json.toString());
            }else{
                Logger.warn(OpenAIRequest.class, "INVALID REQUEST: " + e.getMessage());
                Logger.warn(OpenAIRequest.class, " -  " + method + " : " +json.toString());
            }

            throw new DotRuntimeException(e);
        }

    }


}
