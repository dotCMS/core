package com.dotcms.publishing;

import java.util.ArrayList;
import java.util.List;

public class PublishStatus {

	String message;
	
	List<BundlerStatus> bundlerStatuses = new ArrayList<BundlerStatus>();
	int totalWork=0;
	int progress = 0;
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}


	public List<BundlerStatus> getBundlerStatuses() {
		return bundlerStatuses;
	}
	public void setBundlerStatuses(List<BundlerStatus> bundlerStatuses) {
		this.bundlerStatuses = bundlerStatuses;
	}
	public int getTotalWork() {
		return totalWork;
	}
	public void setTotalWork(int totalWork) {
		this.totalWork = totalWork;
	}
	public int getProgress() {
		return progress;
	}
	public void setProgress(int progress) {
		this.progress = progress;
	}
	
	public void addToProgress(int add){
		this.progress+=add;
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
