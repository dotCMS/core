/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.sitesearch;

import java.util.List;

import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.PublishStatus;

public class SiteSearchPublishStatus extends PublishStatus {

	@Override
	public int getEndProgress() {
		int x=0;
		List<BundlerStatus> list  = getBundlerStatuses();
		for(BundlerStatus bs : list){
			x+=bs.getCount();
			
		}

		return x;
		
		
		
		
	}

	
	
	
	
	
	
}
