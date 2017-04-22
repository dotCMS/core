package com.dotmarketing.velocity.directive;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;

abstract class DotDirective extends InputBase {


  private static final long serialVersionUID = 1L;
  private int maxDepth;

  public final String getScopeName() {
    return "template";
  }


  public final int getType() {
    return LINE;
  }


  public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException {
    super.init(rs, context, node);
    RuntimeServices rsvc = VelocityUtil.getEngine().getRuntimeServices();

    this.maxDepth = rsvc.getInt(RuntimeConstants.PARSE_DIRECTIVE_MAXDEPTH, 10);
  }


  final public boolean render(InternalContextAdapter context, Writer writer, Node node)
      throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

    HttpServletRequest request = (HttpServletRequest) context.get("request");
    Object value = node.jjtGetChild(0).value(context);
    String argument = value == null ? null : value.toString();

    RenderParams params = request.getAttribute(RenderParams.RENDER_PARAMS_ATTRIBUTE) != null
        ? (RenderParams) request.getAttribute(RenderParams.RENDER_PARAMS_ATTRIBUTE) : new RenderParams(request);

    String templatePath = this.resolveTemplate(context, writer, params, argument);

    return this.renderTemplate(context, writer, templatePath);

  }



  public boolean renderTemplate(InternalContextAdapter context, final Writer writer, final String templatePath)
      throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

    RuntimeServices rsvc = VelocityUtil.getEngine().getRuntimeServices();
    Template t;


    try {
      t = rsvc.getTemplate(templatePath, getInputEncoding(context));
    } catch (ResourceNotFoundException rnfe) {
      /*
       * the arg wasn't found. Note it and throw
       */
      Logger.error(this, this.getName() + ": cannot find template '" + templatePath + "', called at "
          + VelocityException.formatFileString(this));
      throw rnfe;
    } catch (ParseErrorException pee) {
      /*
       * the arg was found, but didn't parse - syntax error note it and throw
       */
      Logger.error(this, this.getName() + ": syntax error in template '" + templatePath + "', called at "
          + VelocityException.formatFileString(this));
      throw pee;
    }
    /**
     * pass through application level runtime exceptions
     */
    catch (RuntimeException e) {
      Logger.error(this, "Exception rendering " + this.getName() + " (" + templatePath + ") at "
          + VelocityException.formatFileString(this));
      throw e;
    } catch (Exception e) {
      String msg =
          "Exception " + this.getName() + " (" + templatePath + ") at " + VelocityException.formatFileString(this);
      Logger.error(this, msg, e);
      throw new VelocityException(msg, e);
    }


    /*
     * and render it
     */
    try {
      preRender(context);
      context.pushCurrentTemplateName(templatePath);

      ((SimpleNode) t.getData()).render(context, writer);
    } catch (StopCommand stop) {
      if (!stop.isFor(this)) {
        throw stop;
      }
    }
    /**
     * pass through application level runtime exceptions
     */
    catch (RuntimeException e) {
      /**
       * Log #parse errors so the user can track which file called which.
       */
      Logger.error(this, "Exception rendering " + this.getName() + " (" + templatePath + ") at "
          + VelocityException.formatFileString(this));
      throw e;
    } catch (Exception e) {
      String msg = "Exception rendering" + this.getName() + " (" + templatePath + ") at "
          + VelocityException.formatFileString(this);
      Logger.error(this, msg, e);
      throw new VelocityException(msg, e);
    } finally {
      context.popCurrentTemplateName();
      postRender(context);
    }

    /*
     * note - a blocked input is still a successful operation as this is expected behavior.
     */

    return true;
  }

  abstract String resolveTemplate(Context context, Writer writer, RenderParams params, String argument);



}
