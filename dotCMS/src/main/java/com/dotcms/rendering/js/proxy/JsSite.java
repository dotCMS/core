package com.dotcms.rendering.js.proxy;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.liferay.portal.model.User;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates a {@link com.dotmarketing.beans.Host} in a Js context.
 * @author jsanca
 */
public class JsSite implements Serializable, JsProxyObject<Host> {

	private final Host site;

	public JsSite(final Host site) {
		this.site = site;
	}

	@Override
	public Host getWrappedObject() {
		return site;
	}

	@HostAccess.Export
	public String getInode() {
		return site.getInode();
	}

	@HostAccess.Export
	public String getName() {
		return this.site.getName();
	}

	@HostAccess.Export
	public boolean isParent() {
		return this.site.isParent();
	}

	@HostAccess.Export
	public Object getChildren(final User user,
									  final boolean live,
									  final boolean working,
									  final boolean archived,
									  final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		return JsProxyFactory.createProxy(this.site.getChildren(user, live, working, archived, respectFrontEndPermissions));
	}

	@HostAccess.Export
	@JsonIgnore
	public String getVersionType() {
		return this.site.getVersionType();
	}

	@HostAccess.Export
	public String getAliases() {
		return this.site.getAliases();
	}

	@HostAccess.Export
	public String getHostname() {
		return this.site.getHostname();
	}

	@HostAccess.Export
	public Object getHostThumbnail() {
		return JsProxyFactory.createProxy(this.site.getHostThumbnail());
	}

	@HostAccess.Export
	public boolean isDefault() {
		return this.site.isDefault();
	}

	@HostAccess.Export
	public String getStructureInode() {
		return this.site.getStructureInode();
	}

	@HostAccess.Export
	public boolean isSystemHost() {
		return this.site.isSystemHost();
	}

	@HostAccess.Export
	@JsonIgnore
	public Object getMap() {
		return JsProxyFactory.createProxy(this.site.getMap());
	}

	@HostAccess.Export
	public String getTagStorage() {
		return this.site.getTagStorage();
	}

	@HostAccess.Export
	@Override
	public String toString() {
		return this.site.toString();
	}
}
