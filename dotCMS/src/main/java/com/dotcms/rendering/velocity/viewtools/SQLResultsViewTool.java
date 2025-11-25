package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The SQL ViewTool ({@code $sql}) allows any dotCMS user with proper permissions to run SQL statements from frontend
 * pages. Running the SQL tool allows you to execute a SQL SELECT statement on any datasource defined in your
 * context.xml file (including, but not limited to, the configured dotCMS database).
 * <p>
 * The SQL Viewtool will only work when used on pages (in either live/preview/edit modes or front-end display). Since
 * Velocity code is not pulled or executed on the content search manager or other portlets, calls to the SQL Viewtool on
 * custom fields, for example, will not return any results on the the dotCMS backend.
 * <p>
 * <b>Warning: SQL queries through the SQL Tool access the database directly (without any caching or other performance
 * enhancements), and should be avoided for performance reasons.</b>
 *
 * @author root
 * @since Mar 12th, 2015
 */
public class SQLResultsViewTool implements ViewTool {

    private static final UserAPI userAPI = APILocator.getUserAPI();
    private static final RoleAPI roleAPI = APILocator.getRoleAPI();
    Context ctx;
    ArrayList<HashMap<String, String>> errorResults;
    private InternalContextAdapterImpl ica;

    private static final boolean RESPECT_FRONT_END_ROLES = Boolean.TRUE;
    private static final String HAS_DOT_CONNECT_SQL_ERROR = "hasDotConnectSQLError";
    private static final String DOT_CONNECT_SQL_ERROR = "dotConnectSQLError";
    private static final String DEFAULT_DATASOURCE = "default";

    /**
     * Default ViewTool constructor.
     *
     * @param obj The ViewTool's Context object.
     */
    public void init(final Object obj) {
        final ViewContext context = (ViewContext) obj;
        this.ctx = context.getVelocityContext();
        this.errorResults = new ArrayList<>();
    }

    /**
     * Executes the specified SQL query on a given Data Source. Such a Data Source allows you to specify what database
     * the query will run against. The default Data Set is the database containing all the dotCMS data, referenced by
     * the name {@code "default"}. Results can be paginated as well.
     *
     * @param dataSource The database that the SQL query will run against.
     * @param sql        The SQL query.
     * @param startRow   The initial paginated record included in the result set.
     * @param maxRow     The final paginated record included in the result set.
     *
     * @return The results returned by the SQL query.
     */
    public ArrayList<HashMap<String, String>> getSQLResults(final String dataSource, final String sql, final int
            startRow, final int maxRow) {
        return getResults(dataSource, sql, null, startRow, maxRow);
    }

    /**
     * Executes the specified SQL query on a given Data Source. Such a Data Source allows you to specify what database
     * the query will run against. The default Data Set is the database containing all the dotCMS data, referenced by
     * the name {@code "default"}. For more complex queries and/or proper parameter escaping and sanitation, parameters
     * can be passed down through a list, which will replace the {@code "?"} signs in the SQL query. Results can be
     * paginated as well.
     *
     * @param dataSource    The database that the SQL query will run against.
     * @param sql           The SQL query.
     * @param parameterList The list of parameters that will be included in the query.
     * @param startRow      The initial paginated record included in the result set.
     * @param maxRow        The final paginated record included in the result set.
     *
     * @return The results returned by the SQL query.
     */
    public ArrayList<HashMap<String, String>> getParameterizedSQLResults(final String dataSource, final String sql,
                                                                         final ArrayList<Object> parameterList, int
                                                                                 startRow, int maxRow) {
        return getResults(dataSource, sql, parameterList, startRow, maxRow);
    }

