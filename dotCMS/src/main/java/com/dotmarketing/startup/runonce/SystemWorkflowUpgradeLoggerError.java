package com.dotmarketing.startup.runonce;

import com.dotmarketing.util.Logger;

public class SystemWorkflowUpgradeLoggerError {

    public void logError (final String message) {

        Logger.error(this, message);
    }

    public void logError (final String message, final Throwable e) {

        Logger.error(this, message, e);
    }
}
