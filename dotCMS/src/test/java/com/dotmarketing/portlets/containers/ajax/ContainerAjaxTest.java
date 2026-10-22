package com.dotmarketing.portlets.containers.ajax;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Structural tests for the DWR classes of the Containers portlet.
 */
class ContainerAjaxTest {

    /**
     * Given the legacy Containers portlet reads a container's template dependencies from the REST
     * API, when the DWR classes are inspected, then neither {@link ContainerAjax} nor
     * {@link ContainerAjaxDrawedTemplate} (which inherits from it) exposes a public
     * {@code checkDependencies} method.
     */
    @Test
    void dwrClassesDoNotExposeATemplateDependencyMethod() {
        assertTrue(hasNoPublicMethodNamed(ContainerAjax.class, "checkDependencies"),
                "ContainerAjax must not expose checkDependencies through DWR");
        assertTrue(hasNoPublicMethodNamed(ContainerAjaxDrawedTemplate.class, "checkDependencies"),
                "ContainerAjaxDrawedTemplate must not expose checkDependencies through DWR");
    }

    private static boolean hasNoPublicMethodNamed(final Class<?> type, final String name) {
        return Arrays.stream(type.getMethods()).map(Method::getName).noneMatch(name::equals);
    }
}
