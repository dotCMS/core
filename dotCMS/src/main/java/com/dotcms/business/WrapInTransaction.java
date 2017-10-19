package com.dotcms.business;

import java.lang.annotation.*;

/**
 * A method annotated with WrapInTransaction annotation will handle the local transactional block
 * if a transaction has been not started in the current thread, it will be started automatically otherwise will reuse the same.
 * In addition if a new transaction is started, the connection will be automatically closed at the end of the transaction.
 *
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WrapInTransaction {


} // E:O:F:WrapInTransaction