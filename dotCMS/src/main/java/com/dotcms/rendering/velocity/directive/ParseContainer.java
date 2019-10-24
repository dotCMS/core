package com.dotcms.rendering.velocity.directive;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;
import org.jetbrains.annotations.NotNull;

import java.io.Writer;
import java.util.Arrays;
import java.util.Optional;
/**
 * Could includes a container by id (long or shorty) or a path (with or without host "without will use the current host").
 */
public class ParseContainer extends DotDirective {

	public  static final String PARSE_CONTAINER_UUID_PREFIX = "dotParser_";
	public  static final String DEFAULT_UUID_VALUE = MultiTree.LEGACY_RELATION_TYPE;
	private static final long serialVersionUID     = 1L;

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

		final String[] parserContainersArgument = getDotParserContainerArguments(arguments);

		final TemplatePathStrategyResolver templatePathResolver = TemplatePathStrategyResolver.getInstance();
		final Optional<TemplatePathStrategy> strategy           = templatePathResolver.get(context, params, parserContainersArgument);

		return strategy.isPresent()?
				strategy.get().apply(context, params, parserContainersArgument):
				templatePathResolver.getDefaultStrategy().apply(context, params, parserContainersArgument);
	}

	@NotNull
	private String[] getDotParserContainerArguments(final String[] arguments) {
		final String[] parserContainersArgument = Arrays.copyOf(arguments, 2);

		if (arguments.length < 3 || !Boolean.valueOf(arguments[2])) {
			parserContainersArgument[1] = PARSE_CONTAINER_UUID_PREFIX + (arguments.length > 1
					? arguments[1] : MultiTree.LEGACY_RELATION_TYPE);
		}

		return parserContainersArgument;
	}

	public static boolean isParserContainerUUID(final String uuid) {
		return uuid != null && uuid.startsWith(PARSE_CONTAINER_UUID_PREFIX);
	}
}
