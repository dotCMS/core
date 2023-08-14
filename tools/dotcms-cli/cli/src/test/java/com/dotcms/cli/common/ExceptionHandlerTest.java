package com.dotcms.cli.common;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import java.util.concurrent.CompletionException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionHandlerTest {

    /**
     * Given Scenario: We Wrap an exception within a TraversalTaskException just like our code would
     * Expect Results: The root case exception is returned
     */
    @Test
    void Test_UnWrap_RuntimeException(){
        final TraversalTaskException traversalTaskException = new TraversalTaskException("LOL",
                new CompletionException(new IllegalArgumentException("LOL")));
        Exception ex = ExceptionHandler.handle(traversalTaskException);
        Assertions.assertTrue(ex instanceof IllegalArgumentException);
    }

    /**
     * Given Scenario: We feed various expected flavors of WebApplicationException
     * Expected Results: whatever noisy message that comes in must make it back clean
     */
    @Test
    void Test_Handle_WebApplication_Exception() {

        NotFoundException noise = new NotFoundException("No pineapple Flavor today");
        Exception handled = ExceptionHandler.handle(noise);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(ExceptionHandler.NOT_FOUND));

        BadRequestException badRequestException = new BadRequestException("LOL");
        handled = ExceptionHandler.handle(badRequestException);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(ExceptionHandler.BAD_REQUEST));

        ForbiddenException forbiddenException = new ForbiddenException("LOL");
        handled = ExceptionHandler.handle(forbiddenException);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(ExceptionHandler.FORBIDDEN));

        WebApplicationException unauthorized = new WebApplicationException(401);
        handled = ExceptionHandler.handle(unauthorized);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(ExceptionHandler.UNAUTHORIZED));

        WebApplicationException internalServerError = new WebApplicationException(500);
        handled = ExceptionHandler.handle(internalServerError);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(ExceptionHandler.INTERNAL_SERVER_ERROR));

        NotAllowedException moreNoise = new NotAllowedException("Not Allowed");
        handled = ExceptionHandler.handle(moreNoise);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        Assertions.assertTrue(handled.getMessage().contains(ExceptionHandler.DEFAULT_ERROR));

    }


}
