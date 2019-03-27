package com.dotmarketing.business.cache.provider.hazelcast;

import com.dotcms.cluster.business.HazelcastUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.hazelcast.core.HazelcastInstance;

/**
 * Created by jasontesser on 6/28/17.
 */
public class ReinitializeHazelThread implements Runnable{

    private HazelcastUtil.HazelcastInstanceType hazelInstanceType;
    private AbstractHazelcastCacheProvider hazelProvider;
    private long hazelReconectWaitTime;


    public ReinitializeHazelThread(AbstractHazelcastCacheProvider hazelProvider,HazelcastUtil.HazelcastInstanceType hazelInstanceType){
        this.hazelInstanceType = hazelInstanceType;
        this.hazelProvider = hazelProvider;
        hazelReconectWaitTime = Config.getLongProperty("HAZEL_RECONNECT_SLEEP_TIME", 30000);
    }

    @Override
    public void run() {
        while(hazelProvider.isRecovering()) {
            HazelcastInstance haz = HazelcastUtil.getInstance().getHazel(hazelInstanceType, true);
            try {
                haz.getMap("dotTest").set("test", "test");
                hazelProvider.setRecovering(false);
            }catch (Exception hce){
                Logger.warn(this, "HazelCast is still down trying to connect again");
                Logger.debug(this, hce.getMessage(),hce);
            }
            try {
                Thread.sleep(hazelReconectWaitTime);
            } catch (InterruptedException e) {
                Logger.error(this,"unable to sleep", e);
            }
        }
    }

}
