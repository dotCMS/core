package com.dotmarketing.startup.runonce;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.util.List;
import java.util.Map;

public class Task210719CleanUpDotAssetTitle implements StartupTask {
    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("select s.inode structure_inode, c.inode contentlet_inode, title, "
                        + "f.velocity_var_name, f.sort_order  from contentlet c, structure s, "
                        + "field f where c.structure_inode = s.inode and f.structure_inode = s.inode \n"
                        + "and f.field_type = ? and title=c.identifier "
                        + "and f.indexed=? and s.structuretype=?  group by s.inode, c.inode, "
                        + "title, f.velocity_var_name, f.sort_order order by f.sort_order asc\n")
                .addParam(BinaryField.class.getCanonicalName())
                .addParam(DbConnectionFactory.isPostgres()? Boolean.TRUE: DbConnectionFactory.getDBTrue())
                .addParam(BaseContentType.DOTASSET.getType())
                .loadResults();

        if (null != results && results.size() > 0){
            final List<Params> params = list();
            for (final Map<String, Object> dotAsset:results){
                final String inode = dotAsset.get("contentlet_inode").toString();
                final File binaryFileFolder = new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()
                        + java.io.File.separator
                        + inode.charAt(0)
                        + java.io.File.separator
                        + inode.charAt(1)
                        + java.io.File.separator
                        + inode
                        + java.io.File.separator
                        + dotAsset.get("velocity_var_name"));
                final File[] files = binaryFileFolder.listFiles();

                if (null != files && files.length > 0){
                    params.add(new Params.Builder().add(files[0].getName(), files[0].getName(), inode).build());
                }
            }

            if (UtilMethods.isSet(params)){
                new DotConnect().executeBatch("update contentlet "
                        + "set title=?, friendly_name=? where inode=?", params);
            }
        }
    }
}
