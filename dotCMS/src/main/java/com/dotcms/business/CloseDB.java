package com.dotcms.business;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotated with CloseDB annotation will close resources in the current thread if needed,
 * such as database connections...
 *
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CloseDB {

  /**
   * By default in true, set to false if you do not want to close the connection hold on the current
   * thread
   *
   * @return boolean
   */
  boolean connection() default true;
} // E:O:F:LogTime
