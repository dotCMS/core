package com.dotcms.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotated with LogTime annotation will measure the execution time and log it If the
 * right appender is allowed
 *
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogTime {
  String loggingLevel() default "DEBUG";
} // E:O:F:LogTime
