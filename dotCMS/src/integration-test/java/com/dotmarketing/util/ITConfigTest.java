package com.dotmarketing.util;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.business.APILocator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for the ConfigTest.
 */
public class ITConfigTest extends IntegrationTestBase {

    @BeforeClass
    public static void beforeInit() throws Exception {
        IntegrationTestBase.beforeInit();
        Config.enableSystemTableConfigSource = true;
    }

    @AfterClass
    public static void afterInit() throws Exception {
        Config.enableSystemTableConfigSource = false;
    }

    /**
     * Method to test: {@link Config#getStringProperty(String, String)}
     * Given Scenario: Will request a property that does not exist on config nor system table
     * ExpectedResult: Should return default value
     */
    @Test
    public void test_404_on_system_table () {

        final String value = Config.getStringProperty("NON_EXISTING_KEY", "DEFAULT_VALUE");
        Assert.assertEquals("Should return default value", "DEFAULT_VALUE", value);
    }

    /**
     * Method to test: {@link Config#getStringProperty(String, String)}
     * Given Scenario: Will request a property that exist on the system table
     * ExpectedResult: Should return value from the system table
     */
    @Test
    public void test_resolving_config_on_system_table () {

        APILocator.getSystemAPI().getSystemTable().set("NEW_PROPERTY", "NEW_VALUE");
        final String value = Config.getStringProperty("NEW_PROPERTY", "DEFAULT_VALUE");
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE", value);
        final Object valueFromCache = APILocator.getSystemAPI().getSystemCache().get("NEW_PROPERTY");
        Assert.assertNotNull("Should be on the cache", valueFromCache);
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE", valueFromCache);
    }

    /**
     * Method to test: {@link Config#getStringProperty(String, String)}
     * Given Scenario: Will request a property that exist on the system table and it is updated
     * ExpectedResult: Should return latest value twice
     */
    @Test
    public void test_resolving_config_on_system_table_and_update () {

        APILocator.getSystemAPI().getSystemTable().set("SECOND_PROPERTY", "NEW_VALUE_1");
        String value = Config.getStringProperty("SECOND_PROPERTY", "DEFAULT_VALUE");
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE_1", value);
        APILocator.getSystemAPI().getSystemTable().set("SECOND_PROPERTY", "NEW_VALUE_2");
        value = Config.getStringProperty("SECOND_PROPERTY", "DEFAULT_VALUE");
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE_2", value);
        final Object valueFromCache = APILocator.getSystemAPI().getSystemCache().get("SECOND_PROPERTY");
        Assert.assertNotNull("Should be on the cache", valueFromCache);
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE_2", valueFromCache);
    }

    /**
     * Method to test: {@link Config#getStringProperty(String, String)}
     * Given Scenario: Will request a property that exist on the system table, then updated, finally removed
     * ExpectedResult: Should return latest value twice and finally removed
     */
    @Test
    public void test_resolving_config_on_system_table_update_and_update () {

        APILocator.getSystemAPI().getSystemTable().set("THIRD_PROPERTY", "NEW_VALUE_1");
        String value = Config.getStringProperty("THIRD_PROPERTY", "DEFAULT_VALUE");
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE_1", value);
        APILocator.getSystemAPI().getSystemTable().set("THIRD_PROPERTY", "NEW_VALUE_2");
        value = Config.getStringProperty("THIRD_PROPERTY", "DEFAULT_VALUE");
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE_2", value);
        APILocator.getSystemAPI().getSystemTable().delete("THIRD_PROPERTY");
        value = Config.getStringProperty("THIRD_PROPERTY", "DEFAULT_VALUE");
        Assert.assertEquals("Should return default value since the property has been removed", "DEFAULT_VALUE", value);
    }

    /**
     * Method to test: {@link Config#getStringProperty(String, String)}
     * Given Scenario: This will add two properties, one is set only on the system table, the other is set on the system table and on the environment variables
     * ExpectedResult: The one set on the environment variables should have priority over the one set on the system table
     */
    @Test
    public void test_property_order_resolution () {

        EnvironmentVariablesService.getInstance().put("DOT_ONLY_ENV_PROP", "ENV_VALUE");
        APILocator.getSystemAPI().getSystemTable().set("DOT_ONLY_ENV_PROP", "NEW_VALUE_FROM_SYSTEM_TABLE");
        APILocator.getSystemAPI().getSystemTable().set("DOT_ONLY_SYSTEM_TABLE", "NEW_VALUE_FROM_SYSTEM_TABLE");
        Config.refreshProperties();


        String value = Config.getStringProperty("DOT_ONLY_ENV_PROP", "DEFAULT_VALUE");
        Assert.assertEquals("Should return the value from the environment", "ENV_VALUE", value);
        value = Config.getStringProperty("DOT_ONLY_SYSTEM_TABLE", "DEFAULT_VALUE");
        Assert.assertEquals("Should return the value from the system table", "NEW_VALUE_FROM_SYSTEM_TABLE", value);
    }
}
