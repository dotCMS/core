package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.startup.StartupTask;

public class Task00911CreateDefaultWorkflowSchema implements StartupTask {
    
    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            APILocator.getWorkflowAPI().createDefaultScheme();
        } catch (DotSecurityException e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }
    
}
