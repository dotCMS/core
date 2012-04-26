package com.dotmarketing.quartz;

import java.util.LinkedList;
import java.util.List;

public class TaskRuntimeValues {

	protected int currentProgress = 0;
	protected int startProgress = 0;
	protected int endProgress = 100;
	protected List<String> messages = new LinkedList<String>();
	public int getCurrentProgress() {
		return currentProgress;
	}
	public void setCurrentProgress(int currentProgress) {
		this.currentProgress = currentProgress;
	}
	public int getStartProgress() {
		return startProgress;
	}
	public void setStartProgress(int startProgress) {
		this.startProgress = startProgress;
	}
	public int getEndProgress() {
		return endProgress;
	}
	public void setEndProgress(int endProgress) {
		this.endProgress = endProgress;
	}
	public List<String> getMessages() {
		return messages;
	}
	public void setMessages(List<String> messages) {
		this.messages = messages;
	}
	
	
	
	
	
}