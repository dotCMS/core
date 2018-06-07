package com.dotcms.rendering.velocity.directive;

import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.visitor.domain.Visitor;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;

import java.io.Writer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;

public class ParseContainer extends DotDirective {

    public static final String DEFAULT_UUID_VALUE = MultiTree.LEGACY_RELATION_TYPE;
    private static final long serialVersionUID = 1L;

    @Override
    public final String getName() {
        return "parseContainer";
    }

    public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException {
        super.init(rs, context, node);

    }
    final public static String PERSONALIZATON_MARKER="_persona_";
    @Override
	String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params,
			final String[] arguments) {
        
	    final String    id                = arguments[0];
        final String    uuid               = (arguments.length > 1 && UtilMethods.isSet(arguments[1])) ? arguments[1] :  DEFAULT_UUID_VALUE;
        final Visitor   visitor           = APILocator.getVisitorAPI().getVisitor((HttpServletRequest) context.get("request")).orElse(new Visitor());
        final String    keyTag            = (visitor.getPersona()!=null && visitor.getPersona().getKeyTag()!=null) ? PERSONALIZATON_MARKER + visitor.getPersona().getKeyTag() : "";
        final String    personalizedUUID  = uuid  + keyTag;

        // check if there are personalized content objects for the user's persona, otherwise, set the default
        final Object    defaultCons       = context.get("contentletList" + id + uuid);
        if(context.get("contentletList" + id + personalizedUUID) ==null ) {
            context.put("contentletList" + id + personalizedUUID, defaultCons);
        }





    return"/"+params.mode.name()+"/"+id+"/"+personalizedUUID+"."+VelocityType.CONTAINER.fileExtension;

}}
