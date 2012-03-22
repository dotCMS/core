package com.dotcms.autoupdater;

import com.dotcms.autoupdater.ActivityIndicator;
import com.dotcms.autoupdater.UpdateAgent;

public class ActivityIndicator extends Thread {
	private  String[] progressIndicators= { "-" , "\\" , "|" , "/" };
	private  int i=0;
	
	public  void printActivity () {
		if (!UpdateAgent.isDebug) {
			System.out.print("\r" + progressIndicators[i]);
			i++;
			if (i>=progressIndicators.length) {
				i=0;
			}
		}
	}
	
	public  void cleanLine() {
		if (!UpdateAgent.isDebug) {
			System.out.print("\r");
		}
	}
	
	
	
	private boolean done;
	private int wait= 200;
	
	public void run() {
		
		while (!done) {
			printActivity();
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				
			}
		}
		cleanLine();
		
	}
	
	public void finish() {
		done=true;
	}
	
	private static ActivityIndicator instance;
	
	public synchronized static void startIndicator() {
		
		if (!UpdateAgent.isDebug && instance==null) {
		
			instance=new ActivityIndicator();
			instance.start();
		
		}


	}
	
	public synchronized static void endIndicator(){
		if (instance==null || UpdateAgent.isDebug) {
			return;
		}
		instance.finish();
		try {
			instance.join();
		} catch (InterruptedException e) {
		
		}
		instance=null;
	}
}
