package com.dotcms.contenttype.transform;

public class JsonHelper {
    /**
     * Takes a json string and looks for a property called implClass and
     * returns that class to you
     * @param json
     * @return
     * @throws ClassNotFoundException
     */
    public static Class resolveClass(String json) throws ClassNotFoundException{
        String className = json;
        className = className.substring(className.indexOf("\"implClass\""), className.length());
        className = className.substring(className.indexOf(":")+1, className.length());
        className = className.substring(className.indexOf("\"")+1, className.length());
        className = className.substring(0,className.indexOf("\""));
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
