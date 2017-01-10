package com.dotmarketing.scripting.engine;

/*
 * Copyright 2004,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Iterator;
import java.util.Vector;

import com.dotcms.repackage.org.apache.bsf.BSFDeclaredBean;
import com.dotcms.repackage.org.apache.bsf.BSFException;
import com.dotcms.repackage.org.apache.bsf.BSFManager;
import com.dotcms.repackage.org.apache.bsf.util.BSFEngineImpl;
import com.dotcms.repackage.org.apache.bsf.util.BSFFunctions;
import com.dotcms.repackage.org.mozilla.javascript.Context;
import com.dotcms.repackage.org.mozilla.javascript.EvaluatorException;
import com.dotcms.repackage.org.mozilla.javascript.Function;
import com.dotcms.repackage.org.mozilla.javascript.ImporterTopLevel;
import com.dotcms.repackage.org.mozilla.javascript.JavaScriptException;
import com.dotcms.repackage.org.mozilla.javascript.NativeJavaObject;
import com.dotcms.repackage.org.mozilla.javascript.Scriptable;
import com.dotcms.repackage.org.mozilla.javascript.WrapFactory;
import com.dotcms.repackage.org.mozilla.javascript.WrappedException;
import com.dotcms.repackage.org.mozilla.javascript.Wrapper;

/**
 * This File was adapted by Jason Tesser from BSF's implementation of the Rhino
 * Engine. 
 * <p>
 * The original version of this code was first written by Adam Peller
 * for use in LotusXSL. Sanjiva took his code and adapted it for BSF.
 *
 * Later it was adapted by Sanjiva Weerawarana, Matthew J. Duftler, and
 * Norris Boyd
 * 
 * This version has been modified for use with the Scripting Plugin for dotCMS 
 *
 * @author   Adam Peller <peller@lotus.com>
 * @author   Sanjiva Weerawarana
 * @author   Matthew J. Duftler
 * @author   Norris Boyd
 * @author 	 Jason Tesser
 */
public class JSRhinoEngine extends BSFEngineImpl {
    /**
     * The global script object, where all embedded functions are defined,
     * as well as the standard ECMA "core" objects.
     */
    private Scriptable global;

    /**
     * Return an object from an extension.
     * @param object Object on which to make the call (ignored).
     * @param method The name of the method to call.
     * @param args an array of arguments to be
     * passed to the extension, which may be either
     * Vectors of Nodes, or Strings.
     */
    public Object call(Object object, String method, Object[] args)
        throws BSFException {

        Object retval = null;
        Context cx;

        try {
            cx = Context.enter();

            // REMIND: convert arg list Vectors here?

            Object fun = global.get(method, global);
            // NOTE: Source and line arguments are nonsense in a call().
            //       Any way to make these arguments *sensible?
            if (fun == Scriptable.NOT_FOUND)
                throw new EvaluatorException("function " + method +
                                             " not found.", "none", 0);

            cx.setOptimizationLevel(-1);
            cx.setGeneratingDebug(false);
            cx.setGeneratingSource(false);
            cx.setOptimizationLevel(0);
            cx.setDebugger(null, null);
            
            retval =
                ((Function) fun).call(cx, global, global, args);
            
//                ScriptRuntime.call(cx, fun, global, args, global);

            if (retval instanceof Wrapper)
                retval = ((Wrapper) retval).unwrap();
        } 
        catch (Throwable t) {
            handleError(t);
        } 
        finally {
            Context.exit();
        }
        return retval;
    }

    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        if ((bean.bean instanceof Number) ||
            (bean.bean instanceof String) ||
            (bean.bean instanceof Boolean)) {
            global.put(bean.name, global, bean.bean);
        } 
        else {
            // Must wrap non-scriptable objects before presenting to Rhino
        	WrapFactory wf = new WrapFactory();
        	Scriptable wrapped = wf.wrapAsJavaObject(Context.enter(), global, bean.bean, bean.type);
//            Scriptable wrapped = Context.toObject(bean.bean, global);
            global.put(bean.name, global, wrapped);
        }
    }

    /**
     * This is used by an application to evaluate a string containing
     * some expression.
     */
    public Object eval(String source, int lineNo, int columnNo, Object oscript)
        throws BSFException {

        String scriptText = oscript.toString();
        Object retval = null;
        Context cx;

        try {
            cx = Context.enter();

            cx.setOptimizationLevel(-1);
            cx.setGeneratingDebug(false);
            cx.setGeneratingSource(false);
            cx.setOptimizationLevel(0);
            cx.setDebugger(null, null);

            retval = cx.evaluateString(global, scriptText,
                                       source, lineNo,
                                       null);

            if (retval instanceof NativeJavaObject)
                retval = ((NativeJavaObject) retval).unwrap();

        } 
        catch (Throwable t) { // includes JavaScriptException, rethrows Errors
            handleError(t);
        } 
        finally {
            Context.exit();
        }
        return retval;
    }

    private void handleError(Throwable t) throws BSFException {
        if (t instanceof WrappedException)
            t = ((WrappedException) t).getWrappedException();

        String message = null;
        Throwable target = t;

        if (t instanceof JavaScriptException) {
            message = t.getLocalizedMessage();

            // Is it an exception wrapped in a JavaScriptException?
            Object value = ((JavaScriptException) t).getValue();
            if (value instanceof Throwable) {
                // likely a wrapped exception from a LiveConnect call.
                // Display its stack trace as a diagnostic
                target = (Throwable) value;
            }
        }
        else if (t instanceof EvaluatorException ||
                 t instanceof SecurityException) {
            message = t.getLocalizedMessage();
        }
        else if (t instanceof RuntimeException) {
            message = "Internal Error: " + t.toString();
        }
        else if (t instanceof StackOverflowError) {
            message = "Stack Overflow";
        }

        if (message == null)
            message = t.toString();

        if (t instanceof Error && !(t instanceof StackOverflowError)) {
            // Re-throw Errors because we're supposed to let the JVM see it
            // Don't re-throw StackOverflows, because we know we've
            // corrected the situation by aborting the loop and
            // a long stacktrace would end up on the user's console
            throw (Error) t;
        }
        else {
            throw new BSFException(BSFException.REASON_OTHER_ERROR,
                                   "JavaScript Error: " + message,
                                   target);
        }
    }

    /**
     * Initialize the engine. 
     * Put the manager into the context-manager
     * map hashtable too.
     */
    public void initialize(BSFManager mgr, String lang, Vector declaredBeans)
        throws BSFException {
        
        super.initialize(mgr, lang, declaredBeans);

        // Initialize context and global scope object
        try {
            Context cx = Context.enter();
            global = new ImporterTopLevel(cx);
            Scriptable bsf = Context.toObject(new BSFFunctions(mgr, this), global);
            global.put("bsf", global, bsf);

            for(Iterator it = declaredBeans.iterator(); it.hasNext();) {
            	declareBean((BSFDeclaredBean) it.next());
            }
        } 
        catch (Throwable t) {

        } 
        finally {
            Context.exit();
        }
    }

    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        global.delete(bean.name);
    }
}

