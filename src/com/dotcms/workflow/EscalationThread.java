package com.dotcms.workflow;

import com.dotmarketing.util.Config;

public class EscalationThread extends Thread {
    private static final EscalationThread instance=new EscalationThread();
    
    public static EscalationThread getInstace() {
        return instance;
    }
    
    private EscalationThread() {}
    
    @Override
    public void run() {
        if(Config.getBooleanProperty("ESCALATION_ENABLE",false)) {
            int interval=Config.getIntProperty("ESCALATION_CHECK_INTERVAL_SECS",120);
            while(true) {
                try {
                    
                    
                    
                    Thread.sleep(interval*1000L);
                }
                catch(InterruptedException ex) {
                    return;
                }
            }
        }
    }
}
