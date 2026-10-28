package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Pins the Java signatures of {@link CMSUsersWebAPI}.
 *
 * <p>Templates reach {@code $cmsuser} through {@link CMSUserTool}. {@link CMSUsersWebAPI} stays for
 * Java callers such as OSGi plugins, which link to its methods by exact signature, so its public
 * methods must keep the signatures they had before {@link CMSUserTool} was introduced. A plugin
 * compiled against either release then resolves the same methods.
 */
public class CMSUsersWebAPISignatureTest {

    private static final String USER = "com.liferay.portal.model.User";
    private static final String ROLE = "com.dotmarketing.business.Role";
    private static final String CATEGORY_API = "com.dotmarketing.portlets.categories.business.CategoryAPI";

    /** The public methods of {@link CMSUsersWebAPI}, as "return-type name(parameter-types)". */
    private static final Set<String> EXPECTED_SIGNATURES = Set.of(
            CATEGORY_API + " getCategoryAPI()",
            "void setCategoryAPI(" + CATEGORY_API + ")",
            "void init(java.lang.Object)",
            USER + " getUserByEmail(java.lang.String)",
            USER + " getUserByUserId(java.lang.String)",
            USER + " getLoggedInUser(javax.servlet.http.HttpServletRequest)",
            "java.util.List<" + USER + "> getAllUsersInGroup(java.lang.String)",
            "java.util.List<" + ROLE + "> getUserGroups(" + USER + ")",
            "java.util.List<" + ROLE + "> getUserRoles(" + USER + ")",
            "boolean isUserRole(" + USER + ",java.lang.String)",
            "boolean hasRole(java.lang.String)",
            "boolean isCMSAdmin(" + USER + ")");

    /**
     * Method to test: the public method signatures of {@link CMSUsersWebAPI}.
     * Given scenario: the class is compared with the signatures Java callers were compiled against.
     * Expected result: the same public methods, with the same parameter and return types.
     */
    @Test
    public void testPublicSignatures_WhenComparedWithPreviousRelease_ShouldBeUnchanged() {
        final Set<String> actual = Arrays.stream(CMSUsersWebAPI.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
                .map(CMSUsersWebAPISignatureTest::signature)
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(new TreeSet<>(EXPECTED_SIGNATURES), actual);
    }

    /**
     * Method to test: the {@link Deprecated} marker on {@link CMSUsersWebAPI}.
     * Given scenario: {@code $cmsuser} is provided by {@link CMSUserTool}.
     * Expected result: the class is marked deprecated, so Java callers are pointed to the new tool.
     */
    @Test
    public void testClass_WhenSupersededByCMSUserTool_ShouldBeDeprecated() {
        assertTrue(CMSUsersWebAPI.class.isAnnotationPresent(Deprecated.class));
    }

    /**
     * Formats a method as "return-type name(parameter-types)", using generic type names.
     *
     * @param method the method to describe
     * @return the method's signature
     */
    private static String signature(final Method method) {
        return method.getGenericReturnType().getTypeName() + " " + method.getName() + "("
                + Arrays.stream(method.getGenericParameterTypes()).map(Type::getTypeName)
                        .collect(Collectors.joining(","))
                + ")";
    }
}
