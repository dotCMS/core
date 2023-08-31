package com.dotmarketing.util;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.business.APILocator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the ConfigTest.
 */
public class ITConfigTest extends IntegrationTestBase {

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
}
