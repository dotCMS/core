package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.MethodHook;
import com.dotcms.business.PostHook;
import com.dotcms.business.PreHook;
import com.dotcms.util.AnnotationUtils;
import com.dotcms.util.ReflectionUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Method handler for the {@link MethodHook} annotation aspect
 * @author jsanca
 */
public class MethodHookMethodInterceptor implements MethodInterceptor<Object> {

    public static final MethodHookMethodInterceptor INSTANCE = new MethodHookMethodInterceptor();

    protected MethodHookMethodInterceptor() {

    }

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        final Object[] arguments = delegate.getArguments();
        final Method method      = delegate.getMethod();
        final Optional<Tuple2<Optional<PreHook>, Optional<PostHook>>> hooks =
                this.getHooks (method);

        if (hooks.isPresent()) {

            Object[] newArguments = arguments;
            Object   methodResult = null;
            if (hooks.get()._1.isPresent()) { // prehook present

                final PreHook preHook = hooks.get()._1.get();
                final Tuple2<Boolean,Object[]>  preHookResult = preHook.pre(arguments);
                final boolean continueWith = preHookResult._1;
                if (!continueWith) {

                    return null; // user do not want to continue
                }

                newArguments = preHookResult._2;
            }

            methodResult = delegate.proceed(newArguments);

            if (hooks.get()._2.isPresent()) { // post hook present

                final PostHook postHook = hooks.get()._2.get();
                methodResult = postHook.post(newArguments, methodResult);
            }

            return methodResult;
        }

        return delegate.proceed(arguments);
    } // invoke.

    private Optional<Tuple2<Optional<PreHook>, Optional<PostHook>>> getHooks(final Method method) {

        try {

            final MethodHook methodHook =
                    AnnotationUtils.getMethodAnnotation(method, MethodHook.class);

            if (null != methodHook) {

                final Optional<PreHook> preHook   = (null !=  methodHook.preHook())?
                        Optional.ofNullable(ReflectionUtils.newInstance( methodHook.preHook())):
                        Optional.empty();

                final Optional<PostHook> postHook = (null !=  methodHook.postHook())?
                        Optional.ofNullable(ReflectionUtils.newInstance( methodHook.postHook())):
                        Optional.empty();

                return Optional.of(Tuple.of(preHook, postHook));
            }
        } catch (Exception e) {

            return Optional.empty();
        }

        return Optional.empty();
    }

} // E:O:F:LogTimeMethodInterceptor.