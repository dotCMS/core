package com.dotcms.mock.response;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MockAsyncResponse implements AsyncResponse {

    private final Function<Object, Boolean> resumeFunction;
    private final Function<Object, Boolean> resumeThrowableFunction;

    public MockAsyncResponse(final Function<Object, Boolean> resumeFunction,
                             final Function<Object, Boolean> resumeThrowableFunction) {

        this.resumeFunction = resumeFunction;
        this.resumeThrowableFunction = resumeThrowableFunction;
    }

    @Override
    public boolean resume(Object o) {
        return resumeFunction.apply(o);
    }

    @Override
    public boolean resume(Throwable throwable) {
        return resumeThrowableFunction.apply(throwable);
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public boolean cancel(int i) {
        return false;
    }

    @Override
    public boolean cancel(Date date) {
        return false;
    }

    @Override
    public boolean isSuspended() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean setTimeout(long l, TimeUnit timeUnit) {
        return false;
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler timeoutHandler) {

    }

    @Override
    public Collection<Class<?>> register(Class<?> aClass) {
        return null;
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Class<?> aClass, Class<?>... classes) {
        return null;
    }

    @Override
    public Collection<Class<?>> register(Object o) {
        return null;
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Object o, Object... objects) {
        return null;
    }
}
