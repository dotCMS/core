package com.dotcms.autoupdater;

import java.io.IOException;

import com.dotcms.autoupdater.AntInvoker;
import com.dotcms.autoupdater.UpdateAgent;
import com.dotcms.autoupdater.UpdateException;

public class PostProcess {
	
	private String home;

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public boolean  postProcess(boolean clean){
		AntInvoker invoker=new AntInvoker(home);
		try {
			if (clean) {
				boolean ret=invoker.runTask("clean-plugins",null);
				if (!ret) {
					return false;
				}
			}
			return invoker.runTask("deploy-plugins",null);
			
		} catch (IOException e) {
			UpdateAgent.logger.fatal("IOException: " + e.getMessage(),e);
		}
		return false;
	}

	public boolean checkRequisites() throws UpdateException  {
		AntInvoker invoker=new AntInvoker(home);
		return invoker.checkRequisites();
	}
}
