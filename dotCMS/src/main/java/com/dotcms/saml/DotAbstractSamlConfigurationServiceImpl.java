package com.dotcms.saml;

import com.dotmarketing.util.Logger;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * This service will retrieve information form the idp config, but also if the value is not set will provide the default value for the saml name.
 * It has different implementation for several kinds of values
 * @author jsanca
 */
public abstract class DotAbstractSamlConfigurationServiceImpl implements SamlConfigurationService {

    private static final String NULL = "NULL";
    private static final String UNABLE_TO_READ_FILE = "File does not exist or unable to read : ";
    private static final String NOT_FOUND_ERROR = "Property Name not Found: ";

    private AtomicBoolean init = new AtomicBoolean(false);
    private final Map<String, String> defaultProperties = new ConcurrentHashMap<>();

    @Override
    public void initService(final Map<String, Object> contextMap) {

        if (!this.init.get()) {

            final Map<String, String> samlInitialMap = this.createInitialMap();
            for (final Map.Entry<String, String> entry : samlInitialMap.entrySet()) {

                this.defaultProperties.put(entry.getKey(), null == entry.getValue()?NULL:entry.getValue());
            }

            this.internalInit(contextMap);
        }
    }

    private synchronized void internalInit(final Map<String, Object> contextMap) {

        final String dotSamlDefaultPropertiesValue = (String)contextMap.get(DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY);

        if (null == dotSamlDefaultPropertiesValue) {

            Logger.warn(this, DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY + " must be set on the argument context map");
        } else {

            final File dotSamlDefaultPropertiesFile = new File(dotSamlDefaultPropertiesValue);

            if (!dotSamlDefaultPropertiesFile.exists() || !dotSamlDefaultPropertiesFile.canRead()) {

                Logger.warn(this, "The " + dotSamlDefaultPropertiesValue + " does not exists or can not read");
            } else {

                final Properties properties = new Properties();

                try (InputStream input = Files.newInputStream(dotSamlDefaultPropertiesFile.toPath())) {

                    properties.load(input);
                } catch (IOException ex) {
                    // Since this is optional, it is valid to not have the file.
                    // Log and go on.
                    Logger.warn(this, UNABLE_TO_READ_FILE + dotSamlDefaultPropertiesValue);

                }

                properties.forEach((key, value) -> {

                    final SamlName samlName = SamlName.findProperty((String)key);
                    if (null != samlName) {

                        this.defaultProperties.put(samlName.getPropertyName(), (String)value);
                    }
                });

                this.init.set(true);
            }
        }
    }


