package com.dotcms.rendering.velocity.viewtools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.google.common.annotations.VisibleForTesting;
import org.apache.velocity.tools.view.ImportSupport;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

public class JSONTool extends ImportSupport implements ViewTool {

  private final Supplier<CircuitBreakerUrlBuilder> circuitBreakerUrlSupplier;

  public JSONTool() {
    super();
    this.circuitBreakerUrlSupplier = CircuitBreakerUrl::builder;
  }

  @VisibleForTesting
  JSONTool(final Supplier<CircuitBreakerUrlBuilder> circuitBreakerUrlSupplier) {
    super();
    this.circuitBreakerUrlSupplier = circuitBreakerUrlSupplier;
  }
  public void init(Object obj) {

  }

  public Object fetch(String url, Map<String, String> headers) {
    return fetch(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000), headers);
  }

  /**
   * Takes a url and reads the result. By default, it will timeout in 15 sec
   * 
   * @param url
   * @return
   */
  public Object fetch(String url) {
    return fetch(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000), new HashMap<String, String>());
  }

  /**
   * Will retrieve data from the remote URL returning for you the JSON Object or JSON Array Example
   * use from Velocity would be
   * 
   * #set($myjson =
   * $json.test('http://oohembed.com/oohembed/?url=http%3A//www.amazon.com/Myths-Innovation-Scott-Berkun/dp/0596527055/')
   * ) $myjson.get('title')
   * 
   * A JSONObject is an unordered collection of name/value pairs. Its external form is a string
   * wrapped in curly braces with colons between the names and values, and commas between the values
   * and names. The internal form is an object having <code>get</code> and <code>opt</code> methods
   * for accessing the values by name, and <code>put</code> methods for adding or replacing values by
   * name. The values can be any of these types: <code>Boolean</code>, <code>JSONArray</code>,
   * <code>JSONObject</code>, <code>Number</code>, <code>String</code>, or the
   * <code>JSONObject.NULL</code> object. A JSONObject constructor can be used to convert an external
   * form JSON text into an internal form whose values can be retrieved with the <code>get</code> and
   * <code>opt</code> methods, or to convert values into a JSON text using the <code>put</code> and
   * <code>toString</code> methods. A <code>get</code> method returns a value if one can be found, and
   * throws an exception if one cannot be found. An <code>opt</code> method returns a default value
   * instead of throwing an exception, and so is useful for obtaining optional values.
   * <p>
   * The generic <code>get()</code> and <code>opt()</code> methods return an object, which you can
   * cast or query for type. There are also typed <code>get</code> and <code>opt</code> methods that
   * do type checking and type coercion for you.
   * <p>
   * The <code>put</code> methods adds values to an object. For example,
   * 
   * <pre>
   * myString = new JSONObject().put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
   * </pre>
   * 
   * produces the string <code>{"JSON": "Hello, World"}</code>.
   * <p>
   * The texts produced by the <code>toString</code> methods strictly conform to the JSON syntax
   * rules. The constructors are more forgiving in the texts they will accept:
   * <ul>
   * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just before the closing
   * brace.</li>
   * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single quote)</small>.</li>
   * <li>Strings do not need to be quoted at all if they do not begin with a quote or single quote,
   * and if they do not contain leading or trailing spaces, and if they do not contain any of these
   * characters: <code>{ } [ ] / \ : , = ; #</code> and if they do not look like numbers and if they
   * are not the reserved words <code>true</code>, <code>false</code>, or <code>null</code>.</li>
   * <li>Keys can be followed by <code>=</code> or <code>=></code> as well as by <code>:</code>.</li>
   * <li>Values can be followed by <code>;</code> <small>(semicolon)</small> as well as by
   * <code>,</code> <small>(comma)</small>.</li>
   * <li>Numbers may have the <code>0x-</code> <small>(hex)</small> prefix.</li>
   * </ul>
   * 
   * @param url
   * @return
   */
  public Object fetch(String url, int timeout) {
    return get(url, timeout, new HashMap<String, String>());
  }

  public Object fetch(String url, int timeout, Map<String, String> headers) {
    return get(url, timeout, headers);
  }

  public Object get(final String url, final int timeout) {
    return fetch(url, timeout, new HashMap());
  }

  public Object get(String url, int timeout, Map<String, String> headers) {
    try {
      final String x = this.circuitBreakerUrlSupplier
              .get()
              .setHeaders(headers)
              .setUrl(url)
              .setTimeout(timeout)
              .build()
              .doString();
      return generate(x);
    } catch (Exception e) {
      Logger.warn(this.getClass(), e.getMessage());
      Logger.debug(this.getClass(), e.getMessage(), e);
    }
    return null;
  }

  /**
   * Will put data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to put to
   * @param timeout The timeout in milliseconds
   * @param headers The headers to send in the HTTP request
   * @param rawData The raw data to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object put(final String url, final int timeout, final Map<String, String> headers,
          final String rawData) {
    try {
      final String response = this.circuitBreakerUrlSupplier
              .get()
              .setMethod(Method.PUT)
              .setHeaders(headers)
              .setUrl(url)
              .setRawData(rawData)
              .setTimeout(timeout)
              .build()
              .doString();
      return generate(response);
    } catch (Exception e) {
      Logger.warn(this.getClass(), e.getMessage());
      Logger.debug(this.getClass(), e.getMessage(), e);
    }
    return null;
  }

  /**
   * Will put data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to put to
   * @param headers The headers to send in the HTTP request
   * @param rawData The raw data to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object put(final String url, final Map<String, String> headers, final String rawData) {
    return put(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000), headers, rawData);
  }

  /**
   * Will put data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to put to
   * @param timeout The timeout in milliseconds
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object put(final String url, final int timeout, final Map<String, String> headers,
          final Map<String, Object> params) {
    try {
      final String response = this.circuitBreakerUrlSupplier
              .get()
              .setMethod(Method.PUT)
              .setHeaders(headers)
              .setUrl(url)
              .setParams(convertObjToStringParameters(params))
              .setTimeout(timeout)
              .build()
              .doString();
      return generate(response);
    } catch (Exception e) {
      Logger.warn(this.getClass(), e.getMessage());
      Logger.debug(this.getClass(), e.getMessage(), e);
    }
    return null;
  }

  /**
   * Will put data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to put to
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object put(final String url, final Map<String, String> headers,
                    final Map<String, Object> params) {
    return put(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000), headers, params);
  }

  /**
   * Will put data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to put to
   * @param timeout The timeout in milliseconds
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @param useParamsAsJsonPayload If true, the params will be sent as a JSON payload,
   *                               otherwise they will be sent as request parameters
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object put(final String url, final int timeout, final Map<String, String> headers,
                    final Map<String, Object> params, final boolean useParamsAsJsonPayload) {
    if (useParamsAsJsonPayload) {
      return put(url, timeout, headers, generate(params).toString());
    } else {
      return put(url, timeout, headers, params);
    }
  }

  /**
   * Will put data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to put to
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @param useParamsAsJsonPayload If true, the params will be sent as a JSON payload,
   *                               otherwise they will be sent as request parameters
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object put(final String url, final Map<String, String> headers,
                    final Map<String, Object> params, final boolean useParamsAsJsonPayload) {
    return put(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000),
            headers, params, useParamsAsJsonPayload);
  }

  /**
   * Will post data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to post to
   * @param timeout The timeout in milliseconds
   * @param headers The headers to send in the HTTP request
   * @param rawData The raw data to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object post(final String url, final int timeout, final Map<String, String> headers,
          final String rawData) {
    try {
      final String response = this.circuitBreakerUrlSupplier
              .get()
              .setMethod(Method.POST)
              .setHeaders(headers)
              .setUrl(url)
              .setRawData(rawData)
              .setTimeout(timeout)
              .build()
              .doString();
      return generate(response);
    } catch (Exception e) {
      Logger.warn(this.getClass(), e.getMessage());
      Logger.debug(this.getClass(), e.getMessage(), e);
    }
    return null;
  }

  /**
   * Will post data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to post to
   * @param headers The headers to send in the HTTP request
   * @param rawData The raw data to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object post(final String url, final Map<String, String> headers, final String rawData) {
    return post(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 5000), headers, rawData);
  }

  /**
   * Will post data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to post to
   * @param timeout The timeout in milliseconds
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object post(final String url, final int timeout, final Map<String, String> headers,
          final Map<String, Object> params) {
    try {
      final String response = this.circuitBreakerUrlSupplier.get()
              .setMethod(Method.POST)
              .setHeaders(headers)
              .setUrl(url)
              .setParams(convertObjToStringParameters(params))
              .setTimeout(timeout)
              .build()
              .doString();
      return generate(response);
    } catch (Exception e) {
      Logger.warn(this.getClass(), e.getMessage());
      Logger.debug(this.getClass(), e.getMessage(), e);
    }
    return null;
  }

  /**
   * Will post data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to post to
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object post(final String url, final Map<String, String> headers,
                     final Map<String, Object> params) {
    return post(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000), headers, params);
  }

  /**
   * Will post data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to post to
   * @param timeout The timeout in milliseconds
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @param useParamsAsJsonPayload If true, the params will be sent as a JSON payload,
   *                               otherwise they will be sent as request parameters
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object post(final String url, final int timeout, final Map<String, String> headers,
                     final Map<String, Object> params, final boolean useParamsAsJsonPayload) {
    if (useParamsAsJsonPayload) {
      return post(url, timeout, headers, generate(params).toString());
    } else {
      return post(url, timeout, headers, params);
    }
  }

  /**
   * Will post data to the remote URL returning the JSON Object or JSON Array response from the server
   * @param url The URL to post to
   * @param headers The headers to send in the HTTP request
   * @param params The parameters to send in the request
   * @param useParamsAsJsonPayload If true, the params will be sent as a JSON payload,
   *                               otherwise they will be sent as request parameters
   * @return The JSON Object or JSON Array returned from the remote URL
   */
  public Object post(final String url, final Map<String, String> headers,
                     final Map<String, Object> params, final boolean useParamsAsJsonPayload) {
    return post(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000),
            headers, params, useParamsAsJsonPayload);
  }

  /**
   * Converts the given map of objects to a map of strings to be used as parameters in a request
   * @param objParams The map of objects to convert
   * @return The map of strings to be used as parameters in a request
   */
  private Map<String, String> convertObjToStringParameters(Map<String, Object> objParams) {
    Map<String, String> params = new HashMap<>();
    for (Map.Entry<String, Object> entry : objParams.entrySet()) {
      String value;
      if (entry.getValue() == null) {
        value = "";
      } else if (entry.getValue() instanceof Map || entry.getValue() instanceof List) {
        value = generate(entry.getValue()).toString();
      } else if (entry.getValue() instanceof String) {
        value = (String) entry.getValue();
      } else {
        value = entry.getValue().toString();
      }
      params.put(entry.getKey(), value);
    }
    return params;
  }

  /**
   * Returns a JSONObject from a passed in Map
   * 
   * @param o
   * @return
   */
  public JSONObject generate(Map map) {
    return new JSONObject(map);
  }

  /**
   * Returns a JSONArray from a passed in array
   * 
   * @param o
   * @return
   */
  public JSONArray generate(List list) {
    return new JSONArray(list);
  }

  /**
   * Returns a JSONObject from a passed in Object
   * 
   * @param o
   * @return
   */
  public Object generate(Object o) {
    if (o instanceof List) {
      return generate((List) o);
    }
    return new JSONObject(o);
  }

  private final static Class LIST_MAP_CLASS = new ArrayList<Map<String, Object>>().getClass();
  private final static Class MAP_CLASS      = new HashMap<String, Object>().getClass();
  /**
   * Returns a JSONObject as constructed from the provided string.
   *
   * @param s The JSON string.
   * @return A JSONObject as parsed from the provided string, null in the event of an error.
   */
  public Object generate(final String s) {

    return Config.getBooleanProperty("jsontool.generate.jackson", true)?
            this.jacksonGenerate(s): this.jsonGenerate(s);
  }

  private Object jacksonGenerate(final String s) {

    final DotObjectMapperProvider mapper = DotObjectMapperProvider.getInstance();

    try {

      final String trimmedString = s.trim();
      Logger.debug(this.getClass(), ()->"Json RESPONSE: " + s);

      return trimmedString.startsWith("[") && trimmedString.endsWith("]")?
              mapper.getDefaultObjectMapper().readValue(trimmedString, LIST_MAP_CLASS):
              mapper.getDefaultObjectMapper().readValue(trimmedString, MAP_CLASS);
    } catch (Exception e) {
      Logger.error(this.getClass(), "Error on parsing the String: " + s + ", message: " + e.getMessage());
      Logger.warnAndDebug(this.getClass(), e);
      return null;
    }
  }

  private Object jsonGenerate(final String s) {
    Object result;

    try {

      final String trimmedString = s.trim();
      if (trimmedString.startsWith("[") && trimmedString.endsWith("]")) {
        result = new JSONArray(trimmedString);
      } else {
        result = new JSONObject(trimmedString);
      }
    } catch (Exception e) {
      Logger.warn(this.getClass(), e.getMessage());
      Logger.debug(this.getClass(), e.getMessage(), e);
      result = null;
    }

    return result;
  }
}
