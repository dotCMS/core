package com.dotmarketing.portlets.chains.chainslinks;

import java.util.Map;
import java.util.Map.Entry;

import com.dotmarketing.portlets.chains.ChainControl;
import com.dotmarketing.portlets.chains.ChainLink;
import com.dotmarketing.portlets.chains.ChainLinkParameter;

public class SimpleChainLink extends ChainLink {

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public ChainLinkParameter[] getParameters() {
		return null;
	}

	@Override
	public String[] getRequiredLinks() {
		return null;
	}

	@Override
	public String getTitle() {
		return "Print out all parameters";
	}

	@Override
	public boolean run() {
		
		ChainControl control = getChainData();
		Map<String, Object> properties = control.getChainProperties();
		for(Entry<String, Object> entry : properties.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue() + "\\n");
		}
		
		return true;
		
	}

}

