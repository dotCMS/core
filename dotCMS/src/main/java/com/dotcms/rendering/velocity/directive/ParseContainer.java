package com.dotcms.rendering.velocity.directive;

import com.dotcms.rendering.velocity.VelocityType;

import com.dotmarketing.beans.MultiTree;

import java.io.Writer;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;

public class ParseContainer extends DotDirective {


	private static final long serialVersionUID = 1L;

	@Override
	public final String getName() {

		return "parseContainer";
	}

	public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException {
		super.init(rs, context, node);

	}

	@Override
	String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params,
			final String[] arguments) {
	    final String id = arguments[0];
        final String uid = (arguments.length>1) ? arguments[1] :  MultiTree.LEGACY_RELATION_TYPE;
        final String liveWorking = params.live ? "live" : "working";
        
		return "/" +liveWorking + "/" + id  + "/" + uid + "." + VelocityType.CONTAINER.fileExtension ;
		             
	}
}
