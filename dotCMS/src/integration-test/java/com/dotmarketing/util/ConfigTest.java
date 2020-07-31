package com.dotmarketing.util;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigTest {

    
    private static final Map<String, String> DEFAULTS = new HashMap<>(System.getenv());
    private static final Map<String, String> envMap = new HashMap<>();

    final static String DOT_TESTING_INTEGER="DOT_TESTING_INTEGER";
    final static String XXX_TESTING_INTEGER = "XXX_TESTING_INTEGER";
    final static String DOT_TESTING_LONG= "DOT_TESTING_LONG";
    final static String DOT_TESTING_BOOLEAN ="DOT_TESTING_BOOLEAN";
    final static String DOT_TESTING_FLOAT ="DOT_TESTING_FLOAT";
    
    final static String SIMPLE_TESTING_STRING = "SIMPLE_TESTING_STRING";
    final static String TESTING_VALUE = "TESTING_VALUE";
    final static String[] TESTING_ARRAY = new String[]{"TESTING_VALUE"};
    final static String NO_VALUE = "NO_VALUE";
    final static String DOT_TESTING_STRING_WITH_COMMA ="DOT_TESTING_STRING_WITH_COMMA";
    final static String DOT_TESTING_STRING_WITH_SPACES ="DOT_TESTING_STRING_WITH_SPACES";
    final static String DOT_TESTING_STRING ="DOT_TESTING_STRING";
    final static String UNABLE_TO_READ_VAR="UNABLE_TO_READ_VAR";
    
    
    
    /**
     * This method sets environmental variables that are intended for testing.
     * 
     */
    private static void setTestEnvVariables() {
        
        System.getenv().put(DOT_TESTING_INTEGER, String.valueOf(Integer.MAX_VALUE));
        System.getenv().put(XXX_TESTING_INTEGER, String.valueOf(Integer.MIN_VALUE));
        System.getenv().put(DOT_TESTING_LONG, String.valueOf(Long.MAX_VALUE));
        System.getenv().put(DOT_TESTING_BOOLEAN, String.valueOf(Boolean.TRUE));
        System.getenv().put(DOT_TESTING_FLOAT, String.valueOf(Float.MAX_VALUE));
        System.getenv().put(DOT_TESTING_STRING_WITH_COMMA, "VALUE1,VALUE2");
        System.getenv().put(DOT_TESTING_STRING_WITH_SPACES, "VALUE1 VALUE2");
        System.getenv().put(DOT_TESTING_STRING, "VALUE_ABC");
        System.getenv().put(UNABLE_TO_READ_VAR, "NOPE");
    }
    
    
    
    @Test
    public void testing_string_with_comma() {

        String value = Config.getStringProperty("TESTING_STRING_WITH_COMMA");
        assert(value.equals(Config.getStringProperty("testing.String.with_comma")));

        
    }
    
    
    
    @Test
    public void testing_null_returns() {

        String test = Config.getStringProperty(SIMPLE_TESTING_STRING, null);
        assertNull(test);

        
    }
    
    
    @Test
    public void testing_default_returns() {

        
        String test = Config.getStringProperty(SIMPLE_TESTING_STRING, TESTING_VALUE);
        assertEquals(test, TESTING_VALUE);

        
    }
    
    @Test
    public void testing_notfound_string_returns() {


        assert(Config.getStringProperty("no-property") ==null);
  


        
    }


    
    @Test
    public void testing_notfound_int_returns() {

        try {
            Config.getIntProperty("no-property");
            assert(false);
        }
        catch(Exception e) {
            assert(e instanceof NoSuchElementException);
            
        }
 
        
    }

    @Test
    public void testing_notfound_float_returns() {

        try {
            Config.getFloatProperty("no-property");
            assert(false);
        }
        catch(Exception e) {
            assert(e instanceof NoSuchElementException);
            
        }
 
        
    }
    

    @Test
    public void testing_notfound_booean_returns() {

        try {
            Config.getBooleanProperty("no-property");
            assert(false);
        }
        catch(Exception e) {
            assert(e instanceof NoSuchElementException);
            
        }
 
        
    }
    
    
    
    @Test
    public void test_get_integer_from_env() {


        int value =Config.getIntProperty("no-property", -99);
        assert(value==-99);
        
        // this should not work, as we prefix DOT_ to the env variable lookup
        value =Config.getIntProperty(XXX_TESTING_INTEGER, -99);
        assert(value==-99);
        
        // we should get back Integer.MAX_VALUE, as this get transformed into DOT_TESTING_INTEGER
        value =Config.getIntProperty("testing.integer", -99);
        assert(value==Integer.MAX_VALUE);
    }

    @Test
    public void test_get_float_from_env() {


        float value =Config.getFloatProperty("no-property", 3.14f);
        assert(value==3.14f);
        
        // this should not work, as we prefix DOT_ to the env variable lookup
        value =Config.getFloatProperty(XXX_TESTING_INTEGER, 3.14f);
        assert(value==3.14f);
        
        // we should get back Integer.MAX_VALUE, not the default
        value =Config.getFloatProperty("testing_float",-1f);
        assert(value==Float.MAX_VALUE);
    }

    
    @Test
    public void test_get_string_from_env() {


 
        String value =Config.getStringProperty("no-property", TESTING_VALUE);
        assert(value.equals(TESTING_VALUE));
        
        // this should  work, as we prefix DOT_ to the env variable lookup
        value =Config.getStringProperty("testing.integer", TESTING_VALUE);
        assert(value.equals(String.valueOf(Integer.MAX_VALUE)));
        
        // we should get back VALUE_ABC
        value =Config.getStringProperty("testing.string","VALUE_ABC");

    }
    
    @Test
    public void test_get_string_array_from_env() {


 
        String[] value =Config.getStringArrayProperty("no-property", TESTING_ARRAY);
        assertArrayEquals(value, TESTING_ARRAY);
        
        // this should  work, as we prefix DOT_ to the env variable lookup
        value =Config.getStringArrayProperty("testing_string_with_comma");
        assert(value.length==2);
        assert(value[1].equals("VALUE2"));
        
        String notArray  =Config.getStringProperty("testing_string_with_comma");
        assertEquals(notArray, "VALUE1,VALUE2");
    }
    
    
    /*
     * 
     * Restore default variables for each test
     */
    @AfterClass
    public static void resetMap() {
        envMap.clear();
        envMap.putAll(DEFAULTS);
    }
    
    

    
    
    
    @BeforeClass
    public static void accessFields() throws Exception {

        envMap.putAll(DEFAULTS);
        Class<?> clazz = Class.forName("java.lang.ProcessEnvironment");
        //Field theCaseInsensitiveEnvironmentField = clazz.getDeclaredField("theCaseInsensitiveEnvironment");
        Field theUnmodifiableEnvironmentField = clazz.getDeclaredField("theUnmodifiableEnvironment");
        //removeStaticFinalAndSetValue(theCaseInsensitiveEnvironmentField, envMap);
        removeStaticFinalAndSetValue(theUnmodifiableEnvironmentField, envMap);
        setTestEnvVariables();
    }

    private static void removeStaticFinalAndSetValue(Field field, Object value) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }
    
    
    
    

}
