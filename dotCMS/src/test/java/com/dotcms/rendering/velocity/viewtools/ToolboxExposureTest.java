package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.liferay.portal.model.User;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Contract test for the Velocity tools bound in {@code WEB-INF/toolbox.xml}.
 *
 * <p>Bound tools are thin, template-facing facades. They resolve the current requester
 * themselves, so they must not hand templates the underlying service APIs (which take the acting
 * {@link User} as an argument) nor {@link User} objects for accounts other than the current
 * requester. This test walks every bound tool class and pins both rules, so a new tool or method
 * that breaks them fails here rather than at review time.
 */
public class ToolboxExposureTest {

    private static final String TOOLBOX_PATH = "src/main/webapp/WEB-INF/toolbox.xml";

    /** The template tool bound as {@code $cmsuser}. */
    private static final String CMS_USER_TOOL = "com.dotcms.rendering.velocity.viewtools.CMSUserTool";

    /**
     * Methods allowed to return a {@link User} because they resolve the current requester, never
     * an arbitrary account.
     */
    private static final Set<String> CURRENT_REQUESTER_ACCESSORS = Set.of(
            CMS_USER_TOOL + "#getLoggedInUser",
            WebAPI.class.getName() + "#getLoggedUser");

    private static List<Class<?>> toolClasses;
    private static Map<String, String> classNameByKey;

    @BeforeClass
    public static void loadToolbox() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document toolbox = builder.parse(new File(TOOLBOX_PATH));

        final NodeList toolNodes = toolbox.getElementsByTagName("tool");
        toolClasses = new ArrayList<>();
        classNameByKey = new HashMap<>();
        for (int i = 0; i < toolNodes.getLength(); i++) {
            final Element tool = (Element) toolNodes.item(i);
            final String key = tool.getElementsByTagName("key").item(0).getTextContent().trim();
            final String className = tool.getElementsByTagName("class").item(0).getTextContent().trim();
            classNameByKey.put(key, className);
            // load without initializing: some tools wire services in static initializers
            toolClasses.add(Class.forName(className, false, ToolboxExposureTest.class.getClassLoader()));
        }
        assertFalse("No tools found in " + TOOLBOX_PATH, toolClasses.isEmpty());
    }

    /**
     * {@code $cmsuser} is provided by the template tool {@code CMSUserTool}. {@code CMSUsersWebAPI}
     * keeps its Java signatures for plugins and is no longer bound to templates.
     */
    @Test
    public void cmsuser_is_bound_to_the_template_tool() {
        assertEquals(CMS_USER_TOOL, classNameByKey.get("cmsuser"));
        assertFalse("CMSUsersWebAPI must not be bound to templates",
                classNameByKey.containsValue(CMSUsersWebAPI.class.getName()));
    }

    /**
     * No bound tool may expose a service API object (a type named {@code *API}) to templates.
     * Templates must go through the tool's own methods, which act as the current requester.
     */
    @Test
    public void bound_tools_do_not_expose_service_apis() {
        final List<String> offenders = new ArrayList<>();
        for (final Class<?> toolClass : toolClasses) {
            for (final Method method : templateCallableMethods(toolClass)) {
                final Class<?> returnType = method.getReturnType();
                if (returnType.getSimpleName().endsWith("API")
                        && !ViewTool.class.isAssignableFrom(returnType)) {
                    offenders.add(describe(toolClass, method));
                }
            }
        }
        assertTrue("Bound tools must not return service APIs: " + offenders, offenders.isEmpty());
    }

    /**
     * Bound tools may return a {@link User} (directly or inside a collection) only from methods
     * that resolve the current requester.
     */
    @Test
    public void bound_tools_return_users_only_for_the_current_requester() {
        final List<String> offenders = new ArrayList<>();
        for (final Class<?> toolClass : toolClasses) {
            for (final Method method : templateCallableMethods(toolClass)) {
                if (mentionsUser(method.getGenericReturnType()) && !isCurrentRequesterAccessor(toolClass, method)) {
                    offenders.add(describe(toolClass, method));
                }
            }
        }
        assertTrue("Bound tools must not return User objects for arbitrary accounts: " + offenders,
                offenders.isEmpty());
    }

    private static List<Method> templateCallableMethods(final Class<?> toolClass) {
        final List<Method> methods = new ArrayList<>();
        for (final Method method : toolClass.getMethods()) {
            if (method.getDeclaringClass() != Object.class && Modifier.isPublic(method.getModifiers())) {
                methods.add(method);
            }
        }
        return methods;
    }

    private static boolean isCurrentRequesterAccessor(final Class<?> toolClass, final Method method) {
        // ViewTool#getUser(HttpServletRequest) resolves the logged-in, front-end or anonymous user
        if ("getUser".equals(method.getName())
                && Arrays.equals(method.getParameterTypes(), new Class<?>[]{HttpServletRequest.class})) {
            return true;
        }
        return CURRENT_REQUESTER_ACCESSORS.contains(toolClass.getName() + "#" + method.getName())
                || CURRENT_REQUESTER_ACCESSORS.contains(method.getDeclaringClass().getName() + "#" + method.getName());
    }

    private static boolean mentionsUser(final Type type) {
        if (type instanceof Class) {
            final Class<?> clazz = (Class<?>) type;
            return User.class.isAssignableFrom(clazz)
                    || (clazz.isArray() && User.class.isAssignableFrom(clazz.getComponentType()));
        }
        if (type instanceof ParameterizedType) {
            for (final Type argument : ((ParameterizedType) type).getActualTypeArguments()) {
                if (mentionsUser(argument)) {
                    return true;
                }
            }
            return mentionsUser(((ParameterizedType) type).getRawType());
        }
        return false;
    }

    private static String describe(final Class<?> toolClass, final Method method) {
        return toolClass.getSimpleName() + "#" + method.getName()
                + Arrays.toString(Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toArray());
    }
}
