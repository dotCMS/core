package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

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
public class Task230110MakeSomeSystemFieldsRemovableByVarName extends AbstractJDBCStartupTask {

    private static final String FIND_CONTENT_TYPE_QUERY = "SELECT inode FROM structure WHERE velocity_var_name = ?";
    private static final String UPDATE_CONTENT_TYPE_FIELDS_QUERY = "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = ? AND structure_inode = ?";

    private static final Map<String, List<String>> CONTENT_TYPES_AND_FIELDS = Map.of(
            "htmlpageasset", List.of("friendlyName", "showOnMenu", "sortOrder", "cachettl", "redirecturl", "httpsreq", "seodescription", "seokeywords", "pagemetadata"),
            "FileAsset", List.of("title", "showOnMenu", "sortOrder", "description"),
            "persona", List.of("description"),
            "Host", List.of("hostThumbnail"));

    @Override
    public boolean forceRun() {
        return isPostgres() || isMsSql();
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        if (isPostgres() || isMsSql()) {
            for (final String typeVarName : CONTENT_TYPES_AND_FIELDS.keySet()) {
                final List<Map<String, Object>> contentTypeData =
                        new DotConnect().setSQL(FIND_CONTENT_TYPE_QUERY).addParam(typeVarName).loadObjectResults();
                if (!contentTypeData.isEmpty()) {
                    final String typeId = contentTypeData.get(0).get("inode").toString();
                    final List<String> fieldVarNameList = CONTENT_TYPES_AND_FIELDS.get(typeVarName);
                    fieldVarNameList.forEach(fieldVarName -> {

                        try {
                            new DotConnect().setSQL(UPDATE_CONTENT_TYPE_FIELDS_QUERY).addParam(fieldVarName).addParam(typeId).loadResult();
                            Logger.info(this, String.format("Removable field '%s' in '%s' Type has been updated " +
                                                                    "successfully!", fieldVarName, typeVarName));
                        } catch (final DotDataException e) {
                            Logger.error(this, String.format("An error occurred when updating field '%s' for type " +
                                                                     "'%s': %s", fieldVarName, typeVarName,
                                    e.getMessage()), e);
                        }

                    });
                }
            }
        }
    }

}
