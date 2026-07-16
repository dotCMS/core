package com.dotmarketing.quartz.job;

import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.DotLicenseRepoEntry;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * This job will execute the below tasks after a server in the cluster reach the HEARTBEAT_TIMEOUT.
 *
 * - Frees the license: It will use LicenseUtil.freeLicenseOnRepo().
 * - Clean up the DB tables(cluster_server_uptime, cluster_server).
 * - Updates the replica Count: Only if the license was free successfully.
 * - Rewires the other nodes: Only if the license was free successfully.
 *
 * By default this job will run every minute: HEARTBEAT_CRON_EXPRESSION=0 0/1 * * * ?
 *
 * Created by Oscar Arrieta on 5/18/16.
 */
public class FreeServerFromClusterJob implements StatefulJob {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // Skip execution if shutdown is in progress to prevent component reinitialization
        if (com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
            Logger.info(this.getClass(), "Shutdown in progress - skipping FreeServerFromClusterJob execution");
            return;
        }
        
        try {
            List<Server> inactiveServers = APILocator.getServerAPI().getInactiveServers();

            if (!inactiveServers.isEmpty()) {
                inactiveServers.forEach(this::removeServerFromClusterTables);
                ClusterFactory.rewireClusterIfNeeded();
            }
        } catch (DotDataException dotDataException) {
            Logger.error(FreeServerFromClusterJob.class,
                    "Error trying to Free License or Clean Cluster Tables", dotDataException);
        } catch (Exception exception) {
            Logger.error(FreeServerFromClusterJob.class,
                    "Error trying to Free Server from Cluster", exception);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Removes the specified license serial number from a specific server.
     * @throws Exception
     *
     */
    private boolean freeLicense(final String inactiveServerID)
            throws Exception {

        boolean needsRewire = false;

        for (DotLicenseRepoEntry lic : LicenseUtil.getLicenseRepoList()) {
            if (inactiveServerID.equals(lic.serverId)) {
            	if(APILocator.getServerAPI().readServerId().equals(inactiveServerID)){
            		LicenseUtil.pickLicense(lic.dotLicense.serial);
            	}
            	else{
	                LicenseUtil.freeLicenseOnRepo(lic.dotLicense.serial, inactiveServerID);
	                needsRewire = true;
	                break;
            	}
            }
        }
        return needsRewire;
    }

    /**
     * Clean up the DB tables(cluster_server_uptime, cluster_server).
     *
     */
    private void removeServerFromClusterTables(Server server) {

        try {
            Logger.info(this, String
                .format("Server %s with license %s was Removed", server.getServerId(), server.getLicenseSerial()));
            APILocator.getServerAPI().removeServerFromClusterTable(server.getServerId());

            LicenseUtil.freeLicenseOnRepo(server.getLicenseSerial(), server.getServerId());
        } catch(DotDataException e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }
}
