package com.dotcms.rendering.js.proxy;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
/**
 * This class is used to expose the {@link com.dotmarketing.util.json.JSONObject} object to the javascript engine.
 * @author jsanca
 */

public class JsJSONObject implements Serializable, JsProxyObject<JSONObject> {

    private final JSONObject jsonObject;

    public JsJSONObject(final JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public JSONObject getWrappedObject() {
        return this.jsonObject;
    }

    @HostAccess.Export
    /**
     * Get the value object associated with a key.
     *
     * @param key   A key string.
     * @return      The object associated with the key.
     * @throws   JSONException if the key is not found.
     */
    public Object get(final String key) {

        return JsProxyFactory.createProxy(jsonObject.get(key));
    }

    @HostAccess.Export
    /**
     * Get the boolean value associated with a key.
     *
     * @param key   A key string.
     * @return      The truth.
     * @throws   JSONException
     *  if the value is not a Boolean or the String "true" or "false".
     */
    public boolean getBoolean(final String key)  {
        return this.jsonObject.getBoolean(key);
    }

    @HostAccess.Export
    /**
     * Get the double value associated with a key.
     * @param key   A key string.
     * @return      The numeric value.
     * @throws JSONException if the key is not found or
     *  if the value is not a Number object and cannot be converted to a number.
     */
    public double getDouble(final String key)  {
        return jsonObject.getDouble(key);
    }

    @HostAccess.Export
    /**
     * Get the int value associated with a key. 
     *
     * @param key   A key string.
     * @return      The integer value.
     * @throws   JSONException if the key is not found or if the value cannot
     *  be converted to an integer.
     */
    public int getInt(final String key)  {
        return this.jsonObject.getInt(key);
    }

    @HostAccess.Export
    /**
     * Get the JSONArray value associated with a key.
     *
     * @param key   A key string.
     * @return      A JSONArray which is the value.
     * @throws   JSONException if the key is not found or
     *  if the value is not a JSONArray.
     */
    public Object getJSONArray(final String key)  {
        return JsProxyFactory.createProxy(this.jsonObject.getJSONArray(key));
    }

    @HostAccess.Export
    /**
     * Get the JSONObject value associated with a key.
     *
     * @param key   A key string.
     * @return      A JSONObject which is the value.
     * @throws   JSONException if the key is not found or
     *  if the value is not a JSONObject.
     */
    public Object getJSONObject(final String key)  {
        return JsProxyFactory.createProxy(this.getJSONObjectInternal(key));
    }

    protected JSONObject getJSONObjectInternal(final String key)  {
        return this.jsonObject.getJSONObject(key);
    }

    @HostAccess.Export
    public Object getAsMap() {
      return JsProxyFactory.createProxy(this.getAsMapInternal());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Map getAsMapInternal() {
        return this.jsonObject.getAsMap();
    }

    @HostAccess.Export
    /**
     * Get the long value associated with a key. 
     *
     * @param key   A key string.
     * @return      The long value.
     * @throws   JSONException if the key is not found or if the value cannot
     *  be converted to a long.
     */
    public long getLong(final String key)  {
        return this.jsonObject.getLong(key);
    }

    @HostAccess.Export
    /**
     * Get the string associated with a key.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     * @throws   JSONException if the key is not found.
     */
    public String getString(final String key)  {
        return jsonObject.getString(key);
    }

    @HostAccess.Export
    /**
     * Determine if the JSONObject contains a specific key.
     * @param key   A key string.
     * @return      true if the key exists in the JSONObject.
     */
    public boolean has(final String key) {
        return jsonObject.has(key);
    }

    @HostAccess.Export
    /**
     * Determine if the value associated with the key is null or if there is
     *  no value.
     * @param key   A key string.
     * @return      true if there is no value associated with the key or if
     *  the value is the JSONObject.NULL object.
     */
    public boolean isNull(final String key) {
        return jsonObject.isNull(key);
    }

    @HostAccess.Export
    /**
     * Get an enumeration of the keys of the JSONObject.
     *
     * @return An iterator of the keys.
     */
    public Object keys() {
        return JsProxyFactory.createProxy(keysInternal());
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Iterator keysInternal() {
        return this.jsonObject.keys();
    }

    @HostAccess.Export
    /**
     * Get the number of keys stored in the JSONObject.
     *
     * @return The number of keys in the JSONObject.
     */
    public int length() {
        return this.jsonObject.length();
    }

    @HostAccess.Export
    /**
     * Produce a JSONArray containing the names of the elements of this
     * JSONObject.
     * @return A JSONArray containing the key strings, or null if the JSONObject
     * is empty.
     */
    public Object names() {
        return JsProxyFactory.createProxy(namesInternal());
    }

    protected JSONArray namesInternal() {
        return this.jsonObject.names();
    }

    @HostAccess.Export
    /**
     * Get an optional value associated with a key.
     * @param key   A key string.
     * @return      An object which is the value, or null if there is no value.
     */
    public Object opt(final String key) {
        return JsProxyFactory.createProxy(this.jsonObject.opt(key));
    }

    @HostAccess.Export
    /**
     * Get an optional boolean associated with a key.
     * It returns false if there is no such key, or if the value is not
     * Boolean.TRUE or the String "true".
     *
     * @param key   A key string.
     * @return      The truth.
     */
    public boolean optBoolean(final String key) {
        return this.jsonObject.optBoolean(key);
    }

    @HostAccess.Export
    /**
     * Get an optional boolean associated with a key.
     * It returns the defaultValue if there is no such key, or if it is not
     * a Boolean or the String "true" or "false" (case insensitive).
     *
     * @param key              A key string.
     * @param defaultValue     The default.
     * @return      The truth.
     */
    public boolean optBoolean(final String key, final boolean defaultValue) {
       return this.jsonObject.optBoolean(key, defaultValue);
    }

    @HostAccess.Export
    /**
     * Get an optional double associated with a key,
     * or NaN if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A string which is the key.
     * @return      An object which is the value.
     */
    public double optDouble(final String key) {
        return this.jsonObject.optDouble(key);
    }

    @HostAccess.Export
    /**
     * Get an optional double associated with a key, or the
     * defaultValue if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public double optDouble(final String key, final double defaultValue) {
        return this.jsonObject.optDouble(key, defaultValue);
    }

    @HostAccess.Export
    /**
     * Get an optional int value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public int optInt(final String key) {
        return this.jsonObject.optInt(key);
    }

    @HostAccess.Export
    /**
     * Get an optional int value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public int optInt(final String key, final int defaultValue) {
        return this.jsonObject.optInt(key, defaultValue);
    }

    @HostAccess.Export
    /**
     * Get an optional JSONArray associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * JSONArray.
     *
     * @param key   A key string.
     * @return      A JSONArray which is the value.
     */ // proxy this
    public JSONArray optJSONArray(final String key) {
        return this.jsonObject.optJSONArray(key);
    }

    @HostAccess.Export
    /**
     * Get an optional JSONObject associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * JSONObject.
     *
     * @param key   A key string.
     * @return      A JSONObject which is the value.
     */
    public Object optJSONObject(final String key) {
        return JsProxyFactory.createProxy(this.optJSONObjectInternal(key));
    }

    protected JSONObject optJSONObjectInternal(final String key) {
        return this.jsonObject.optJSONObject(key);
    }

    @HostAccess.Export
    /**
     * Get an optional long value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public long optLong(final String key) {
        return this.jsonObject.optLong(key);
    }

    @HostAccess.Export
    /**
     * Get an optional long value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public long optLong(final String key, final long defaultValue) {
        return this.jsonObject.optLong(key, defaultValue);
    }

    @HostAccess.Export
    /**
     * Get an optional string associated with a key.
     * It returns an empty string if there is no such key. If the value is not
     * a string and is not null, then it is coverted to a string.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     */
    public String optString(final String key) {
        return this.jsonObject.optString(key);
    }

    @HostAccess.Export
    /**
     * Get an optional string associated with a key.
     * It returns the defaultValue if there is no such key.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      A string which is the value.
     */
    public String optString(final String key, final String defaultValue) {
        return this.jsonObject.optString(key, defaultValue);
    }

    @HostAccess.Export
    /**
     * Get an enumeration of the keys of the JSONObject.
     * The keys will be sorted alphabetically.
     *
     * @return An iterator of the keys.
     */
    public Iterator sortedKeys() { //  proxy this
      return this.jsonObject.sortedKeys();
    }

    @HostAccess.Export
    /**
     * Produce a JSONArray containing the values of the members of this
     * JSONObject.
     * @param names A JSONArray containing a list of key strings. This
     * determines the sequence of the values in the result.
     * @return A JSONArray of values.
     * @throws JSONException If any of the values are non-finite numbers.
     */
    public JSONArray toJSONArray(final JSONArray names)  { //  proxy this
        return this.jsonObject.toJSONArray(names);
    }

    @HostAccess.Export
    /**
     * Make a JSON text of this JSONObject. For compactness, no whitespace is added. If this would not
     * result in a syntactically correct JSON text, then null will be returned instead.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable representation of the object, beginning
     *         with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     *         <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    public String toString() {
       return jsonObject.toString();
    }

    @HostAccess.Export
    /**
     * Make a prettyprinted JSON text of this JSONObject.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    public String toString(final int indentFactor)  {
        return jsonObject.toString(indentFactor);
    }
    @HostAccess.Export
    public int size() {
        return this.jsonObject.size();
    }

    @HostAccess.Export
    public boolean isEmpty() {
 
        return this.jsonObject.isEmpty();
    }

    @HostAccess.Export
    public boolean containsKey(final Object key) { // unwrap it

        return this.jsonObject.containsKey(key);
    }

    @HostAccess.Export
    public boolean containsValue(final Object value) {// unwrap it
        return this.jsonObject.containsValue(value);
    }

    @HostAccess.Export
    public Object get(final Object key) {
        return JsProxyFactory.createProxy(this.jsonObject.get(key));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @HostAccess.Export
    public Set keySet() { // proxy it
        return this.jsonObject.keySet();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @HostAccess.Export
    public Collection values() { //  proxy it
        return this.jsonObject.values();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @HostAccess.Export
    public Set entrySet() { // proxy it
        return this.jsonObject.entrySet();
    }

}
