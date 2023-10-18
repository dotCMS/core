package com.dotcms.rendering.js.proxy;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates a {@link com.dotmarketing.portlets.containers.model.Container} in a Js context.
 * @author jsanca
 */
public class JsContainer implements Serializable, JsProxyObject<Container> {

	private final Container container;

	public JsContainer(final Container container) {
		this.container = container;
	}

	@HostAccess.Export
	public String getURI(final JsFolder folder) {
		return this.container.getURI(folder.getWrappedObject());
	}

	@HostAccess.Export
	public String getInode() {
		return this.container.getInode();
	}

	@HostAccess.Export
	/**
	 * @deprecated  As of release 3.0, see {@link ContainerStructure#getCode()}
	 *
	 * <p>Since 3.0, containers can have multiple structures related. To get the code for a particular Structure related
	 * to this container then use {@link ContainerStructure#getCode()}
	 */
	@Deprecated
	public String getCode() {
		return this.container.getCode();
	}

	@HostAccess.Export
	/**
	 * Returns the maxContentlets.
	 *
	 * @return int
	 */
	public int getMaxContentlets() {
		return this.container.getMaxContentlets();
	}

	@HostAccess.Export
	/**
	 * Returns the sortContentletsBy.
	 *
	 * @return String
	 */
	@JsonIgnore
	public String getSortContentletsBy() {
		return this.container.getSortContentletsBy();
	}

	@HostAccess.Export
	/**
	 * Returns the useDiv.
	 *
	 * @return boolean
	 */
	public boolean isUseDiv() {
		return this.container.isUseDiv();
	}


	@HostAccess.Export
	/**
	 * Returns the postLoop.
	 *
	 * @return String
	 */
	public String getPostLoop() {
		return this.container.getPostLoop();
	}

	@HostAccess.Export
	/**
	 * Returns the preLoop.
	 *
	 * @return String
	 */
	public String getPreLoop() {
		return this.container.getPreLoop();
	}

	@HostAccess.Export
	/**
	 * Returns the staticify.
	 *
	 * @return boolean
	 */
	public boolean isStaticify() {
		return this.container.isStaticify();
	}

	@HostAccess.Export
	/**
	 * Returns the staticify.
	 *
	 * @return boolean
	 */
	@JsonIgnore
	public boolean getStaticify() {
		return this.container.getStaticify();
	}

	@HostAccess.Export
	@JsonIgnore
	public String getLuceneQuery() {
		return this.container.getLuceneQuery();
	}

	@HostAccess.Export
	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.container.toString();
	}

	@HostAccess.Export
	public String getNotes() {
		return this.container.getNotes();
	}


	@Override
	public Container getWrappedObject() {
		return this.container;
	}
}
