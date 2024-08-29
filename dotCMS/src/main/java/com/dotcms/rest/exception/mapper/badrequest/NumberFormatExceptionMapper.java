
package com.dotcms.rest.exception.mapper.badrequest;

import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NumberFormatExceptionMapper
        extends DotBadRequestExceptionMapper<NumberFormatException> {

}