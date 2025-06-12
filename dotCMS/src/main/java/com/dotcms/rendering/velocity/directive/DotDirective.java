package com.dotcms.rendering.velocity.directive;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.util.Logger;


abstract class DotDirective extends InputBase {

  private static final long serialVersionUID = 1L;

  public final String getScopeName() {
    return "template";
  }

  public final int getType() {
    return LINE;
  }


  abstract String resolveTemplatePath(Context context, Writer writer, RenderParams params, String[] arguments);

  final Template loadTemplate(InternalContextAdapter context,  String templatePath){
    
    try {
      RuntimeServices rsvc = VelocityUtil.getEngine().getRuntimeServices();
      return rsvc.getTemplate(templatePath, getInputEncoding(context));
    } catch (ResourceNotFoundException rnfe) {
        if(VelocityType.resolveVelocityType(templatePath) != VelocityType.CONTENT) {
            Logger.warn(this, this.getName() + ": cannot find template '" + templatePath + "', called at "+ VelocityException.formatFileString(this));
        }
        Logger.debug(this, () -> this.getName() + ": cannot find template '" + templatePath + "', called at "+ VelocityException.formatFileString(this));

      throw rnfe;
    } catch (ParseErrorException pee) {
      Logger.warn(this, this.getName() + ": syntax error in template '" + templatePath + "', called at "
          + VelocityException.formatFileString(this));
      throw pee;
    }
    catch (RuntimeException e) {
      Logger.warn(this, "Exception rendering " + this.getName() + " (" + templatePath + ") at "
          + VelocityException.formatFileString(this));
      throw e;
    } catch (Exception e) {
      String msg =
          "Exception " + this.getName() + " (" + templatePath + ") at " + VelocityException.formatFileString(this);
      Logger.error(this, msg, e);
      throw new VelocityException(msg, e);
    }
  }


  /**
   * Return the value from Cache if the Directive is using some kind of Cache level
   * the default implementation return a Empty Optional.
   *
   * @return Value from cache
   */
  public Optional<String> getFromCache(final String[] arguments) {
    return Optional.empty();
  }

  final public boolean render(InternalContextAdapter context, Writer writer, Node node)
      throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

    HttpServletRequest request = (HttpServletRequest) context.get("request");
    int args = node.jjtGetNumChildren();
    String[] arguments = new String[args];
    
    for(int i=0;i<args;i++) {
        Object value = node.jjtGetChild(i).value(context);
        arguments[i]= (value == null) ? null : value.toString();
    }

    final boolean dontCache = VelocityUtil.getDontUseDirectiveCache(context);
    final Optional<String> fromCache = dontCache ?
            Optional.empty() : getFromCache(arguments);

    if (fromCache.isPresent()) {
      writer.write(fromCache.get());
      return true;
    }

    RenderParams params = new RenderParams(request);

    try{
      String templatePath = this.resolveTemplatePath(context, writer, params, arguments);
      if(null ==templatePath) {
          throw new ResourceNotFoundException("null template");
      }
      final boolean loadAndRender = shouldLoadAndRenderTemplate(context, arguments);
      if (!loadAndRender) {
        afterRender(StringUtils.EMPTY, arguments, context);
        return true;
      }
      Template t = loadTemplate(context, templatePath);

      final Writer innerWriter = new StringWriter();
      final boolean result = this.renderTemplate(context, innerWriter, t, templatePath);
      this.afterRender(innerWriter.toString(), arguments, context);
      writer.write(innerWriter.toString());

      return result;
    } catch(ParseErrorException|ResourceNotFoundException rnfe){
      context.remove("ContentIdentifier");
      postRender(context);
      return true;
    }

  }

  /**
   * Call before render the Template, it allows to check if the Template should be loaded and rendered
   * before the execution of {@link DotDirective#render(InternalContextAdapter, Writer, Node)}
   * By default, it returns true, meaning the template will be loaded and rendered.
   *
   * @param context The context in which the directive is executed
   * @param arguments The arguments passed to the directive
   * @return True if the template should be loaded and rendered, false otherwise.
   */
  boolean shouldLoadAndRenderTemplate(final Context context, final String[] arguments) {
    return true;
  }

  /**
   * Call after render the Template, it allow you to do something before the return of
   * the {@link DotDirective#render(InternalContextAdapter, Writer, Node)}
   *
   * @param render    Template content after render
   * @param arguments Arguments passed to the directive
   * @param context   The context in which the directive is executed
   */
  void afterRender(final String render, final String[] arguments, final Context context) {

  }


  final boolean renderTemplate(InternalContextAdapter context, final Writer writer, final Template t, final String templatePath)
      throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {


    try {
    	Logger.debug(this, "Rendering templatePath: "+templatePath);
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
    	String msg = "Exception rendering " + this.getName() + " (" + templatePath + ") at "
    	          + VelocityException.formatFileString(this)+(e.getMessage() != null?". Cause of error: "+e.getMessage():"");
      Logger.error(this, msg);
      
      Logger.debug(this, msg, e);
      
      return false;
    } catch (Exception e) {
        if(!e.getClass().getSimpleName().equals("ClientAbortException")) {
            String msg = "Exception rendering " + this.getName() + " (" + templatePath + ") at "
                    + VelocityException.formatFileString(this);
            Logger.error(this, msg, e);
        }
      return false;
    } finally {
      context.popCurrentTemplateName();
      postRender(context);
    }

    /*
     * note - a blocked input is still a successful operation as this is expected behavior.
     */

    return true;
  }

}
