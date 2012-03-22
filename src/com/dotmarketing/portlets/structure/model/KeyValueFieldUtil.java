package com.dotmarketing.portlets.structure.model;

import java.util.Map;
import java.util.LinkedHashMap;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * Utility class to handle Key Value field 
 * @author Roger 
 *
 */
public class KeyValueFieldUtil {
	
	private static final JsonFactory factory = new JsonFactory(); 
	private static final ObjectMapper mapper = new ObjectMapper(factory); 
	
	public static Map<String,Object> JSONValueToHashMap(final String json){
		LinkedHashMap<String,Object> keyValueMap  = new LinkedHashMap<String,Object>();
		if(UtilMethods.isSet(json)){
			TypeReference<LinkedHashMap<String,Object>> typeRef = new TypeReference<LinkedHashMap<String,Object>>() {}; 
			try {
				keyValueMap = mapper.readValue(json, typeRef);
			} catch (Exception e) {
				//TODO
			} 
		}
		return keyValueMap;
	}
	

}
