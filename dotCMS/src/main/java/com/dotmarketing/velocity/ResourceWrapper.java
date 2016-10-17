package com.dotmarketing.velocity;

import java.io.Serializable;

import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.resource.Resource;

public class ResourceWrapper implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2833599158288791699L;
	
	private Resource resource = null;
	
	public ResourceWrapper(Resource resource) {
		this.resource = resource;
	}
	
    public Resource getResource() {
		return resource;
	}
}
