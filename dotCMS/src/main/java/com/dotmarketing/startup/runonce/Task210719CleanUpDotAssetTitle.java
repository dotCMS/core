package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDUtil;
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
                        + "and f.indexed=true and s.structuretype=?  group by s.inode, c.inode, "
                        + "title, f.velocity_var_name, f.sort_order order by f.sort_order asc\n")
                .addParam(BinaryField.class.getCanonicalName())
                .addParam(BaseContentType.DOTASSET.getType())
                .loadResults();

        if (null != results && results.size() > 0){
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
                    new DotConnect().setSQL("update contentlet "
                            + "set title='" + files[0].getName() + "', friendly_name='"
                            + files[0].getName() + "' where inode='" + inode + "'").loadResult();
                }
            }
        }
    }
}
