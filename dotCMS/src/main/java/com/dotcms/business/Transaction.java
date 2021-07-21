package com.dotcms.business;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotated with Transaction annotation will handle the new transactional block
 * A transaction will be started in the current thread,
 * if previous transaction was already in place will do the transaction with a new connection,
 * after any result will set to the current thread the previous connection to continue the transaction started before.
 *
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Transaction {


} // E:O:F:WrapInTransaction
