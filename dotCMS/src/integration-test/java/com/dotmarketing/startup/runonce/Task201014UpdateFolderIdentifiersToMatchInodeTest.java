package com.dotmarketing.startup.runonce;

import static org.junit.Assert.*;
import org.junit.Test;
import com.dotcms.datagen.FolderDataGen;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;

public class Task201014UpdateFolderIdentifiersToMatchInodeTest {

    @Test
    public void test() throws DotDataException, DotRuntimeException {

        
        Folder folder = new FolderDataGen().nextPersisted();
        
        assertTrue(folder.getIdentifier().equals(folder.getInode()));
        
        
        Task201014UpdateFolderIdentifiersToMatchInode task = new Task201014UpdateFolderIdentifiersToMatchInode();
        task.executeUpgrade();
        assertTrue("No exception has been thrown", true);
        
        
        

        
        
        
        
        
    }

}
