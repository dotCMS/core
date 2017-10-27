package com.dotmarketing.velocity.directive;

import java.io.Writer;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;

import com.dotmarketing.util.Config;

public class ParseContainer extends DotDirective {

	final static String EXTENSION = Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION", "container");

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
			final String argument) {
		if (argument.contains("/")) {
			
		}else{
		
			return (params.live) ? "/live/" + argument + "." + EXTENSION : "/working/" + argument + "." + EXTENSION;
		}
	}

}