    @Override
    public String getConfigAsString(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {

        try {

            final String value = identityProviderConfiguration.containsOptionalProperty(samlName.getPropertyName())?
                    (String) identityProviderConfiguration.getOptionalProperty(samlName.getPropertyName()):
                    this.getDefaultStringParameter(samlName);

            Logger.debug(this,
                    ()-> "Found " + samlName.getPropertyName() + " : " + ((value == null) ? "null" : value));

            return value;
        } catch (Exception e) {

            Logger.warn(this, ()-> "Cast exception on " + samlName.getPropertyName()
                    + " property. idpConfigId: " + identityProviderConfiguration.getId());
        }

        return null;
    }

    @Override
    public String getConfigAsString(final IdentityProviderConfiguration identityProviderConfiguration,
                                    final SamlName samlName, final Supplier<String> defaultValueSupplier) {

        try {

            final String value = identityProviderConfiguration.containsOptionalProperty(samlName.getPropertyName())?
                    (String) identityProviderConfiguration.getOptionalProperty(samlName.getPropertyName()):
                    defaultValueSupplier.get();

            Logger.debug(this,
                    ()-> "Found " + samlName.getPropertyName() + " : " + ((value == null) ? "null" : value));

            return value;
        } catch (Exception e) {

            Logger.warn(this, ()-> "Cast exception on " + samlName.getPropertyName()
                    + " property. idpConfigId: " + identityProviderConfiguration.getId());
        }

        return defaultValueSupplier.get();
    }

    @Override
    public Boolean getConfigAsBoolean(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {

        try {

            final Boolean value =  identityProviderConfiguration.containsOptionalProperty(samlName.getPropertyName())?
                Boolean.parseBoolean((String) identityProviderConfiguration.getOptionalProperty(samlName.getPropertyName())):
                this.getDefaultBooleanParameter(samlName);

            Logger.debug(this,
                    ()->"Found " + samlName.getPropertyName() + " : " + ((value == null) ? "null" : value));

            return value;
        } catch (Exception e) {

            Logger.warn(this, "Cast exception on " + samlName.getPropertyName()
                    + " property. idpConfigId: " + identityProviderConfiguration.getId());
        }

        return false;
    }

    @Override
    public Boolean getConfigAsBoolean(IdentityProviderConfiguration identityProviderConfiguration, SamlName samlName, Supplier<Boolean> defaultValueSupplier) {

        try {

            final Boolean value =  identityProviderConfiguration.containsOptionalProperty(samlName.getPropertyName())?
                    Boolean.parseBoolean((String) identityProviderConfiguration.getOptionalProperty(samlName.getPropertyName())):
                    defaultValueSupplier.get();

            Logger.debug(this,
                    ()->"Found " + samlName.getPropertyName() + " : " + ((value == null) ? "null" : value));

            return value;
        } catch (Exception e) {

            Logger.warn(this, "Cast exception on " + samlName.getPropertyName()
                    + " property. idpConfigId: " + identityProviderConfiguration.getId());
        }

        return false;
    }

    @Override
    public String[] getConfigAsArrayString(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {

        try {

            final String[] array = identityProviderConfiguration.containsOptionalProperty(samlName.getPropertyName())?
                    StringUtils.split((String) identityProviderConfiguration.getOptionalProperty(samlName.getPropertyName()), DotSamlConstants.ARRAY_SEPARATOR_CHAR):
                    this.getDefaultArrayStringParameter(samlName);

            Logger.debug(this, ()-> "Found " + samlName.getPropertyName() + " : " + ((array == null) ? "null" : array));

            return array;
        } catch (Exception e) {

            Logger.warn(this, "Cast exception on " + samlName.getPropertyName()
                    + " property. idpConfigId: " + identityProviderConfiguration.getId());
        }

        return null;
    }

    @Override
    public Integer getConfigAsInteger(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {

        try {

            final Integer value = identityProviderConfiguration.containsOptionalProperty(samlName.getPropertyName())?
                Integer.parseInt((String) identityProviderConfiguration.getOptionalProperty(samlName.getPropertyName())):
                this.getDefaultIntegerParameter(samlName);

            Logger.debug(this, ()-> "Found " + samlName.getPropertyName() + " : " + ((value == null) ? "null" : value));

            return value;
        } catch (Exception e) {

            Logger.warn(this, "Cast exception on " + samlName.getPropertyName()
                    + " property. idpConfigId: " + identityProviderConfiguration.getId());
        }

        return null;
    }

    ////////

    private Integer getDefaultIntegerParameter(final SamlName samlName) {

        if (samlName == null) {

            throw new DotSamlException("The 'getDefaultIntegerParameter' property is null");
        }

        if (this.defaultProperties.containsKey(samlName.getPropertyName())) {

            return Integer.parseInt(this.defaultProperties.get(samlName.getPropertyName()));
        }

        throw new DotSamlException(NOT_FOUND_ERROR + samlName.getPropertyName());
    }

    private String[] getDefaultArrayStringParameter(final SamlName samlName) {

        final String value = this.getDefaultStringParameter(samlName);
        if (value != null) {
            return StringUtils.split(value, DotSamlConstants.ARRAY_SEPARATOR_CHAR);
        }

        throw new DotSamlException(NOT_FOUND_ERROR + samlName.getPropertyName());
    }

    public String getDefaultStringParameter(final SamlName property) {

        if (property == null) {

            throw new DotSamlException("The 'getDefaultStringParameter' property is null");
        }

        if (this.defaultProperties.containsKey(property.getPropertyName())) {

            final String string = this.defaultProperties.get(property.getPropertyName());
            return NULL == string? null : string;
        }

        throw new DotSamlException(NOT_FOUND_ERROR + property.getPropertyName());
    }

    public  boolean getDefaultBooleanParameter(final SamlName samlName) {

        if (samlName == null) {

            throw new DotSamlException("The 'getDefaultBooleanParameter' property is null");
        }

        if (this.defaultProperties.containsKey(samlName.getPropertyName())) {

            return Boolean.parseBoolean(this.defaultProperties.get(samlName.getPropertyName()));
        }

        throw new DotSamlException(NOT_FOUND_ERROR + samlName.getPropertyName());
    }
}
