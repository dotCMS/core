
package com.dotmarketing.util;

import java.util.regex.Pattern;

import com.dotcms.repackage.com.google.common.base.CaseFormat;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import static com.dotcms.repackage.org.apache.commons.lang.StringUtils.*;
public class StringUtils {
    public static String formatPhoneNumber(String phoneNumber) {
        try {
            String s = phoneNumber.replaceAll("\\(|\\)|:|-|\\.", "");
            ;
            s = s.replaceAll("(\\d{3})(\\d{3})(\\d{4})(\\d{3})*", "($1) $2-$3x$4");

            if (s.endsWith("x"))
                s = s.substring(0, s.length() - 1);
            return s;
        } catch (Exception ex) {
            return "";
        }
    }




    public static boolean isJson(String jsonString) {
        if(jsonString.indexOf("{") <0 || jsonString.indexOf("}") <0){
            return false;
        }
        try {
            if (jsonString.startsWith("{"))
                new JSONObject(jsonString);
            else if (jsonString.startsWith("["))
                new JSONArray(jsonString);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

       // Pattern is threadsafe
    private static Pattern camelCaseLowerPattern = Pattern.compile("^[a-z]+([A-Z][a-z0-9]+)+");
    private static Pattern camelCaseUpperPattern = Pattern.compile("^[A-Z]+([A-Z][a-z0-9]+)+");
    
    
    public static String camelCaseLower(String variable) {
        // are we already camelCase?
        if(camelCaseLowerPattern.matcher(variable).find()){
            return variable;
        }
        String var = variable.toLowerCase().replaceAll("[^a-z\\d]", "-");
        while(var.startsWith("-")){
            var =var.substring(1, var.length());
        }
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL,var);
    }
    
    public static String camelCaseUpper(String variable) {
            // are we already camelCase?
        if(camelCaseUpperPattern.matcher(variable).find()){
            return variable;
        }
        String ret = camelCaseLower(variable);
        String firtChar = ret.substring(0,1);
        
        return firtChar.toUpperCase() + ret.substring(1,ret.length());
    }
    
    public static boolean isSet(String test){
        return UtilMethods.isSet(test);
    }
    
    public static String nullEmptyStr(String test){
        return isSet(test) ? test : null;
    }
    
    /**
     * Split the string by commans
     * Pre: string argument must be not null
     * @param string {@link String}
     * @return String array
     */
    final static char COMMA = ',';
    public static String [] splitByCommas (final String string) {

        return split(string, COMMA);
    } // splitByComma.
    
    
}



