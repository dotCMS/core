package com.dotcms.personalization;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.personas.model.Persona;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;

/**
 * Util for personalization stuff
 * @see Visitor
 * @see Persona
 */
public class PersonalizationUtil {

    /**
     * Gets the personalization for a container
     * If request is null will return  {@link MultiTree#DOT_PERSONALIZATION_DEFAULT}
     * @param request {@link HttpServletRequest}
     * @return String
     */
    public static String getContainerPersonalization (final HttpServletRequest request) {

        if (null != request) {

            final Visitor visitor = APILocator.getVisitorAPI().getVisitor(request).orElse(null);
            return null != visitor && visitor.getPersona() != null && visitor.getPersona().getKeyTag() != null ?
                    Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + visitor.getPersona().getKeyTag() : MultiTree.DOT_PERSONALIZATION_DEFAULT;
        }

        return MultiTree.DOT_PERSONALIZATION_DEFAULT;
    }

    /**
     * Gets the personalization for a container
     * This method will tries to figure out the personalization based on the current thread local context, if not will return just {@link MultiTree#DOT_PERSONALIZATION_DEFAULT}
     * @return String
     */
    public static String getContainerPersonalization () {

        return getContainerPersonalization(HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }
}
