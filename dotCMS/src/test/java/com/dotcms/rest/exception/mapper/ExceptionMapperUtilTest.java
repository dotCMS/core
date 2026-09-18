package com.dotcms.rest.exception.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class ExceptionMapperUtilTest {

    @Test
    public void entityless_WebApplicationException_gets_its_message_as_json() {
        final Response rsp = ExceptionMapperUtil.createResponse(
                new BadRequestException("why it failed"), null, Response.Status.BAD_REQUEST);

        assertEquals(400, rsp.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, rsp.getMediaType());
        assertEquals("why it failed", ((Map<?, ?>) rsp.getEntity()).get("message"));
    }

    @Test
    public void adding_the_json_body_keeps_the_exceptions_own_headers() {
        // NotAuthorizedException carries WWW-Authenticate; a rebuilt Response must not drop it.
        final Response rsp = ExceptionMapperUtil.createResponse(
                new NotAuthorizedException("Bearer realm=\"dotcms\""), null,
                Response.Status.UNAUTHORIZED);

        assertEquals(401, rsp.getStatus());
        assertEquals("Bearer realm=\"dotcms\"", rsp.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        assertTrue(((Map<?, ?>) rsp.getEntity()).containsKey("message"));
    }

    @Test
    public void null_message_falls_back_to_the_reason_phrase_instead_of_a_500() {
        final Response rsp = ExceptionMapperUtil.createResponse(
                new javax.ws.rs.WebApplicationException((String) null, Response.status(400).build()),
                null, Response.Status.BAD_REQUEST);

        assertEquals(400, rsp.getStatus());
        assertEquals("Bad Request", ((Map<?, ?>) rsp.getEntity()).get("message"));
    }

    @Test
    public void redirect_and_no_content_statuses_get_no_body() {
        // 3xx/204/304 must not carry an entity; only error statuses get the JSON message.
        final Response redirect = ExceptionMapperUtil.createResponse(
                new javax.ws.rs.RedirectionException(Response.Status.SEE_OTHER,
                        java.net.URI.create("/elsewhere")),
                null, Response.Status.SEE_OTHER);

        assertEquals(303, redirect.getStatus());
        assertEquals("/elsewhere", redirect.getHeaderString(HttpHeaders.LOCATION));
        assertEquals(false, redirect.hasEntity());
    }

    @Test
    public void WebApplicationException_with_an_entity_is_returned_untouched() {
        final Response own = Response.status(409).entity("already there").build();
        final Response rsp = ExceptionMapperUtil.createResponse(
                new javax.ws.rs.WebApplicationException(own), null, Response.Status.CONFLICT);

        assertEquals("already there", rsp.getEntity());
    }
}
