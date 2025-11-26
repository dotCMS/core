/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import com.dotcms.enterprise.priv.DashboardDataGeneratorImpl;
import com.dotcms.enterprise.priv.DashboardFactoryImpl;
import com.dotcms.enterprise.priv.DashboardJobImpl;
import com.dotmarketing.portlets.dashboard.business.DashboardDataGenerator;
import com.dotmarketing.portlets.dashboard.business.DashboardFactory;

public class DashboardProxy extends ParentProxy{

	public static DashboardFactory getDashboardFactory(){
		return new DashboardFactoryImpl();
	}
	
	public static DashboardJobImpl getDashboardJob(){
		return new DashboardJobImpl();
	}
	
	public static DashboardDataGenerator getDashboardDataGenerator(int monthFrom, int yearFrom, int monthTo, int yearTo){
		return new DashboardDataGeneratorImpl(monthFrom, yearFrom, monthTo, yearTo);		
	}
	
	public static Class getDashboardJobImplClass(){
		return DashboardJobImpl.class;
	}
}