    /**
     * Executes the specified SQL query, based on the information specified by the user/developer.
     *
     * @param dataSource    The database that the SQL query will run against.
     * @param sql           The SQL query.
     * @param parameterList The list of parameters that will be included in the query.
     * @param startRow      The initial paginated record included in the result set.
     * @param maxRow        The final paginated record included in the result set.
     *
     * @return The results returned by the SQL query.
     */
    @CloseDBIfOpened
    private ArrayList<HashMap<String, String>> getResults(final String dataSource, final String sql, final
    ArrayList<Object> parameterList, int startRow, int maxRow) {
        if (dataSource.equals(DEFAULT_DATASOURCE) && !Config.getBooleanProperty
                ("ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB", false)) {
            return reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "SQLResultsViewTool failed to execute " +
                    "query on default connection pool because ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false" +
                    ".")), "ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false.");
        }
        if (!UtilMethods.isSet(sql)) {
            // SQL Query is not Set. So, return an empty list
            return new ArrayList<>();
        }
        if (!UtilMethods.isSet(dataSource)) {
            return reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "Failed to call the SQLResultsViewTool. " +
                    "Invalid Data Source.")));
        }
        if (startRow < 0) {
            startRow = 0;
        }
        if (maxRow < 0) {
            maxRow = 0;
        }
        try {
            final Contentlet contentlet = getDbAccessorContentlet();
            if (null == contentlet || !UtilMethods.isSet(contentlet.getIdentifier())) {
                return reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "Failed to call the SQLResultsViewTool" +
                        ". User failed to execute SQL queries.")));
            }
            if (!isSQLValid(sql, contentlet)) {
                return this.errorResults;
            }
            final DotConnect dc = new DotConnect().setSQL(sql).setStartRow(startRow).setMaxRows(maxRow);
            if (UtilMethods.isSet(parameterList)) {
                final int totalParams = StringUtils.countMatches(sql, "?");
                if (totalParams != parameterList.size()) {
                    // The amount of params is different. Check them
                    return reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "The number of params in SQL query" +
                            " is different from the params list size.")));
                }
                for (final Object parameter : parameterList) {
                    if (isSQLValid(parameter.toString(), contentlet)) {
                        dc.addParam(parameter);
                    } else {
                        return this.errorResults;
                    }
                }
            }
            if (dataSource.equals(DEFAULT_DATASOURCE)) {
                return dc.loadResults();
            }
            return dc.getResults(dataSource);
        } catch (final Exception e) {
            return reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "An error occurred when executing the SQL " +
                    "query: " + e.getMessage())));
        }
    }

    /**
     * Verifies that the piece of content (usually a Widget) that is calling this {@code sql} ViewTool was modified by a
     * {@link User} which has the expected {@code Scripting Developer} Role. If it does, then return such a piece of
     * content.
     *
     * @return If the User who created/modified the content that calls this ViewTool has the expected Role, returns such
     * a Contentlet. Otherwise, or if an error occurs, returns {@code null}.
     */
    protected Contentlet getDbAccessorContentlet() {
        this.ica = new InternalContextAdapterImpl(this.ctx);
        final String fieldResourceName = this.ica.getCurrentTemplateName();
        try {
            final Pattern pattern = Pattern.compile("[^/LIVE][^/WORKING][^/EDIT_MODE][^/PREVIEW_MODE][^/ADMIN_MODE](.*?)(?=\\/)");
            final Matcher matcher = pattern.matcher(fieldResourceName);
            if (matcher.find()) {
                final String contentletInode = matcher.group(0);
                final Contentlet contentlet = APILocator.getContentletAPI().find(contentletInode, userAPI
                        .getSystemUser(), RESPECT_FRONT_END_ROLES);
                if (null != contentlet && UtilMethods.isSet(contentlet.getIdentifier())) {
                    final User modUser = userAPI.loadUserById(contentlet.getModUser(), APILocator.getUserAPI()
                            .getSystemUser(), RESPECT_FRONT_END_ROLES);
                    final boolean hasRequiredRole = roleAPI.doesUserHaveRole(modUser, roleAPI.loadRoleByKey
                            ("Scripting Developer"));
                    return hasRequiredRole ? contentlet : null;
                } else {
                    Logger.warn(this.getClass(), String.format("Contentlet with Inode '%s' was not found!",
                            contentletInode));
                }
            }
        } catch (final Exception e) {
            Logger.warn(this.getClass(), "An error occurred when calling the Scripting Tool: " + e);
        }
        return null;
    }

    /**
     * Analyzes the words contained in the SQL query specified by the user/developer, and reports back when an invalid
     * keyword or forbidden table is referenced.
     *
     * @param sql        The SQL query.
     * @param contentlet The Contentlet containing the SQL code.
     *
     * @return If the SQL query contains no dangerous code, returns {@code true}. Otherwise, returns {@code false}.
     */
    protected boolean isSQLValid(final String sql, final Contentlet contentlet) {
        final String lowerCasedSql = sql.toLowerCase();
        if (lowerCasedSql.contains("user_")) {
            reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "SQLResultsViewTool access to user_ table is " +
                    "forbidden.")), "Check content with id: " + contentlet.getIdentifier());
            return Boolean.FALSE;
        }
        if (lowerCasedSql.contains("cms_role")) {
            reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "SQLResultsViewTool access to cms_role table is " +
                    "forbidden.")), "Check content with id: " + contentlet.getIdentifier());
            return Boolean.FALSE;
        }
        if (SQLUtil.containsEvilSqlWords(lowerCasedSql)) {
            reportError(new HashMap<>(Map.of(DOT_CONNECT_SQL_ERROR, "SQLResultsViewTool is trying to execute a " +
                    "forbidden query.")), "Check content with id: " + contentlet.getIdentifier());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Utility method used to report errors.
     *
     * @param errors A simple map containing errors.
     *
     * @return The list of errors that will be reported back to the user.
     */
    protected ArrayList<HashMap<String, String>> reportError(final Map<String, String> errors) {
        return reportError(errors, null);
    }

    /**
     * Utility method used to report errors.
     *
     * @param errors   A simple map containing errors.
     * @param debugMsg An error for the DEBUG logging, if necessary.
     *
     * @return The list of errors that will be reported back to the user.
     */
    protected ArrayList<HashMap<String, String>> reportError(final Map<String, String> errors, final String debugMsg) {
        errors.put(HAS_DOT_CONNECT_SQL_ERROR, Boolean.TRUE.toString());
        Logger.error(this, errors.get(DOT_CONNECT_SQL_ERROR));
        if (UtilMethods.isSet(debugMsg)) {
            Logger.debug(this, debugMsg);
        }
        this.errorResults.add(HashMap.class.cast(errors));
        return this.errorResults;
    }

}
