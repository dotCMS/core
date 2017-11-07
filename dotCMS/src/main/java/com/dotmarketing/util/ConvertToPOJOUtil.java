package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.beanutils.PropertyUtils;
import com.google.common.base.CaseFormat;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class used to map query results to POJO objects
 * @author nollymar
 */
public class ConvertToPOJOUtil {

    /**
     * Creates new instances of T given a List<Map> of DB results
     * @param results
     * @param classToUse
     * @param <T>
     * @return
     * @throws Exception
     */
    public static<T> List<T> convertDotConnectMapToPOJO(List<Map<String,String>> results, Class classToUse)
            throws Exception {

        DateFormat df;
        List<T> ret;
        Map<String, String> properties;

        ret = new ArrayList<>();

        if(results == null || results.size()==0){
            return ret;
        }

        df = new SimpleDateFormat("yyyy-MM-dd");

        for (Map<String, String> map : results) {
            Constructor<?> ctor = classToUse.getConstructor();
            T object = (T) ctor.newInstance();

            properties = map.keySet().stream().collect(Collectors
                    .toMap(key -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key), key ->map.get(key)));

            for (String property: properties.keySet()){
                if (properties.get(property) != null){
                    if (isFieldPresent(classToUse, String.class, property)){
                        PropertyUtils.setProperty(object, property, properties.get(property));
                    }else if (isFieldPresent(classToUse, Integer.TYPE, property)){
                        PropertyUtils.setProperty(object, property, Integer.parseInt(properties.get(property)));
                    }else if (isFieldPresent(classToUse, Boolean.TYPE, property)){
                        PropertyUtils.setProperty(object, property, Boolean.parseBoolean(properties.get(property)));
                    }else if (isFieldPresent(classToUse, Date.class, property)){
                        PropertyUtils.setProperty(object, property, df.parse(properties.get(property)));
                    }else{
                        Logger.warn(classToUse, "Property " + property + "not set for " + classToUse.getName());
                    }
                }
            }

            ret.add(object);
        }
        return ret;
    }

    /**
     * Searches a property recursively through classes and superclasses
     * @param classToUse
     * @param fieldType
     * @param property
     * @return
     * @throws NoSuchFieldException
     */
    private static boolean isFieldPresent(Class classToUse, Class fieldType, String property)
            throws NoSuchFieldException {

        try{
            return classToUse.getDeclaredField(property).getType() == fieldType;
        }catch(NoSuchFieldException e){
            if (classToUse.getSuperclass()!=null) {
                return isFieldPresent(classToUse.getSuperclass(), fieldType, property);
            }
        }
        return false;
    }

}
