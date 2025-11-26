package com.dotcms.rendering.velocity.directive;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.context.Context;
import org.jetbrains.annotations.NotNull;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Could includes a container by id (long or shorty) or a path (with or without host "without will use the current host").
 */
public class ParseContainer extends DotDirective {

	public static final String PARSE_CONTAINER_UUID_PREFIX = "dotParser_";
	public static final String DEFAULT_UUID_VALUE = MultiTree.LEGACY_RELATION_TYPE;
	public static final String DOT_TEMPLATE_CONTAINER_IDS = "_dotTemplateContainerIds";
	public static final String DONT_LOAD_CONTAINERS = "_dotDontLoadContainers";

	private static final long serialVersionUID     = 1L;

	public static String getDotParserContainerUUID(final String uniqueId) {
		final String parserContainerUUID;

		if (uniqueId == null) {
			parserContainerUUID = ContainerUUID.UUID_START_VALUE;
		} else {
			parserContainerUUID = ContainerUUID.UUID_LEGACY_VALUE.equals(uniqueId) ? ContainerUUID.UUID_START_VALUE : uniqueId;
		}

		return PARSE_CONTAINER_UUID_PREFIX + parserContainerUUID;
	}

    @Override
	public final String getName() {
		return "parseContainer";
	}

	/**
	 * Returns if the template should be loaded and rendered based on the context,
	 * by checking the DONT_LOAD_CONTAINERS flag.
	 *
	 * @param context The context in which the directive is executed
	 * @param arguments The arguments passed to the directive,
	 * @return true if the template should be loaded and rendered, else otherwise.
	 */
	@Override
	boolean shouldLoadAndRenderTemplate(final Context context, final String[] arguments) {
		return !getDontLoadContainers(context);
	}

	/**
	 * Stores the list of container IDs collected during the rendering process.
	 *
	 * @param render    The rendered output (not used in this method)
	 * @param arguments The arguments passed to the directive,
	 *                  where the first argument is expected to be the container identifier
	 *                  and the second argument is expected to be the container UUID
	 *                  (if not present, the default UUID value is used).
	 * @param context   The context in which the directive is executed,
	 */
	@Override
	public void afterRender(final String render, final String[] arguments, final Context context) {

		final Collection<Pair<String,String>> containerIds = getContainerIdsFromContext(context);

		if (arguments.length > 0 && UtilMethods.isSet(arguments[0])) {
			final String containerId = arguments[0];
			final String parserContainerUUID = arguments.length > 1 && UtilMethods.isSet(arguments[1]) ?
				arguments[1] : DEFAULT_UUID_VALUE;
			containerIds.add(Pair.of(containerId, parserContainerUUID));
		}
	}

	/**
	 * Safely retrieves the containerIds from the context.
	 * @param context The context containing the stored containerIds
	 * @return The list of containerIds if found and properly typed,
	 * otherwise initializes a new list and stores it in the context.
	 */
	@SuppressWarnings("unchecked")
	private Collection<Pair<String, String>> getContainerIdsFromContext(final Context context) {
		final Object value = context.get(DOT_TEMPLATE_CONTAINER_IDS);
		if (value instanceof Collection<?>) {
			return (Collection<Pair<String, String>>) value;
		} else {
			Collection<Pair<String, String>> emptyList = new ArrayList<>();
			context.put(DOT_TEMPLATE_CONTAINER_IDS, emptyList);
			return emptyList;
		}
	}

	/**
	 * Get the value of the DONT_LOAD_CONTAINERS flag from the context.
	 * @param context the internal context adapter
	 * @return true if containers should not be loaded, false otherwise
	 */
	private boolean getDontLoadContainers(final Context context) {
		final Object dontLoadContainersObj = context.get(ParseContainer.DONT_LOAD_CONTAINERS);
		if (dontLoadContainersObj instanceof Boolean) {
			return (Boolean) dontLoadContainersObj;
		} else if (dontLoadContainersObj instanceof String) {
			return Boolean.parseBoolean((String) dontLoadContainersObj);
		}
		return false;
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

		if ((arguments.length < 3) || (!isParserContainerUUID(arguments[2]) && !Boolean.valueOf(arguments[2]))) {
			parserContainersArgument[1] = getDotParserContainerUUID(arguments.length > 1 ? arguments[1] : null);
		}

		return parserContainersArgument;
	}

	public static boolean isParserContainerUUID(final String uuid) {
		return uuid != null && uuid.startsWith(PARSE_CONTAINER_UUID_PREFIX);
	}
}
