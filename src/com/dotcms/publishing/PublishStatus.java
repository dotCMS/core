package com.dotcms.publishing;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.quartz.TaskRuntimeValues;

public class PublishStatus extends TaskRuntimeValues {


	List<BundlerStatus> bundlerStatuses = new ArrayList<BundlerStatus>();




	public List<BundlerStatus> getBundlerStatuses() {
		return bundlerStatuses;
	}
	public void setBundlerStatuses(List<BundlerStatus> bundlerStatuses) {
		this.bundlerStatuses = bundlerStatuses;
	}

	
	public void addToBs(BundlerStatus bs){
		this.bundlerStatuses.add(bs);
	}
	
	public int getTotalBundleWork() {
		int x=0;
		for(BundlerStatus status:bundlerStatuses){
			x+=status.getTotal();
			
		}

		return x;
	}
	public int getCurrentBundleWork() {
		int x=0;
		for(BundlerStatus status:bundlerStatuses){
			x+=status.getCount();
			
		}

		return x;
	}
	public int getBundleErrors() {
		int x=0;
		for(BundlerStatus status:bundlerStatuses){
			x+=status.getFailures();
			
		}

		return x;
	}
}
