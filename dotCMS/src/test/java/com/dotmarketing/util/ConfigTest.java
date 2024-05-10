package com.dotmarketing.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.tuckey.web.filters.urlrewrite.Conf;

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

        EnvironmentVariablesService.getInstance().put(DOT_TESTING_INTEGER, String.valueOf(Integer.MAX_VALUE))
        .put(XXX_TESTING_INTEGER, String.valueOf(Integer.MIN_VALUE))
        .put(DOT_TESTING_LONG, String.valueOf(Long.MAX_VALUE))
        .put(DOT_TESTING_BOOLEAN, String.valueOf(Boolean.TRUE))
        .put(DOT_TESTING_FLOAT, String.valueOf(Float.MAX_VALUE))
        .put(DOT_TESTING_STRING_WITH_COMMA, "VALUE1,VALUE2")
        .put(DOT_TESTING_STRING_WITH_SPACES, "VALUE1 VALUE2")
        .put(DOT_TESTING_STRING, "VALUE_ABC")
        .put(UNABLE_TO_READ_VAR, "NOPE");

        //This forces a re-load.
        Config.props.clear();
        Config.initializeConfig();
    }



    @Test
    public void testing_string_with_comma() {

        String value = Config.getStringProperty("TESTING_STRING_WITH_COMMA");
        assert(value.equals(Config.getStringProperty("testing.String.with_comma")));


    }

    /**
     * Method to rest: {@link org.apache.commons.configuration.PropertiesConfiguration#addProperty(String, Object)}
     * Given Scenario: We add several properties under the same key to demo that the internal method addProperty generates an array list
     * Expected result: The property now holds an array.
     */
    @Test
    public void Test_Multiple_Calls_To_AddProperty_On_The_Same_Key() {

        Config.props.addProperty("anyKey","anyValue");
        assertEquals ("anyValue",Config.props.getProperty("anyKey"));

        Config.props.addProperty("anyKey","anyValue");
        final List<String> values1 = Arrays.asList("anyValue", "anyValue");

        final Object property1 = Config.props.getProperty("anyKey");
        final boolean equals1 = values1.equals(property1);
        assertTrue(equals1);

        Config.props.addProperty("anyKey","anyValue");
        final List<String> values2 = Arrays.asList("anyValue", "anyValue", "anyValue");
        final Object property2 = Config.props.getProperty("anyKey");
        final boolean equals2 = values2.equals(property2);
        assertTrue(equals2);
    }

    /**
     * Method to rest: {@link Config#getStringProperty(String)}
     * Given Scenario: We set an environment prop that starts with DOT_ and certain value. Lets say X then we test getting that value through the property name that does not stat with such prefix.
     * Expected result: We should get the values set to the env variable since it overrides the original property.
     */
    @Test
    public void Test_Add_Env_Prop_Then_Test_Read_Value() {
        final String propertyName = "fictional_property";
        // Here we need to set the original property
        // as we are testing overriding it with the "DOT_" env variable
        // Otherwise our setProperty will update as a configOverride and we
        // will not see the change of the env variable
        Config.props.setProperty(propertyName,"var");
        final String fictionalProperty = Config.getStringProperty(propertyName);
        assertEquals("var",fictionalProperty);
        EnvironmentVariablesService.getInstance().put("DOT_FICTIONAL_PROPERTY", "foo");
        Config.props.clear(); //force props reload

        final String fictionalPropertyOverride = Config.getStringProperty(propertyName);
        assertEquals("foo",fictionalPropertyOverride);
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

    /**
     * Method to test {@link Config#getBooleanProperty(String, boolean)}
     * Here we test that the "same" property seen as an environmental var overrides the other regular property already set in the map
     */
    @Test
    @Ignore("Logic needs updating when initial config is set by env vars in test ")
    public void Test_Env_Prop_Overrides_Regular_Prop(){


            final String DOT_MY_BOOLEAN_PROPERTY = "DOT_MY_BOOLEAN_PROPERTY";
            final String MY_BOOLEAN_PROPERTY = "my.boolean.property";

            //Now lets suppose the two properties exits in the Config
            Config.setProperty(DOT_MY_BOOLEAN_PROPERTY, false);
            Config.setProperty(MY_BOOLEAN_PROPERTY, true);
            //But one must take precedence over the other and that's the one that starts with dot.

            //if I request the dot prop one should easily expect the value it was initialized with
            assertFalse(Config.getBooleanProperty(DOT_MY_BOOLEAN_PROPERTY, true));
            //if I request the regular non-dot prop we should still get the value assigned to the dot prop because it overrides it
            assertFalse(Config.getBooleanProperty(MY_BOOLEAN_PROPERTY, true));

            //The second I get rid of the DOT property now I should get the original regular prop
            Config.setProperty(DOT_MY_BOOLEAN_PROPERTY, null);
            assertTrue(Config.getBooleanProperty(MY_BOOLEAN_PROPERTY, false));
    }

    /**
     * Method to test {@link Config#isKeyEnvBased(String)}
     * Given a property name verify if it belongs to the properties set by environment variables, that is with the
     * 'DOT_' prefix.
     */
    @Test
    public void test_isKeyEnvBased() {
        final String DOT_MY_ENV_VAR_PROPERTY = "DOT_MY_ENV_VAR_PROPERTY";
        final String MY_ENV_VAR_PROPERTY = "my.env.var.property";

        // no sight of property
        assertFalse(Config.isKeyEnvBased(MY_ENV_VAR_PROPERTY));

        // add empty property
        Config.setProperty(DOT_MY_ENV_VAR_PROPERTY, null);
        assertFalse(Config.isKeyEnvBased(MY_ENV_VAR_PROPERTY));

        // add property with actual value
        Config.setProperty(DOT_MY_ENV_VAR_PROPERTY, "not null");
        assertTrue(Config.isKeyEnvBased(MY_ENV_VAR_PROPERTY));
    }

    /**
     * Method to test {@link Config#envKey(String)}
     * Checks if the method is converting the key to env key successfully.
     */
    @Test
    public void test_envKey_keyWithoutPrefix_returnsEnvKey() {
        final String MY_ENV_VAR_PROPERTY = "my.env.var.property";
        final String convertedProperty = Config.envKey(MY_ENV_VAR_PROPERTY);

        assertTrue(convertedProperty.contains("DOT_"));
        assertFalse(convertedProperty.contains("."));
    }

    /**
     * Method to test {@link Config#envKey(String)}
     * If the key is already an Env Key it doesn't need to be converted.
     */
    @Test
    public void test_envKey_keyWithPrefix_returnsEnvKey() {
        final String DOT_MY_ENV_VAR_PROPERTY = "DOT_MY_ENV_VAR_PROPERTY";
        final String convertedProperty = Config.envKey(DOT_MY_ENV_VAR_PROPERTY);

        assertTrue(convertedProperty.contains("DOT_"));
        assertFalse(convertedProperty.contains("."));
        assertFalse(convertedProperty.contains("DOT_DOT_"));
    }

    /**
     * Method to test {@link Config#subsetContainsAsList(String)}
     * Pull out all the properties that contains the given string, in this case testSubset.
     */
    @Test
    public void test_subsetContainsAsList(){
        Config.props.addProperty("DOT_testSubset_anyKey","anyValue");
        Config.props.addProperty("testSubset.anyKey","anyValue");

        final List<String> properties = Config.subsetContainsAsList("testSubset");
        assertTrue(properties.size()>=2);
        assertTrue(properties.contains("DOT_testSubset_anyKey"));
        assertTrue(properties.contains("testSubset.anyKey"));
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



    private static void removeStaticFinalAndSetValue(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
        Field modifiersField = null;
        for (Field each : fields) {
            if ("modifiers".equals(each.getName())) {
                modifiersField = each;
                break;
            }
        }
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }



}
