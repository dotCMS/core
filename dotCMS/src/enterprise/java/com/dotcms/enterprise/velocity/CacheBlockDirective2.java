/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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