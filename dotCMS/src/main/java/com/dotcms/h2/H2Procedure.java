package com.dotcms.h2;

import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_PARENT_PATH;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.h2.tools.SimpleResultSet;

public class H2Procedure {

    public static String dotFolderPath(String parentPath, String assetName) throws SQLException {
        if(parentPath.equals(SYSTEM_FOLDER_PARENT_PATH)) {
            return "/";
        }
        else {
            return parentPath + assetName + "/";
        }
    }
}
