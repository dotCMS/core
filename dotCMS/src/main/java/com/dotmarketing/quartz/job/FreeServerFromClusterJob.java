package com.dotmarketing.quartz.job;

import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.DotLicenseRepoEntry;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
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

        try {
            List<Server> inactiveServers = APILocator.getServerAPI().getInactiveServers();
            boolean shouldExecute = !inactiveServers.isEmpty();

            if (shouldExecute) {
                boolean shouldRewire = false;

                for (Server inactiveServer : inactiveServers) {
                    shouldRewire = freeLicense(inactiveServer.getServerId());
                    removeServerFromClusterTable(inactiveServer.getServerId());
                }

                if (shouldRewire) {
                    ClusterFactory.rewireCluster();
                }
            }
        } catch (DotDataException dotDataException) {
            Logger.error(FreeServerFromClusterJob.class,
                    "Error trying to Free License or Clean Cluster Tables", dotDataException);
        } catch (IOException iOException) {
            Logger.error(FreeServerFromClusterJob.class,
                    "Error trying to get the License Repo List", iOException);
        } catch (Exception exception) {
            Logger.error(FreeServerFromClusterJob.class,
                    "Error trying to Free Server from Cluster", exception);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Removes the specified license serial number from a specific server.
     *
     */
    private boolean freeLicense(String serverID)
            throws IOException, DotDataException {

        boolean needsRewire = false;

        for (DotLicenseRepoEntry lic : LicenseUtil.getLicenseRepoList()) {
            if (serverID.equals(lic.serverId)) {
                LicenseUtil.freeLicenseOnRepo(lic.dotLicense.serial, serverID);
                needsRewire = true;
                break;
            }
        }
        return needsRewire;
    }

    /**
     * Clean up the DB tables(cluster_server_uptime, cluster_server).
     *
     */
    private void removeServerFromClusterTable( String serverID )
            throws DotDataException {

        Logger.info(this, String.format("Server %s was Removed", serverID));
        APILocator.getServerAPI().removeServerFromClusterTable(serverID);
    }
}
