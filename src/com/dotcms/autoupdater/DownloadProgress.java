package com.dotcms.autoupdater;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.dotcms.autoupdater.Messages;

public class DownloadProgress {
	
	public DownloadProgress (long length) {
		this.length=length;
		nf=new DecimalFormat("#,#00.00"); //$NON-NLS-1$
		
	}

	NumberFormat nf;
	int maxMessageLength=0;
	long kcount=0;
	long length;
	
	public String getProgressMessage(int count , long startTime, long currentTime) {
		long diff=count-kcount;
		long speed=0;
		if ((currentTime-startTime)!=0) {
			speed=(diff/(currentTime-startTime))*1000; // in b/s
		}
		speed/=1024; // in Kb/s
		startTime=currentTime;
		
		kcount=count;
		String message=kcount/1024+" kB "+Messages.getString("DownloadProgress.text.downloaded"); //$NON-NLS-1$ //$NON-NLS-2$
		if (length>0) {
			//Calculate percent done.
			float percent=((new Float(kcount)*100f)/new Float(length));
			message+=", " + nf.format(percent) + "% " + Messages.getString("DownloadProgress.text.done"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
		}
		message += " (" + speed + " kB/s"; //$NON-NLS-1$ //$NON-NLS-2$
		if (length>0) {
			String timeString="--";//$NON-NLS-1$ 
			if (speed!=0) {
				long left =((length-kcount) / 1024)/speed ;
				timeString=left+"";//$NON-NLS-1$ 
				
			} 
			message+=", " + timeString + " " +Messages.getString("DownloadProgress.text.seconds.left");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		message +=")"; //$NON-NLS-1$
		
		if (message.length()<maxMessageLength) {
			//Pad the message								
			for (int j=0;j<maxMessageLength-message.length();j++) {
				message+=" "; //$NON-NLS-1$
			}
		} else {
			maxMessageLength=message.length();
		}
		return message;
	}
	
}
