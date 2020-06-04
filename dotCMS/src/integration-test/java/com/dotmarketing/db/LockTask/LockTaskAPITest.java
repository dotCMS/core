package com.dotmarketing.db.LockTask;

import static org.junit.Assert.*;
import java.util.Date;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;

public class LockTaskAPITest {
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }
    
    private LockTaskAPI locks = LockTaskAPI.getInstance();
    
    private final String SERVER2 = "SERVER2";
    private LockTaskAPI locks2 = new LockTaskAPI(SERVER2);
    
    private String TASK1 = "LOCK_TASK1";
    
    private String TASK2 = "LOCK_TASK2";
    
    private String TASK3 = "LOCK_TASK3";
    

    
    @Test
    public void test() {
        
        Optional<LockTask> taskOpt  =locks.lockTask(TASK1, 10);
        
        LockTask task = taskOpt.get();
        assertEquals(task.serverId, APILocator.getServerAPI().readServerId());
        assert(task.lockedUntil.after(new Date()));
        assertEquals(task.task, TASK1);
        
        
    }
    
    
    

}
