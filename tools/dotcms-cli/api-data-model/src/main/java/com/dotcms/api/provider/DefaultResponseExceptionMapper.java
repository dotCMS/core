package com.dotcms.api.provider;

import com.dotcms.model.ResponseEntityView;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.Priority;
import io.quarkus.security.UnauthorizedException;
import java.io.ByteArrayInputStream;
import javax.validation.ValidationException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 *
 */
@Priority(4000)
public class DefaultResponseExceptionMapper implements
        ResponseExceptionMapper<RuntimeException> {

    @Override
    public RuntimeException toThrowable(Response response) {
        int status = response.getStatus();

        String message = "";
      if (response.hasEntity() && response.getMediaType()
              .isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
        GenericType<ResponseEntityView<String>> genericType = new GenericType<>() {
        };//needs empty body to preserve generic type
        ResponseEntityView<String> resp;
        try {
          resp = response.readEntity(genericType);
          message = resp.errors().get(0).message();
        } catch (Exception px) {
          // Fallback to json
          JsonNode jsonResp = response.readEntity(JsonNode.class);
          if (null != jsonResp) {
            message = String.format(" Status: (%s) With error: %s ", status, jsonResp.toPrettyString());
          }
        }

      } else {
        message = getBody(response);
      }

        RuntimeException re;
        switch (status) {
            case 412:
                re = new ValidationException(message);
                break;
            case 401:
                re = new UnauthorizedException(message);
                break;
            case 403:
                re = new ForbiddenException(message);
                break;
            case 404:
                re = new NotFoundException(message);
                break;
            default:
                re = new WebApplicationException(message, status);
        }
        return re;
    }

    private String getBody(Response response) {
        ByteArrayInputStream is = (ByteArrayInputStream) response.getEntity();
      if (response.hasEntity() && is != null) {
        byte[] bytes = new byte[is.available()];
        is.read(bytes, 0, is.available());
        return new String(bytes);
      } else {
        return "";
      }

    }
}
