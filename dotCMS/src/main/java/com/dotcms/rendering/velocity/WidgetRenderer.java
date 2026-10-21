package com.dotcms.rendering.velocity;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
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
 * <p>
 * The widget code is evaluated once, and that is the result. Evaluating the produced text a
 * second time, which makes Velocity written inside the widget field values run, happens only
 * for widgets last modified by a user holding the {@link Role#SCRIPTING_DEVELOPER} role, the
 * rule the {@code $sql} and {@code $secrets} view tools apply to the content that calls them.
 * The system user never qualifies, so content checked in by import, push publishing or startup
 * renders its field values as text until a Scripting Developer saves it.
 * {@link FeatureFlagName#FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER}, on by default, is the
 * switch that turns the second evaluation off for everyone.
 * <p>
 * This class only covers rendering through REST. Pages render widgets through the Velocity
 * resources that {@code ContentletLoader} writes, which have their own handling of field values.
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
     * Text sent to API consumers when a widget's field values were left as text because the
     * second evaluation did not run. It is the same text on every endpoint, so consumers can match it.
     */
    public static final String FIELD_SCRIPTING_OFF_WARNING =
            "Widget field scripting is off; widget field values were rendered as text.";

    /**
     * What a widget rendered to.
     *
     * @param text                  the rendered text
     * @param fieldScriptingSkipped {@code true} when the second evaluation did not run and some
     *                              field value holds Velocity, so it came out as text
     */
    public record Rendering(String text, boolean fieldScriptingSkipped) {
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
        return renderWithDetails(request, response, widget).text();
    }

    /**
     * Renders the given widget and tells whether its field values were left as text. Contentlets
     * whose type is not a Widget render as an empty string.
     *
     * @param request  the current request, used to build the web context
     * @param response the current response, used to build the web context
     * @param widget   the widget contentlet to render
     * @return the rendered text and the field-scripting indicator
     */
    public Rendering renderWithDetails(final HttpServletRequest request, final HttpServletResponse response,
            final Contentlet widget) {

        final Structure contStructure = widget.getStructure();
        if (contStructure.getStructureType() != Structure.STRUCTURE_TYPE_WIDGET) {
            return new Rendering("", false);
        }

        final StringWriter firstEval = new StringWriter();
        final StringBuilder widgetExecuteCode = new StringBuilder();

        Context context = VelocityUtil.getBasicContext();
        for (final String key : widget.getMap().keySet()) {
            // The contentlet map drops null values on put, so this is only a guard against a null getter
            final Object rawValue = widget.getMap().get(key);
            final String value = rawValue == null ? "" : rawValue.toString();
            context.put(key, value);
            context.put("widget" + key, value);
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
        if (codeField != null) {
            final String fval = codeField.getValues() != null ? codeField.getValues().trim() : "";
            widgetExecuteCode.append(fval).append("\n");
        }

        VelocityUtil.getEngine().evaluate(context, firstEval, "", widgetExecuteCode.toString());
        final String firstPass = firstEval.toString();
        if (!rendersFieldVelocity(widget)) {
            return new Rendering(firstPass, fieldValuesHoldVelocity(widget));
        }

        final StringWriter secondEval = new StringWriter();
        VelocityUtil.getEngine().evaluate(context, secondEval, "", firstPass);
        return new Rendering(secondEval.toString(), false);
    }

    /**
     * Tells whether any field value of the widget looks like Velocity. This is the test a page
     * render applies to decide whether a field value is merged as a template, so it marks exactly
     * the values that a page would have evaluated and this renderer left as text. Text produced
     * by the widget code itself (a price, a colour, an anchor) does not count.
     *
     * @param widget the widget being rendered
     * @return {@code true} when some string property contains {@code $} or {@code #}
     */
    private static boolean fieldValuesHoldVelocity(final Contentlet widget) {
        for (final Object value : widget.getMap().values()) {
            if (value instanceof String text && (text.contains("$") || text.contains("#"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the text produced by the widget code gets evaluated once more as Velocity,
     * which runs any Velocity found in the widget field values.
     *
     * @param widget the widget being rendered
     * @return {@code true} only when the feature flag is on (its default) and the widget's last
     * modifier holds the Scripting Developer role
     */
    boolean rendersFieldVelocity(final Contentlet widget) {
        if (!Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, true)) {
            Logger.debug(this, () -> String.format("Widget %s: field values render as text, %s is off",
                    widget.getIdentifier(), FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER));
            return false;
        }
        return lastModifiedByScriptingDeveloper(widget);
    }

    /**
     * Resolves the user who last modified the widget and checks the Scripting Developer role on it.
     * The system user is not accepted even though {@code RoleAPI} treats it as holding every role:
     * it is the modifier recorded for imported, pushed and startup content, not for a person.
     *
     * @param widget the widget being rendered
     * @return {@code true} when that user exists and holds the role; {@code false} otherwise or on any lookup error
     */
    private boolean lastModifiedByScriptingDeveloper(final Contentlet widget) {
        final String modUserId = widget.getModUser();
        if (!UtilMethods.isSet(modUserId) || UserAPI.SYSTEM_USER_ID.equals(modUserId)) {
            Logger.debug(this, () -> String.format(
                    "Widget %s: field values render as text, last modifier is '%s'",
                    widget.getIdentifier(), modUserId));
            return false;
        }
        try {
            final User modUser = APILocator.getUserAPI().loadUserById(modUserId, APILocator.systemUser(), false);
            final Role scriptingDeveloper = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
            final boolean hasRole = modUser != null && scriptingDeveloper != null
                    && APILocator.getRoleAPI().doesUserHaveRole(modUser, scriptingDeveloper);
            if (!hasRole) {
                Logger.debug(this, () -> String.format(
                        "Widget %s: field values render as text, last modifier %s lacks the %s role",
                        widget.getIdentifier(), modUserId, Role.SCRIPTING_DEVELOPER));
            }
            return hasRole;
        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(), String.format(
                    "Widget %s: field values render as text, could not resolve last modifier %s: %s",
                    widget.getIdentifier(), modUserId, e.getMessage()), e);
            return false;
        }
    }

}
