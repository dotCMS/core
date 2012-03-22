package com.dotmarketing.scripting.util;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.InstanceVariables;

public class JRubyUtil {

	private Ruby ruby = Ruby.newInstance();
//  private ThreadContext threadContext =
//    Ruby.getDefaultInstance().getCurrentContext();

  public JRubyUtil() {
    setLoadPath();
  }

  private void addLoadPath(String path) {
    String expr = "$: << '" + path + "'";
    ruby.evalScriptlet(expr);
  }

  public RubyObject callMethod(RubyObject receiver, String methodName) {
    return (RubyObject) receiver.callMethod(receiver.getRuntime().getThreadService().getCurrentContext(), methodName);
  }

  public void enableWarning() {
    // Gives lots of "Useless use of :: in void context" warnings
    // that don't make sense!
    ruby.setVerbose(ruby.getTrue());
  }

  public Object evalScript(String script) {
    return ruby.evalScriptlet(script);
  }

  public static Object getAttribute(
    RubyObject object, String attributeName) {

    // The keys in this RubyHash are java.lang.String objects.
    // Are the values always java.lang.String object?
    RubyHash attributes =
      (RubyHash) object.getInstanceVariable("@attributes");
    return attributes == null ? null : attributes.get(attributeName);
  }

  public static void listInstanceVariables(
    String name, RubyObject object) {

    System.out.println("Instance variables of " + name + ":");
    InstanceVariables vars = object.getInstanceVariables();
    
    List<String> l = vars.getInstanceVariableNameList();
    for (String s : l) {
    	System.out.println("  " + s + " = " + vars.getInstanceVariable(s).asJavaString());
	}
  }

  private void setLoadPath() {
    String jrubyLib = System.getProperty("jruby.lib");
    addLoadPath(jrubyLib);
    addLoadPath(jrubyLib + "/ruby/site_ruby/1.8");
    addLoadPath(jrubyLib + "/ruby/site_ruby/1.8/java");
    addLoadPath(jrubyLib + "/ruby/site_ruby");
    addLoadPath(jrubyLib + "/ruby/1.8");
    addLoadPath(jrubyLib + "/ruby/1.8/java");
    addLoadPath("lib/ruby/1.8");
  }
}
