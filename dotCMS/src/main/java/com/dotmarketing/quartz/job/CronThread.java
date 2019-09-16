package com.dotmarketing.quartz.job;

import com.dotmarketing.util.Logger;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class does not execute anything and should be removed in a major release
 */
@Deprecated
public class CronThread implements Runnable {
    private int emailCampaign = 1; //every minute

    public CronThread() {
        //super(name);
    }

    public void run() {
	    Logger.info(this, "Starting annoying CronThread running every minute");
        
        GregorianCalendar greg = null;



        //Fire up the cron thread
        while (true) {
            greg = new GregorianCalendar();

            try {
                if ((greg.get(Calendar.MINUTE) % emailCampaign) == 0) {
                    //EmailFactory.deliverCampaigns();
                }
            } catch (Exception e) {
                Logger.error(this, "CronThread: Error occurred delivering campaigns.", e);
            }

    	    Logger.debug(this, "CronThread.Minute: " + greg.get(Calendar.MINUTE));

            try {
                Thread.sleep(1000 * 60); // sleep a minute
            } catch (Exception e) {
                break;
            }
        }
    }
    
  
    /* (non-Javadoc)
     * @see java.lang.Thread#destroy()
     */
    public void destroy() {

    }
}
