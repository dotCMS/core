package com.dotcms.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class RemoteAPIClient implements java.util.Map<String, Object> {

	
	private String remoteURL = null;
	private Map<String, Object> underMap= new HashMap<String, Object>();
	
	
	@Override
	public void clear() {
		underMap.clear();
		
	}

	@Override
	public boolean containsKey(Object key) {

		return underMap.containsKey(key.toString());
	}

	@Override
	public boolean containsValue(Object value) {
		return underMap.containsKey(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return null;
	}

	@Override
	public Object get(Object key) {
		return underMap.get(key);

	}

	@Override
	public boolean isEmpty() {
		return underMap.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return underMap.keySet();
	}

	@Override
	public Object put(String key, Object value) {
		return underMap.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		 underMap.putAll(m);
		
	}

	@Override
	public Object remove(Object key) {
		return underMap.remove(key);
	}

	@Override
	public int size() {
		return underMap.size();
	}

	@Override
	public Collection<Object> values() {

		return null;
	}

}
