package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.util.List;
import java.util.Map;

/**
 * This upgrade task for make binary fields of the File Asset content type, will make the fields listed and indexed
 *
 * @author jsanca
 */
public class Task05220MakeFileAssetContentTypeBinaryFieldIndexedListed implements StartupTask {

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        // look for the file asset content types
        final List<Map<String, Object>> fileAssetMaps = new DotConnect().setSQL(
                "select inode from structure where structuretype = 4").loadObjectResults();

        if (null != fileAssetMaps) {

            for (final Map<String, Object> fileAssetRow : fileAssetMaps) {

                // update the binary fields of the file assets, in order to be indexed, listed and searchable.
                final String contentTypeInode = (String) fileAssetRow.get("inode");
                new DotConnect().setSQL(
                        "update field set indexed = ?, listed=?, searchable=? where structure_inode = ? and field_type = 'com.dotcms.contenttype.model.field.BinaryField' ")
                        .addParam(true).addParam(true).addParam(true).addParam(contentTypeInode).loadResult();
            }
        }
    }

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

}
