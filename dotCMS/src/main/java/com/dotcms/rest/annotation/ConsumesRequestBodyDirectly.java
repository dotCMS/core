package com.dotcms.rest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark REST endpoint methods that consume request bodies directly from HttpServletRequest
 * rather than through standard JAX-RS @RequestBody parameters.
 * 
 * This is typically used for:
 * - Asynchronous streaming endpoints
 * - Custom content processing
 * - Legacy endpoints that handle raw request data
 * - Methods that need direct access to request InputStreams
 * 
 * When this annotation is present, the REST endpoint validation tests will recognize that
 * the method consumes a request body even though it may not have explicit @RequestBody parameters.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @POST
 * @Path("/upload-stream")
 * @Consumes(MediaType.APPLICATION_OCTET_STREAM)
 * @ConsumesRequestBodyDirectly("Handles binary stream upload via HttpServletRequest.getInputStream()")
 * public Response uploadStream(@Context HttpServletRequest request) {
 *     // Process request.getInputStream() directly
 * }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConsumesRequestBodyDirectly {
    
    /**
     * Description of how the method consumes the request body directly.
     * This helps with documentation and code understanding.
     * 
     * @return description of the direct body consumption mechanism
     */
    String value() default "Consumes request body directly from HttpServletRequest";
}