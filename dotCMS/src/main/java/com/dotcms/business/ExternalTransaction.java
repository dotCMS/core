package com.dotcms.business;

/**
 * A method annotated with ExternalTransaction annotation will handle the local transactional block
 * This means if there is already a transaction in the current thread, it will start a new one, keeping the current one open.
 * As soon as the external transaction is completed, the current transaction will be set to the current thread and continue with the
 * original transaction.
 * @author jsanca
 */
public @interface ExternalTransaction {
}
