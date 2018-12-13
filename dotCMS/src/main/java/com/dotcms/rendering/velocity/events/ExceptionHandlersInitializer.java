package com.dotcms.rendering.velocity.events;

import com.dotcms.config.DotInitializer;
import org.apache.velocity.exception.ParseErrorException;

public class ExceptionHandlersInitializer implements DotInitializer {

    @Override
    public void init() {

        DotVelocityExceptionHandlerFactory.register(ParseErrorException.class, new ExceptionHandlerParseErrorException());
    }
}
