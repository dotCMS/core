package com.dotcms.publishing.sitesearch;

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
