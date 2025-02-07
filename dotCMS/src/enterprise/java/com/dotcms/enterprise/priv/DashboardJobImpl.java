/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import java.util.Date;

import com.dotcms.enterprise.license.LicenseLevel;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.enterprise.ParentProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.dashboard.business.DashboardAPI;
import com.dotmarketing.util.Logger;

public class DashboardJobImpl extends ParentProxy implements Runnable, StatefulJob {

    private DashboardAPI dashboardAPI = APILocator.getDashboardAPI();

    public void run() {

        Logger.debug(this, "Running DashboardJobImpl - " + new Date());

        try {
            if ( allowExecution() ) {
                dashboardAPI.populateAnalyticSummaryTables();
            }
        } catch (Exception e) {
            Logger.error(this, "Error ocurred while trying to populate analytic summary tables", e);
        } finally {

            DbConnectionFactory.closeSilently();
        }

    }

    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        run();
    }

	@Override
	protected int[] getAllowedVersions() {
        return new int[]{LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
                LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level};
    }



}
