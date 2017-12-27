package com.dotcms.rendering.velocity.directive;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.parser.node.Node;

import com.dotmarketing.util.UtilMethods;

public final class FixBreaks extends InputBase {


  private static final long serialVersionUID = 1L;

  public final String getScopeName() {
    return "template";
  }

  public final int getType() {
    return LINE;
  }

  @Override
  public String getName() {
    return "fixBreaks";
  }

  final public boolean render(InternalContextAdapter context, Writer writer, Node node)
      throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

    Object value = node.jjtGetChild(0).value(context);
    String argument = value == null ? null : value.toString();

    writer.write(UtilMethods.fixBreaks(argument));
    return true;
  }



}
