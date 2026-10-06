package com.dotcms.inference.rest;

import javax.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a resource or method as part of the {@code /api/inference/v1} family.
 *
 * <p>Binds the family's request and response filters to these endpoints alone. Without the
 * binding a {@code @Provider} filter applies to every JAX-RS response in dotCMS, which is
 * emphatically not wanted: the size ceiling and the resolved-site header are properties of this
 * family, not of the product.</p>
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface InferenceEndpoint {
}
