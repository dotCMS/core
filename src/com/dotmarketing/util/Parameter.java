package com.dotmarketing.util;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Jtesser
 */
public class Parameter {
  /**
   * Returns a long value based on a String parameter.  If the value cannot be parsed, zero is returned.
   * @param parameter String representation of a long value
   * @return long value
   */
  public static long getLong(String parameter) {
    try {
      return Long.parseLong(parameter);
    } catch (Exception ex) {
      return 0;
    }
  }
  
  /**
   * Returns a long value based on a String parameter and a fail-safe value.  If the value cannot be parsed, defaultValue is returned.
   * @param parameter String representation of a long value
   * @param defaultValue long value to return if parameter cannot be parsed
   * @return long value
   */
  public static long getLong(String parameter, long defaultValue) {
    try {
      return Long.parseLong(parameter);
    } catch (Exception ex) {
      return defaultValue;
    }
  }
  
  /**
   * @deprecated
   * Returns true if parameter is not null and false if parameter is null.
   * @param parameter String value to test.
   * @return boolean value
   */
  public static boolean getBool(String parameter) {
    return parameter != null;
  }
  
  /**
   * @param parameter String value to test.
   * @return true if parameter is not null and false if parameter is null.
   */
  public static boolean isNotNull(String parameter) {
    return parameter != null;
  }
  
  /**
   * @param paramter String value to extract.
   * @return true if parameter == "true" else returns false
   */
  public static boolean getBooleanFromString(String paramter) {
    return getBooleanFromString(paramter, false);
  }
  
  /**
   * @param parameter String value to extract.
   * @param defaultValue boolean fall-through value
   * @return true if parameter is "true", false if parameter is "false", defaultValue otherwise
   */
  public static boolean getBooleanFromString(String parameter, boolean defaultValue) {
    if (parameter == null) {
      return defaultValue;
    } else if (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("t") || parameter.equalsIgnoreCase("1")) {
      return true;
    } else if (parameter.equalsIgnoreCase("false") || parameter.equalsIgnoreCase("f") || parameter.equalsIgnoreCase("0")){
      return false;
    } else {
      return defaultValue;
    }
  }
  
  /**
   * Returns an int value based on a String parameter.  If the value cannot be parsed, zero is returned.
   * @param parameter String representation of an int value
   * @return int value
   */
  public static int getInt(String parameter) {
    try {
      return Integer.parseInt(parameter);
    } catch (Exception ex) {
      return 0;
    }
  }
  
  public static int getInt(String parameter, HttpServletRequest req) {
    return getInt(req.getParameter(parameter));
  }
  
  /**
   * Returns an int value based on a String parameter and a fail-safe value.  If the value cannot be parsed, defaultValue is returned.
   * @param parameter String representation of an int value
   * @param defaultValue int value to return if parameter cannot be parsed
   * @return int value
   */
  public static int getInt(String parameter, int defaultValue) {
    try {
      return Integer.parseInt(parameter);
    } catch (Exception ex) {
      return defaultValue;
    }
  }
  
  public static int getInt(String parameter, int defaultValue, HttpServletRequest req) {
    return getInt(req.getParameter(parameter), defaultValue);
  }
  
  /**
   * Returns empty String if parameter is null or parameter if parameter is not null.
   * @param parameter String parameter to test
   * @return a non-null String
   */
  public static String getString(String parameter) {
    return (parameter == null) ? "" : parameter;
  }
  
  /**
   * @param parameter String parameter to test
   * @param defaultValue String to use if parameter is null
   * @return defaultValue if parameter is null or parameter if parameter is not null
   */
  public static String getString(String parameter, String defaultValue) {
    return (parameter == null) ? defaultValue : parameter;
  }
  
  /**
   * Returns 0 if parameter cannot be parsed or parameter is parameter can be parsed.
   * @param parameter String parameter to test
   * @return float value
   */
  public static float getFloat(String parameter) {
    try {
      return Float.parseFloat(parameter);
    } catch (Exception ex) {
      return 0;
    }
  }
  
  /**
   * Returns defaultValue if parameter cannot be parsed or parameter is parameter can be parsed.
   * @param parameter String parameter to test
   * @param defaultValue float value to return if parameter cannot be parsed
   * @return float value
   */
  public static float getFloat(String parameter, float defaultValue) {
    try {
      return Float.parseFloat(parameter);
    } catch (Exception ex) {
      return defaultValue;
    }
  }
  
  /**
   * Returns 0 if parameter cannot be parsed or parameter is parameter can be parsed.
   * @param parameter String parameter to test
   * @return double value
   */
  public static double getDouble(String parameter) {
    try {
      return Double.parseDouble(parameter);
    } catch (Exception ex) {
      return 0;
    }
  }
  
  /**
   * Returns defaultValue if parameter cannot be parsed or parameter is parameter can be parsed.
   * @param parameter String parameter to test
   * @param defaultValue double value to return if parameter cannot be parsed
   * @return double value
   */
  public static double getDouble(String parameter, double defaultValue) {
    try {
      return Double.parseDouble(parameter);
    } catch (Exception ex) {
      return defaultValue;
    }
  }
}

