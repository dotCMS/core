package com.dotcms.enterprise.velocity;

import com.dotcms.enterprise.license.LicenseLevel;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PageMode;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

public class CacheBlockDirective extends DotDirective {

	private static final long serialVersionUID = 1L;

	public String getName() {
		return "dotcache";
	}

	public int getType() {
		return BLOCK;
	}
	

	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException,
			ResourceNotFoundException, ParseErrorException, MethodInvocationException {


		boolean _noCache = false;
		boolean _refreshCache = false;


		
		
		
		HttpServletRequest request = (HttpServletRequest) context.get("request");

		if (request != null && request.getParameter(getName()) != null) {
			_noCache = (request.getParameter(getName()).equals("no"));
			_refreshCache = (request.getParameter(getName()).equals("refresh"));
		}


		if (!allowExecution()) {
			_noCache = true;
		} else if (PageMode.get(request).isAdmin) {
			_noCache = true;
		}

		if (_noCache) {
			node.jjtGetChild(2).render(context, writer);
			return true;
		}
		
		
		String key = String.valueOf(node.jjtGetChild(0).value(context));
		int ttl = (Integer) node.jjtGetChild(1).value(context);
		
		String x = null;
		if (_refreshCache) {
			CacheLocator.getBlockDirectiveCache().remove(key);
		}
		else{
			x = CacheLocator.getBlockDirectiveCache().get(key, ttl);
		}
		if (x == null) {
			// read and parse content in
			StringWriter content = new StringWriter();
			node.jjtGetChild(2).render(context, content);
			x = content.toString();
			content = null;
			CacheLocator.getBlockDirectiveCache().add(key, x, ttl);
		}

		try {
			writer.write(x);
		} catch (Exception e) {
			throw new DotRuntimeException(this.getClass()  + " failed", e);
		}

		return true;
	}
	@Override
	protected int[] getAllowedVersions() {
		return new int[]{LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level};
	}
}