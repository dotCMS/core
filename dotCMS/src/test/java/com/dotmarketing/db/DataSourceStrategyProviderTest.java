package com.dotmarketing.db;

import com.liferay.util.SystemEnvironmentProperties;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.postgresql.jdbc.PgConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
                "Tomcat",
                "CustomProvider"
        };
    }

    /**
     * Method to test: {@link DataSourceStrategyProvider#get()}
     * Test case: Verify this order is respected when DataSource credentials are obtained:
     * <ul>
     *            <li>Custom provider</li>
     *            <li>db.properties file</li>
     *            <li>System environment variables</li>
     *            <li>Docker secrets</li>
     *            <li>context.xml</li>
     * </ul>
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
            Mockito.when(properties.getVariable("DB_BASE_URL")).thenReturn("dummy_url");
        } else {
            Mockito.when(properties.getVariable("DB_BASE_URL")).thenReturn(null);
        }

        if (testCase.equals("DockerSecret")) {
            Mockito.when(dockerSecretStrategy.dockerSecretPathExists()).thenReturn(true);
        } else {
            Mockito.when(dockerSecretStrategy.dockerSecretPathExists()).thenReturn(false);
        }

        if (testCase.equals("CustomProvider")){
            Mockito.when(provider.getCustomDataSourceProvider()).thenReturn("DummyProvider");
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
        Mockito.verify(tomcatDataSourceStrategy, Mockito.times(
                (testCase.equals("Tomcat") | testCase.equals("CustomProvider")) ? 1 : 0)).apply();

    }


    /**
     * Method to test: {@link DataSourceStrategyProvider#get()}
     * Test case: Use {@link TomcatDataSourceStrategy} provider when any of these strategies fails
     * <ul>
     *            <li>Custom provider</li>
     *            <li>db.properties file</li>
     *            <li>System environment variables</li>
     *            <li>Docker secrets</li>
     *            <li>context.xml</li>
     * </ul>
     *
     * Expected result: A DataSource is initialized using the {@link TomcatDataSourceStrategy} provider
     * @param testCase
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    @UseDataProvider("testCases")
    @Test
    public void testGetFallback(final String testCase)
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
            Mockito.when(dbStrategy.getPropertiesFile()).thenReturn(null);
        } else {
            Mockito.when(dbStrategy.existsDBPropertiesFile()).thenReturn(false);
        }

        if (testCase.equals("SystemEnv")){
            Mockito.when(properties.getVariable("DB_BASE_URL")).thenReturn("dummy_url");
        } else {
            Mockito.when(properties.getVariable("DB_BASE_URL")).thenReturn(null);
        }

        if (testCase.equals("DockerSecret")) {
            Mockito.when(dockerSecretStrategy.dockerSecretPathExists()).thenReturn(true);
        } else {
            Mockito.when(dockerSecretStrategy.dockerSecretPathExists()).thenReturn(false);
        }

        if (testCase.equals("CustomProvider")){
            Mockito.when(provider.getCustomDataSourceProvider()).thenReturn("DummyProvider");
        }

        Mockito.when(tomcatDataSourceStrategy.apply()).thenReturn(dummyDatasource);

        //Gets the provider strategy
        DataSource result = provider.get();

        assertNotNull(result);
        assertEquals(dummyDatasource, result);

        Mockito.verify(dbStrategy, Mockito.times(testCase.equals("DBProperties")? 1: 0)).apply();
        Mockito.verify(systemEnvStrategy, Mockito.times(testCase.equals("SystemEnv")? 1: 0)).apply();
        Mockito.verify(dockerSecretStrategy, Mockito.times(testCase.equals("DockerSecret")? 1: 0)).apply();
        Mockito.verify(tomcatDataSourceStrategy, Mockito.times(1)).apply();

    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link DataSourceStrategyProvider#get()}</li>
     *     <li><b>Given Scenario:</b> By default, all database connection objects must be created using the UTC Time
     *     Zone, regardless of the current default or server Time Zone.</li>
     *     <li><b>Expected Result:</b> If the default Time Zone is NOT in UTC, the {@link Connection} object generated
     *     by our {@link DataSourceStrategyProvider} must always reference the UTC Time Zone.</li>
     * </ul>
     */
    @Test
    public void testConnectionObjectTimeZone() {
        final TimeZone defaultTz = TimeZone.getDefault();
        if ("UTC".equalsIgnoreCase(defaultTz.getID())) {
            // If the default TimeZone is already set to UTC, this test can be ignored
            return;
        }
        try {
            final DataSource dataSource = DataSourceStrategyProvider.getInstance().get();
            final Connection connection = dataSource.getConnection();
            final PgConnection pgConn = connection.unwrap(PgConnection.class);
            final TimeZone connectionTz = pgConn.getQueryExecutor().getTimeZone();
            assertFalse("TimeZone value from the Connection object must always be UTC!",
                    connectionTz.getID().equals(defaultTz.getID()));
        } catch (final ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
            throw new RuntimeException("An error occurred when checking the TimeZone value form the Connection " +
                                               "object: " + e.getMessage(), e);
        }
    }

}
