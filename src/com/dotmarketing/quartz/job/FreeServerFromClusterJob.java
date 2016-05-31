package com.dotmarketing.quartz.job;

import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This job will execute the below tasks after a server in the cluster reach the HEARTBEAT_TIMEOUT.
 *
 * - Frees the license: It will use LicenseUtil.freeLicenseOnRepo().
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
            boolean shouldExecute = inactiveServers.isEmpty() ? false : true;

            if (shouldExecute) {
                boolean shouldRewire = false;

                for (Server inactiveServer : inactiveServers) {
                    String serverID = inactiveServer.getServerId();

                    //Frees the license.
                    for (Map<String, Object> lic : LicenseUtil.getLicenseRepoList()) {

                        if (serverID.equals(lic.get("serverid"))) {
                            LicenseUtil.freeLicenseOnRepo((String) lic.get("serial"), serverID);
                            shouldRewire = true;
                            break;
                        }
                    }

                    if (shouldRewire) {
                        //Rewires the other nodes.
                        ClusterFactory.addNodeToCacheCluster(APILocator.getServerAPI().getCurrentServer());
                        ClusterFactory.addNodeToESCluster();
                    }

                }
            }

        } catch (DotDataException dotDataException) {
            Logger.error(FreeServerFromClusterJob.class, "Error trying to Free License", dotDataException);
        } catch (IOException iOException) {
            Logger.error(FreeServerFromClusterJob.class, "Error trying to get the License Repo List", iOException);
        } catch (Exception exception) {
            Logger.error(FreeServerFromClusterJob.class, "Error trying to Free Server from Cluster", exception);
        } finally {
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, e.getMessage(), e);
            }
            finally {
                DbConnectionFactory.closeConnection();
            }
        }
    }
}
