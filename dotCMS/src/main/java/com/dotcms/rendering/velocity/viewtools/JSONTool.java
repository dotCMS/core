package com.dotcms.rendering.velocity.viewtools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.tools.view.ImportSupport;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

public class JSONTool extends ImportSupport implements ViewTool {

	public void init(Object obj) {

	}
	
	
	public Object fetch(String url, Map<String, String> headers) {
		return fetch(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000), headers);
	}
	
	/**
	 * Takes a url and reads the result.  By default, it will timeout in 15 sec
	 * @param url
	 * @return
	 */
	public Object fetch(String url) {
		return fetch(url, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000), new HashMap<String, String>());
	}
	
	
	/**
	 * Will retrieve data from the remote URL returning for you the JSON Object
	 * or JSON Array
	 * Example use from Velocity would be
	 * 
	 * #set($myjson = $json.test('http://oohembed.com/oohembed/?url=http%3A//www.amazon.com/Myths-Innovation-Scott-Berkun/dp/0596527055/')
	 * ) $myjson.get('title')
	 * 
	 * A JSONObject is an unordered collection of name/value pairs. Its external
	 * form is a string wrapped in curly braces with colons between the names
	 * and values, and commas between the values and names. The internal form is
	 * an object having <code>get</code> and <code>opt</code> methods for
	 * accessing the values by name, and <code>put</code> methods for adding or
	 * replacing values by name. The values can be any of these types:
	 * <code>Boolean</code>, <code>JSONArray</code>, <code>JSONObject</code>,
	 * <code>Number</code>, <code>String</code>, or the
	 * <code>JSONObject.NULL</code> object. A JSONObject constructor can be used
	 * to convert an external form JSON text into an internal form whose values
	 * can be retrieved with the <code>get</code> and <code>opt</code> methods,
	 * or to convert values into a JSON text using the <code>put</code> and
	 * <code>toString</code> methods. A <code>get</code> method returns a value
	 * if one can be found, and throws an exception if one cannot be found. An
	 * <code>opt</code> method returns a default value instead of throwing an
	 * exception, and so is useful for obtaining optional values.
	 * <p>
	 * The generic <code>get()</code> and <code>opt()</code> methods return an
	 * object, which you can cast or query for type. There are also typed
	 * <code>get</code> and <code>opt</code> methods that do type checking and
	 * type coercion for you.
	 * <p>
	 * The <code>put</code> methods adds values to an object. For example,
	 * 
	 * <pre>
	 * myString = new JSONObject().put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
	 * </pre>
	 * 
	 * produces the string <code>{"JSON": "Hello, World"}</code>.
	 * <p>
	 * The texts produced by the <code>toString</code> methods strictly conform
	 * to the JSON syntax rules. The constructors are more forgiving in the
	 * texts they will accept:
	 * <ul>
	 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
	 * before the closing brace.</li>
	 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single quote)</small>.
	 * </li>
	 * <li>Strings do not need to be quoted at all if they do not begin with a
	 * quote or single quote, and if they do not contain leading or trailing
	 * spaces, and if they do not contain any of these characters:
	 * <code>{ } [ ] / \ : , = ; #</code> and if they do not look like numbers
	 * and if they are not the reserved words <code>true</code>,
	 * <code>false</code>, or <code>null</code>.</li>
	 * <li>Keys can be followed by <code>=</code> or <code>=></code> as well as
	 * by <code>:</code>.</li>
	 * <li>Values can be followed by <code>;</code> <small>(semicolon)</small>
	 * as well as by <code>,</code> <small>(comma)</small>.</li>
	 * <li>Numbers may have the <code>0x-</code> <small>(hex)</small> prefix.</li>
	 * </ul>
	 * 
	 * @param url
	 * @return
	 */
	public Object fetch(String url, int timeout) {
		return fetch(url, timeout, new HashMap<String, String>());
	}

	public Object fetch(String url, int timeout, Map<String, String> headers) {
		try {
			String x = CircuitBreakerUrl.builder().setHeaders(headers).setUrl(url).setTimeout(timeout).build().doString();
			return generate(x);
		} catch (Exception e) {
            Logger.warn(this.getClass(), e.getMessage());
            Logger.debug(this.getClass(), e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * Returns a JSONObject from a passed in Map
	 * @param o
	 * @return
	 */
	public JSONObject generate(Map map){
		return new JSONObject(map);
	}
	
	/**
	 * Returns a JSONArray from a passed in array
	 * @param o
	 * @return
	 */
	public JSONArray generate(List list){
		return new JSONArray(list);
	}
	/**
	 * Returns a JSONObject from a passed in Object
	 * @param o
	 * @return
	 */
	public Object generate(Object o){
		if(o instanceof List){
			return generate((List)o);
		}
		return new JSONObject(o);
	}
	
	
	
	

    /**
     * Returns a JSONObject as constructed from the provided string.
     *
     * @param s The JSON string.
     * @return A JSONObject as parsed from the provided string, null in the event of an error.
     */
    public Object generate(String s) {
        Object result;

        try {
    		if(s.startsWith("[") && s.endsWith("]")){
    			result = new JSONArray(s);
    		}
    		else{
    			result = new JSONObject(s);
    		}
        }
        catch (Exception e)
        {
            Logger.warn(this.getClass(), e.getMessage());
            Logger.debug(this.getClass(), e.getMessage(), e);
            result = null;
        }

        return result;
    }
}
