package com.dotcms.rendering.velocity;

import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import io.vavr.Lazy;
import java.io.StringWriter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.context.Context;

/**
 * Renders a Widget contentlet outside of a page: the REST widget endpoint and the
 * {@code render=true} option of the content REST endpoints use it to turn a widget into
 * the text its Velocity code produces.
 * <p>
 * The Velocity context exposes every contentlet property under its own name and again
 * under the {@code widget} prefix, so the widget code can use either {@code $title} or
 * {@code $widgettitle}. The web context (request, session, view tools) is added on top
 * and overrides any property with the same name.
 */
public class WidgetRenderer {

    private static final Lazy<WidgetRenderer> INSTANCE = Lazy.of(WidgetRenderer::new);

    /**
     * Returns the shared renderer.
     *
     * @return the renderer used by the REST endpoints
     */
    public static WidgetRenderer getInstance() {
        return INSTANCE.get();
    }

    /**
     * Renders the given widget. Contentlets whose type is not a Widget render as an empty string.
     *
     * @param request  the current request, used to build the web context
     * @param response the current response, used to build the web context
     * @param widget   the widget contentlet to render
     * @return the rendered text
     */
    public String render(final HttpServletRequest request, final HttpServletResponse response,
            final Contentlet widget) {

        final Structure contStructure = widget.getStructure();
        if (contStructure.getStructureType() != Structure.STRUCTURE_TYPE_WIDGET) {
            return "";
        }

        final StringWriter firstEval = new StringWriter();
        final StringWriter secondEval = new StringWriter();
        final StringBuilder widgetExecuteCode = new StringBuilder();

        Context context = VelocityUtil.getBasicContext();
        for (final String key : widget.getMap().keySet()) {
            context.put(key, widget.getMap().get(key).toString());
            context.put("widget" + key, widget.getMap().get(key).toString());
        }

        final List<Field> fields = FieldsCache.getFieldsByStructureInode(contStructure.getInode());
        for (final Field field : fields) {
            if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                final String host = widget.getHost();
                final String folder = widget.getFolder();
                final String fieldValue =
                        UtilMethods.isSet(folder) && !folder.equals(FolderAPI.SYSTEM_FOLDER) ? folder : host;
                context.put(field.getVelocityVarName(), fieldValue);
                context.put("widget" + field.getVelocityVarName(), fieldValue);
            }
        }

        // web context should overwrite everything
        context = VelocityUtil.getWebContext(context, request, response);

        final Field preExecuteField = contStructure.getFieldVar("widgetPreexecute");
        if (preExecuteField != null) {
            final String fval = preExecuteField.getValues() != null ? preExecuteField.getValues().trim() : "";
            widgetExecuteCode.append(fval).append("\n");
        }

        final Field codeField = contStructure.getFieldVar("widgetCode");
        final String fval = codeField.getValues() != null ? codeField.getValues().trim() : "";
        widgetExecuteCode.append(fval).append("\n");

        VelocityUtil.getEngine().evaluate(context, firstEval, "", widgetExecuteCode.toString());
        VelocityUtil.getEngine().evaluate(context, secondEval, "", firstEval.toString());
        return secondEval.toString();
    }

}
