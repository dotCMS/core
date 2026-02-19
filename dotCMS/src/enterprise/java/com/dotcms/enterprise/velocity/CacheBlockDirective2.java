/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.velocity;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PageMode;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

public class CacheBlockDirective2 extends DotDirective {

	private static final long serialVersionUID = 1L;

	public String getName() {
		return "dotcache";
	}

	public int getType() {
		return BLOCK;
	}
	

	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException,
			ResourceNotFoundException, ParseErrorException, MethodInvocationException {


		boolean noCache = false;
		boolean refreshCache = false;


		
		
		
		HttpServletRequest request = (HttpServletRequest) context.get("request");

		if (request != null && request.getParameter(getName()) != null) {
			noCache = (request.getParameter(getName()).equals("no"));
			refreshCache = (request.getParameter(getName()).equals("refresh"));
		}


		if (!allowExecution()) {
			noCache = true;
		} 
		if (PageMode.get(request).isAdmin) {
			noCache = true;
		}

		if (noCache) {
			node.jjtGetChild(2).render(context, writer);
			return true;
		}
		
		
        final String key = String.valueOf(node.jjtGetChild(0).value(context));
        final int ttl = (Integer) node.jjtGetChild(1).value(context);
        if (refreshCache) {
            CacheLocator.getBlockDirectiveCache().remove(key);
        }

        Map<String, Serializable> savedMap = new ConcurrentHashMap<>(CacheLocator.getBlockDirectiveCache().get(key));

		if (savedMap.isEmpty()) {
			// read and parse content in
			final StringWriter content = new StringWriter();
			
			DotCachedInternalContextAdapter localContext = new DotCachedInternalContextAdapter(context,savedMap);
			
			node.jjtGetChild(2).render(localContext, content);
			
			savedMap.put(BlockDirectiveCache.PAGE_CONTENT_KEY, content.toString());
			CacheLocator.getBlockDirectiveCache().add(key, savedMap, Duration.ofSeconds(ttl));
			
		}else {
		    savedMap.forEach(context::put);
		}
		writer.write((String)savedMap.get(BlockDirectiveCache.PAGE_CONTENT_KEY));
		

		return true;
	}
	@Override
	protected int[] getAllowedVersions() {
		return new int[]{LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level};
	}
}
