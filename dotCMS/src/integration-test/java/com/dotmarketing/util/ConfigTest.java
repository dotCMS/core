package com.dotmarketing.util;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigTest {

    
    private static final Map<String, String> DEFAULTS = new HashMap<>(System.getenv());
    private static final Map<String, String> envMap = new HashMap<>();

    final String MY_TEST_KEY                   = "MY_TEST_KEY";
    final String MY_TEST_VALUE_DEFAULT         = "MY_TEST_VALUE_DEFAULT";
    final String MY_TEST_KEY_ENV               = "MY_TEST_KEY_ENV";
    final String MY_TEST_VALUE_DEFAULT_ENV     = "MY_TEST_VALUE_DEFAULT_ENV";
    final String MY_TEST_VALUE_RETURN_ENV      = "MY_TEST_VALUE_RETURN_ENV";
    
    
    
    @Test
    public void testing_null_returns() {

        
        String test = Config.getStringProperty(MY_TEST_KEY, null);
        assertNull(test);

        
    }
    
    
    @Test
    public void testing_default_returns() {

        
        String test = Config.getStringProperty(MY_TEST_KEY, MY_TEST_VALUE_DEFAULT);
        assertEquals(test, MY_TEST_VALUE_DEFAULT);

        
    }
    
    
    @Test
    public void testing_environment_variable() {

        System.getenv().put("NUMBER_OF_PROCESSORS", "77");
        
        String test = Config.getStringProperty(MY_TEST_STRING, MY_TEST_STRING_DEFAULT);
        assertEquals(test, MY_TEST_STRING_DEFAULT);

        
    }

    

    /*
     * 
     * Restore default variables for each test
     */
    @AfterClass
    public void resetMap() {
        envMap.clear();
        envMap.putAll(DEFAULTS);
    }
    
    @BeforeClass
    public static void accessFields() throws Exception {

        envMap.putAll(DEFAULTS);
        Class<?> clazz = Class.forName("java.lang.ProcessEnvironment");
        Field theCaseInsensitiveEnvironmentField = clazz.getDeclaredField("theCaseInsensitiveEnvironment");
        Field theUnmodifiableEnvironmentField = clazz.getDeclaredField("theUnmodifiableEnvironment");
        removeStaticFinalAndSetValue(theCaseInsensitiveEnvironmentField, envMap);
        removeStaticFinalAndSetValue(theUnmodifiableEnvironmentField, envMap);
    }

    private static void removeStaticFinalAndSetValue(Field field, Object value) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }
    
    
    
    

}
