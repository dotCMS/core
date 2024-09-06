package com.dotmarketing.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import sun.misc.Unsafe;

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
    final static String DOT_TESTING_STRING_WITH_COMMA ="DOT_TESTING_STRING_WITH_COMMA";
    final static String DOT_TESTING_STRING_WITH_SPACES ="DOT_TESTING_STRING_WITH_SPACES";
    final static String DOT_TESTING_STRING ="DOT_TESTING_STRING";
    final static String UNABLE_TO_READ_VAR="UNABLE_TO_READ_VAR";

    /**
     * This method sets environmental variables that are intended for testing.
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

        // This forces a re-load.
        Config.props.clear();
        Config.initializeConfig();
    }

    @Test
    public void testing_string_with_comma() {
        String value = Config.getStringProperty("TESTING_STRING_WITH_COMMA");
        assertEquals(value, Config.getStringProperty("testing.String.with_comma"));
    }

    @Test
    public void Test_Multiple_Calls_To_AddProperty_On_The_Same_Key() {
        Config.props.addProperty("anyKey", "anyValue");
        assertEquals("anyValue", Config.props.getProperty("anyKey"));

        Config.props.addProperty("anyKey", "anyValue");
        final List<String> values1 = Arrays.asList("anyValue", "anyValue");

        final Object property1 = Config.props.getProperty("anyKey");
        final boolean equals1 = values1.equals(property1);
        assertTrue(equals1);

        Config.props.addProperty("anyKey", "anyValue");
        final List<String> values2 = Arrays.asList("anyValue", "anyValue", "anyValue");
        final Object property2 = Config.props.getProperty("anyKey");
        final boolean equals2 = values2.equals(property2);
        assertTrue(equals2);
    }

    @Test
    public void Test_Add_Env_Prop_Then_Test_Read_Value() {
        final String propertyName = "fictional_property";
        Config.props.setProperty(propertyName, "var");
        final String fictionalProperty = Config.getStringProperty(propertyName);
        assertEquals("var", fictionalProperty);
        EnvironmentVariablesService.getInstance().put("DOT_FICTIONAL_PROPERTY", "foo");
        Config.props.clear(); // force props reload

        final String fictionalPropertyOverride = Config.getStringProperty(propertyName);
        assertEquals("foo", fictionalPropertyOverride);
    }

    @Test
    public void testing_null_returns() {
        String test = Config.getStringProperty(SIMPLE_TESTING_STRING, null);
        assertNull(test);
    }

    @Test
    public void testing_default_returns() {
        String test = Config.getStringProperty(SIMPLE_TESTING_STRING, TESTING_VALUE);
        assertEquals(TESTING_VALUE, test);
    }

    @Test
    public void testing_notfound_string_returns() {
        assertNull(Config.getStringProperty("no-property"));
    }

    @Test
    public void testing_notfound_int_returns() {
        try {
            Config.getIntProperty("no-property");
            assert(false);
        } catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
        }
    }

    @Test
    public void testing_notfound_float_returns() {
        try {
            Config.getFloatProperty("no-property");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
        }
    }

    @Test
    public void testing_notfound_boolean_returns() {
        try {
            Config.getBooleanProperty("no-property");
            Assert.fail("Expected a NoSuchElementException to be thrown");
        } catch (Exception e) {
            Assert.assertTrue("Exception should be of type NoSuchElementException",
                    e instanceof NoSuchElementException);
        }
    }

    @Test
    public void test_get_integer_from_env() {
        int value = Config.getIntProperty("no-property", -99);
        assertEquals(-99, value);

        value = Config.getIntProperty(XXX_TESTING_INTEGER, -99);
        assertEquals(-99, value);

        value = Config.getIntProperty("testing.integer", -99);
        assertEquals(Integer.MAX_VALUE, value);
    }

    @Test
    public void test_get_float_from_env() {
        float value = Config.getFloatProperty("no-property", 3.14f);
        assertEquals(3.14f, value, 0.0);

        value = Config.getFloatProperty(XXX_TESTING_INTEGER, 3.14f);
        assertEquals(3.14f, value, 0.0);

        value = Config.getFloatProperty("testing_float", -1f);
        assertEquals(Float.MAX_VALUE, value, 0.0);
    }

    @Test
    public void test_get_string_from_env() {
        String value = Config.getStringProperty("no-property", TESTING_VALUE);
        assertEquals(TESTING_VALUE, value);

        value = Config.getStringProperty("testing.integer", TESTING_VALUE);
        assertEquals(value, String.valueOf(Integer.MAX_VALUE));

        value = Config.getStringProperty("testing.string", "VALUE_ABC");
        assertEquals("VALUE_ABC", value);
    }

    @Test
    public void test_get_string_array_from_env() {
        String[] value = Config.getStringArrayProperty("no-property", TESTING_ARRAY);
        assertArrayEquals(value, TESTING_ARRAY);

        value = Config.getStringArrayProperty("testing_string_with_comma");
        assertEquals(2, value.length);
        assertEquals("VALUE2", value[1]);

        String notArray = Config.getStringProperty("testing_string_with_comma");
        assertEquals("VALUE1,VALUE2", notArray);
    }

    @Test
    @Ignore("Logic needs updating when initial config is set by env vars in test ")
    public void Test_Env_Prop_Overrides_Regular_Prop() {
        final String DOT_MY_BOOLEAN_PROPERTY = "DOT_MY_BOOLEAN_PROPERTY";
        final String MY_BOOLEAN_PROPERTY = "my.boolean.property";

        Config.setProperty(DOT_MY_BOOLEAN_PROPERTY, false);
        Config.setProperty(MY_BOOLEAN_PROPERTY, true);

        assertFalse(Config.getBooleanProperty(DOT_MY_BOOLEAN_PROPERTY, true));
        assertFalse(Config.getBooleanProperty(MY_BOOLEAN_PROPERTY, true));

        Config.setProperty(DOT_MY_BOOLEAN_PROPERTY, null);
        assertTrue(Config.getBooleanProperty(MY_BOOLEAN_PROPERTY, false));
    }

    @Test
    public void test_isKeyEnvBased() {
        final String DOT_MY_ENV_VAR_PROPERTY = "DOT_MY_ENV_VAR_PROPERTY";
        final String MY_ENV_VAR_PROPERTY = "my.env.var.property";

        assertFalse(Config.isKeyEnvBased(MY_ENV_VAR_PROPERTY));

        Config.setProperty(DOT_MY_ENV_VAR_PROPERTY, null);
        assertFalse(Config.isKeyEnvBased(MY_ENV_VAR_PROPERTY));

        Config.setProperty(DOT_MY_ENV_VAR_PROPERTY, "not null");
        assertTrue(Config.isKeyEnvBased(MY_ENV_VAR_PROPERTY));
    }

    @Test
    public void test_envKey_keyWithoutPrefix_returnsEnvKey() {
        final String MY_ENV_VAR_PROPERTY = "my.env.var.property";
        final String convertedProperty = Config.envKey(MY_ENV_VAR_PROPERTY);

        assertTrue(convertedProperty.contains("DOT_"));
        assertFalse(convertedProperty.contains("."));
    }

    @Test
    public void test_envKey_keyWithPrefix_returnsEnvKey() {
        final String DOT_MY_ENV_VAR_PROPERTY = "DOT_MY_ENV_VAR_PROPERTY";
        final String convertedProperty = Config.envKey(DOT_MY_ENV_VAR_PROPERTY);

        assertTrue(convertedProperty.contains("DOT_"));
        assertFalse(convertedProperty.contains("."));
        assertFalse(convertedProperty.contains("DOT_DOT_"));
    }

    @Test
    public void test_subsetContainsAsList() {
        Config.props.addProperty("DOT_testSubset_anyKey", "anyValue");
        Config.props.addProperty("testSubset.anyKey", "anyValue");

        final List<String> properties = Config.subsetContainsAsList("testSubset");
        assertTrue(properties.size() >= 2);
        assertTrue(properties.contains("DOT_testSubset_anyKey"));
        assertTrue(properties.contains("testSubset.anyKey"));
    }

    @AfterClass
    public static void resetMap() {
        envMap.clear();
        envMap.putAll(DEFAULTS);
    }

    @BeforeClass
    public static void accessFields() throws Exception {
        envMap.putAll(DEFAULTS);
        Class<?> clazz = Class.forName("java.lang.ProcessEnvironment");
        Field theUnmodifiableEnvironmentField = clazz.getDeclaredField("theUnmodifiableEnvironment");
        removeStaticFinalAndSetValue(theUnmodifiableEnvironmentField);
        setTestEnvVariables();
    }

    private static void removeStaticFinalAndSetValue(Field field) throws Exception {
        // Obtain the Unsafe instance
        Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafeField.get(null);

        // Make the field accessible and remove the final modifier
        long fieldOffset = unsafe.staticFieldOffset(field);
        Object staticFieldBase = unsafe.staticFieldBase(field);

        // Use Unsafe to update the field value
        unsafe.putObject(staticFieldBase, fieldOffset, ConfigTest.envMap);
    }
}