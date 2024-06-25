package com.dotcms.cli.common;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.cli.exception.ExceptionHandlerImpl;
import com.dotcms.cli.exception.ExceptionMappingConfig;
import io.quarkus.test.junit.QuarkusTest;
import java.util.concurrent.CompletionException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
        // On recent versions of Quarkus, the custom message is set as the reason phrase of the response
        //WebApplications have an immutable message so we can't change it. 404 will always be Not Found etc...
        ResponseImpl response = (ResponseImpl) ((WebApplicationException) handled).getResponse();
        //Therefore the custom message needs to be extracted from the response
        Assertions.assertTrue(response.getStatusInfo().getReasonPhrase().contains(config.messages().get(404)));

        BadRequestException badRequestException = new BadRequestException("LOL");
        handled = exceptionHandler.handle(badRequestException);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        response = (ResponseImpl) ((WebApplicationException) handled).getResponse();
        Assertions.assertTrue(response.getStatusInfo().getReasonPhrase().contains(config.messages().get(400)));

        ForbiddenException forbiddenException = new ForbiddenException("LOL");
        handled = exceptionHandler.handle(forbiddenException);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        response = (ResponseImpl) ((WebApplicationException) handled).getResponse();
        Assertions.assertTrue(response.getStatusInfo().getReasonPhrase().contains(config.messages().get(403)));

        WebApplicationException unauthorized = new WebApplicationException(401);
        handled = exceptionHandler.handle(unauthorized);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        response = (ResponseImpl) ((WebApplicationException) handled).getResponse();
        Assertions.assertTrue(response.getStatusInfo().getReasonPhrase().contains(config.messages().get(401)));

        WebApplicationException internalServerError = new WebApplicationException(500);
        handled = exceptionHandler.handle(internalServerError);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        response = (ResponseImpl) ((WebApplicationException) handled).getResponse();
        Assertions.assertTrue(response.getStatusInfo().getReasonPhrase().contains(config.messages().get(500)));

        NotAllowedException moreNoise = new NotAllowedException("Not Allowed");
        handled = exceptionHandler.handle(moreNoise);
        Assertions.assertTrue(handled instanceof WebApplicationException);
        response = (ResponseImpl) ((WebApplicationException) handled).getResponse();
        Assertions.assertTrue(response.getStatusInfo().getReasonPhrase().contains(config.fallback()));

    }


}
