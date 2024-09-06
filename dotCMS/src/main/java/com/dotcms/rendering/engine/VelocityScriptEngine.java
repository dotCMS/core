package com.dotcms.rendering.engine;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Velocity script engine implementation
 * @author jsanca
 */
public class VelocityScriptEngine implements ScriptEngine {

    @Override
    public Object eval(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final Reader velocityReader,
                       final Map<String, Object> contextParams) {

        final Context context = null == request || null == response?
                com.dotmarketing.util.VelocityUtil.getBasicContext():
                VelocityUtil.getInstance().getContext(request, response);

        contextParams.forEach(context::put);
        context.put("dotJSON", new DotJSON());

        final StringWriter evalResult = new StringWriter();

        try {

            VelocityUtil.getEngine().evaluate(context, evalResult, StringPool.BLANK, velocityReader);
        } catch(MethodInvocationException e) {
            if(e.getCause() instanceof DotToolException) {
                Logger.error(this,"Error evaluating velocity: " + (e.getCause()).getCause().getMessage());
                throw new DotRuntimeException(e.getCause());
            }
        }

        return new HashMap<>(Map.of("output", evalResult, "dotJSON", context.get("dotJSON")));
    }

}
