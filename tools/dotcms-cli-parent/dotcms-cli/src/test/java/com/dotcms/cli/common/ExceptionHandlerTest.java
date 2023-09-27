package com.dotcms.cli.common;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import io.quarkus.test.junit.QuarkusTest;
import java.util.concurrent.CompletionException;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ExceptionHandlerTest {

    @Inject
    ExceptionHandlerImpl exceptionHandler;
    /**
     * Given Scenario: We Wrap an exception within a TraversalTaskException just like our code would
     * Expect Results: The root case exception is returned
     */
    @Test
    void Test_UnWrap_RuntimeException(){
        final TraversalTaskException traversalTaskException = new TraversalTaskException("LOL",
                new CompletionException(new IllegalArgumentException("LOL")));
        Exception ex = exceptionHandler.unwrap(traversalTaskException);
        Assertions.assertTrue(ex instanceof IllegalArgumentException);
    }

    @Inject
    ExceptionMappingConfig config;

    /**
     * Given Scenario: We feed various expected flavors of WebApplicationException
     * Expected Results: whatever noisy message that comes in must make it back clean
     */
    @Test
    void Test_Handle_WebApplication_Exception() {

        NotFoundException noise = new NotFoundException("No pineapple Flavor today");
        Exception handled = exceptionHandler.handle(noise);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(config.messages().get(404)));

        BadRequestException badRequestException = new BadRequestException("LOL");
        handled = exceptionHandler.handle(badRequestException);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(config.messages().get(400)));

        ForbiddenException forbiddenException = new ForbiddenException("LOL");
        handled = exceptionHandler.handle(forbiddenException);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(config.messages().get(403)));

        WebApplicationException unauthorized = new WebApplicationException(401);
        handled = exceptionHandler.handle(unauthorized);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(config.messages().get(401)));

        WebApplicationException internalServerError = new WebApplicationException(500);
        handled = exceptionHandler.handle(internalServerError);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(config.messages().get(500)));

        NotAllowedException moreNoise = new NotAllowedException("Not Allowed");
        handled = exceptionHandler.handle(moreNoise);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(config.fallback()));

    }


}
