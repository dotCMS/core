package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang.math.NumberUtils;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.db.DbConnectionFactory.getDBFalse;
import static com.dotmarketing.db.DbConnectionFactory.isMsSql;
import static com.dotmarketing.db.DbConnectionFactory.isPostgres;

/**
 * <p>
 *     <b>NOTE: This Upgrade Task does the same work done by {@link Task220825MakeSomeSystemFieldsRemovable}. However,
 *     this one uses the Velocity Variable Name of the affected Content Types instead of their IDs to find and update
 *     them.</b>
 * </p>
 * There are several System fields that should be removable. Even though they should be present in the Base Content Type
 * automatically -- and some of them have already been taken care of -- they should be removable if users choose to. This
 * Upgrade Task changes the status of such fields so that they can be deleted via the UI. The affected base types are
 * the following:
 * <ul>
 *     <li>
 *         Page Asset Type:
 *         <ul>
 *             <li>Friendly Name.</li>
 *             <li>Show on Menu.</li>
 *             <li>Sort Order.</li>
 *             <li>Cache TTL.</li>
 *             <li>Redirect URL.</li>
 *             <li>HTTPS Required.</li>
 *             <li>SEO Description.</li>
 *             <li>SEO Keywords.</li>
 *             <li>Page Metadata.</li>
 *         </ul>
 *     </li>
 *     <li>
 *         File Asset Type:
 *         <ul>
 *             <li>Title.</li>
 *             <li>Show on Menu.</li>
 *             <li>Sort Order.</li>
 *             <li>Description.</li>
 *         </ul>
 *     </li>
 *     <li>
 *         Persona Type:
 *         <ul>
 *             <li>Description.</li>
 *         </ul>
 *     </li>
 *     <li>
 *         Host Type:
 *         <ul>
 *             <li>Host Thumbnail.</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Jose Castro
 * @since Jan 10th, 2023
 */
public class Task230110MakeSomeSystemFieldsRemovableByBaseType extends AbstractJDBCStartupTask {

    private static final String FIND_CONTENT_TYPE_QUERY = "SELECT inode, name FROM structure WHERE %s";
    private static final String UPDATE_CONTENT_TYPE_FIELDS_QUERY = "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = ? AND structure_inode = ?";

    private static final Map<Object, List<String>> BASE_TYPES_AND_FIELDS = Map.of(
            BaseContentType.HTMLPAGE.getType(), List.of("friendlyName", "showOnMenu", "sortOrder", "cachettl",
                    "redirecturl", "httpsreq", "seodescription", "seokeywords", "pagemetadata"),
            BaseContentType.FILEASSET.getType(), List.of("title", "showOnMenu", "sortOrder", "description"),
            BaseContentType.PERSONA.getType(), List.of("description"),
            "Host", List.of("hostThumbnail"));

    @Override
    public boolean forceRun() {
        return isPostgres() || isMsSql();
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {
        if (isPostgres() || isMsSql()) {
            for (final Map.Entry<Object, List<String>> entry : BASE_TYPES_AND_FIELDS.entrySet()) {
                final Object type = entry.getKey();
                final List<Map<String, Object>> contentTypeList = this.getContentTypes(type);
                for (final Map<String, Object> contentTypeData : contentTypeList) {
                    final String typeId = contentTypeData.get("inode").toString();
                    final String name = contentTypeData.get("name").toString();
                    final List<String> fieldVarNameList = entry.getValue();
                    fieldVarNameList.forEach(fieldVarName -> {

                        try {
                            new DotConnect().setSQL(UPDATE_CONTENT_TYPE_FIELDS_QUERY).addParam(fieldVarName).addParam(typeId).loadResult();
                            Logger.info(this, String.format("Removable field '%s' in '%s' Type has been updated " +
                                                                    "successfully!", fieldVarName, name));
                        } catch (final DotDataException e) {
                            Logger.error(this, String.format("An error occurred when updating field '%s' for type " +
                                                                     "'%s': %s", fieldVarName, name,
                                    e.getMessage()), e);
                        }

                    });
                }

            }
        }
    }

    /**
     * Returns the list of Content Types that match either a specific Base Type, or a Velocity Variable Name.
     *
     * @param type The Base Type -- see {@link BaseContentType} -- or a specific Velocity Variable Name.
     *
     * @return The list with one or more Content Types that match the specified search criterion.
     */
    private List<Map<String, Object>> getContentTypes(final Object type) throws DotDataException {
        Object param;
        String sqlQuery;
        if (NumberUtils.isNumber(type.toString())) {
            sqlQuery = String.format(FIND_CONTENT_TYPE_QUERY, "structuretype = ?");
            param = Integer.valueOf(type.toString());
        } else {
            sqlQuery = String.format(FIND_CONTENT_TYPE_QUERY, "velocity_var_name = ?");
            param = type;
        }
        return new DotConnect().setSQL(sqlQuery).addParam(param).loadObjectResults();
    }

}
