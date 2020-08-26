package com.dotcms.ema.proxy;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This viewtool provides a method to make a simple post request and get the value.
 * 
 * @author Aquent, LLC. (cfalzone@aquent.com)
 */
public class ProxyTool {



    private static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";

    public static final int ERR_CODE_UNKNOWN_ERR = 888;
    public static final int ERR_CODE_UNIMPLEMENTED_METHOD = 777;

    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_DELETE = "DELETE";



    /**
     * Get a credentials object to send to authenticated requests.
     *
     * @param user - The username
     * @param pass - The password
     * @return A Credentials object that can be used in authenticated requests.
     */
    public Credentials createCreds(String user, String pass) {
        return new UsernamePasswordCredentials(user, pass);
    }

    /**
     * This is used to send a single string payload to a url. Can be used to send JSON or XML to a url.
     * Only Supports POST and PUT methods.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param method - The Method (POST/PUT)
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendStringData(String url, String data, String method, String contentType, Credentials creds) {
        Logger.debug(this, "sendStringData called with url=" + url + ", data=" + data + ", and method=" + method + ", contentType = "
                + contentType + ", creds=" + UtilMethods.isSet(creds));

        StringEntity entity;
        try {
            entity = new StringEntity(data, ContentType.create(contentType, "UTF-8"));
        } catch (Exception e) {
            Logger.error(this, "Exception creating RequestEntity for: " + url, e);
            return new ProxyResponse(ERR_CODE_UNKNOWN_ERR, null, new Header[0]);
        }

        CloseableHttpClient client;

        // Authentication if passed in
        if (creds != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY, creds);
            client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        } else {
            client = HttpClients.createDefault();
        }

        if (method.equalsIgnoreCase(METHOD_POST)) {
            HttpPost m = new HttpPost(url);
            m.setEntity(entity);
            try {
                CloseableHttpResponse r = client.execute(m);
                return new ProxyResponse(r.getStatusLine().getStatusCode(), EntityUtils.toByteArray(r.getEntity()), r.getAllHeaders());
            } catch (Exception e) {
                Logger.error(this, "Exception posting to url: " + url, e);
                return new ProxyResponse(ERR_CODE_UNKNOWN_ERR, null, new Header[0]);
            } finally {
                if (method != null) {
                    m.releaseConnection();
                }
            }
        } else if (method.equalsIgnoreCase(METHOD_PUT)) {
            HttpPut m = new HttpPut(url);
            m.setEntity(entity);
            try {
                CloseableHttpResponse r = client.execute(m);
                return new ProxyResponse(r.getStatusLine().getStatusCode(), EntityUtils.toByteArray(r.getEntity()), r.getAllHeaders());
            } catch (Exception e) {
                Logger.error(this, "Exception posting to url: " + url, e);
                return new ProxyResponse(ERR_CODE_UNKNOWN_ERR, null, null);
            } finally {
                if (method != null) {
                    m.releaseConnection();
                }
            }
        } else {
            Logger.error(this, "Unimplemented Method: " + method);
            return new ProxyResponse(ERR_CODE_UNIMPLEMENTED_METHOD, null, null);
        }

    }

    /**
     * This is used to send a single string payload to a url. Can be used to send JSON or XML to a url.
     * Only Supports POST and PUT methods.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param method - The Method (POST/PUT)
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendStringData(String url, String data, String method, String contentType) {
        return sendStringData(url, data, method, contentType, null);
    }

    /**
     * This is used to send a single string payload to a url. Can be used to send JSON or XML to a url.
     * Only supports PUT and POST Methods.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param method - The Method (POST/PUT)
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendStringData(String url, String data, String method, Credentials creds) {
        return sendStringData(url, data, method, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * This is used to send a single string payload to a url. Can be used to send JSON or XML to a url.
     * Only supports PUT and POST Methods.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param method - The Method (POST/PUT)
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendStringData(String url, String data, String method) {
        return sendStringData(url, data, method, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * This is used to post a single string payload to a url. Can be used to send JSON or XML to a url.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendStringData(String url, String data, Credentials creds) {
        return sendStringData(url, data, METHOD_POST, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * This is used to post a single string payload to a url. Can be used to send JSON or XML to a url.
     *
     * @param url - The URL
     * @param data - The String Data
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendStringData(String url, String data) {
        return sendStringData(url, data, METHOD_POST, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse send(String url, Map<String, String> params, String method, String contentType, Credentials creds) {
        Logger.debug(this, "send(Map) called with url=" + url + ", params=" + params + ", and method=" + method + ", contentType = "
                + contentType + ", creds=" + UtilMethods.isSet(creds));


        try {
            List<NameValuePair> data = new ArrayList<NameValuePair>();
            StringBuilder urlParamsSB = new StringBuilder();
            String appender = "?";
            if (url.contains("?")) {
                appender = "&";
            }
            for (Entry<String, String> e : params.entrySet()) {
                data.add(new BasicNameValuePair(e.getKey(), e.getValue()));
                urlParamsSB.append(appender + e.getKey() + "=" + e.getValue());
                appender = "&";
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));


            CloseableHttpClient client = HttpClients.createDefault();
            

            // Authentication if passed in
            if (creds != null) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY, creds);
                client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
            } 
        
            
            Logger.debug(this, method  + " " + url);
            
            if (method.equalsIgnoreCase(METHOD_POST)) {
                HttpPost m = new HttpPost(url);

                

                m.setEntity(entity);
                try(CloseableHttpResponse r = client.execute(m)){
                    StatusLine status =r.getStatusLine();
                    return new ProxyResponse(status, EntityUtils.toByteArray(r.getEntity()), r.getAllHeaders());
                }
            } else if (method.equalsIgnoreCase(METHOD_PUT)) {
                HttpPut m = new HttpPut(url);
                m.setEntity(entity);
                try(CloseableHttpResponse r = client.execute(m)){
                    return new ProxyResponse(r.getStatusLine(), EntityUtils.toByteArray(r.getEntity()), r.getAllHeaders());
                }
            } else if (method.equalsIgnoreCase(METHOD_HEAD)) {
                HttpHead m = new HttpHead(url + urlParamsSB.toString());
                try(CloseableHttpResponse r = client.execute(m)){
                    return new ProxyResponse(r.getStatusLine(), EntityUtils.toByteArray(r.getEntity()), r.getAllHeaders());
                }
            } else if (method.equalsIgnoreCase(METHOD_DELETE)) {
                HttpDelete m = new HttpDelete(url + urlParamsSB.toString());
                try(CloseableHttpResponse r = client.execute(m)){
                    return new ProxyResponse(r.getStatusLine(), EntityUtils.toByteArray(r.getEntity()), r.getAllHeaders());
                }
            } else if (method.equalsIgnoreCase(METHOD_GET)) {
                HttpGet m = new HttpGet(url + urlParamsSB.toString());
                try(CloseableHttpResponse r = client.execute(m)){
                    return new ProxyResponse(r.getStatusLine(), EntityUtils.toByteArray(r.getEntity()), r.getAllHeaders());
                }
            } else {
                Logger.error(this, "Unimplemented Method: " + method);
                return new ProxyResponse(ERR_CODE_UNIMPLEMENTED_METHOD, null, new Header[0]);
            }

        } catch (Exception e) {
            Logger.warn(this, "Exception posting to url: " + url, e);
            return new ProxyResponse(ERR_CODE_UNKNOWN_ERR, null, new Header[0]);
        }

    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse send(String url, Map<String, String> params, String method, String contentType) {
        return send(url, params, method, contentType, null);
    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from URLDecoder.decode
     */
    public ProxyResponse send(String url, String params, String method, String contentType, Credentials creds) {
        Logger.debug(this, "send(String) called with url=" + url + ", params=" + params + ", and method=" + method + ", contentType = "
                + contentType + ", creds=" + UtilMethods.isSet(creds));

        Map<String, String> queryPairs = new LinkedHashMap<String, String>();
        if (params.length() > 0) {
            String[] pairs = params.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try {
                    queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new DotRuntimeException(e);
                }
            }
        }
        return send(url, queryPairs, method, contentType, creds);
    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object @ from URLDecoder.decode
     */
    public ProxyResponse send(String url, String params, String method, String contentType) {
        return send(url, params, method, contentType, null);
    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse send(String url, Map<String, String> params, String method, Credentials creds) {
        return send(url, params, method, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @return A ProxyResponse Object
     */
    public ProxyResponse send(String url, Map<String, String> params, String method) {
        return send(url, params, method, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @return A ProxyResponse Object
     * @param creds - A credentials object for authenticated requests. @ from send
     */
    public ProxyResponse send(String url, String params, String method, Credentials creds) {
        return send(url, params, method, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param method - The Method (POST/GET/PUT/HEAD/DELETE)
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse send(String url, String params, String method) {
        return send(url, params, method, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse send(String url, Map<String, String> params, Credentials creds) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     */
    public ProxyResponse send(String url, Map<String, String> params) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse send(String url, String params, Credentials creds) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse send(String url, String params) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse send(String url, Credentials creds) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse send(String url) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendGet(String url, String params, String contentType, Credentials creds) {
        return send(url, params, METHOD_GET, contentType, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     * @param contentType - The content type for this request @ from send
     */
    public ProxyResponse sendGet(String url, String params, String contentType) {
        return send(url, params, METHOD_GET, contentType, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendGet(String url, Map<String, String> params, String contentType, Credentials creds) {
        return send(url, params, METHOD_GET, contentType, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendGet(String url, Map<String, String> params, String contentType) {
        return send(url, params, METHOD_GET, contentType, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendGet(String url, String params, Credentials creds) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendGet(String url, String params) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendGet(String url, Map<String, String> params, Credentials creds) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendGet(String url, Map<String, String> params) {
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendGet(String url, Credentials creds) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a get request to a url.
     *
     * @param url - The URL
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendGet(String url) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_GET, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Post a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse postStringData(String url, String data, String contentType, Credentials creds) {
        return sendStringData(url, data, METHOD_POST, contentType, creds);
    }

    /**
     * Post a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse postStringData(String url, String data, String contentType) {
        return sendStringData(url, data, METHOD_POST, contentType, null);
    }

    /**
     * Post a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse postStringData(String url, String data, Credentials creds) {
        return sendStringData(url, data, METHOD_POST, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Post a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @return A ProxyResponse Object
     */
    public ProxyResponse postStringData(String url, String data) {
        return sendStringData(url, data, METHOD_POST, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url, String params, String contentType, Credentials creds) {
        return send(url, params, METHOD_POST, contentType, creds);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url, String params, String contentType) {
        return send(url, params, METHOD_POST, contentType, null);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendPost(String url, Map<String, String> params, String contentType, Credentials creds) {
        return send(url, params, METHOD_POST, contentType, creds);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendPost(String url, Map<String, String> params, String contentType) {
        return send(url, params, METHOD_POST, contentType, null);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url, String params, Credentials creds) {
        return send(url, params, METHOD_POST, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url, String params) {
        return send(url, params, METHOD_POST, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url, Credentials creds) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_POST, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_POST, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url, Map<String, String> params, Credentials creds) {
        return send(url, params, METHOD_POST, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a post request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPost(String url, Map<String, String> params) {
        return send(url, params, METHOD_POST, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Put a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse putStringData(String url, String data, String contentType, Credentials creds) {
        return sendStringData(url, data, METHOD_PUT, contentType, creds);
    }

    /**
     * Put a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse putStringData(String url, String data, String contentType) {
        return sendStringData(url, data, METHOD_PUT, contentType, null);
    }

    /**
     * Put a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse putStringData(String url, String data, Credentials creds) {
        return sendStringData(url, data, METHOD_PUT, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Put a Single String payload to a url. Used to send json or xml data.
     *
     * @param url - The URL
     * @param data - The String Data
     * @return A ProxyResponse Object
     */
    public ProxyResponse putStringData(String url, String data) {
        return sendStringData(url, data, METHOD_PUT, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPut(String url, String params, String contentType, Credentials creds) {
        return send(url, params, METHOD_PUT, contentType, creds);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     * @param contentType - The content type for this request @ from send
     */
    public ProxyResponse sendPut(String url, String params, String contentType) {
        return send(url, params, METHOD_PUT, contentType, null);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendPut(String url, Map<String, String> params, String contentType, Credentials creds) {
        return send(url, params, METHOD_PUT, contentType, creds);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendPut(String url, Map<String, String> params, String contentType) {
        return send(url, params, METHOD_PUT, contentType, null);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPut(String url, String params, Credentials creds) {
        return send(url, params, METHOD_PUT, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPut(String url, String params) {
        return send(url, params, METHOD_PUT, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendPut(String url, Map<String, String> params, Credentials creds) {
        return send(url, params, METHOD_PUT, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendPut(String url, Map<String, String> params) {
        return send(url, params, METHOD_PUT, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPut(String url, Credentials creds) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_PUT, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a put request to a url.
     *
     * @param url - The URL
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendPut(String url) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_PUT, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendHead(String url, String params, String contentType, Credentials creds) {
        return send(url, params, METHOD_HEAD, contentType, creds);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     * @param contentType - The content type for this request @ from send
     */
    public ProxyResponse sendHead(String url, String params, String contentType) {
        return send(url, params, METHOD_HEAD, contentType, null);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendHead(String url, Map<String, String> params, String contentType, Credentials creds) {
        return send(url, params, METHOD_HEAD, contentType, creds);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendHead(String url, Map<String, String> params, String contentType) {
        return send(url, params, METHOD_HEAD, contentType, null);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendHead(String url, String params, Credentials creds) {
        return send(url, params, METHOD_HEAD, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendHead(String url, String params) {
        return send(url, params, METHOD_HEAD, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param creds - A credentials object for authenticated requests.
     * @param params - The Query String
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendHead(String url, Map<String, String> params, Credentials creds) {
        return send(url, params, METHOD_HEAD, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendHead(String url, Map<String, String> params) {
        return send(url, params, METHOD_HEAD, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendHead(String url, Credentials creds) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_HEAD, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a head request to a url.
     *
     * @param url - The URL
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendHead(String url) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_HEAD, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendDelete(String url, String params, String contentType, Credentials creds) {
        return send(url, params, METHOD_DELETE, contentType, creds);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     * @param contentType - The content type for this request @ from send
     */
    public ProxyResponse sendDelete(String url, String params, String contentType) {
        return send(url, params, METHOD_DELETE, contentType, null);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendDelete(String url, Map<String, String> params, String contentType, Credentials creds) {
        return send(url, params, METHOD_DELETE, contentType, creds);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param contentType - The content type for this request
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendDelete(String url, Map<String, String> params, String contentType) {
        return send(url, params, METHOD_DELETE, contentType, null);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendDelete(String url, String params, Credentials creds) {
        return send(url, params, METHOD_DELETE, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendDelete(String url, String params) {
        return send(url, params, METHOD_DELETE, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendDelete(String url, Map<String, String> params, Credentials creds) {
        return send(url, params, METHOD_DELETE, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param params - The Query String
     * @return A ProxyResponse Object
     */
    public ProxyResponse sendDelete(String url, Map<String, String> params) {
        return send(url, params, METHOD_DELETE, DEFAULT_CONTENT_TYPE, null);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @param creds - A credentials object for authenticated requests.
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendDelete(String url, Credentials creds) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_DELETE, DEFAULT_CONTENT_TYPE, creds);
    }

    /**
     * Sends a delete request to a url.
     *
     * @param url - The URL
     * @return A ProxyResponse Object @ from send
     */
    public ProxyResponse sendDelete(String url) {
        String params = "";
        if (url.contains("?")) {
            int idx = url.indexOf("?");
            params = url.substring(idx + 1);
            url = url.substring(0, idx);
        }
        return send(url, params, METHOD_DELETE, DEFAULT_CONTENT_TYPE, null);
    }
}
