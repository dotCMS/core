package com.dotmarketing.db;

import javax.sql.DataSource;

/**
 * Specification for a DataSource definition. Examples: {@link DBPropertiesDataSourceStrategy}, {@link DockerSecretDataSourceStrategy}
 * To create a custom implementation, the property <b>DATASOURCE_PROVIDER_STRATEGY_CLASS</b> must be set
 * with the fully qualified name of a class that implements this interface
 * @author nollymar
 */
public interface DotDataSourceStrategy {
    DataSource apply();
}
