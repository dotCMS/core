package com.liferay.util;

/**
 * Utility class used to obtain system environment variables
 * @author nollymar
 */
public class SystemEnvironmentProperties {

    /**
     * Returns the system environment value for the given variable name if it finds one
     * otherwise will try to prepend DOT_
     *
     * @param variableName {@link String}
     * @return
     */
    public String getVariable(final String variableName) {
        String variable = System.getProperty(variableName);
        if (variable != null) {
            return variable;
        }

        variable = System.getProperty("DOT_" + variableName);
        if (variable != null) {
            return variable;
        }

        variable = System.getenv(variableName);
        if (variable != null) {
            return variable;
        }

        return System.getenv("DOT_" + variableName);
    }

}
