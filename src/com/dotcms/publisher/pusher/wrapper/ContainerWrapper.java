package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.containers.model.Container;

public class ContainerWrapper {
	private Identifier containerId;
	private Container container;
	private VersionInfo cvi;
	private Operation operation;
	
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

	/**
	 * @return the cvi
	 */
	public VersionInfo getCvi() {
		return cvi;
	}

	/**
	 * @param cvi the cvi to set
	 */
	public void setCvi(VersionInfo cvi) {
		this.cvi = cvi;
	}
	
	/**
	 * @return the operation
	 */
	public Operation getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
