package com.liferay.util;

/**
 * Utility class used to obtain system environment variables
 * @author nollymar
 */
public class SystemEnvironmentProperties {

    /**
     * Returns the system environment value for the given variable name
     * @param variableName {@link String}
     * @return
     */
    public String getVariable(final String variableName){
        return System.getenv(variableName);
    }

}
