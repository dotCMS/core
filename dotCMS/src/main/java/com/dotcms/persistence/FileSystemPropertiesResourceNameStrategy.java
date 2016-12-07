package com.dotcms.persistence;

import com.dotcms.util.InputStreamUtils;
import com.dotmarketing.db.DbConnectionFactory;
import com.liferay.util.PropertiesUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will returns the generic and vendor resource name to load the Properties and XML Properties from the classpath.
 *
 * The file name on the file system will follow the pattern
 *
 * com.dotcms.thing.ThingDAOImpl
 *
 * And My SQL DB, It will returns
 *
 * [file://{basepath}/com.dotcms.thing.ThingDAOImpl.properties, file://{basepath}/com.dotcms.thing.ThingDAOImpl.xml, file://{basepath}/com.dotcms.thing.ThingDAOImplMySQL.properties, file://{basepath}/com.dotcms.thing.ThingDAOImplMySQL.xml]
 *
 * Keep in mind with this strategy all files will be in the same folder (basepath} and the name of the file will be the full class name.
 *
 * @author jsanca
 */
public class FileSystemPropertiesResourceNameStrategy implements ResourceNameStrategy {


    private final String basepath;

    public FileSystemPropertiesResourceNameStrategy(final String basepath) {

        this.basepath = basepath;
    }

    @Override
    public List<String> getResourceNames(final Class persistenceClass) {

        final List<String> filesystemResourcesNameList =
                new ArrayList<>();
        final String vendorName = this.getVendorName();

        final String path = InputStreamUtils.PREFIX_FILE + this.basepath +
                File.separator + persistenceClass.getName();

        // adding the generic prop and xml
        filesystemResourcesNameList.add(path + PropertiesUtil.PROP_EXT);
        filesystemResourcesNameList.add(path + PropertiesUtil.XML_EXT);

        // adding the vendor prop and xml
        filesystemResourcesNameList.add(path + vendorName + PropertiesUtil.PROP_EXT);
        filesystemResourcesNameList.add(path + vendorName + PropertiesUtil.XML_EXT);

        return filesystemResourcesNameList;
    } // getResourceNames.

    /**
     * Get the vendor name for the current persistance.
     * @return String
     */
    protected String getVendorName () {

        return DbConnectionFactory.getDBType();
    }
} // E:O:F:ClasspathPropertiesResourceNameStrategy.
