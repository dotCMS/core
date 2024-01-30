package com.dotcms.rendering.js.proxy;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates a {@link Treeable} in a Js context.
 * @author jsanca
 */
public class JsTreeable implements Serializable, JsProxyObject<Treeable> {

	private final Treeable treeable;

	public JsTreeable(final Treeable treeable) {
		this.treeable = treeable;
	}

	@Override
	public Treeable getWrappedObject() {
		return this.treeable;
	}

	@HostAccess.Export
	public String getInode() {
		return this.treeable.getInode();
	}

	@HostAccess.Export
	public String getIdentifier() {
		return this.treeable.getIdentifier();
	}

	@HostAccess.Export
	public String getType() {
		return this.treeable.getType();
	}

	@HostAccess.Export
	public Date getModDate() {
		return this.treeable.getModDate();
	}

	@HostAccess.Export
	public String getName() {
		return this.treeable.getName();
	}

	@HostAccess.Export
	public Object getMap() throws DotStateException, DotDataException, DotSecurityException {
		return JsProxyFactory.createProxy(this.treeable.getMap());
	}

}
