package com.dotcms.content.elasticsearch.business;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton used for store temporarily the Contentlet map used for index it. Since into the ESContentletIndexAPI indexContentletList method 
 * for the same contentlet we call twice the mappingAPI.toMap method, I use this temp memory for avoid to recreate the same map if I already have.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Jun 7, 2013 - 3:44:35 PM
 */
public class ESMappingMemory {
	
	public static ESMappingMemory INSTANCE = new ESMappingMemory();
	private static final String MOD_DATE_KEY = "moddate";
	
	private Map<String, Map<String, Object>> contentsToIndex = new ConcurrentHashMap<String, Map<String, Object>>();
	
	private ESMappingMemory(){}
	
	public void insertIntoMap(Map<String, Object> aMap, String identifier){
		if(!contentsToIndex.containsKey(identifier))
			contentsToIndex.put(identifier, aMap);
		else if(hasChanged((String)aMap.get(MOD_DATE_KEY), 
				(String)contentsToIndex.get(identifier).get(MOD_DATE_KEY))){
			contentsToIndex.remove(identifier);
			contentsToIndex.put(identifier,aMap);
		}
	}
	
	public Map<String, Object> getFromMap(String identifier) {
		return contentsToIndex.get(identifier);
	}
	
	public void removeFromMap(String identifier) {
		contentsToIndex.remove(identifier);
	}
	
	public void clean(){
		for(String key:contentsToIndex.keySet())
			contentsToIndex.remove(key);
	}
	
	private boolean hasChanged(String _newModDate, String _oldModDate){		
		return _newModDate.equals(_oldModDate);
	}

}
