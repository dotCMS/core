package com.dotcms.contenttype.transform;

import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

public class JsonHelper {
    /**
     * Takes a json string and looks for a property called implClass and
     * returns that class to you
     * @param json
     * @return
     * @throws ClassNotFoundException
     * @throws JSONException 
     */
    public static Class resolveClass(String json) throws ClassNotFoundException, JSONException{
        
        
        JSONObject jo = new JSONObject(json);
        
        
        String className = jo.getString("implClass");

        className = className.replaceAll(".Immutable", ".") ;
        
        String immut = className.substring(0, className.lastIndexOf(".")) + ".Immutable" + className.substring(className.lastIndexOf(".") +1, className.length());
        try{
            return Class.forName(immut);
        }
        catch(ClassNotFoundException e){
            return Class.forName(className);
        }
    }
    

    
}
