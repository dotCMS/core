package com.dotcms.contenttype.transform;

import com.dotmarketing.business.DotStateException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonHelper {
    /**
     * Takes a json string and looks for a property called implClass and
     * returns that class to you
     * @param json
     * @return
     * @throws ClassNotFoundException
     */
    private static Class resolveClass(String json) throws ClassNotFoundException{
        String className = json;
        className = className.substring(className.indexOf("\"implClass\""), className.length());
        className = className.substring(className.indexOf(":")+1, className.length());
        className = className.substring(className.indexOf("\"")+1, className.length());
        className = className.substring(0,className.indexOf("\""));
        return Class.forName(className);
    }
    
    public static Object fromJson(String json){
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            

            return mapper.readValue(json, JsonHelper.resolveClass(json));
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }
    
}
