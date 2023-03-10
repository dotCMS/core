package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple2;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.db.DbConnectionFactory.isMsSql;
import static com.dotmarketing.db.DbConnectionFactory.isPostgres;
import static com.dotmarketing.db.DbConnectionFactory.getDBFalse;

/**
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
 * @since Aug 25th, 2022
 */
public class Task220825MakeSomeSystemFieldsRemovable extends AbstractJDBCStartupTask {

    private static final String PAGE_ASSET_BASE_TYPE_ID = "c541abb1-69b3-4bc5-8430-5e09e5239cc8";
    private static final String FILE_ASSET_BASE_TYPE_ID = "33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d";
    private static final String PERSONA_BASE_TYPE_ID = "c938b15f-bcb6-49ef-8651-14d455a97045";
    private static final String HOST_TYPE_ID = "855a2d72-f2f3-4169-8b04-ac5157c4380c";
    private static final String FIND_CONTENT_TYPE_QUERY = "SELECT inode FROM structure WHERE inode = ?";
    private static final String UPDATE_PAGE_ASSET_FIELDS_QUERY = "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'friendlyName' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'showOnMenu' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'sortOrder' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'cachettl' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'redirecturl' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'httpsreq' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'seodescription' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'seokeywords' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'pagemetadata' AND structure_inode = '" + PAGE_ASSET_BASE_TYPE_ID + "';";
    private static final String UPDATE_FILE_ASSET_FIELDS_QUERY = "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'title' AND structure_inode = '" + FILE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'showOnMenu' AND structure_inode = '" + FILE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'sortOrder' AND structure_inode = '" + FILE_ASSET_BASE_TYPE_ID + "';" +
                                                                         "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'description' AND structure_inode = '" + FILE_ASSET_BASE_TYPE_ID + "';";
    private static final String UPDATE_PERSONA_FIELDS_QUERY = "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'description' AND structure_inode = '" + PERSONA_BASE_TYPE_ID + "';";
    private static final String UPDATE_HOST_FIELDS_QUERY = "UPDATE field SET fixed = " + getDBFalse() + " WHERE velocity_var_name = 'hostThumbnail' AND structure_inode = '" + HOST_TYPE_ID + "';";

    private static final Map<String, Tuple2<String, String>> newBaseTypes = Map.of(
            "Page Asset", new Tuple2<>(PAGE_ASSET_BASE_TYPE_ID, UPDATE_PAGE_ASSET_FIELDS_QUERY),
            "File Asset", new Tuple2<>(FILE_ASSET_BASE_TYPE_ID, UPDATE_FILE_ASSET_FIELDS_QUERY),
            "Persona", new Tuple2<>(PERSONA_BASE_TYPE_ID, UPDATE_PERSONA_FIELDS_QUERY),
            "Host", new Tuple2<>(HOST_TYPE_ID, UPDATE_HOST_FIELDS_QUERY));

    @Override
    public boolean forceRun() {
        boolean forceRun = Boolean.TRUE;
        try {
            for (final String baseType : newBaseTypes.keySet()) {
                final List<Map<String, Object>> contentTypeData =
                        new DotConnect().setSQL(FIND_CONTENT_TYPE_QUERY).addParam(newBaseTypes.get(baseType)._1()).loadObjectResults();
                if (contentTypeData.isEmpty()) {
                    Logger.error(this, String.format("%s Base Type does not have the expected Inode: '%s'. Therefore," +
                                                             " removable fields must be manually updated.", baseType,
                            newBaseTypes.get(baseType)._1()));
                    forceRun = Boolean.FALSE;
                    break;
                }
            }
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when running this Upgrade Task: %s", e.getMessage()),
                    e);
            forceRun = Boolean.FALSE;
        }
        return forceRun;
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {
        if (isPostgres() || isMsSql()) {
            for (final String baseType : newBaseTypes.keySet()) {
                new DotConnect().setSQL(newBaseTypes.get(baseType)._2()).loadObjectResults();
                Logger.info(this, String.format("Removable fields in %s Base Type have been updated successfully!",
                        baseType));
            }
        }
    }

}
