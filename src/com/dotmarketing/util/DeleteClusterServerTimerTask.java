package com.dotmarketing.util;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

/*
 * Remove server from  cluster tables if the server doesn't have a license associated
 */
public class DeleteClusterServerTimerTask extends TimerTask {
    
	private String serverId;
	
	public DeleteClusterServerTimerTask(String serverId) {
		this.serverId = serverId;
	}
	
	@Override
	public void run() {
		try {
			boolean removeServer=true;
			for(Map<String, Object> lic : LicenseUtil.getLicenseRepoList()){
				if( serverId.equals((String)lic.get("serverid"))) {
					removeServer=false;
					break;
				}
			}
			if(removeServer){
				List<Server> servers = APILocator.getServerAPI().getAllServers();
				
				Server serverToDelete = null;
				for (Server server : servers) {
					if(server.getServerId().equals(serverId)){
						serverToDelete = server;
					}
				}
				
				if(serverToDelete != null && serverToDelete.getLastHeartBeat() != null){
					long timeOff = (new Date().getTime() - serverToDelete.getLastHeartBeat().getTime())/1000;
					if(timeOff >= Config.getIntProperty("HEARTBEAT_TIMEOUTHEARTBEAT_TIMEOUT",10)){
						APILocator.getServerAPI().removeServerFromCluster(serverId);
					}
				} else {
					Logger.error(DeleteClusterServerTimerTask.class, "Server or Heartbeat NOT FOUND, cancelling JOB");
				}
			}
			
		} catch (DotDataException e) {
			Logger.error(DeleteClusterServerTimerTask.class, e.getMessage(),e);
		} catch (IOException e) {
			Logger.error(DeleteClusterServerTimerTask.class, e.getMessage(),e);
		}finally{
			cancel();
		}
	}
}
