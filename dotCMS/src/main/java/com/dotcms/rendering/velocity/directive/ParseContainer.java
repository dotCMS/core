package com.dotcms.rendering.velocity.directive;

import com.dotmarketing.beans.MultiTree;
import java.io.Writer;
import java.util.Optional;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * Could includes a container by id (long or shorty) or a path (with or without host "without will
 * use the current host").
 */
public class ParseContainer extends DotDirective {

  public static final String DEFAULT_UUID_VALUE = MultiTree.LEGACY_RELATION_TYPE;
  private static final long serialVersionUID = 1L;

  @Override
  public final String getName() {
    return "parseContainer";
  }

  public void init(RuntimeServices rs, InternalContextAdapter context, Node node)
      throws TemplateInitException {
    super.init(rs, context, node);
  }

  @Override
  String resolveTemplatePath(
      final Context context,
      final Writer writer,
      final RenderParams params,
      final String[] arguments) {

    final TemplatePathStrategyResolver templatePathResolver =
        TemplatePathStrategyResolver.getInstance();
    final Optional<TemplatePathStrategy> strategy =
        templatePathResolver.get(context, params, arguments);

    return strategy.isPresent()
        ? strategy.get().apply(context, params, arguments)
        : templatePathResolver.getDefaultStrategy().apply(context, params, arguments);
  }
}
