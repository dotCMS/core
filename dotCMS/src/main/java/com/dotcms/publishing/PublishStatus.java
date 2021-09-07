package com.dotcms.publishing;

import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.publishing.output.BundleOutput;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.dotmarketing.quartz.TaskRuntimeValues;
import java.util.Set;
import java.util.stream.Collectors;

public class PublishStatus extends TaskRuntimeValues {


	private List<BundleOutput> outputs;

	public PublishStatus() {
		super();
		outputs = new ArrayList<>();

	}
	List<BundlerStatus> bundlerStatuses = new ArrayList<BundlerStatus>();
	Date lastRun;



	public Date getLastRun() {
		return lastRun;
	}
	public void setLastRun(Date lastRun) {
		this.lastRun = lastRun;
	}
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

	public void addOutput(BundleOutput output) {
		outputs.add(output);
	}

	public List<File> getOutputFiles() {
		return outputs.stream().map(output -> output.getFile()).collect(Collectors.toList());
	}
}
