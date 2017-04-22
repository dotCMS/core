package com.dotmarketing.velocity.directive;

import java.io.Writer;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;

public class ParseContainer extends DotDirective {


  /**
   * 
   */
  private static final long serialVersionUID = 1L;


  @Override
  public final String getName() {

    return "parseContainer";
  }



  public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException {
    super.init(rs, context, node);

  }


  @Override
  String resolveTemplate(final Context context, final Writer writer, final RenderParams params, final String argument) {

    return (params.live) ? "/live/" + argument + ".container" : "/working/" + argument + ".container";



  }

}

