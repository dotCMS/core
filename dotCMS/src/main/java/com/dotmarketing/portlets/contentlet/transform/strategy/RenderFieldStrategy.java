package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.api.web.HttpServletRequestImpersonator;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class intended to collect the {@link Field}s that are velocity renderable and present them in their
 * rendered form in the content map
 */
public class RenderFieldStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Regular constructor takes a toolbox
     * @param toolBox
     */
    public RenderFieldStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Main Transform function
     * @param contentlet
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    protected Map<String, Object> transform(final Contentlet contentlet,
    final Map<String, Object> map, final Set<TransformOptions> options, final User user) {

        final ContentType contentType = contentlet.getContentType();
        final List<Field> rederableFields = contentType.fields().stream()
                .filter(RenderFieldStrategy::isFieldRenderable).collect(Collectors.toList());

        final HttpServletRequestImpersonator impersonator = HttpServletRequestImpersonator.newInstance();

        if (UtilMethods.isSet(rederableFields)) {
            rederableFields.forEach(field ->
                    map.put(field.variable(),
                            renderFieldValue(impersonator.request(),
                                    HttpServletResponseThreadLocal.INSTANCE.getResponse(),
                                    RenderFieldStrategy.getFieldValue(contentlet, field), contentlet,
                                    field.variable())));
        }

        return map;
    }

    private static String getFieldValue(final Contentlet contentlet, final Field field) {
        String fieldValue = (String) contentlet.get(field.variable());

        if(!UtilMethods.isSet(fieldValue)) {
            // null value - maybe a constant field
            if(field instanceof ConstantField) {
                fieldValue = field.values();
            } else {
                fieldValue = field.defaultValue();
            }
        }

        return fieldValue;
    }


    public static boolean isFieldRenderable(final Field field) {
        return field instanceof WysiwygField || field instanceof TextField ||
                field instanceof TextAreaField || field instanceof CustomField
                || field instanceof ConstantField || field instanceof StoryBlockField;
    }

    /**
     * Evaluates the fieldValue as velocity and then parses the result as JSON, only if
     * a $dotJSON.put was found. Otherwise returns empty map
     */
    public static Object parseAsJSON(HttpServletRequest request,
            HttpServletResponse response, final String fieldValue,
            final Contentlet contentlet, final String fieldVar) {
        if(!UtilMethods.isSet(fieldValue)) return null;

        request = request == null ? HttpServletRequestThreadLocal.INSTANCE.getRequest()
                : request;

        response = response == null ? HttpServletResponseThreadLocal.INSTANCE.getResponse()
                : response;

        if(request==null || response==null) return null;

        final org.apache.velocity.context.Context context = (request!=null && response!=null)
                ? VelocityUtil.getInstance().getContext(request, response)
                : VelocityUtil.getBasicContext();

        final Language lang = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final PageMode pageMode = PageMode.get(request);

        context.put("content", contentlet);
        context.put("contentlet", contentlet);
        context.put("dotJSON", new DotJSON());

        final VelocityResourceKey key = new VelocityResourceKey(contentlet, pageMode, lang.getId());
        try {
            VelocityUtil.mergeTemplate(key.path, context);
        } catch (Exception e) {
            Logger.warn(ContainerRenderedBuilder.class, e.getMessage());
        }

        final DotJSON dotJSON = (DotJSON) context.get("dotJSON");

        return UtilMethods.isSet(dotJSON) ? dotJSON.getMap() : Collections.emptyMap();
    }

    public static Object renderFieldValue(final HttpServletRequest request,
            final HttpServletResponse response, final String fieldValue,
            final Contentlet contentlet, final String fieldVar) {
        if(!UtilMethods.isSet(fieldValue)) return null;

        final org.apache.velocity.context.Context context = (request!=null && response!=null)
                ? VelocityUtil.getInstance().getContext(request, response)
                : VelocityUtil.getBasicContext();

        context.put("content", contentlet);
        context.put("contentlet", contentlet);
        context.put("dotJSON", new DotJSON());

        final StringWriter evalResult = new StringWriter();

        Try.runRunnable(()-> com.dotmarketing.util.VelocityUtil
                .getEngine()
                .evaluate(context, evalResult, "", fieldValue)).onFailure((error)->
                Logger.warnAndDebug(RenderFieldStrategy.class, "Unable to render velocity in field: "
                        + fieldVar, error)
        );

        final DotJSON dotJSON = (DotJSON) context.get("dotJSON");

        return UtilMethods.isSet(evalResult.toString())
                ? dotJSON.size() > 0
                    ? dotJSON.getMap() : evalResult.toString()
                : fieldValue;
    }
}
