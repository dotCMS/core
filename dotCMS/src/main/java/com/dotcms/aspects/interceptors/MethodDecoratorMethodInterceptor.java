package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.MethodDecorator;
import com.dotcms.business.ParameterDecorator;
import com.dotcms.util.AnnotationUtils;
import com.dotcms.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Method handler for the {@link com.dotcms.business.MethodDecorator} annotation aspect
 * @author jsanca
 */
public class MethodDecoratorMethodInterceptor implements MethodInterceptor<Object> {

    public static final MethodDecoratorMethodInterceptor INSTANCE = new MethodDecoratorMethodInterceptor();

    protected MethodDecoratorMethodInterceptor() {

    }

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        final Object[] arguments = delegate.getArguments();
        final Method method      = delegate.getMethod();
        final Optional<ParameterDecorator> parameterDecorator =
                this.getParameterDecorator (method);

        final Object [] newArguments =  parameterDecorator.isPresent()?
                parameterDecorator.get().decorate(arguments):arguments;

        return delegate.proceed(newArguments);
    } // invoke.

    private Optional<ParameterDecorator> getParameterDecorator(final Method method) {

        try {

            final MethodDecorator methodDecorator =
                    AnnotationUtils.getMethodAnnotation(method, MethodDecorator.class);

            if (null != methodDecorator) {

                final Class<? extends ParameterDecorator> parameterDecoratorClass =
                        methodDecorator.parameterDecorator();

                if (null != parameterDecoratorClass) {
                    final ParameterDecorator parameterDecorator =
                            ReflectionUtils.newInstance(parameterDecoratorClass);

                    return Optional.ofNullable(parameterDecorator);
                }
            }
        } catch (Exception e) {

            return Optional.empty();
        }

        return Optional.empty();
    }

} // E:O:F:LogTimeMethodInterceptor.