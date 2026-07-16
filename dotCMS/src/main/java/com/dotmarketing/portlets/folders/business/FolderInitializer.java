package com.dotmarketing.portlets.folders.business;


import com.dotcms.config.DotInitializer;
import com.dotmarketing.startup.runonce.Task250604UpdateFolderInodes;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

public class FolderInitializer implements DotInitializer {


    @Override
    public void init() {
        Task250604UpdateFolderInodes  task = new Task250604UpdateFolderInodes();
        if(task.forceRun()){
            Try.run(task::executeUpgrade).onFailure(e-> Logger.error(FolderInitializer.class,e));
        }
    }
}
