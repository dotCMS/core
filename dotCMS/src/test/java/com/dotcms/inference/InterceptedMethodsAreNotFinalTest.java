package com.dotcms.inference;

import com.dotcms.inference.rest.ChatCompletionsResource;
import com.dotcms.inference.rest.EmbeddingsResource;
import com.dotcms.inference.rest.ImagesResource;
import com.dotcms.inference.rest.ModelsResource;
import com.dotcms.cost.RequestCost;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Fails if a CDI-intercepted resource method is declared {@code final}.
 *
 * <p>Weld cannot intercept a final method: it proxies by subclassing, and a final method cannot be
 * overridden. Annotating one with {@code @RequestCost} — an interceptor binding — makes Weld throw
 * {@code WELD-001504} while building the injection target, which fails
 * {@code DotRestApplication}'s servlet init. That does not break one endpoint; it takes down
 * <strong>every</strong> REST endpoint in dotCMS, and the log then fills with a secondary
 * "resource configuration is not modifiable" error from each retry, which looks like an entirely
 * different problem and is where an investigation naturally starts.</p>
 *
 * <p>This exists because nothing else here would catch it. Every integration test in this family
 * invokes the resource methods directly, so they never pass through Weld or Jersey at all — the
 * whole suite was green while the application could not start. That blind spot is structural, not
 * an oversight in any one test, so the guard has to be explicit.</p>
 *
 * <p>Asserted by reflection over every method rather than against a list of known ones: a fifth
 * operation added later with {@code public final} is precisely the mistake this catches, and a
 * test naming today's four would not.</p>
 */
public class InterceptedMethodsAreNotFinalTest {

    /**
     * Given every resource in this endpoint family
     * When a method carries a CDI interceptor binding
     * Then it is not final, so Weld can proxy it
     */
    @Test
    public void test_noInterceptedResourceMethodIsFinal() {
        final List<String> offenders = new ArrayList<>();

        for (final Class<?> resource : new Class<?>[]{
                ChatCompletionsResource.class,
                EmbeddingsResource.class,
                ImagesResource.class,
                ModelsResource.class}) {

            for (final Method method : resource.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestCost.class)
                        && Modifier.isFinal(method.getModifiers())) {
                    offenders.add(resource.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertTrue("Weld proxies an intercepted bean by subclassing it, so a final method cannot "
                        + "be intercepted: it throws WELD-001504 during deployment and takes the "
                        + "whole REST servlet down with it, not just these endpoints. Drop the "
                        + "final keyword from: " + offenders,
                offenders.isEmpty());
    }
}
