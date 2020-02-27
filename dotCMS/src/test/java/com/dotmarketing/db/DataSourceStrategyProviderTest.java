package com.dotmarketing.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.liferay.util.SystemEnvironmentProperties;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Tests for {@link DataSourceStrategyProvider}
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class DataSourceStrategyProviderTest {

    @DataProvider
    public static Object[] testCases() {

        return new String[]{
                "DBProperties",
                "SystemEnv",
                "DockerSecret",
                "Tomcat"
        };
    }

    /**
     * Method to test: {@link DataSourceStrategyProvider#get()}
     * Test case: Verify this order is respected when DataSource credentials are obtained when a custom
     * provider is not set:
     *            2. db.properties file
     *            3. System environment variables
     *            4. Docker secrets
     *            5. context.xml
     *
     * Expected result: A DataSource is returned using the right provider
     * @param testCase
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    @UseDataProvider("testCases")
    @Test
    public void testGet(final String testCase)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {

        final DataSourceStrategyProvider provider = Mockito.spy(DataSourceStrategyProvider.class);
        final SystemEnvironmentProperties properties = Mockito.mock(SystemEnvironmentProperties.class);
        final DBPropertiesDataSourceStrategy dbStrategy = Mockito.mock(DBPropertiesDataSourceStrategy.class);
        final SystemEnvDataSourceStrategy systemEnvStrategy = Mockito.mock(SystemEnvDataSourceStrategy.class);
        final DockerSecretDataSourceStrategy dockerSecretStrategy = Mockito.mock(DockerSecretDataSourceStrategy.class);
        final TomcatDataSourceStrategy tomcatDataSourceStrategy = Mockito.mock(TomcatDataSourceStrategy.class);

        final HikariDataSource dummyDatasource = new HikariDataSource();

        Mockito.when(provider.getSystemEnvironmentProperties()).thenReturn(properties);
        Mockito.when(provider.getDBPropertiesInstance()).thenReturn(dbStrategy);
        Mockito.when(provider.getSystemEnvDataSourceInstance()).thenReturn(systemEnvStrategy);
        Mockito.when(provider.getDockerSecretDataSourceInstance()).thenReturn(dockerSecretStrategy);
        Mockito.when(provider.getTomcatDataSourceInstance()).thenReturn(tomcatDataSourceStrategy);

        if (testCase.equals("DBProperties")) {
            Mockito.when(dbStrategy.existsDBPropertiesFile()).thenReturn(true);
        } else {
            Mockito.when(dbStrategy.existsDBPropertiesFile()).thenReturn(false);
        }

        if (testCase.equals("SystemEnv")){
            Mockito.when(properties.getVariable("connection_db_base_url")).thenReturn("dummy_url");
        } else {
            Mockito.when(properties.getVariable("connection_db_base_url")).thenReturn(null);
        }

        if (testCase.equals("DockerSecret")) {
            Mockito.when(dockerSecretStrategy.dockerSecretPathExists()).thenReturn(true);
        } else {
            Mockito.when(dockerSecretStrategy.dockerSecretPathExists()).thenReturn(false);
        }

        Mockito.when(dbStrategy.apply()).thenReturn(dummyDatasource);
        Mockito.when(systemEnvStrategy.apply()).thenReturn(dummyDatasource);
        Mockito.when(dockerSecretStrategy.apply()).thenReturn(dummyDatasource);
        Mockito.when(tomcatDataSourceStrategy.apply()).thenReturn(dummyDatasource);


        //Gets the provider strategy
        DataSource result = provider.get();

        assertNotNull(result);
        assertEquals(dummyDatasource, result);

        Mockito.verify(dbStrategy, Mockito.times(testCase.equals("DBProperties")? 1: 0)).apply();
        Mockito.verify(systemEnvStrategy, Mockito.times(testCase.equals("SystemEnv")? 1: 0)).apply();
        Mockito.verify(dockerSecretStrategy, Mockito.times(testCase.equals("DockerSecret")? 1: 0)).apply();
        Mockito.verify(tomcatDataSourceStrategy, Mockito.times(testCase.equals("Tomcat")? 1: 0)).apply();

    }

    /**
     * Method to test: {@link DataSourceStrategyProvider#get()}
     * Test case: When a custom provider class is set, the DataSourceStrategyProvider should try to use it
     * instead of the others providers
     * Expected result: An exception should be thrown as the custom provider class does not exist
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    @Test(expected=ClassNotFoundException.class)
    public void testGetUsingCustomProvider()
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {

        final DataSourceStrategyProvider provider = Mockito.spy(DataSourceStrategyProvider.class);

        Mockito.when(provider.getCustomDataSourceProvider()).thenReturn("DummyProvider");

        //Gets the provider strategy
        provider.get();
    }


}
