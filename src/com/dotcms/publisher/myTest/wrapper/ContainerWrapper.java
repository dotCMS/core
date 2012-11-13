package com.dotcms.publisher.myTest.wrapper;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.containers.model.Container;

public class ContainerWrapper {
	private Identifier containerId;
	private Container container;
	
	public ContainerWrapper(Identifier containerId, Container container) {
		this.containerId = containerId;
		this.container = container;
	}

	public Identifier getContainerId() {
		return containerId;
	}

	public void setContainerId(Identifier containerId) {
		this.containerId = containerId;
	}

	public Container getContainer() {
		return container;
	}

	public void setContainer(Container container) {
		this.container = container;
	}
}
