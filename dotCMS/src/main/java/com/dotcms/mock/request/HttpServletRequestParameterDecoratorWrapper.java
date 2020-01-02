package com.dotcms.mock.request;

import org.apache.commons.collections.map.HashedMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Map;

/**
 * Decorates the request parameters based on {@link ParameterDecorator}s
 * @author jsanca
 */
public class HttpServletRequestParameterDecoratorWrapper extends HttpServletRequestWrapper {

    private final Map<String, ParameterDecorator> decoratorMap = new HashedMap();

    public HttpServletRequestParameterDecoratorWrapper(final HttpServletRequest request, final ParameterDecorator... decorators) {
        super(request);

        for (final ParameterDecorator decorator : decorators) {

            this.decoratorMap.put(decorator.key(), decorator);
        }
    }

    @Override
    public String getParameter(final String name) {

        final String parameter = super.getParameter(name);
        return decoratorMap.containsKey(name)?
                decoratorMap.get(name).decorate(parameter):parameter;
    }
} // E:O:F:HttpServletRequestParameterDecoratorWrapper.
